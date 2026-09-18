package omnivoxel.client.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import omnivoxel.client.game.entity.EntityMeshWrapper;
import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.generators.MeshDataGenerator;
import omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting.ChunkMeshDataLightingGenerator;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.EntityMeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.EntityMeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.LightingChunkMeshDataTask;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.player.PlayerController;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.client.network.request.*;
import omnivoxel.client.network.util.ByteBufUtils;
import omnivoxel.common.entity.EntityVertex;
import omnivoxel.common.network.NetworkService;
import omnivoxel.common.network.NetworkUser;
import omnivoxel.common.resource.GameResources;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.ConstantNetworkSettings;
import omnivoxel.common.settings.Settings;
import omnivoxel.server.PackageID;
import omnivoxel.server.entity.*;
import omnivoxel.server.io.entity.EntityIO;
import omnivoxel.util.bytes.ByteUtils;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.thread.WorkerThreadPool;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk2d.Chunk2D;
import omnivoxel.world.chunk2d.SingleBlockChunk2D;
import org.joml.Matrix4f;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: Clean this up
public final class Client implements NetworkUser {
    private final byte[] clientID;
    private final ClientWorldDataService worldDataService;
    private final AtomicBoolean clientRunning = new AtomicBoolean(true);
    private final Queue<ChunkRequest> queuedChunkTasks = new LinkedBlockingDeque<>();
    private final ClientWorld world;
    private final BlockService<BlockWithMesh> blockService;
    private final Settings settings;
    private WorkerThreadPool<MeshDataTask> meshDataGenerators;
    private WorkerThreadPool<LightingChunkMeshDataTask> lightingGenerators;
    private EventLoopGroup group;
    private Channel channel;
    private long lastFlushedTime = System.currentTimeMillis();
    private PlayerController player = null;
    private GameResources resources;

    public Client(byte[] clientID, ClientWorldDataService worldDataService, ClientWorld world, BlockService<BlockWithMesh> blockService, Settings settings) {
        this.clientID = clientID;
        this.worldDataService = worldDataService;
        this.world = world;
        this.blockService = blockService;
        this.settings = settings;
    }

    // TODO: Move these to the respective records
    private static ServerEntityMesh readMesh(ByteBuf buf) {
        String id = readString(buf);
        String shapeID = readString(buf);
        String textureID = readString(buf);

        int childCount = buf.readInt();
        String[] childrenIDs = new String[childCount];

        for (int i = 0; i < childCount; i++) {
            childrenIDs[i] = readString(buf);
        }

        return new ServerEntityMesh(id, shapeID, textureID, childrenIDs);
    }

    private static ServerEntityTexture readTexture(ByteBuf buf) {
        int idLength = buf.readUnsignedShort();
        byte[] idBytes = new byte[idLength];
        buf.readBytes(idBytes);

        return new ServerEntityTexture(
                new String(idBytes, StandardCharsets.UTF_8)
        );
    }

    private static ServerEntityShape readShape(ByteBuf buf) {
        int idLength = buf.readUnsignedShort();
        byte[] idBytes = new byte[idLength];
        buf.readBytes(idBytes);
        String id = new String(idBytes, StandardCharsets.UTF_8);

        EntityVertex[][] vertices = new EntityVertex[6][];
        int[][] indices = new int[6][];

        for (int polygon = 0; polygon < 6; polygon++) {
            int vertexCount = buf.readUnsignedShort();
            vertices[polygon] = new EntityVertex[vertexCount];

            for (int i = 0; i < vertexCount; i++) {
                vertices[polygon][i] = new EntityVertex(
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat(),
                        buf.readFloat()
                );
            }

            int indexCount = buf.readUnsignedShort();
            indices[polygon] = new int[indexCount];

            for (int i = 0; i < indexCount; i++) {
                indices[polygon][i] = buf.readInt();
            }
        }

        return new ServerEntityShape(id, vertices, indices);
    }

