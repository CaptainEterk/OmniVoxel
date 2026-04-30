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
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.thread.WorkerThreadPool;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk2d.Chunk2D;

import java.util.*;

public class ChunkMeshDataLightingGenerator {
    private final Map<LightChannels, Map<Direction, LightNodeQueue>> borderLightQueues = new EnumMap<>(LightChannels.class);
    private final LightNodeQueue chunkLights;
    private final ClientWorld world;
    private final ClientWorldDataService worldDataService;
    private final WorkerThreadPool<MeshDataTask> meshDataGenerators;
    private final BlockService<BlockWithMesh> blockService;
    private final State state;

    public ChunkMeshDataLightingGenerator(ClientWorld world, ClientWorldDataService worldDataService, WorkerThreadPool<MeshDataTask> meshDataGenerators, BlockService<BlockWithMesh> blockService, State state) {
        this.world = world;
        this.worldDataService = worldDataService;
        this.meshDataGenerators = meshDataGenerators;
        this.blockService = blockService;
        this.state = state;
        this.chunkLights = new LightNodeQueue();

        for (LightChannels channel : LightChannels.values()) {
            Map<Direction, LightNodeQueue> directionQueueMap = new EnumMap<>(Direction.class);
            for (Direction dir : Direction.VALUES) {
                directionQueueMap.put(dir, new LightNodeQueue());
            }
            borderLightQueues.put(channel, directionQueueMap);
        }
    }

