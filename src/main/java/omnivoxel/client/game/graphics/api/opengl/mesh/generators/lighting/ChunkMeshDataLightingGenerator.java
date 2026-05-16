package omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting;

import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.generators.MeshDataGenerator;
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
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.thread.WorkerThreadPool;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk2d.Chunk2D;

import java.util.*;

public class ChunkMeshDataLightingGenerator {
    private final Map<Direction, LightNodeQueue> borderLightQueues = new EnumMap<>(Direction.class);
    private final LightNodeQueue chunkLights;
    private final ClientWorld world;
    private final ClientWorldDataService worldDataService;
    private final WorkerThreadPool<MeshDataTask> meshDataGenerators;
    private final BlockService<BlockWithMesh> blockService;
    private final State state;
    private final Map<Integer, Integer> neighborMap = new HashMap<>();

    public ChunkMeshDataLightingGenerator(ClientWorld world, ClientWorldDataService worldDataService, WorkerThreadPool<MeshDataTask> meshDataGenerators, BlockService<BlockWithMesh> blockService, State state) {
        this.world = world;
        this.worldDataService = worldDataService;
        this.meshDataGenerators = meshDataGenerators;
        this.blockService = blockService;
        this.state = state;
        this.chunkLights = new LightNodeQueue();

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

    private static short addLight(short uv, byte light) {
        return (short) (
                (uv & 0x03FF) |          // keep lower 10 bits (a + b)
                        ((light & 0x0F) << 10)   // pack light into upper 4 bits
        );
    }

    public Set<LightingChunkMeshDataTask> generateLightingMeshData(LightingChunkMeshDataTask lightingChunkMeshDataTask, int queueSize) {
        state.setItem(Thread.currentThread().getName() + "_queue_size_cmdlg", queueSize);
        if (lightingChunkMeshDataTask.blocks() != null) {
            MeshDataGenerator.unpackChunkPadded(lightingChunkMeshDataTask.blocks(), lightingChunkMeshDataTask.position3D(), worldDataService, blockService, world);
        }
        try {
            return generateChunkMeshDataLighting(lightingChunkMeshDataTask.position3D());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean calculateNeighborChunkLighting(Position3D position3D) {
        ClientWorldChunk clientWorldChunk = world.get(position3D, false, false);
        if (clientWorldChunk != null && clientWorldChunk.getChunkData() != null) {
            if (!clientWorldChunk.isCleanLighting()) {
                clientWorldChunk.setChunkLightingData(generateLighting(clientWorldChunk, position3D).chunkLightingData());
            }
            return false;
        }
        return true;
    }

    private Set<LightingChunkMeshDataTask> generateChunkMeshDataLighting(Position3D position3D) throws InterruptedException {
        ClientWorldChunk clientWorldChunk = world.get(position3D, false, false);
        if (clientWorldChunk == null || clientWorldChunk.isCleanLighting()) {
            return null;
        }

        Set<LightingChunkMeshDataTask> meshDataTasks = new HashSet<>();

        boolean failed = false;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (!(x == 0 && y == 0 && z == 0)) {
                        Position3D neighborPosition = position3D.add(x, y, z);
                        failed = failed || calculateNeighborChunkLighting(neighborPosition);
                    }
                }
            }
        }

        if (failed) {
            calculateNeighborChunkLighting(position3D);
            if (world.isChunkInflight(position3D)) {
                meshDataTasks.add(new LightingChunkMeshDataTask(null, position3D));
            }
        }

        ChunkLightingDataAndTasks chunkLightingDataAndTasks = generateLighting(clientWorldChunk, position3D);
        ChunkLightingData chunkLightingData = chunkLightingDataAndTasks.chunkLightingData();
        clientWorldChunk.setChunkLightingData(chunkLightingData);

        if (!failed) {
            if (chunkLightingDataAndTasks.meshDataTasks() != null) {
                meshDataTasks.addAll(chunkLightingDataAndTasks.meshDataTasks());
                clientWorldChunk.setCleanLighting(true);
            }
        }

        meshDataGenerators.submit(new ChunkMeshDataTask(null, position3D));

        return meshDataTasks;
    }

