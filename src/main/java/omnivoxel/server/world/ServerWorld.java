package omnivoxel.server.world;

import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.server.ConstantServerSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk2d.Chunk2D;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerWorld {
    private final Map<Position3D, ChunkValue> chunks;
    private final Map<Position2D, Chunk2D<Integer>> chunkHeights;
    private int request = 0;

    public ServerWorld() {
        chunks = new ConcurrentHashMap<>();
        chunkHeights = new ConcurrentHashMap<>();
    }

    public void tick() {
        for (Map.Entry<Position3D, ChunkValue> entry : chunks.entrySet()) {
            checkForOldChunks(entry.getKey(), entry.getValue());
        }

        request++;
    }

    // TODO: Remove chunkHeights too
    private void checkForOldChunks(Position3D position3D, ChunkValue chunkValue) {
        if (chunkValue.shouldSave(this.request)) {
            chunks.remove(position3D);
        }
    }

    public Chunk2D<Integer> getChunkHeights(Position2D position2D) {
        return chunkHeights.get(position2D);
    }

    public void putChunkHeights(Position2D position2D, Chunk2D<Integer> chunkHeights) {
        this.chunkHeights.put(position2D, chunkHeights);
    }

    public void put(Position3D position3D, Chunk<ServerBlock> chunk) {
        chunks.put(position3D, new ChunkValue(chunk, request));
    }

    public Chunk<ServerBlock> get(Position3D position3D) {
        ChunkValue chunkValue = chunks.get(position3D);
        return chunkValue == null ? null : chunkValue.get(request);
    }

    public ServerBlock getBlock(Position3D chunkPosition, int x, int y, int z) {
        final int CW = ConstantGameSettings.CHUNK_WIDTH;
        final int CH = ConstantGameSettings.CHUNK_HEIGHT;
        final int CL = ConstantGameSettings.CHUNK_LENGTH;

        int dx = Math.floorDiv(x, CW);
        int dy = Math.floorDiv(y, CH);
        int dz = Math.floorDiv(z, CL);

        Position3D neighborChunk = chunkPosition.add(dx, dy, dz);
        Chunk<ServerBlock> chunk = get(neighborChunk);
        if (chunk == null) return null;

        int lx = Math.floorMod(x, CW);
        int ly = Math.floorMod(y, CH);
        int lz = Math.floorMod(z, CL);

        return chunk.getBlock(lx, ly, lz);
    }

    private static class ChunkValue {
        private final Chunk<ServerBlock> chunk;
        private int request;

        public ChunkValue(Chunk<ServerBlock> chunk, int request) {
            this.chunk = chunk;
            this.request = request;
        }

        public boolean shouldSave(int request) {
            return request - this.request > ConstantServerSettings.CHUNK_TIME_LIMIT;
        }

        public Chunk<ServerBlock> get(int request) {
            this.request = request;
            return chunk;
        }
    }
}