    private static String readString(ByteBuf buf) {
        int length = buf.readInt();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static EntityDefinition readDefinition(ByteBuf buf, Map<String, ServerEntityMesh> serverEntityMeshes) {
        String id = readString(buf);
        String meshId = readString(buf);

        ServerEntityMesh mesh = serverEntityMeshes.get(meshId);
        if (mesh == null) {
            throw new IllegalStateException("Unknown entity mesh: " + meshId);
        }

        return new EntityDefinition(id, mesh);
    }

    public boolean isClientRunning() {
        return clientRunning.get();
    }

    @Override
    public void handlePackage(ChannelHandlerContext ctx, PackageID packageID, ByteBuf byteBuf) {
        try {
            switch (packageID) {
                case CHUNK:
                    receiveChunk(byteBuf);
                    break;
                case HEIGHTS:
                    receiveChunkHeights(byteBuf);
                    break;
                case ENTITY_UPDATE:
                    updateEntity(byteBuf);
                    byteBuf.release();
                    break;
                case CLOSE:
                    String playerID = ByteUtils.bytesToHex(ByteUtils.getBytes(byteBuf, 8, 32));
                    world.removeEntity(playerID);
                    Logger.info("Removed Player: " + playerID);
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
                case REGISTER_BLOCK_HITBOX:
                    ByteBufUtils.cacheBlockHitboxFromByteBuf(byteBuf);
                    byteBuf.release();
                    break;
                case REGISTER_BLOCK:
                    worldDataService.addBlock(ByteBufUtils.registerBlockFromByteBuf(byteBuf));
                    byteBuf.release();
                    break;
                case REPLACE_BLOCK:
                    int replacedBlocks = byteBuf.getInt(8);
                    int index = 12;
                    for (int i = 0; i < replacedBlocks; i++) {
                        int worldX = byteBuf.getInt(index);
                        int worldY = byteBuf.getInt(index + 4);
                        int worldZ = byteBuf.getInt(index + 8);
                        index += 12;

                        int highestY = byteBuf.getByte(index);
                        index += 4;

                        byte rotation = (byte) (byteBuf.getByte(index++) & 3);

                        short paletteLength = byteBuf.getShort(index);
                        index += 2;

                        byte[] idBytes = new byte[paletteLength];
                        byteBuf.getBytes(index, idBytes);
                        String blockID = new String(idBytes);
                        index += paletteLength;

                        replaceBlock(worldX, worldY, worldZ, highestY, blockID, rotation);
                    }

                    byteBuf.release();
                    break;
                case PLAYER_STATE:
                    double[] data = new double[5];
                    for (int i = 0; i < 5; i++) {
                        data[i] = byteBuf.getDouble(8 + i * Double.BYTES);
                    }
                    double x = data[0];
                    double y = data[1];
                    double z = data[2];
                    double pitch = data[3];
                    double yaw = data[4];
                    player.set(x, y, z, pitch, yaw);
                    byteBuf.release();
                    break;
                case REGISTER_GAME_RESOURCES:
                    byteBuf.readerIndex(8);
                    int shapeCount = byteBuf.readInt();
                    Map<String, ServerEntityShape> shapes = new HashMap<>(shapeCount);
                    for (int i = 0; i < shapeCount; i++) {
                        ServerEntityShape shape = readShape(byteBuf);
                        shapes.put(shape.id(), shape);
                    }

                    int textureCount = byteBuf.readInt();
                    Map<String, ServerEntityTexture> textures = new HashMap<>(textureCount);
                    for (int i = 0; i < textureCount; i++) {
                        ServerEntityTexture texture = readTexture(byteBuf);
                        textures.put(texture.id(), texture);
                    }

                    int meshCount = byteBuf.readInt();
                    Map<String, ServerEntityMesh> meshes = new HashMap<>(meshCount);
                    for (int i = 0; i < meshCount; i++) {
                        ServerEntityMesh mesh = readMesh(byteBuf);
                        meshes.put(mesh.id(), mesh);
                    }

                    int entityCount = byteBuf.readInt();
                    EntityDefinition[] entityDefinitions = new EntityDefinition[entityCount];
                    for (int i = 0; i < entityCount; i++) {
                        EntityDefinition definition = readDefinition(byteBuf, meshes);
                        entityDefinitions[i] = definition;
                    }

                    this.resources = new GameResources(shapes, textures, meshes, entityDefinitions);

                    for (EntityDefinition entityDefinition : entityDefinitions) {
                        meshDataGenerators.submit(new EntityMeshDataTask(entityDefinition.entityMesh(), resources));
                    }

                    byteBuf.release();
                    break;
                default:
                    Logger.error(Logger.Priority.HIGH, "Unexpected package key: " + packageID);
                    byteBuf.release();
                    break;
            }
        } catch (RuntimeException e) {
            byteBuf.release();
            clientRunning.set(false);
            throw e;
        }
    }

    private void replaceBlock(int worldX, int worldY, int worldZ, int highestY, String blockID, byte rotation) {
        int chunkX = Math.floorDiv(worldX, ConstantCommonSettings.CHUNK_WIDTH);
        int chunkY = Math.floorDiv(worldY, ConstantCommonSettings.CHUNK_HEIGHT);
        int chunkZ = Math.floorDiv(worldZ, ConstantCommonSettings.CHUNK_LENGTH);

        int x = Math.floorMod(worldX, ConstantCommonSettings.CHUNK_WIDTH);
        int y = Math.floorMod(worldY, ConstantCommonSettings.CHUNK_HEIGHT);
        int z = Math.floorMod(worldZ, ConstantCommonSettings.CHUNK_LENGTH);
        Position2D chunkPosition2D = new Position2D(chunkX, chunkZ);
        Chunk2D<Integer> skylightChunk = world.getChunkHeights(chunkPosition2D);
        if (skylightChunk != null) {
            world.setChunkHeights(chunkPosition2D, skylightChunk.setBlock(x, z, highestY));
        }

        Position3D chunkPosition = new Position3D(chunkX, chunkY, chunkZ);

        ClientWorldChunk clientWorldChunk = world.get(chunkPosition, false, false);
        if (clientWorldChunk != null) {
            Chunk<BlockWithMesh> chunkData = clientWorldChunk.getChunkData(-1);

            if (chunkData != null) {
                clientWorldChunk.setCleanLighting(false);
                BlockWithMesh block = blockService.getBlock(blockID);
                if (chunkData.getBlock(x, y, z) != block || chunkData.getBlockRotation(x, y, z) != rotation) {
                    clientWorldChunk.setChunkData(chunkData.setBlock(x, y, z, block, rotation));
                    lightingGenerators.submit(new LightingChunkMeshDataTask(null, chunkPosition, null, true), true);

                    if (x == 0)
                        lightingGenerators.submit(new LightingChunkMeshDataTask(null, chunkPosition.add(-1, 0, 0), null, true), true);
                    if (x == ConstantCommonSettings.CHUNK_WIDTH - 1)
                        lightingGenerators.submit(new LightingChunkMeshDataTask(null, chunkPosition.add(1, 0, 0), null, true), true);

                    if (y == 0)
                        lightingGenerators.submit(new LightingChunkMeshDataTask(null, chunkPosition.add(0, -1, 0), null, true), true);
                    if (y == ConstantCommonSettings.CHUNK_HEIGHT - 1)
                        lightingGenerators.submit(new LightingChunkMeshDataTask(null, chunkPosition.add(0, 1, 0), null, true), true);

                    if (z == 0)
                        lightingGenerators.submit(new LightingChunkMeshDataTask(null, chunkPosition.add(0, 0, -1), null, true), true);
                    if (z == ConstantCommonSettings.CHUNK_LENGTH - 1)
                        lightingGenerators.submit(new LightingChunkMeshDataTask(null, chunkPosition.add(0, 0, 1), null, true), true);
                }
            }
        }
    }

    private void updateEntity(ByteBuf byteBuf) {
        int offset = 8;

        int entityType = byteBuf.getInt(offset);
        offset += Integer.BYTES;

        int idLength = byteBuf.getInt(offset);
        offset += Integer.BYTES;

        byte[] entityIDBytes = new byte[idLength];
        byteBuf.getBytes(offset, entityIDBytes);
        offset += idLength;

        String entityID = new String(entityIDBytes, StandardCharsets.UTF_8);

        int meshIDLength = byteBuf.getInt(offset);
        offset += Integer.BYTES;

        byte[] meshIDBytes = new byte[meshIDLength];
        byteBuf.getBytes(offset, entityIDBytes);
        offset += meshIDLength;

        String meshID = new String(meshIDBytes, StandardCharsets.UTF_8);

        EntityMeshWrapper entityMeshWrapper = world.getEntity(entityID);
        if (entityMeshWrapper == null) {
            Logger.warn(Logger.Priority.NORMAL, "Received update for unknown entity: " + entityID);
            return;
        }

        double x = byteBuf.getDouble(offset);
        offset += Double.BYTES;

        double y = byteBuf.getDouble(offset);
        offset += Double.BYTES;

        double z = byteBuf.getDouble(offset);
        offset += Double.BYTES;

        double pitch = byteBuf.getDouble(offset);
        offset += Double.BYTES;

        double yaw = byteBuf.getDouble(offset);

        Entity entity = entityMeshWrapper.entity();

        entity.set(x, y, z, pitch, yaw);

        EntityMeshData entityMeshData = entityMeshWrapper.entityMeshReference().getEntityMesh() == null ? null : entityMeshWrapper.entityMeshReference().getEntityMesh().getMeshData();

        if (entityMeshData != null) {
            entityMeshData.setModel(
                    new Matrix4f()
                            .identity()
                            .translate((float) x, (float) (y - 0.75f / 2), (float) z)
                            .scale(0.5f)
                            .rotateY((float) -yaw)
            );

            if (entityMeshData.children().length > 0) {
                entityMeshData.children()[0]
                        .setModel(
                                new Matrix4f()
                                        .translate(0, 0.75f, 0)
                                        .rotateX((float) -pitch)
                        );

//                entityMeshData.children()[1]
//                        .setModel(new Matrix4f().translate(-0.5f, 0.75f, 0));
//
//                entityMeshData.children()[2]
//                        .setModel(new Matrix4f().translate(0.5f, 0.75f, 0));
//
//                entityMeshData.children()[3]
//                        .setModel(new Matrix4f().translate(-0.25f, -0.75f, 0));
//
//                entityMeshData.children()[4]
//                        .setModel(new Matrix4f().translate(0.25f, -0.75f, 0));
            } else {
                Logger.warn("No children?");
            }
        } else {
            Logger.warn("No mesh found for entity: " + entityID);
        }
    }

    private void receiveChunk(ByteBuf byteBuf) {
        int x = byteBuf.getInt(8);
        int y = byteBuf.getInt(12);
        int z = byteBuf.getInt(16);
        Position3D position3D = new Position3D(x, y, z);

        world.receivedChunk(position3D);

        lightingGenerators.submit(new LightingChunkMeshDataTask(byteBuf, position3D, null, false));
    }

    private void receiveChunkHeights(ByteBuf byteBuf) {
        byteBuf.readerIndex(8);
        int cx = byteBuf.readInt();
        int cz = byteBuf.readInt();
        Chunk2D<Integer> chunkHeights = new SingleBlockChunk2D<>(0);
        int x = 0, z = 0;
        for (int i = 0; i < ConstantCommonSettings.BLOCKS_IN_CHUNK_2D; i++) {
            chunkHeights = chunkHeights.setBlock(x, z, byteBuf.readInt());
            x++;
            if (x >= ConstantCommonSettings.CHUNK_WIDTH) {
                x = 0;
                z++;
            }
        }
        world.setChunkHeights(new Position2D(cx, cz), chunkHeights);
        byteBuf.release();
    }

    private void newEntity(ByteBuf byteBuf) {
        byteBuf.readerIndex(8);
        int entityType = byteBuf.readInt();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);

        Entity entity = EntityIO.decode(entityType, bytes);

        world.addEntity(entity, resources.serverEntityMeshes().get(entity.getMeshID()).shapeID());
    }