    private ChunkLightingDataAndTasks generateLighting(ClientWorldChunk clientWorldChunk, Position3D chunkPos) {
        LightChannelAndMeshTasks redChannel = generateLightChannel(clientWorldChunk, chunkPos, LightChannels.RED);
        LightChannelAndMeshTasks greenChannel = generateLightChannel(clientWorldChunk, chunkPos, LightChannels.GREEN);
        LightChannelAndMeshTasks blueChannel = generateLightChannel(clientWorldChunk, chunkPos, LightChannels.BLUE);
        LightChannelAndMeshTasks skyChannel = generateLightChannel(clientWorldChunk, chunkPos, LightChannels.SKYLIGHT);

        Set<LightingChunkMeshDataTask> meshDataTasks = null;
        if (redChannel.meshDataTasks != null) {
            meshDataTasks = redChannel.meshDataTasks;
        }
        if (greenChannel.meshDataTasks != null) {
            if (meshDataTasks == null) {
                meshDataTasks = greenChannel.meshDataTasks;
            } else {
                meshDataTasks.addAll(greenChannel.meshDataTasks);
            }
        }
        if (blueChannel.meshDataTasks != null) {
            if (meshDataTasks == null) {
                meshDataTasks = blueChannel.meshDataTasks;
            } else {
                meshDataTasks.addAll(blueChannel.meshDataTasks);
            }
        }
        if (skyChannel.meshDataTasks != null) {
            if (meshDataTasks == null) {
                meshDataTasks = skyChannel.meshDataTasks;
            } else {
                meshDataTasks.addAll(skyChannel.meshDataTasks);
            }
        }

        return new ChunkLightingDataAndTasks(new ChunkLightingData(redChannel.lightChannel, greenChannel.lightChannel, blueChannel.lightChannel, skyChannel.lightChannel), meshDataTasks);
    }

