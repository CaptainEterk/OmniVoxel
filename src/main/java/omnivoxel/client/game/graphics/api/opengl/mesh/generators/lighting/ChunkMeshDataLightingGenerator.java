package omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting;

import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.ChunkMeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.LightingChunkMeshDataTask;
import omnivoxel.client.game.graphics.block.BlockMesh;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.light.ChunkLightingData;
import omnivoxel.client.game.graphics.light.channel.GeneralLightChannel;
import omnivoxel.client.game.graphics.light.channel.LightChannel;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.client.game.graphics.light.channel.SingleLightChannel;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.chunk.ChunkUnpacker;
import omnivoxel.common.face.BlockFace;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.data.Direction;
import omnivoxel.util.log.Logger;
import omnivoxel.util.map.IntegerMap;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.thread.WorkerThreadPool;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.ChunkShell;
import omnivoxel.world.chunk2d.Chunk2D;

import java.util.*;

public class ChunkMeshDataLightingGenerator {
    private static final Position3D[] DIRECT_NEIGHBOR_OFFSETS = {
            new Position3D(-1, 0, 0),
            new Position3D(1, 0, 0),
            new Position3D(0, -1, 0),
            new Position3D(0, 1, 0),
            new Position3D(0, 0, -1),
            new Position3D(0, 0, 1)
    };
    private final Map<Direction, LightNodeQueue> borderLightQueues = new EnumMap<>(Direction.class);
    private final LightNodeQueue chunkLights;
    private final ClientWorld world;
    private final WorkerThreadPool<MeshDataTask> meshDataGenerators;
    private final BlockService<BlockWithMesh> blockService;
    private final State state;
    private final IntegerMap neighborMap = new IntegerMap();
    private final Set<Position3D> completeDirtyChunks;
    private final Position3D[] foundCompleteDirtyNeighborPositions = new Position3D[26];
    private final ClientWorldChunk[] foundCompleteDirtyNeighborChunks = new ClientWorldChunk[26];

    public ChunkMeshDataLightingGenerator(ClientWorld world, WorkerThreadPool<MeshDataTask> meshDataGenerators, BlockService<BlockWithMesh> blockService, State state, Set<Position3D> completeDirtyChunks) {
        this.world = world;
        this.meshDataGenerators = meshDataGenerators;
        this.blockService = blockService;
        this.state = state;
        this.completeDirtyChunks = completeDirtyChunks;
        this.chunkLights = new LightNodeQueue();
        if (ConstantCommonSettings.CHUNK_WIDTH > 32
                || ConstantCommonSettings.CHUNK_HEIGHT > 32
                || ConstantCommonSettings.CHUNK_LENGTH > 32) {
            throw new IllegalStateException("Border light encoding supports chunk dimensions up to 32.");
        }

        for (Direction dir : Direction.VALUES) {
            borderLightQueues.put(dir, new LightNodeQueue());
        }
    }

    private static short encodeUV(int x, int y, int z, Direction dir) {
        int a = 0, b = 0;

        switch (dir) {
            case UP, DOWN -> {
                a = x;
                b = z;
            }
            case NORTH, SOUTH -> {
                a = x;
                b = y;
            }
            case EAST, WEST -> {
                a = y;
                b = z;
            }
        }

        return (short) (
                (a & 0b11111) |
                        ((b & 0b11111) << 5)
        );
    }

    private static int getNeighborLightIndex(Direction dir, int x, int y, int z) {
        return switch (dir) {
            case UP -> IndexCalculator.calculateBlockIndex(
                    x,
                    ConstantCommonSettings.CHUNK_HEIGHT - 1,
                    z);

            case DOWN -> IndexCalculator.calculateBlockIndex(
                    x,
                    0,
                    z);

            case NORTH -> IndexCalculator.calculateBlockIndex(
                    x,
                    y,
                    0);

            case SOUTH -> IndexCalculator.calculateBlockIndex(
                    x,
                    y,
                    ConstantCommonSettings.CHUNK_LENGTH - 1);

            case EAST -> IndexCalculator.calculateBlockIndex(
                    ConstantCommonSettings.CHUNK_WIDTH - 1,
                    y,
                    z);

            case WEST -> IndexCalculator.calculateBlockIndex(
                    0,
                    y,
                    z);
        };
    }

    private static void createLightingDataIfEmpty(ClientWorldChunk clientWorldChunk) {
        if (clientWorldChunk.getLightingData() == null) {
            clientWorldChunk.setChunkLightingData(new ChunkLightingData(null, null, null, null));
        }
    }

