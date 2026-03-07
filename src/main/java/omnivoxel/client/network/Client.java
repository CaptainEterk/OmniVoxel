package omnivoxel.client.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import omnivoxel.client.game.entity.ClientEntity;
import omnivoxel.client.game.graphics.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.opengl.mesh.definition.EntityMeshDataDefinition;
import omnivoxel.client.game.graphics.opengl.mesh.generators.MeshDataGenerator;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.ModelEntityMeshData;
import omnivoxel.client.game.graphics.opengl.mesh.tasks.ChunkMeshDataTask;
import omnivoxel.client.game.graphics.opengl.mesh.tasks.EntityMeshDataTask;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.client.network.request.ChunkRequest;
import omnivoxel.client.network.request.CloseRequest;
import omnivoxel.client.network.request.PlayerUpdateRequest;
import omnivoxel.client.network.request.Request;
import omnivoxel.client.network.util.ByteBufUtils;
import omnivoxel.server.ConstantServerSettings;
import omnivoxel.server.PackageID;
import omnivoxel.server.entity.EntityType;
import omnivoxel.util.bytes.ByteUtils;
import omnivoxel.util.cache.IDCache;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.thread.WorkerThreadPool;
import omnivoxel.world.block.Block;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Clean this up
public final class Client {
    private final Map<String, ClientEntity> entities;
    private final byte[] clientID;
    private final ClientWorldDataService worldDataService;
    private final Logger logger;
    private final AtomicBoolean clientRunning = new AtomicBoolean(true);
    private final Queue<Position3D> queuedChunkTasks = new LinkedBlockingDeque<>();
    private final ClientWorld world;
    private final BlockService blockService = new BlockService();
    private WorkerThreadPool<MeshDataTask> meshDataGenerators;
    private EventLoopGroup group;
    private Channel channel;
    private long lastFlushedTime = System.currentTimeMillis();

    public Client(byte[] clientID, ClientWorldDataService worldDataService, Logger logger, ClientWorld world) {
        this.clientID = clientID;
        this.worldDataService = worldDataService;
        this.logger = logger;
        this.world = world;
        entities = new ConcurrentHashMap<>();
    }

    private static void sendDoubles(Channel channel, PackageID id, byte[] clientID, double... numbers) {
        if (channel == null) {
            System.err.println("[ERROR] Channel is null! Client may not be connected.");
            return;
        }
        if (!channel.isActive()) {
            System.out.println("[ERROR] Channel is closed! Cannot send data.");
            return;
        }

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(id.ordinal());
        buffer.writeBytes(clientID);
        for (double i : numbers) {
            buffer.writeDouble(i);
        }
        flush(channel, buffer);
    }

    private static void flush(Channel channel, ByteBuf byteBuf) {
        channel.writeAndFlush(byteBuf).addListener(f -> {
            if (!f.isSuccess()) {
                System.err.println("[ERROR] Failed: " + f.cause());
                f.cause().printStackTrace();
            }
        });
    }

    public boolean isClientRunning() {
        return clientRunning.get();
    }

