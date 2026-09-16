package omnivoxel.server.world;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.ConstantServerSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.io.chunk.ChunkIO;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk2d.Chunk2D;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerWorld {
    private final Map<Position3D, ChunkValue> chunks;
    private final Map<Position2D, Chunk2DValue> chunkHeights;
    private int request = 0;

    public ServerWorld() {
        chunks = new ConcurrentHashMap<>();
        chunkHeights = new ConcurrentHashMap<>();
    }

    public void tick() {
        for (Map.Entry<Position3D, ChunkValue> entry : chunks.entrySet()) {
            checkOldChunk3D(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Position2D, Chunk2DValue> entry : chunkHeights.entrySet()) {
            checkOldChunk2D(entry.getKey(), entry.getValue());
        }

        request++;
    }

    private void checkOldChunk3D(Position3D position3D, ChunkValue chunkValue) {
        if (chunkValue.shouldSave(this.request)) {
            if (ChunkIO.writeChunk(position3D, chunks.get(position3D).chunk, false)) {
                chunks.remove(position3D);
            }
        }
    }

    private void checkOldChunk2D(Position2D position2D, Chunk2DValue chunkValue) {
        if (chunkValue.shouldSave(this.request)) {
            if (ChunkIO.writeChunk2D(position2D, chunkHeights.get(position2D).chunk, false)) {
                chunkHeights.remove(position2D);
            }
        }
    }

    public void put(Position3D position3D, Chunk<ServerBlock> chunk) {
        chunks.put(position3D, new ChunkValue(chunk, request));
    }

    public Chunk2D<Integer> getStoredChunkHeights(Position2D position2D) {
        try {
            Chunk2D<Integer> chunk2D = getChunkHeights(position2D);

            if (chunk2D == null) {
                chunk2D = ChunkIO.decodeChunk2D(ChunkIO.getChunk2D(position2D));
            }

            return chunk2D;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Chunk<ServerBlock> get(Position3D position3D) {
        ChunkValue chunkValue = chunks.get(position3D);
        return chunkValue == null ? null : chunkValue.get(request);
    }

    public void putChunkHeights(Position2D position2D, Chunk2D<Integer> chunkHeights) {
        this.chunkHeights.put(position2D, new Chunk2DValue(chunkHeights, request));
    }

    public Chunk2D<Integer> getChunkHeights(Position2D position2D) {
        Chunk2DValue chunk2DValue = chunkHeights.get(position2D);
        return chunk2DValue == null ? null : chunk2DValue.get(request);
    }

    public ServerBlock getBlock(Position3D chunkPosition, int x, int y, int z) {
        final int CW = ConstantCommonSettings.CHUNK_WIDTH;
        final int CH = ConstantCommonSettings.CHUNK_HEIGHT;
        final int CL = ConstantCommonSettings.CHUNK_LENGTH;

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
            return request - this.request > ConstantCommonSettings.CHUNK_TICK_TIMEOUT;
        }

        public Chunk<ServerBlock> get(int request) {
            this.request = request;
            return chunk;
        }
    }

    private static class Chunk2DValue {
        private final Chunk2D<Integer> chunk;
        private int request;

        public Chunk2DValue(Chunk2D<Integer> chunk, int request) {
            this.chunk = chunk;
            this.request = request;
        }

        public boolean shouldSave(int request) {
            return request - this.request > ConstantCommonSettings.CHUNK_TICK_TIMEOUT;
        }

        public Chunk2D<Integer> get(int request) {
            this.request = request;
            return chunk;
        }
    }
}