    public Set<LightingChunkMeshDataTask> generateLightingMeshData(LightingChunkMeshDataTask lightingChunkMeshDataTask, int queueSize) {
        state.setItem(Thread.currentThread().getName() + "_queue_size_cmdlg", queueSize);
        if (lightingChunkMeshDataTask.blocks() != null) {
            Position3D position = lightingChunkMeshDataTask.position3D();
            int previousLod = getNativeLod(position);
            ChunkUnpacker.unpackChunkPadded(lightingChunkMeshDataTask.blocks(), position, blockService, world);
            if (previousLod != -1 && previousLod != getNativeLod(position)) {
                remeshDirectNeighbors(position);
            }
        }
        return generateChunkMeshDataLighting(lightingChunkMeshDataTask.position3D(), lightingChunkMeshDataTask.channel());
    }

    private int getNativeLod(Position3D position) {
        ClientWorldChunk chunk = world.get(position, false, false);
        return chunk == null || chunk.getChunkData(-1) == null ? -1 : chunk.getChunkData(-1).getLOD();
    }

    private void remeshDirectNeighbors(Position3D position) {
        for (Position3D offset : DIRECT_NEIGHBOR_OFFSETS) {
            Position3D neighborPosition = position.add(offset.x(), offset.y(), offset.z());
            ClientWorldChunk neighbor = world.get(neighborPosition, false, false);
            if (neighbor != null && neighbor.getLightingData() != null && neighbor.isCleanLighting() && neighbor.getChunkData(-1).getLOD() == 0) {
                meshDataGenerators.submit(new ChunkMeshDataTask(neighborPosition));
            }
        }
    }