    private void sendBytes(Channel channel, PackageID id, byte[]... bytes) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(id.ordinal());
        for (byte[] bites : bytes) {
            buffer.writeBytes(bites);
        }
        flush(channel, buffer);
    }

    private void sendInts(Channel channel, PackageID id, byte[] clientID, int... numbers) {
        if (channel == null) {
            logger.error(String.format("Failed to send PackageID.%s because channel is null. Client may not be connected.", id.toString()));
            return;
        }
        if (!channel.isActive()) {
            close();
            logger.error("Channel is closed. Cannot send data.");
            return;
        }

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(id.ordinal());
        buffer.writeBytes(clientID);
        for (int i : numbers) {
            buffer.writeInt(i);
        }
        flush(channel, buffer);
    }

    void handlePackage(ChannelHandlerContext ctx, PackageID packageID, ByteBuf byteBuf) throws InterruptedException {
        try {
            switch (packageID) {
                case CHUNK:
                    receiveChunk(byteBuf);
                    break;
                case ENTITY_UPDATE:
                    updateEntity(byteBuf);
                    byteBuf.release();
                    break;
                case CLOSE:
                    String playerID = ByteUtils.bytesToHex(ByteUtils.getBytes(byteBuf, 8, 32));
                    entities.remove(playerID);
                    logger.info("Removed Player: " + playerID);
                    world.removeEntity(playerID);
                    byteBuf.release();
                    break;
                case NEW_ENTITY:
                    newEntity(byteBuf);
                    byteBuf.release();
                    break;
                case REGISTER_BLOCK_SHAPE:
                    ByteBufUtils.cacheBlockShapeFromByteBuf(byteBuf);
                    byteBuf.release();
                    break;
                case REGISTER_BLOCK: {
                    worldDataService.addBlock(ByteBufUtils.registerBlockFromByteBuf(byteBuf));
                    byteBuf.release();
                    break;
                }
                case REPLACE_BLOCK:
                    int replacedBlocks = byteBuf.getInt(8);
                    int index = 12;
                    for (int i = 0; i < replacedBlocks; i++) {
                        int worldX = byteBuf.getInt(index);
                        int worldY = byteBuf.getInt(index + 4);
                        int worldZ = byteBuf.getInt(index + 8);
                        index += 12;

                        // Use floorDiv so negative world coordinates map to correct chunk indices
                        int chunkX = Math.floorDiv(worldX, ConstantGameSettings.CHUNK_WIDTH);
                        int chunkY = Math.floorDiv(worldY, ConstantGameSettings.CHUNK_HEIGHT);
                        int chunkZ = Math.floorDiv(worldZ, ConstantGameSettings.CHUNK_LENGTH);

                        // Local block coordinates inside the chunk (handles negatives correctly)
                        int x = Math.floorMod(worldX, ConstantGameSettings.CHUNK_WIDTH);
                        int y = Math.floorMod(worldY, ConstantGameSettings.CHUNK_HEIGHT);
                        int z = Math.floorMod(worldZ, ConstantGameSettings.CHUNK_LENGTH);

                        short paletteLength = byteBuf.getShort(index);
                        index += 2;

                        byte[] idBytes = new byte[paletteLength];
                        byteBuf.getBytes(index, idBytes);
                        StringBuilder blockID = new StringBuilder();
                        for (byte b : idBytes) {
                            blockID.append((char) b);
                        }
                        index += paletteLength;

                        Position3D chunkPosition = new Position3D(chunkX, chunkY, chunkZ);

                        ClientWorldChunk clientWorldChunk = world.get(chunkPosition, false, false);
                        if (clientWorldChunk != null) {
                            Chunk<Block> chunkData = clientWorldChunk.getChunkData();

                            if (chunkData != null) {
                                Block block = blockService.getBlock(blockID.toString());
                                if (chunkData.getBlock(x, y, z) != block) {
                                    clientWorldChunk.setChunkData(chunkData.setBlock(x, y, z, block));
                                    meshDataGenerators.submit(new ChunkMeshDataTask(null, chunkPosition));

                                    if (x == 0) meshDataGenerators.submit(new ChunkMeshDataTask(null, chunkPosition.add(-1, 0, 0)));
                                    if (x == ConstantGameSettings.CHUNK_WIDTH - 1) meshDataGenerators.submit(new ChunkMeshDataTask(null, chunkPosition.add(1, 0, 0)));

                                    if (y == 0) meshDataGenerators.submit(new ChunkMeshDataTask(null, chunkPosition.add(0, -1, 0)));
                                    if (y == ConstantGameSettings.CHUNK_HEIGHT - 1) meshDataGenerators.submit(new ChunkMeshDataTask(null, chunkPosition.add(0, 1, 0)));

                                    if (z == 0) meshDataGenerators.submit(new ChunkMeshDataTask(null, chunkPosition.add(0, 0, -1)));
                                    if (z == ConstantGameSettings.CHUNK_LENGTH - 1) meshDataGenerators.submit(new ChunkMeshDataTask(null, chunkPosition.add(0, 0, 1)));
                                }
                            }
                        }
                    }

                    byteBuf.release();
                    break;
                default:
                    System.err.println("Unexpected package key: " + packageID);
                    byteBuf.release();
                    break;
            }
        } catch (RuntimeException e) {
            byteBuf.release();
            clientRunning.set(false);
            throw e;
        }
    }

    private void updateEntity(ByteBuf byteBuf) {
        int offset = 8;

        int idLength = byteBuf.getInt(offset);
        offset += Integer.BYTES;

        byte[] entityIDBytes = new byte[idLength];
        byteBuf.getBytes(offset, entityIDBytes);
        offset += idLength;

        String entityID = ByteUtils.bytesToHex(entityIDBytes);
        ClientEntity entity = entities.get(entityID);
        if (entity == null) {
            System.err.println("Received update for unknown entity: " + entityID);
            return;
        }

        offset += Integer.BYTES;

        double x = byteBuf.getDouble(offset);
        offset += Double.BYTES;
        double y = byteBuf.getDouble(offset);
        offset += Double.BYTES;
        double z = byteBuf.getDouble(offset);
        offset += Double.BYTES;
        double pitch = byteBuf.getDouble(offset);
        offset += Double.BYTES;
        double yaw = byteBuf.getDouble(offset);

        entity.set(x, y, z, pitch, yaw);

        if (entity.getMesh() != null) {
            entity.getMeshData().setModel(new Matrix4f().identity()
                    .translate((float) x, (float) (y - 0.75f / 2), (float) z)
                    .scale(0.5f)
                    .rotateY((float) -yaw));

            if (!entity.getMesh().getChildren().isEmpty()) {
                entity.getMesh().getChildren().getFirst().getMeshData().setModel(new Matrix4f().translate(0, 0.75f, 0).rotateX((float) -pitch));
                entity.getMesh().getChildren().get(1).getMeshData().setModel(new Matrix4f().translate(-0.5f, 0.75f, 0));
                entity.getMesh().getChildren().get(2).getMeshData().setModel(new Matrix4f().translate(0.5f, 0.75f, 0));
                entity.getMesh().getChildren().get(3).getMeshData().setModel(new Matrix4f().translate(-0.25f, -0.75f, 0));
                entity.getMesh().getChildren().get(4).getMeshData().setModel(new Matrix4f().translate(0.25f, -0.75f, 0));
            }
        }
    }

    private void receiveChunk(ByteBuf byteBuf) throws InterruptedException {
        int x = byteBuf.getInt(8);
        int y = byteBuf.getInt(12);
        int z = byteBuf.getInt(16);
        Position3D position3D = new Position3D(x, y, z);

        meshDataGenerators.submit(new ChunkMeshDataTask(byteBuf, position3D));
    }

    private void loadPlayer(byte[] playerID, String name) throws InterruptedException {
        String id = ByteUtils.bytesToHex(playerID);
        ClientEntity playerEntity = new ClientEntity(name, id, new EntityType(EntityType.Type.PLAYER, name));
        entities.put(id, playerEntity);

        logger.info("Added player: " + id);

        meshDataGenerators.submit(new EntityMeshDataTask(playerEntity));
    }

    private void newPlayer(ByteBuf byteBuf) throws InterruptedException {
        loadPlayer(ByteUtils.getBytes(byteBuf, 8, 32), "Other client!!");
    }

    private void newEntity(ByteBuf byteBuf) throws InterruptedException {
        int entityIDLength = byteBuf.getInt(8);

        byte[] entityID = new byte[entityIDLength];
        byteBuf.getBytes(12, entityID);

        int typeStart = 12 + entityIDLength;
        int typeOrdinal = byteBuf.getInt(typeStart);
        EntityType.Type type = EntityType.Type.values()[typeOrdinal];

        int doubleStart = typeStart + Integer.BYTES;
        double x = byteBuf.getDouble(doubleStart);
        double y = byteBuf.getDouble(doubleStart + Double.BYTES);
        double z = byteBuf.getDouble(doubleStart + Double.BYTES * 2);
        double pitch = byteBuf.getDouble(doubleStart + Double.BYTES * 3);
        double yaw = byteBuf.getDouble(doubleStart + Double.BYTES * 4);

        int nameLength = byteBuf.getInt(doubleStart + Double.BYTES * 5);
        int nameStart = doubleStart + Double.BYTES * 5 + Integer.BYTES;

        byte[] nameBytes = new byte[nameLength];
        byteBuf.getBytes(nameStart, nameBytes);
        String name = new String(nameBytes);

        String id = ByteUtils.bytesToHex(entityID);
        ClientEntity entity = new ClientEntity(name, id, new EntityType(type, name));
        entity.set(x, y, z, pitch, yaw);
        entity.setMeshData(new ModelEntityMeshData(entity)
                .setModel(new Matrix4f().translate((float) x, (float) y, (float) z)));

        entities.put(id, entity);
        meshDataGenerators.submit(new EntityMeshDataTask(entity));
    }

    public Map<String, ClientEntity> getEntities() {
        return entities;
    }

    public void tick() {
        if (!clientRunning.get()) {
            return;
        }
        long time = System.currentTimeMillis();
        if (time - lastFlushedTime > ConstantServerSettings.CHUNK_REQUEST_BATCHING_TIME || queuedChunkTasks.size() > ConstantServerSettings.CHUNK_REQUEST_BATCHING_LIMIT) {
            List<Position3D> queuedChunkTasksBatch = new ArrayList<>();
            while (!queuedChunkTasks.isEmpty()) {
                queuedChunkTasksBatch.add(queuedChunkTasks.remove());
                if (queuedChunkTasksBatch.getLast() == null) {
                    queuedChunkTasksBatch.removeLast();
                    break;
                }
            }
            if (!queuedChunkTasksBatch.isEmpty()) {
                int[] data = new int[queuedChunkTasksBatch.size() * 3 + 1];
                data[0] = queuedChunkTasksBatch.size();
                for (int i = 0; !queuedChunkTasksBatch.isEmpty(); i++) {
                    Position3D req = queuedChunkTasksBatch.removeLast();

                    data[i * 3 + 1] = req.x();
                    data[i * 3 + 2] = req.y();
                    data[i * 3 + 3] = req.z();
                }
                sendInts(channel, PackageID.CHUNK_REQUEST, clientID, data);
            }
            lastFlushedTime += ConstantServerSettings.CHUNK_REQUEST_BATCHING_TIME;
        }
    }

    public void sendRequest(Request request) {
        switch (request.getType()) {
            case CHUNK:
                Position3D position3D = ((ChunkRequest) request).position3D();
                queuedChunkTasks.add(position3D);
                break;
            case CLOSE:
                sendBytes(channel, PackageID.CLOSE, clientID);
                break;
            case PLAYER_UPDATE:
                PlayerUpdateRequest r = (PlayerUpdateRequest) request;
                sendDoubles(channel, PackageID.PLAYER_UPDATE, clientID, r.x(), r.y(), r.z(), r.pitch(), r.yaw());
                break;
            default:
                System.err.println("Unexpected request type: " + request.getType());
        }
    }

    byte[] getClientID() {
        return clientID;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setGroup(EventLoopGroup group) {
        this.group = group;
    }

    public void close() {
        logger.debug("Disconnecting from server...");
        sendRequest(new CloseRequest());
        try {
            if (channel != null) {
                channel.close().sync();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (group != null) {
                group.shutdownGracefully();
            }
            clientRunning.set(false);
            meshDataGenerators.shutdown();
            meshDataGenerators.awaitTermination();
        }
        logger.info("Client disconnected");
    }

    public void setListeners(IDCache<String, EntityMeshDataDefinition> entityMeshDefinitionCache, Set<String> queuedEntityMeshData) {
        meshDataGenerators = new WorkerThreadPool<>(
                ConstantGameSettings.MAX_MESH_GENERATOR_THREADS,
                () -> new MeshDataGenerator(
                        worldDataService,
                        entityMeshDefinitionCache,
                        queuedEntityMeshData,
                        world,
                        blockService
                )::generateMeshData,
                true
        );
    }
}