    private void loadChunkLights(LightChannels channel, Position3D chunkPos, Chunk<BlockWithMesh> chunk) {
        int chunkYOffset = chunkPos.y() * ConstantCommonSettings.CHUNK_HEIGHT;

        Chunk2D<Integer> chunkHeights = channel == LightChannels.SKYLIGHT ? world.getChunkHeights(chunkPos.getPosition2D()) : null;
        if (channel == LightChannels.SKYLIGHT && chunkHeights == null) {
            Logger.error("chunkHeights is null (" + chunkPos.x() + ", " + chunkPos.z() + ")");
            return;
        }

        for (int x = 0; x < ConstantCommonSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantCommonSettings.CHUNK_LENGTH; z++) {
                for (int y = ConstantCommonSettings.CHUNK_HEIGHT - 1; y >= 0; y--) {
                    if (channel == LightChannels.SKYLIGHT) {
                        int highestY = chunkHeights.getBlock(x, z);
                        if (chunkYOffset + y >= highestY) {
                            chunkLights.add(x, y, z, (byte) 15);
                        }
                    } else {
                        BlockMesh mesh = chunk.getBlock(x, y, z).blockMesh();

                        if (mesh != null) {
                            if (mesh.getLightEmitting(channel) > 0) {
                                chunkLights.add(x, y, z, mesh.getLightEmitting(channel));
                            }
                        }
                    }
                }
            }
        }
    }

    private void floodFill(byte[] lightChannel, Chunk<BlockWithMesh> chunk, LightChannels channel) {
        while (!chunkLights.isEmpty()) {
            chunkLights.poll();
            int x = chunkLights.x();
            int y = chunkLights.y();
            int z = chunkLights.z();
            byte light = chunkLights.lightLevel();

            int idx = IndexCalculator.calculateBlockIndex(x, y, z);

            if (light < lightChannel[idx]) {
                continue;
            }

            lightChannel[idx] = light;

            if (light < 1) continue;

            int attenuated = light - chunk.getBlock(x, y, z).blockMesh().getLightDiffuse(channel);
            if (attenuated <= 0) continue;

            for (Direction direction : Direction.VALUES) {
                int nx = x + direction.dx;
                int ny = y + direction.dy;
                int nz = z + direction.dz;

                if (IndexCalculator.checkBounds(nx, ny, nz)) {
                    int nIdx = IndexCalculator.calculateBlockIndex(nx, ny, nz);

                    if ((byte) attenuated > lightChannel[nIdx]) {
                        lightChannel[nIdx] = (byte) attenuated;
                        chunkLights.add(nx, ny, nz, (byte) attenuated);
                    }

                } else {
                    int ox = nx < 0 ? nx + ConstantCommonSettings.CHUNK_WIDTH :
                            (nx >= ConstantCommonSettings.CHUNK_WIDTH ? nx - ConstantCommonSettings.CHUNK_WIDTH : nx);

                    int oy = ny < 0 ? ny + ConstantCommonSettings.CHUNK_HEIGHT :
                            (ny >= ConstantCommonSettings.CHUNK_HEIGHT ? ny - ConstantCommonSettings.CHUNK_HEIGHT : ny);

                    int oz = nz < 0 ? nz + ConstantCommonSettings.CHUNK_LENGTH :
                            (nz >= ConstantCommonSettings.CHUNK_LENGTH ? nz - ConstantCommonSettings.CHUNK_LENGTH : nz);

                    borderLightQueues.get(direction).add(ox, oy, oz, (byte) attenuated);
                }
            }
        }
    }

    private Set<LightingChunkMeshDataTask> propagateLighting(Position3D chunkPosition, LightChannels channel) {
        Set<LightingChunkMeshDataTask> lightingTasks = new HashSet<>();

        for (Direction direction : Direction.VALUES) {
            Position3D directionPosition = chunkPosition.add(direction.dx, direction.dy, direction.dz);
            ClientWorldChunk neighborChunk = world.get(directionPosition, false, true);
            if (neighborChunk == null) continue;

            LightNodeQueue overflowQueue = borderLightQueues.get(direction);

            short[] oldOverflow = neighborChunk.getNeighborLightOverflow(channel, direction.opposite());

            neighborMap.clear();

            while (!overflowQueue.isEmpty()) {
                overflowQueue.poll();
                int x = overflowQueue.x();
                int y = overflowQueue.y();
                int z = overflowQueue.z();
                int newLight = overflowQueue.lightLevel();

                int idx = encodeUV(x, y, z, direction);

                neighborMap.merge(idx, newLight, Math::max);
            }

            short[] newOverflow = new short[neighborMap.size()];

            int i = 0;
            for (Map.Entry<Integer, Integer> entry : neighborMap.entrySet()) {
                newOverflow[i++] = (short) (
                        (entry.getKey() & 0x3FF)
                                | ((entry.getValue() & 0xF) << 10)
                );
            }

            Arrays.sort(newOverflow);

            if (!Arrays.equals(oldOverflow, newOverflow)) {
                neighborChunk.setNeighborLightOverflow(channel, direction.opposite(), newOverflow);
                neighborChunk.setCleanLighting(false);

                lightingTasks.add(new LightingChunkMeshDataTask(null, directionPosition));
            }
        }

        return lightingTasks;
    }

    private void clearQueues() {
        chunkLights.clear();
        borderLightQueues.values().forEach(LightNodeQueue::clear);
    }

    // TODO: Only update changed light channels
    private LightChannelAndMeshTasks generateLightChannel(
            ClientWorldChunk clientWorldChunk,
            Position3D chunkPos,
            LightChannels channel
    ) {
        clearQueues();

        byte[] lightChannel = new byte[ConstantCommonSettings.BLOCKS_IN_CHUNK];

        loadChunkLights(channel, chunkPos, clientWorldChunk.getChunkData());

        for (Direction dir : Direction.VALUES) {
            short[] neighbor = clientWorldChunk.getNeighborLightOverflow(channel, dir);

            for (short overflowNode : neighbor) {
                int a = (overflowNode) & 0b11111;
                int b = (overflowNode >> 5) & 0b11111;
                byte light = (byte) ((overflowNode >> 10) & 0xF);

                int x = 0, y = 0, z = 0;

                switch (dir) {
                    case UP -> {
                        x = a;
                        z = b;
                        y = 31;
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
                        z = 31;
                    }

                    case EAST -> {
                        y = a;
                        z = b;
                        x = 31;
                    }

                    case WEST -> {
                        y = a;
                        z = b;
                    }
                }

                int idx = IndexCalculator.calculateBlockIndex(x, y, z);

                if (light > lightChannel[idx]) {
                    lightChannel[idx] = light;
                    chunkLights.add(x, y, z, light);
                }
            }
        }

        if (chunkLights.isEmpty()) {
            return new LightChannelAndMeshTasks(new SingleLightChannel((byte) 0), null);
        }

        floodFill(lightChannel, clientWorldChunk.getChunkData(), channel);

        return new LightChannelAndMeshTasks(new GeneralLightChannel(lightChannel), propagateLighting(chunkPos, channel));
    }

    public enum Direction {
        UP(0, 1, 0),
        DOWN(0, -1, 0),
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1),
        EAST(1, 0, 0),
        WEST(-1, 0, 0);

        public static final Direction[] VALUES = values();
        public final int dx, dy, dz;

        Direction(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }

        public Direction opposite() {
            return switch (this) {
                case UP -> DOWN;
                case DOWN -> UP;
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case EAST -> WEST;
                case WEST -> EAST;
            };
        }
    }

    private record LightChannelAndMeshTasks(LightChannel lightChannel, Set<LightingChunkMeshDataTask> meshDataTasks) {
    }

    public record ChunkLightingDataAndTasks(ChunkLightingData chunkLightingData,
                                            Set<LightingChunkMeshDataTask> meshDataTasks) {
    }

    private static final class LightNodeQueue {
        private static final int DEFAULT_CAPACITY = 256;

        private int[] xs = new int[DEFAULT_CAPACITY];
        private int[] ys = new int[DEFAULT_CAPACITY];
        private int[] zs = new int[DEFAULT_CAPACITY];
        private byte[] lightLevels = new byte[DEFAULT_CAPACITY];
        private int head;
        private int tail;
        private int x;
        private int y;
        private int z;
        private byte lightLevel;

        public void add(int x, int y, int z, byte lightLevel) {
            ensureCapacity();
            xs[tail] = x;
            ys[tail] = y;
            zs[tail] = z;
            lightLevels[tail] = lightLevel;
            tail++;
        }

        public void poll() {
            x = xs[head];
            y = ys[head];
            z = zs[head];
            lightLevel = lightLevels[head];
            head++;
        }

        public boolean isEmpty() {
            return head == tail;
        }

        public void clear() {
            head = 0;
            tail = 0;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int z() {
            return z;
        }

        public byte lightLevel() {
            return lightLevel;
        }

        private void ensureCapacity() {
            if (tail < xs.length) {
                return;
            }

            if (head > 0) {
                int size = tail - head;
                System.arraycopy(xs, head, xs, 0, size);
                System.arraycopy(ys, head, ys, 0, size);
                System.arraycopy(zs, head, zs, 0, size);
                System.arraycopy(lightLevels, head, lightLevels, 0, size);
                head = 0;
                tail = size;
                return;
            }

            int newCapacity = xs.length << 1;
            xs = Arrays.copyOf(xs, newCapacity);
            ys = Arrays.copyOf(ys, newCapacity);
            zs = Arrays.copyOf(zs, newCapacity);
            lightLevels = Arrays.copyOf(lightLevels, newCapacity);
        }
    }
}
