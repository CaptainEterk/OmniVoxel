package omnivoxel.server.client.chunk;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.network.NetworkService;
import omnivoxel.server.PackageID;
import omnivoxel.server.client.ServerClient;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.result.ChunkResult;
import omnivoxel.server.client.chunk.result.generated.EmptyGeneratedChunk;
import omnivoxel.server.client.chunk.result.generated.GeneratedChunk;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.util.boundingBox.WorldBoundingBox;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk2d.Chunk2D;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ChunkService {
    private final ChunkGenerator chunkGenerator;
    private final ServerWorld world;

    public ChunkService(ServerWorldDataService worldDataService, ServerBlockService blockService, ServerWorld world, Set<WorldBoundingBox> worldBoundingBoxes) {
        this.chunkGenerator = new ChunkGenerator(worldDataService, blockService, world, worldBoundingBoxes);
        this.world = world;
    }

    public List<ChunkTask> serve(ChunkTask chunkTask, int queueSize) {
        try {
            Position3D chunkPosition = new Position3D(chunkTask.x(), chunkTask.y(), chunkTask.z());
            byte[] chunk = getChunkBytes(chunkPosition, chunkTask.serverClient());

            if (chunkTask.serverClient() != null) {
                Position2D position2D = chunkPosition.getPosition2D();
                Chunk2D<Integer> chunk2D = world.getChunkHeights(position2D);
                if (chunk2D != null) {
                    NetworkService.sendBytes2D(chunkTask.serverClient().getCTX().channel(), PackageID.HEIGHTS, position2D.x(), position2D.z(), ChunkIO.encodeIntegerChunk2D(chunk2D));
                } else {
                    Logger.error("heights is null: " + chunkPosition);
                }

                NetworkService.sendBytes3D(chunkTask.serverClient().getCTX().channel(), PackageID.CHUNK, chunkPosition.x(), chunkPosition.y(), chunkPosition.z(), chunk);
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getChunkBytes(Position3D chunkPosition, ServerClient client) throws IOException {
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
                    if (chunk == null) {
                        chunk = chunkGenerator.generateChunk(newChunkPosition);
                        world.put(newChunkPosition, chunk);
                    }
                    chunks[i] = chunk;
                    i++;
                }
            }
        }

        GeneratedChunk builtChunk = new EmptyGeneratedChunk();
        for (int x = -1; x <= ConstantCommonSettings.CHUNK_WIDTH; x++) {
            for (int z = -1; z <= ConstantCommonSettings.CHUNK_LENGTH; z++) {
                for (int y = -1; y <= ConstantCommonSettings.CHUNK_HEIGHT; y++) {
                    int cx = x < 0 ? -1 : (x == ConstantCommonSettings.CHUNK_WIDTH ? 1 : 0);
                    int cy = y < 0 ? -1 : (y == ConstantCommonSettings.CHUNK_HEIGHT ? 1 : 0);
                    int cz = z < 0 ? -1 : (z == ConstantCommonSettings.CHUNK_LENGTH ? 1 : 0);

                    int chunkIndex = (cx + 1) * 9 + (cz + 1) * 3 + (cy + 1);
                    Chunk<ServerBlock> chunk = chunks[chunkIndex];

                    int lx = x < 0 ? ConstantCommonSettings.CHUNK_WIDTH - 1 : (x == ConstantCommonSettings.CHUNK_WIDTH ? 0 : x);
                    int ly = y < 0 ? ConstantCommonSettings.CHUNK_HEIGHT - 1 : (y == ConstantCommonSettings.CHUNK_HEIGHT ? 0 : y);
                    int lz = z < 0 ? ConstantCommonSettings.CHUNK_LENGTH - 1 : (z == ConstantCommonSettings.CHUNK_LENGTH ? 0 : z);

                    builtChunk = builtChunk.setBlock(x, y, z, chunk.getBlock(lx, ly, lz));
                }
            }
        }

        ChunkResult chunkResult = GeneratedChunk.getResult(builtChunk, client);
        return chunkResult.bytes();
    }
}