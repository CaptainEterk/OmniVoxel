package omnivoxel.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import omnivoxel.common.BlockShape;
import omnivoxel.server.client.ServerClient;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.block.ServerBlockAndPosition;
import omnivoxel.server.client.chunk.ChunkIO;
import omnivoxel.server.client.chunk.ChunkService;
import omnivoxel.server.client.chunk.ChunkTask;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.games.Game;
import omnivoxel.server.world.ChunkCacheHandler;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.server.world.ServerWorldHandler;
import omnivoxel.util.boundingBox.WorldBoundingBox;
import omnivoxel.util.bytes.ByteUtils;
import omnivoxel.util.game.GameParser;
import omnivoxel.util.game.nodes.ArrayGameNode;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.thread.WorkerThreadPool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int HANDSHAKE_ID = 0;
    private static final int TPS = 20;
    private static final Set<Position3D> positions = new HashSet<>();
    private final Map<String, ServerClient> clients;
    private final WorkerThreadPool<ChunkTask> workerThreadPool;
    private final ServerWorld world;
    private final Map<String, String> blockIDMap;
    private final Map<String, BlockShape> blockShapeCache;
    private final ServerBlockService blockService;
    private final ServerWorldHandler worldHandler;

    // TODO: Cleanup the server
    private boolean done = false;

    public Server(Map<String, ServerClient> clients, long seed, ServerWorld world, Map<String, BlockShape> blockShapeCache, ServerBlockService blockService, Map<String, String> blockIDMap, ServerWorldHandler worldHandler) throws InterruptedException, IOException {
        this.clients = clients;
        this.world = world;
        this.blockShapeCache = blockShapeCache;
        this.blockIDMap = blockIDMap;
        this.blockService = blockService;
        this.worldHandler = worldHandler;

        GameNode gameNode = GameParser.parseNode(Files.readString(Path.of("game/main.json")), Game.checkGameNodeType(GameParser.parseNode(Files.readString(Path.of("game/constants.json")), null), ArrayGameNode.class));

        if (gameNode instanceof ObjectGameNode objectGameNode) {
            Set<WorldBoundingBox> worldBoundingBoxes = ConcurrentHashMap.newKeySet();
            workerThreadPool = new WorkerThreadPool<>(ConstantServerSettings.CHUNK_GENERATOR_THREAD_LIMIT, () ->
                    new ChunkService(
                            new ServerWorldDataService(
                                    blockService,
                                    blockShapeCache,
                                    objectGameNode.object().get("world_generator"),
                                    seed
                            ),
                            blockService,
                            world,
                            worldBoundingBoxes
                    )::serve,
                    true
            );
        } else {
            throw new IllegalArgumentException("gameNode must be an ObjectGameNode, not " + gameNode.getClass());
        }
    }

    private static void sendBytes(ChannelHandlerContext ctx, PackageID id, byte[]... bytes) {
        ByteBuf buffer = Unpooled.buffer();
        int length = 4;
        for (byte[] bites : bytes) {
            length += bites.length;
        }
        buffer.writeInt(length);
        buffer.writeInt(id.ordinal());
        for (byte[] bites : bytes) {
            buffer.writeBytes(bites);
        }
        ctx.channel().writeAndFlush(buffer);
    }

    private static void sendBlockShape(ChannelHandlerContext ctx, BlockShape blockShape) {
        ByteBuf buffer = Unpooled.buffer();
        byte[] bytes = blockShape.getBytes();
        buffer.writeInt(4 + bytes.length);
        buffer.writeInt(PackageID.REGISTER_BLOCK_SHAPE.ordinal());
        buffer.writeBytes(bytes);
        ctx.channel().writeAndFlush(buffer);
    }

    public void handlePackage(ChannelHandlerContext ctx, PackageID packageID, ByteBuf byteBuf) throws InterruptedException {
        String clientID = ByteUtils.bytesToHex(byteBuf, 4, 32);
        switch (packageID) {
            case CHUNK_REQUEST:
                int count = byteBuf.getInt(36);
                for (int i = 0; i < count; i++) {
                    int x = byteBuf.getInt(i * 3 * Integer.BYTES + 40);
                    int y = byteBuf.getInt(i * 3 * Integer.BYTES + 44);
                    int z = byteBuf.getInt(i * 3 * Integer.BYTES + 48);
                    workerThreadPool.submit(new ChunkTask(clients.get(clientID), x, y, z));
                }
                done = true;
                byteBuf.release();
                break;
            case REGISTER_CLIENT:
                registerClient(ctx, byteBuf);
                byteBuf.release();
                break;
            case CLOSE:
                ServerClient client = clients.get(clientID);
                clients.remove(clientID);
                clients.values().forEach(player -> sendBytes(player.getCTX(), PackageID.CLOSE, client.getPlayerID()));
                ServerLogger.logger.debug("Client Disconnected: " + clientID + " playerID: " + ByteUtils.bytesToHex(client.getPlayerID()));
                byteBuf.release();
                break;
            case PLAYER_UPDATE:
                double[] data = new double[5];
                for (int i = 0; i < 5; i++) {
                    data[i] = byteBuf.getDouble(36 + i * Double.BYTES);
                }
                double x = data[0];
                double y = data[1];
                double z = data[2];
                double pitch = data[3];
                double yaw = data[4];
                ServerClient serverClient = clients.get(clientID);
                serverClient.set(x, y, z, pitch, yaw);

                clients.values().forEach(player -> {
                    if (!Arrays.equals(player.getPlayerID(), serverClient.getPlayerID())) {
                        sendBytes(player.getCTX(), PackageID.ENTITY_UPDATE, serverClient.getBytes());
                    }
                });
                byteBuf.release();
                break;
            case REPLACE_BLOCK:
                int bx = byteBuf.getInt(36);
                int by = byteBuf.getInt(40);
                int bz = byteBuf.getInt(44);
                int length = byteBuf.getInt(48);
                byte[] bytes = new byte[length];
                byteBuf.getBytes(52, bytes);
                StringBuilder blockID = new StringBuilder();
                for (byte b : bytes) {
                    blockID.append((char) b);
                }
                worldHandler.replaceBlock(bx, by, bz, blockService.getBlock(blockID.toString()), clients.get(clientID));

                byteBuf.release();
                break;
            default:
                System.err.println("Unknown package key: " + packageID);
        }
    }

    private void registerClient(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        byte[] versionID = ByteUtils.getBytes(byteBuf, 4, 8);
        if (Arrays.equals(versionID, String.format("%-8s", HANDSHAKE_ID).getBytes())) {
            String clientID = ByteUtils.bytesToHex(byteBuf, 12, 32);
            ServerClient serverClient = new ServerClient(clientID, ctx);
            byte[] encodedServerPlayer = serverClient.getBytes();

            clients.values().forEach(player -> {
                sendBytes(player.getCTX(), PackageID.NEW_ENTITY, encodedServerPlayer);
                sendBytes(ctx, PackageID.NEW_ENTITY, player.getBytes());
            });

            blockShapeCache.forEach((id, blockShape) -> sendBlockShape(serverClient.getCTX(), blockShape));

            blockService.getAllBlocks().forEach((id, serverBlock) -> {
                if (serverClient.registerBlockID(id)) {
                    ChannelHandlerContext ctx1 = serverClient.getCTX();
                    ChunkIO.sendBlock(ctx1, serverBlock);
                }
            });

            clients.put(clientID, serverClient);

            ServerLogger.logger.debug("Client Connected: " + clientID + " playerID: " + ByteUtils.bytesToHex(serverClient.getPlayerID()));
        } else {
            System.err.println("Client has an incompatible version, disconnecting...");
            System.err.println("\tClient: " + Arrays.toString(versionID));
            System.err.println("\tServer: " + Arrays.toString(String.format("%-8s", HANDSHAKE_ID).getBytes()));
            ctx.close();
        }
    }

    public void run() {
        try {
            final long tickIntervalNanos = 1_000_000_000L / TPS;

            int tick = 0;
            while (true) {
                long startNano = System.nanoTime();

                if (tick % 10 == 0) {
                    ChunkCacheHandler.cacheAll();
                }

                for (int i = 0; i < 10; i++) {
                    worldHandler.replaceBlock((int) Math.floor(Math.random() * 16), (int) Math.floor(Math.random() * 8) + 100, (int) Math.floor(Math.random() * 16), ServerBlock.AIR, null);
                }

                world.tick();

                sendQueuedClientPackets();

                long elapsed = System.nanoTime() - startNano;
                long sleepNanos = tickIntervalNanos - elapsed;

                tick++;

                if (sleepNanos > 0) {
                    try {
                        long sleepMillis = sleepNanos / 1_000_000;
                        int sleepSubNanos = (int) (sleepNanos % 1_000_000);
                        Thread.sleep(sleepMillis, sleepSubNanos);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    System.err.println("Tick took too long: " + (elapsed / 1_000_000.0) + " ms");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendQueuedClientPackets() {
        clients.forEach((id, serverClient) -> {
            // Replaced blocks
            Queue<ServerBlockAndPosition> queuedReplacedBlocks = serverClient.getReplacedBlocks();

            int size = queuedReplacedBlocks.size();
            byte[][] outBytes = new byte[size][];
            int byteCount = 4;
            for (int i = 0; i < size; i++) {
                ServerBlockAndPosition block = queuedReplacedBlocks.poll();
                if (block == null) {
                    break;
                }
                byte[] blockBytes = block.serverBlock().getBlockBytes();
                byte[] out = new byte[12 + blockBytes.length];
                ByteUtils.addInt(out, block.x(), 0);
                ByteUtils.addInt(out, block.y(), 4);
                ByteUtils.addInt(out, block.z(), 8);
                System.arraycopy(blockBytes, 0, out, 12, blockBytes.length);
                outBytes[i] = out;
                byteCount += out.length;
            }

            byte[] out = new byte[byteCount];
            ByteUtils.addInt(out, size, 0);

            int index = 4;
            for (int i = 0; i < size; i++) {
                System.arraycopy(outBytes[i], 0, out, index, outBytes[i].length);
                index += outBytes[i].length;
            }

            sendBytes(serverClient.getCTX(), PackageID.REPLACE_BLOCK, out);
        });
    }
}