    public List<LightingChunkMeshDataTask> generateLightingMeshData(LightingChunkMeshDataTask lightingChunkMeshDataTask, int queueSize) {
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
            clientWorldChunk.setChunkLightingData(generateLighting(clientWorldChunk, position3D).chunkLightingData());
            return false;
        }
        return true;
    }

    private boolean calculateChunkLighting(Position3D position3D) {
        return calculateNeighborChunkLighting(position3D);
    }

    private List<LightingChunkMeshDataTask> generateChunkMeshDataLighting(Position3D position3D) throws InterruptedException {
        ClientWorldChunk clientWorldChunk = world.get(position3D, false, false);
        if (clientWorldChunk == null) {
            return null;
        }

        List<LightingChunkMeshDataTask> meshDataTasks = new ArrayList<>();

        boolean failed = false;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (!(x == 0 && y == 0 && z == 0)) {
                        Position3D neighborPos = position3D.add(x, y, z);
                        failed = failed || calculateChunkLighting(neighborPos);
                    }
                }
            }
        }

        if (failed) {
            calculateChunkLighting(position3D);
            if (world.isChunkInflight(position3D)) {
                meshDataTasks.add(new LightingChunkMeshDataTask(null, position3D));
            }
        }

        ChunkLightingDataAndTasks chunkLightingDataAndTasks = generateLighting(clientWorldChunk, position3D);
        ChunkLightingData chunkLightingData = chunkLightingDataAndTasks.chunkLightingData();
        clientWorldChunk.setChunkLightingData(chunkLightingData);

        if (chunkLightingDataAndTasks.meshDataTasks() != null && !failed) {
            meshDataTasks.addAll(chunkLightingDataAndTasks.meshDataTasks());
        }

        meshDataGenerators.submit(new ChunkMeshDataTask(null, position3D));

        return meshDataTasks;
    }

    private ChunkLightingDataAndTasks generateLighting(ClientWorldChunk clientWorldChunk, Position3D chunkPos) {
        LightChannelAndMeshTasks redChannel = generateLightChannel(clientWorldChunk, chunkPos, LightChannels.RED);
        LightChannelAndMeshTasks greenChannel = generateLightChannel(clientWorldChunk, chunkPos, LightChannels.GREEN);
        LightChannelAndMeshTasks blueChannel = generateLightChannel(clientWorldChunk, chunkPos, LightChannels.BLUE);
        LightChannelAndMeshTasks skyChannel = generateLightChannel(clientWorldChunk, chunkPos, LightChannels.SKYLIGHT);

        List<LightingChunkMeshDataTask> meshDataTasks = null;
        if (redChannel.meshDataTasks != null) {
            meshDataTasks = new ArrayList<>(redChannel.meshDataTasks);
        }
        if (greenChannel.meshDataTasks != null) {
            if (meshDataTasks == null) {
                meshDataTasks = new ArrayList<>(greenChannel.meshDataTasks);
            } else {
                meshDataTasks.addAll(greenChannel.meshDataTasks);
            }
        }
        if (blueChannel.meshDataTasks != null) {
            if (meshDataTasks == null) {
                meshDataTasks = new ArrayList<>(blueChannel.meshDataTasks);
            } else {
                meshDataTasks.addAll(blueChannel.meshDataTasks);
            }
        }
        if (skyChannel.meshDataTasks != null) {
            if (meshDataTasks == null) {
                meshDataTasks = new ArrayList<>(skyChannel.meshDataTasks);
            } else {
                meshDataTasks.addAll(skyChannel.meshDataTasks);
            }
        }

        return new ChunkLightingDataAndTasks(new ChunkLightingData(redChannel.lightChannel, greenChannel.lightChannel, blueChannel.lightChannel, skyChannel.lightChannel), meshDataTasks);
    }

    private void loadChunkLights(LightChannels channel, Position3D chunkPos, Chunk<BlockWithMesh> chunk) {
        int chunkYOffset = chunkPos.y() * ConstantGameSettings.CHUNK_HEIGHT;

        Chunk2D<Integer> chunkHeights = channel == LightChannels.SKYLIGHT ? world.getChunkHeights(chunkPos.getPosition2D()) : null;
        if (channel == LightChannels.SKYLIGHT && chunkHeights == null) {
            Logger.error("chunkHeights is null (" + chunkPos.x() + ", " + chunkPos.z() + ")");
            return;
        }

        for (int x = 0; x < ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantGameSettings.CHUNK_LENGTH; z++) {
                for (int y = ConstantGameSettings.CHUNK_HEIGHT - 1; y >= 0; y--) {
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

            lightChannel[idx] = light;

            if (light < 1) continue;

            byte newLight = (byte) (light - chunk.getBlock(x, y, z).blockMesh().getLightDiffuse(channel));

            for (Direction direction : Direction.VALUES) {
                int nx = x + direction.dx;
                int ny = y + direction.dy;
                int nz = z + direction.dz;

                if (IndexCalculator.checkBounds(nx, ny, nz)) {
                    int nIdx = IndexCalculator.calculateBlockIndex(nx, ny, nz);
                    if (newLight > lightChannel[nIdx]) {
                        lightChannel[nIdx] = newLight;
                        chunkLights.add(nx, ny, nz, newLight);
                    }
                } else {
                    int ox = nx < 0 ? nx + ConstantGameSettings.CHUNK_WIDTH : (nx >= ConstantGameSettings.CHUNK_WIDTH ? nx - ConstantGameSettings.CHUNK_WIDTH : nx);
                    int oy = ny < 0 ? ny + ConstantGameSettings.CHUNK_HEIGHT : (ny >= ConstantGameSettings.CHUNK_HEIGHT ? ny - ConstantGameSettings.CHUNK_HEIGHT : ny);
                    int oz = nz < 0 ? nz + ConstantGameSettings.CHUNK_LENGTH : (nz >= ConstantGameSettings.CHUNK_LENGTH ? nz - ConstantGameSettings.CHUNK_LENGTH : nz);
                    borderLightQueues.get(channel).get(direction).add(ox, oy, oz, newLight);
                }
            }
        }
    }

    private List<LightingChunkMeshDataTask> propagateLighting(Position3D chunkPos, LightChannels channel) {
        List<LightingChunkMeshDataTask> meshDataTasks = null;

        final int W = ConstantGameSettings.CHUNK_WIDTH;
        final int H = ConstantGameSettings.CHUNK_HEIGHT;
        final int L = ConstantGameSettings.CHUNK_LENGTH;

        for (Direction dir : Direction.VALUES) {
            LightNodeQueue overflowQueue = borderLightQueues.get(channel).get(dir);
            if (overflowQueue == null || overflowQueue.isEmpty()) continue;

            ClientWorldChunk neighborChunk = world.get(chunkPos.add(dir.dx, dir.dy, dir.dz), false, true);
            if (neighborChunk == null) continue;

            Map<Integer, Byte> neighborMap = new HashMap<>();

            boolean changed = false;
            while (!overflowQueue.isEmpty()) {
                overflowQueue.poll();
                int x = overflowQueue.x();
                int y = overflowQueue.y();
                int z = overflowQueue.z();

                if (x < 0) x = W - 1;
                else if (x >= W) x = 0;

                if (y < 0) y = H - 1;
                else if (y >= H) y = 0;

                if (z < 0) z = L - 1;
                else if (z >= L) z = 0;

                int idx = IndexCalculator.calculateBlockIndex(x, y, z);

                byte newLight = overflowQueue.lightLevel();
                Byte oldLight = neighborMap.get(idx);

                if (oldLight == null || newLight > oldLight) {
                    neighborMap.put(idx, newLight);
                    changed = true;
                }
            }

            Map<Integer, Byte> neighborLightOverflowMap = neighborChunk.getNeighborLightOverflowMap(channel, dir);
            if (neighborLightOverflowMap.equals(neighborMap)) continue;

            neighborLightOverflowMap.clear();
            neighborLightOverflowMap.putAll(neighborMap);

            if (changed) {
                if (meshDataTasks == null) meshDataTasks = new ArrayList<>();
                meshDataTasks.add(new LightingChunkMeshDataTask(null, chunkPos.add(dir.dx, dir.dy, dir.dz)));
            }
        }

        return meshDataTasks;
    }

    private void clearQueues() {
        chunkLights.clear();
        borderLightQueues.values().forEach(c -> c.values().forEach(LightNodeQueue::clear));
    }

    // TODO: Only update changed light channels
    private LightChannelAndMeshTasks generateLightChannel(
            ClientWorldChunk clientWorldChunk,
            Position3D chunkPos,
            LightChannels channel
    ) {
        clearQueues();

        byte[] lightChannel = new byte[ConstantGameSettings.BLOCKS_IN_CHUNK];

        loadChunkLights(channel, chunkPos, clientWorldChunk.getChunkData());

        for (Direction dir : Direction.VALUES) {
            Map<Integer, Byte> neighborMap = clientWorldChunk.getNeighborLightOverflowMap(channel, dir);

            if (neighborMap.isEmpty()) continue;

            for (Map.Entry<Integer, Byte> entry : neighborMap.entrySet()) {
                int idx = entry.getKey();
                byte lvl = entry.getValue();

                int x = IndexCalculator.x(idx);
                int y = IndexCalculator.y(idx);
                int z = IndexCalculator.z(idx);

                if (lvl > lightChannel[idx]) {
                    lightChannel[idx] = lvl;
                    chunkLights.add(x, y, z, lvl);
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

    private record LightChannelAndMeshTasks(LightChannel lightChannel, List<LightingChunkMeshDataTask> meshDataTasks) {
    }

    public record ChunkLightingDataAndTasks(ChunkLightingData chunkLightingData,
                                            List<LightingChunkMeshDataTask> meshDataTasks) {
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
            if (head == tail) {
                clear();
            }
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

    public record LightNode(Position3D position, byte lightLevel) {
        public LightNode(int x, int y, int z, byte lightLevel) {
            this(new Position3D(x, y, z), lightLevel);
        }

        public int x() {
            return position.x();
        }

        public int y() {
            return position.y();
        }

        public int z() {
            return position.z();
        }
    }
}
