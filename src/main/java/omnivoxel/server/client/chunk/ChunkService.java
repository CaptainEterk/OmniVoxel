package omnivoxel.server.client.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import omnivoxel.client.game.settings.ConstantGameSettings;
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
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;

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

    private void sendChunkBytes(ChannelHandlerContext ctx, int x, int y, int z, byte[] chunk) {
        ByteBuf buffer = ctx.alloc().buffer(16 + chunk.length);
        int length = 16 + chunk.length;
        buffer.writeInt(length);
        buffer.writeInt(PackageID.CHUNK.ordinal());
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
        buffer.writeBytes(chunk);
        ctx.channel().writeAndFlush(buffer);
    }

    public List<ChunkTask> serve(ChunkTask chunkTask) {
        try {
            Position3D chunkPosition = new Position3D(chunkTask.x(), chunkTask.y(), chunkTask.z());
            byte[] chunk = world.getBytes(chunkPosition);

            if (chunk == null) {
                chunk = getChunkBytes(chunkPosition, chunkTask.serverClient());
            }

            if (chunkTask.serverClient() != null) {
                sendChunkBytes(chunkTask.serverClient().getCTX(), chunkTask.x(), chunkTask.y(), chunkTask.z(), chunk);
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
                        world.put(newChunkPosition, chunk);
                    }
                    if (chunk == null) {
                        chunk = chunkGenerator.generateChunk(newChunkPosition.x(), newChunkPosition.y(), newChunkPosition.z());
                        world.put(newChunkPosition, chunk);
                    }
                    chunks[i] = chunk;
                    i++;
                }
            }
        }

        GeneratedChunk builtChunk = new EmptyGeneratedChunk();
        for (int x = -1; x <= ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int z = -1; z <= ConstantGameSettings.CHUNK_LENGTH; z++) {
                for (int y = -1; y <= ConstantGameSettings.CHUNK_HEIGHT; y++) {
                    int cx = x < 0 ? -1 : (x == ConstantGameSettings.CHUNK_WIDTH ? 1 : 0);
                    int cy = y < 0 ? -1 : (y == ConstantGameSettings.CHUNK_HEIGHT ? 1 : 0);
                    int cz = z < 0 ? -1 : (z == ConstantGameSettings.CHUNK_LENGTH ? 1 : 0);

                    int chunkIndex = (cx + 1) * 9 + (cz + 1) * 3 + (cy + 1);
                    Chunk<ServerBlock> chunk = chunks[chunkIndex];

                    int lx = x < 0 ? ConstantGameSettings.CHUNK_WIDTH - 1 : (x == ConstantGameSettings.CHUNK_WIDTH ? 0 : x);
                    int ly = y < 0 ? ConstantGameSettings.CHUNK_HEIGHT - 1 : (y == ConstantGameSettings.CHUNK_HEIGHT ? 0 : y);
                    int lz = z < 0 ? ConstantGameSettings.CHUNK_LENGTH - 1 : (z == ConstantGameSettings.CHUNK_LENGTH ? 0 : z);

                    if ((cx != 0 || cy != 0 || cz != 0) && chunkIndex == 13) {
                        System.out.println(lx + " " + ly + " " + lz);
                    }

                    builtChunk = builtChunk.setBlock(x, y, z, chunk.getBlock(lx, ly, lz));
                }
            }
        }

        ChunkResult chunkResult = GeneratedChunk.getResult(builtChunk, client);
        return chunkResult.bytes();
    }
}