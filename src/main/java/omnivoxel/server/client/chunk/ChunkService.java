package omnivoxel.server.client.chunk;

import omnivoxel.client.game.state.State;
import omnivoxel.common.network.NetworkService;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.server.PackageID;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.result.generated.GeneratedChunk;
import omnivoxel.server.client.chunk.result.generated.SingleBlockGeneratedChunk;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.io.chunk.ChunkIO;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.util.boundingBox.WorldBoundingBox;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.ChunkLODSampler;
import omnivoxel.world.chunk2d.Chunk2D;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ChunkService {
    private final ChunkGenerator chunkGenerator;
    private final ServerWorld world;
    private final State state;

    public ChunkService(ServerWorldDataService worldDataService, ServerBlockService blockService, ServerWorld world, Set<WorldBoundingBox> worldBoundingBoxes, State state) {
        this.state = state;
        this.chunkGenerator = new ChunkGenerator(worldDataService, blockService, world, worldBoundingBoxes);
        this.world = world;
    }

    private static GeneratedChunk createBuiltChunk(int lod, Chunk<ServerBlock>[] chunks) {
        GeneratedChunk builtChunk = new SingleBlockGeneratedChunk(ServerBlock.AIR, (byte) 0, lod);

        int size = ConstantCommonSettings.CHUNK_WIDTH >> lod;

        for (int x = -1; x <= size; x++) {
            for (int z = -1; z <= size; z++) {
                for (int y = -1; y <= size; y++) {

                    int cx = x < 0 ? -1 : (x == size ? 1 : 0);
                    int cy = y < 0 ? -1 : (y == size ? 1 : 0);
                    int cz = z < 0 ? -1 : (z == size ? 1 : 0);

                    int chunkIndex = (cx + 1) * 9 + (cz + 1) * 3 + (cy + 1);
                    Chunk<ServerBlock> chunk = chunks[chunkIndex];
                    if (chunk.getLOD() != lod) {
                        throw new IllegalStateException("You cannot mix LODs...");
                    }

                    int lx = x < 0 ? size - 1 : (x == size ? 0 : x);
                    int ly = y < 0 ? size - 1 : (y == size ? 0 : y);
                    int lz = z < 0 ? size - 1 : (z == size ? 0 : z);

                    builtChunk = builtChunk.setBlock(
                            x,
                            y,
                            z,
                            chunk.getBlock(lx, ly, lz)
                    );

                    builtChunk = builtChunk.setBlockRotation(
                            x,
                            y,
                            z,
                            chunk.getBlockRotation(lx, ly, lz)
                    );
                }
            }
        }

        return builtChunk;
    }

    public List<ChunkTask> serve(ChunkTask chunkTask, int queueSize) {
        try {
            Position3D chunkPosition = new Position3D(chunkTask.x(), chunkTask.y(), chunkTask.z());
            byte[] chunk = getChunkBytes(chunkPosition, chunkTask.lod());

            if (chunkTask.serverClient() != null && chunkTask.serverClient().isOpen()) {
                Position2D position2D = chunkPosition.getPosition2D();

                if (chunkTask.lod() == 0) {
                    Chunk2D<Integer> chunk2D = world.getStoredChunkHeights(position2D);

                    if (chunk2D == null) {
                        Logger.warn("Chunk heights are null at " + position2D + ". Rebuilding heightmap...");
                        chunk2D = chunkGenerator.getWorldDataService()
                                .getWorldGenerator()
                                .rebuildChunkHeights(world, position2D);
                    }

                    NetworkService.sendBytes2D(
                            chunkTask.serverClient().getCTX().channel(),
                            PackageID.HEIGHTS,
                            position2D.x(),
                            position2D.z(),
                            chunkTask.serverClient()::disconnect,
                            ChunkIO.encodeIntegerChunk2D(chunk2D)
                    );
                }

                NetworkService.sendBytes3D(
                        chunkTask.serverClient().getCTX().channel(),
                        PackageID.CHUNK,
                        chunkPosition.x(),
                        chunkPosition.y(),
                        chunkPosition.z(),
                        chunkTask.serverClient()::disconnect,
                        chunk
                );
            }

            state.setItem(Thread.currentThread().getName() + "_wg_queue", queueSize);

            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getChunkBytes(Position3D chunkPosition, int lod) throws IOException {
        @SuppressWarnings("unchecked")
        Chunk<ServerBlock>[] chunks = new Chunk[27];

        int i = 0;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = -1; y <= 1; y++) {
                    Position3D newChunkPosition = chunkPosition.add(x, y, z);
                    Chunk<ServerBlock> chunk = world.get(newChunkPosition);

                    if (chunk == null) {
                        chunk = ChunkIO.decode(ChunkIO.get(newChunkPosition));

                        if (chunk != null) {
                            world.put(newChunkPosition, chunk);
                        }
                    }

                    if (chunk == null || lod < chunk.getLOD()) {
                        chunk = chunkGenerator.generateChunk(newChunkPosition, lod);
                        world.put(newChunkPosition, chunk);
                    }

                    chunks[i++] = ChunkLODSampler.sample(chunk, lod);
                }
            }
        }

        return GeneratedChunk.getResult(createBuiltChunk(lod, chunks)).bytes();
    }
}