    private void checkCompleteDirtyNeighbor(Position3D position3D, ClientWorldChunk clientWorldChunk) {
        if (clientWorldChunk != null && clientWorldChunk.getLightingData() != null) {
            if (clientWorldChunk.isCleanLighting()) {
                if (completeDirtyChunks.remove(position3D)) {
                    meshDataGenerators.submit(new ChunkMeshDataTask(position3D));
                }
            } else {
                boolean failed = false;
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (!(x == 0 && y == 0 && z == 0)) {
                                for (LightChannels c : LightChannels.VALUES) {
                                    failed |= isNeighborLightingInvalid(world.get(position3D.add(x, y, z), false, false), c);
                                }
                            }
                        }
                    }
                }
                if (!failed) {
                    if (completeDirtyChunks.remove(position3D)) {
                        meshDataGenerators.submit(new ChunkMeshDataTask(position3D));
                    }
                    clientWorldChunk.setCleanLighting(true);
                }
            }
        }
    }

    private Set<LightingChunkMeshDataTask> generateChunkMeshDataLighting(Position3D position3D, LightChannels channel) {
        ClientWorldChunk clientWorldChunk = world.get(position3D, false, false);

        if (clientWorldChunk == null || clientWorldChunk.getChunkData(-1) == null || clientWorldChunk.getChunkData(-1) instanceof ChunkShell<BlockWithMesh>) {
            return null;
        }

        Chunk<BlockWithMesh> chunkData = clientWorldChunk.getChunkData(-1);

        if (chunkData.getLOD() > 0) {
            clientWorldChunk.setChunkLightingData(new ChunkLightingData(new SingleLightChannel((byte) 0), new SingleLightChannel((byte) 0), new SingleLightChannel((byte) 0), new SingleLightChannel((byte) 15)));
            clientWorldChunk.setCleanLighting(true);

            completeDirtyChunks.remove(position3D);

            meshDataGenerators.submit(new ChunkMeshDataTask(position3D));

            int foundCompleteDirtyChunkCount = 0;
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (!(x == 0 && y == 0 && z == 0)) {
                            Position3D neighborPos = position3D.add(x, y, z);
                            if (completeDirtyChunks.contains(neighborPos)) {
                                foundCompleteDirtyNeighborPositions[foundCompleteDirtyChunkCount] = neighborPos;
                                foundCompleteDirtyNeighborChunks[foundCompleteDirtyChunkCount++] = world.get(neighborPos, false, false);
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < foundCompleteDirtyChunkCount; i++) {
                checkCompleteDirtyNeighbor(foundCompleteDirtyNeighborPositions[i], foundCompleteDirtyNeighborChunks[i]);
            }

            return null;
        }

        if (channel == null) {
            Set<LightingChunkMeshDataTask> out = new HashSet<>();
            for (LightChannels lightChannel : LightChannels.values()) {
                Set<LightingChunkMeshDataTask> lightingChunkMeshDataTasks = generateChunkMeshDataLighting(position3D, lightChannel);
                if (lightingChunkMeshDataTasks != null) {
                    out.addAll(lightingChunkMeshDataTasks);
                }
            }
            return out;
        }

        createLightingDataIfEmpty(clientWorldChunk);

        int foundCompleteDirtyChunkCount = 0;

        boolean failed = false;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (!(x == 0 && y == 0 && z == 0)) {
                        Position3D neighborPos = position3D.add(x, y, z);
                        ClientWorldChunk neighborChunk = world.get(neighborPos, false, false);
                        failed |= isNeighborLightingInvalid(neighborChunk, channel);
                        if (completeDirtyChunks.contains(neighborPos)) {
                            foundCompleteDirtyNeighborPositions[foundCompleteDirtyChunkCount] = neighborPos;
                            foundCompleteDirtyNeighborChunks[foundCompleteDirtyChunkCount++] = neighborChunk;
                        }
                    }
                }
            }
        }

        Set<LightingChunkMeshDataTask> meshDataTasks = new HashSet<>();

        LightChannel lightChannel = generateLighting(clientWorldChunk, chunkData, position3D, channel, meshDataTasks);
        clientWorldChunk.getLightingData().setChannel(channel, lightChannel);

        clientWorldChunk.setCleanLighting(channel, !failed);

        for (int i = 0; i < foundCompleteDirtyChunkCount; i++) {
            checkCompleteDirtyNeighbor(foundCompleteDirtyNeighborPositions[i], foundCompleteDirtyNeighborChunks[i]);
        }

        if (clientWorldChunk.getLightingData().isComplete()) {
            if (clientWorldChunk.isCleanLighting()) {
                completeDirtyChunks.remove(position3D);
                meshDataGenerators.submit(new ChunkMeshDataTask(position3D));
            } else {
                completeDirtyChunks.add(position3D);
            }
        }

        return meshDataTasks;
    }

    private LightChannel generateLighting(ClientWorldChunk clientWorldChunk, Chunk<BlockWithMesh> chunk, Position3D chunkPos, LightChannels lightChannels, Collection<LightingChunkMeshDataTask> meshDataTasks) {
        if (lightChannels == LightChannels.RED) {
            return generateLightChannel(meshDataTasks, clientWorldChunk, chunk, chunkPos, LightChannels.RED);
        } else if (lightChannels == LightChannels.GREEN) {
            return generateLightChannel(meshDataTasks, clientWorldChunk, chunk, chunkPos, LightChannels.GREEN);
        } else if (lightChannels == LightChannels.BLUE) {
            return generateLightChannel(meshDataTasks, clientWorldChunk, chunk, chunkPos, LightChannels.BLUE);
        } else if (lightChannels == LightChannels.SKYLIGHT) {
            return generateLightChannel(meshDataTasks, clientWorldChunk, chunk, chunkPos, LightChannels.SKYLIGHT);
        } else {
            throw new IllegalArgumentException("Unexpected light channel: " + lightChannels);
        }
    }

    private LightChannel generateLightChannel(
            Collection<LightingChunkMeshDataTask> lightingTasks,
            ClientWorldChunk clientWorldChunk,
            Chunk<BlockWithMesh> chunk,
            Position3D chunkPos,
            LightChannels channel
    ) {
        clearQueues();

        int[] lightChannel = new int[ConstantCommonSettings.BLOCKS_IN_CHUNK >> 3];

        loadChunkLights(channel, chunkPos, chunk);

        for (Direction dir : Direction.VALUES) {
            short[] neighbor =
                    clientWorldChunk.getNeighborLightOverflow(channel, dir);

            for (short overflowNode : neighbor) {
                int a = overflowNode & 0b11111;
                int b = (overflowNode >> 5) & 0b11111;
                int light = (overflowNode >> 10) & 0xF;

                int x = 0;
                int y = 0;
                int z = 0;

                switch (dir) {
                    case UP -> {
                        x = a;
                        z = b;
                        y = ConstantCommonSettings.CHUNK_HEIGHT - 1;
                    }

                    case DOWN -> {
                        x = a;
                        z = b;
                    }

                    case NORTH -> {
                        x = a;
                        y = b;
                    }

                    case SOUTH -> {
                        x = a;
                        y = b;
                        z = ConstantCommonSettings.CHUNK_LENGTH - 1;
                    }

                    case EAST -> {
                        y = a;
                        z = b;
                        x = ConstantCommonSettings.CHUNK_WIDTH - 1;
                    }

                    case WEST -> {
                        y = a;
                        z = b;
                    }
                }

                int blockIndex =
                        IndexCalculator.calculateBlockIndex(x, y, z);

                int arrayIndex = blockIndex >> 3;
                int shift = (blockIndex & 7) << 2;

                int oldLight =
                        (lightChannel[arrayIndex] >>> shift) & 0xF;

                if (light > oldLight) {
                    int mask = 0xF << shift;

                    lightChannel[arrayIndex] =
                            (lightChannel[arrayIndex] & ~mask)
                                    | (light << shift);

                    chunkLights.add(
                            x,
                            y,
                            z,
                            (byte) light
                    );
                }
            }
        }

        if (chunkLights.isEmpty()) {
            return new SingleLightChannel((byte) 0);
        }

        floodFill(lightChannel, chunk, channel);

        propagateLighting(chunkPos, channel, lightingTasks);

        return new GeneralLightChannel(lightChannel);
    }

    // TODO: Make this also load all channels and only do a complete search once
    private void loadChunkLights(LightChannels channel, Position3D chunkPos, Chunk<BlockWithMesh> chunk) {
        int chunkYOffset = chunkPos.y() * ConstantCommonSettings.CHUNK_HEIGHT;

        Chunk2D<Integer> chunkHeights = channel == LightChannels.SKYLIGHT ? world.getChunkHeights(chunkPos.getPosition2D()) : null;
        if (channel == LightChannels.SKYLIGHT && chunkHeights == null) {
            Logger.error("chunkHeights is null (" + chunkPos.x() + ", " + chunkPos.z() + ")");
            return;
        }

        for (int x = 0; x < ConstantCommonSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantCommonSettings.CHUNK_LENGTH; z++) {
                if (channel == LightChannels.SKYLIGHT) {
                    int highestY = chunkHeights.getBlock(x, z);

                    if (chunkYOffset + ConstantCommonSettings.CHUNK_HEIGHT < highestY) {
                        continue;
                    }

                    for (int y = ConstantCommonSettings.CHUNK_HEIGHT - 1; y >= 0; y--) {
                        if (chunkYOffset + y >= highestY) {
                            chunkLights.add(x, y, z, (byte) 15);
                        } else {
                            break;
                        }
                    }
                } else {
                    for (int y = ConstantCommonSettings.CHUNK_HEIGHT - 1; y >= 0; y--) {
                        BlockMesh mesh = chunk.getBlock(x, y, z).blockMesh();

                        if (mesh != null) {
                            if (mesh.getLightEmitting(BlockFace.TOP, channel) > 0) {
                                if (x < ConstantCommonSettings.CHUNK_WIDTH - 1) {
                                    chunkLights.add(x + 1, y, z, mesh.getLightEmitting(BlockFace.EAST, channel));
                                }
                                if (x > 0) {
                                    chunkLights.add(x - 1, y, z, mesh.getLightEmitting(BlockFace.WEST, channel));
                                }
                                if (y < ConstantCommonSettings.CHUNK_HEIGHT - 1) {
                                    chunkLights.add(x, y + 1, z, mesh.getLightEmitting(BlockFace.TOP, channel));
                                }
                                if (y > 0) {
                                    chunkLights.add(x, y - 1, z, mesh.getLightEmitting(BlockFace.BOTTOM, channel));
                                }
                                if (z < ConstantCommonSettings.CHUNK_LENGTH - 1) {
                                    chunkLights.add(x, y, z + 1, mesh.getLightEmitting(BlockFace.NORTH, channel));
                                }
                                if (z > 0) {
                                    chunkLights.add(x, y, z - 1, mesh.getLightEmitting(BlockFace.SOUTH, channel));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void floodFill(int[] lightChannel, Chunk<BlockWithMesh> chunk, LightChannels channel) {
        while (!chunkLights.isEmpty()) {
            chunkLights.poll();

            int x = chunkLights.x();
            int y = chunkLights.y();
            int z = chunkLights.z();
            int light = chunkLights.lightLevel() & 0xF;

            int blockIndex = IndexCalculator.calculateBlockIndex(x, y, z);
            int arrayIndex = blockIndex >> 3;
            int shift = (blockIndex & 7) << 2;

            int oldLight = (lightChannel[arrayIndex] >>> shift) & 0xF;

            if (light < oldLight) {
                continue;
            }

            int mask = 0xF << shift;
            lightChannel[arrayIndex] =
                    (lightChannel[arrayIndex] & ~mask) | (light << shift);

            if (light < 1) {
                continue;
            }

            BlockMesh mesh = chunk.getBlock(x, y, z).blockMesh();

            for (Direction direction : Direction.VALUES) {
                int nx = x + direction.dx;
                int ny = y + direction.dy;
                int nz = z + direction.dz;

                int diffuse = mesh == null
                        ? 1
                        : mesh.getLightDiffuse(
                        direction.opposite().getBlockFace(),
                        channel
                );

                int attenuated = light - diffuse;

                if (attenuated <= 0) {
                    continue;
                }

                if (IndexCalculator.checkBounds(nx, ny, nz)) {
                    int neighborBlockIndex =
                            IndexCalculator.calculateBlockIndex(nx, ny, nz);

                    int neighborArrayIndex = neighborBlockIndex >> 3;
                    int neighborShift = (neighborBlockIndex & 7) << 2;

                    int neighborLight =
                            (lightChannel[neighborArrayIndex] >>> neighborShift) & 0xF;

                    if (attenuated > neighborLight) {
                        int neighborMask = 0xF << neighborShift;

                        lightChannel[neighborArrayIndex] =
                                (lightChannel[neighborArrayIndex] & ~neighborMask)
                                        | (attenuated << neighborShift);

                        chunkLights.add(
                                nx,
                                ny,
                                nz,
                                (byte) attenuated
                        );
                    }

                } else {
                    int ox = nx < 0
                            ? nx + ConstantCommonSettings.CHUNK_WIDTH
                            : (nx >= ConstantCommonSettings.CHUNK_WIDTH
                            ? nx - ConstantCommonSettings.CHUNK_WIDTH
                            : nx);

                    int oy = ny < 0
                            ? ny + ConstantCommonSettings.CHUNK_HEIGHT
                            : (ny >= ConstantCommonSettings.CHUNK_HEIGHT
                            ? ny - ConstantCommonSettings.CHUNK_HEIGHT
                            : ny);

                    int oz = nz < 0
                            ? nz + ConstantCommonSettings.CHUNK_LENGTH
                            : (nz >= ConstantCommonSettings.CHUNK_LENGTH
                            ? nz - ConstantCommonSettings.CHUNK_LENGTH
                            : nz);

                    borderLightQueues.get(direction)
                            .add(ox, oy, oz, (byte) attenuated);
                }
            }
        }
    }

    private void propagateLighting(Position3D chunkPosition, LightChannels channel, Collection<LightingChunkMeshDataTask> lightingTasks) {
        for (Direction direction : Direction.VALUES) {
            neighborMap.reset();

            Position3D directionPosition = chunkPosition.add(direction.dx, direction.dy, direction.dz);
            ClientWorldChunk neighborChunk = world.get(directionPosition, false, true);
            LightChannel neighborLightChannel = neighborChunk == null ? null : (neighborChunk.getLightingData() == null ? null : neighborChunk.getLightingData().getChannel(channel));

            LightNodeQueue overflowQueue = borderLightQueues.get(direction);

            short[] oldOverflow = neighborChunk == null ? null : neighborChunk.getNeighborLightOverflow(channel, direction.opposite());

            boolean recalculateNeighbor = false;

            while (!overflowQueue.isEmpty()) {
                overflowQueue.poll();
                int x = overflowQueue.x();
                int y = overflowQueue.y();
                int z = overflowQueue.z();
                int newLight = overflowQueue.lightLevel();

                int idx = encodeUV(x, y, z, direction);

                if (neighborLightChannel != null && !recalculateNeighbor) {
                    byte lightValue = neighborLightChannel.getLighting(getNeighborLightIndex(direction, x, y, z));
                    if (newLight > lightValue) {
                        recalculateNeighbor = true;
                    }
                }

                neighborMap.putLarger(idx, newLight);
            }

            short[] newOverflow = new short[neighborMap.size()];

            final int[] i = {0};
            neighborMap.forEach((k, v) -> newOverflow[i[0]++] = (short) (
                    (k & 0x3FF)
                            | ((v & 0xF) << 10)
            ));

            Arrays.sort(newOverflow);

            if (neighborChunk == null) {
                world.add(directionPosition, direction.opposite(), newOverflow, channel);
            } else {
                if (!Arrays.equals(oldOverflow, newOverflow)) {
                    neighborChunk.setNeighborLightOverflow(channel, direction.opposite(), newOverflow);
                    if (recalculateNeighbor && lightingTasks != null) {
                        completeDirtyChunks.add(directionPosition);
                        neighborChunk.setCleanLighting(channel, false);
                    }
                }
            }
        }
    }

    private boolean isNeighborLightingInvalid(ClientWorldChunk clientWorldChunk, LightChannels channel) {
        return clientWorldChunk == null || clientWorldChunk.getLightingData() == null || clientWorldChunk.getLightingData().getChannel(channel) == null;
    }

    private void clearQueues() {
        chunkLights.clear();
        borderLightQueues.values().forEach(LightNodeQueue::clear);
    }
}