    public void tick() {
        if (!clientRunning.get()) {
            return;
        }
        long time = System.currentTimeMillis();
        if (time - lastFlushedTime > ConstantNetworkSettings.CHUNK_REQUEST_BATCHING_TIME || queuedChunkTasks.size() > ConstantNetworkSettings.CHUNK_REQUEST_BATCHING_LIMIT) {
            List<ChunkRequest> queuedChunkTasksBatch = new ArrayList<>();
            while (!queuedChunkTasks.isEmpty()) {
                ChunkRequest chunkRequest = queuedChunkTasks.remove();
                if (chunkRequest != null) {
                    queuedChunkTasksBatch.add(chunkRequest);
                } else {
                    break;
                }
            }
            if (!queuedChunkTasksBatch.isEmpty()) {
                int[] data = new int[queuedChunkTasksBatch.size() * 4 + 1];
                data[0] = queuedChunkTasksBatch.size();
                for (int i = 0; !queuedChunkTasksBatch.isEmpty(); i++) {
                    ChunkRequest req = queuedChunkTasksBatch.removeFirst();

                    data[i * 4 + 1] = req.lod();
                    data[i * 4 + 2] = req.position3D().x();
                    data[i * 4 + 3] = req.position3D().y();
                    data[i * 4 + 4] = req.position3D().z();
                }
                NetworkService.sendInts(channel, PackageID.CHUNK_REQUEST, clientID, channel::close, data);
            }
            lastFlushedTime += ConstantNetworkSettings.CHUNK_REQUEST_BATCHING_TIME;
        }
    }

