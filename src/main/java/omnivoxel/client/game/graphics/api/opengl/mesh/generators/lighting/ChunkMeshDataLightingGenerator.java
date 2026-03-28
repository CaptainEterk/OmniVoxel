package omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting;

import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.block.BlockMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.ChunkMeshDataTask;
import omnivoxel.client.game.graphics.light.ChunkLightingData;
import omnivoxel.client.game.graphics.light.channel.GeneralLightChannel;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.Block;
import omnivoxel.world.chunk.Chunk;

import java.util.*;

public class ChunkMeshDataLightingGenerator {
    private final Map<Direction, Queue<LightNode>> borderLightQueues = new EnumMap<>(Direction.class);
    private final Queue<LightNode> chunkLights;
    private final ClientWorld world;
    private final ClientWorldDataService worldDataService;

    public ChunkMeshDataLightingGenerator(ClientWorld world, ClientWorldDataService worldDataService) {
        this.world = world;
        this.worldDataService = worldDataService;
        this.chunkLights = new ArrayDeque<>();

        for (Direction dir : Direction.VALUES) {
            borderLightQueues.put(dir, new ArrayDeque<>());
        }
    }

    public ChunkLightingDataAndTasks generateLighting(ClientWorldChunk clientWorldChunk, Position3D chunkPos) {
        Chunk<Block> chunk = clientWorldChunk.getChunkData();
        byte[] blockLights = new byte[ConstantGameSettings.BLOCKS_IN_CHUNK];
        chunkLights.clear();
        borderLightQueues.values().forEach(Queue::clear);

        for (int x = 0; x < ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantGameSettings.CHUNK_LENGTH; z++) {
                for (int y = 0; y < ConstantGameSettings.CHUNK_HEIGHT; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    // TODO: Remove hardcoding
                    BlockMesh mesh = worldDataService.getBlock(
                            block == null ? "omnivoxel:air" : block.id()
                    );
                    if (mesh != null && "core:stone".equals(mesh.getModID())) {
                        int localIndex = IndexCalculator.calculateBlockIndex(x, y, z);
                        blockLights[localIndex] = 15;
                        chunkLights.add(new LightNode(x, y, z, (byte) 15));
                    }
                }
            }
        }

        for (Direction value : Direction.values()) {
            Map<Position3D, LightNode> neighborOverflow = world.get(chunkPos, false, false).getNeighborLightOverflowQueue(value);
            if (neighborOverflow != null) {
                chunkLights.addAll(neighborOverflow.values());
            }
        }

        while (!chunkLights.isEmpty()) {
            LightNode node = chunkLights.poll();
            int x = node.x();
            int y = node.y();
            int z = node.z();
            byte light = node.lightLevel();
            if (light <= 1) continue;

            for (Direction dir : Direction.VALUES) {
                int nx = x + dir.dx;
                int ny = y + dir.dy;
                int nz = z + dir.dz;
                byte newLight = (byte) (light - 1);

                if (!IndexCalculator.checkBounds(nx, ny, nz)) {
                    int lx = nx;
                    int ly = ny;
                    int lz = nz;

                    if (nx < 0) lx = ConstantGameSettings.CHUNK_WIDTH - 1;
                    else if (nx >= ConstantGameSettings.CHUNK_WIDTH) lx = 0;

                    if (ny < 0) ly = ConstantGameSettings.CHUNK_HEIGHT - 1;
                    else if (ny >= ConstantGameSettings.CHUNK_HEIGHT) ly = 0;

                    if (nz < 0) lz = ConstantGameSettings.CHUNK_LENGTH - 1;
                    else if (nz >= ConstantGameSettings.CHUNK_LENGTH) lz = 0;

                    borderLightQueues.get(dir).add(new LightNode(lx, ly, lz, newLight));
                    continue;
                }

                int neighborIndex = IndexCalculator.calculateBlockIndex(nx, ny, nz);
                if (newLight > blockLights[neighborIndex]) {
                    blockLights[neighborIndex] = newLight;
                    chunkLights.add(new LightNode(nx, ny, nz, newLight));
                }
            }
        }

        List<MeshDataTask> meshDataTasks = null;

        for (Direction dir : Direction.VALUES) {
            Position3D neighborPos = chunkPos.add(dir.dx, dir.dy, dir.dz);
            Queue<LightNode> overflowQueue = borderLightQueues.get(dir);

            if (overflowQueue.isEmpty()) continue;
            var neighborChunk = world.get(neighborPos, false, true);
            if (neighborChunk == null) continue;

            Map<Position3D, LightNode> neighborQueue = neighborChunk.getNeighborLightOverflowQueue(dir);
//            world.getCleanLitChunks().remove(neighborPos);

            if (meshDataTasks == null) meshDataTasks = new ArrayList<>();
            meshDataTasks.add(new ChunkMeshDataTask(null, neighborPos));

            neighborQueue.clear();
            for (LightNode node : overflowQueue) {
                Position3D localPos = convertToLocalCoordinates(node.position(), dir);
                neighborQueue.merge(localPos, new LightNode(localPos, node.lightLevel()),
                        (existing, incoming) -> existing.lightLevel() < incoming.lightLevel() ? incoming : existing);
            }
        }

        return new ChunkLightingDataAndTasks(new ChunkLightingData(null, null, null, new GeneralLightChannel(blockLights)), meshDataTasks);
    }

    /**
     * Convert overflow node position to neighbor-local coordinates
     */
    private Position3D convertToLocalCoordinates(Position3D pos, Direction dir) {
        int x = pos.x(), y = pos.y(), z = pos.z();
        int W = ConstantGameSettings.CHUNK_WIDTH;
        int H = ConstantGameSettings.CHUNK_HEIGHT;
        int L = ConstantGameSettings.CHUNK_LENGTH;

        if (dir == Direction.EAST) x = 0;
        if (dir == Direction.WEST) x = W - 1;
        if (dir == Direction.UP) y = 0;
        if (dir == Direction.DOWN) y = H - 1;
        if (dir == Direction.SOUTH) z = 0;
        if (dir == Direction.NORTH) z = L - 1;

        return new Position3D(x, y, z);
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

    public record ChunkLightingDataAndTasks(ChunkLightingData chunkLightingData, List<MeshDataTask> meshDataTasks) {
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