    public void sendRequest(Request request) {
        switch (request.getType()) {
            case CHUNK:
                queuedChunkTasks.add((ChunkRequest) request);
                break;
            case CLOSE:
                NetworkService.sendBytes(channel, PackageID.CLOSE, clientID, channel::close);
                break;
            case PLAYER_UPDATE:
                PlayerUpdateRequest playerUpdateRequest = (PlayerUpdateRequest) request;
                NetworkService.sendDoubles(channel, PackageID.PLAYER_UPDATE, clientID, channel::close, playerUpdateRequest.x(), playerUpdateRequest.y(), playerUpdateRequest.z(), playerUpdateRequest.pitch(), playerUpdateRequest.yaw());
                break;
            case BLOCK_REPLACE:
                BlockReplaceRequest blockReplaceRequest = (BlockReplaceRequest) request;
                byte[] bytes = new byte[Integer.BYTES * 4 + 1 + blockReplaceRequest.newBlock().id().length()];
                ByteUtils.addInt(bytes, blockReplaceRequest.position3D().x(), 0);
                ByteUtils.addInt(bytes, blockReplaceRequest.position3D().y(), Integer.BYTES);
                ByteUtils.addInt(bytes, blockReplaceRequest.position3D().z(), Integer.BYTES * 2);
                ByteUtils.addInt(bytes, blockReplaceRequest.newBlock().id().length(), Integer.BYTES * 3);
                bytes[Integer.BYTES * 4] = (byte) (blockReplaceRequest.rotation() & 3);
                System.arraycopy(blockReplaceRequest.newBlock().id().getBytes(), 0, bytes, Integer.BYTES * 4 + 1, blockReplaceRequest.newBlock().id().length());
                NetworkService.sendBytes(channel, PackageID.REPLACE_BLOCK, clientID, channel::close, bytes);

                // TODO: Calculate highestY
                replaceBlock(blockReplaceRequest.position3D().x(), blockReplaceRequest.position3D().y(), blockReplaceRequest.position3D().z(), 0, blockReplaceRequest.newBlock().id(), blockReplaceRequest.rotation());
                break;
            default:
                Logger.error(Logger.Priority.HIGH, "Unexpected request type: " + request.getType());
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
        Logger.debug("Disconnecting from server...");
        sendRequest(new CloseRequest());
        try {
            if (channel != null) {
                channel.close().sync();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (group != null) {
                group.shutdownGracefully();
            }
            clientRunning.set(false);
            lightingGenerators.shutdown();
            meshDataGenerators.shutdown();
            lightingGenerators.awaitTermination();
            meshDataGenerators.awaitTermination();
        }
        Logger.info("Client disconnected");
    }

    public void setListeners(State state) {
        meshDataGenerators = new WorkerThreadPool<>(
                settings.getIntSetting("max_mesh_generator_threads", Runtime.getRuntime().availableProcessors()),
                () -> new MeshDataGenerator(
                        world,
                        state,
                        settings
                )::generateMeshData,
                true
        );
        Set<Position3D> completeDirtyNeighbors = ConcurrentHashMap.newKeySet();
        lightingGenerators = new WorkerThreadPool<>(
                settings.getIntSetting("max_lighting_generator_threads", Runtime.getRuntime().availableProcessors()),
                () -> new ChunkMeshDataLightingGenerator(
                        world,
                        meshDataGenerators,
                        blockService,
                        state,
                        completeDirtyNeighbors
                )::generateLightingMeshData,
                true
        );
    }

    public void setPlayer(PlayerController player) {
        if (this.player == null) {
            this.player = player;
        } else {
            throw new IllegalArgumentException("Cannot set player twice");
        }
    }
}
