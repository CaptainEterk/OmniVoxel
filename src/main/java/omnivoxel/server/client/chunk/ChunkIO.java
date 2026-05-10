package omnivoxel.server.client.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.ConstantServerSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.result.ChunkCacheItem;
import omnivoxel.server.world.ChunkCacheHandler;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.bytes.ByteUtils;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.thread.AsyncWorkerThread;
import omnivoxel.world.chunk.BiBlockChunk;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk2d.Chunk2D;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChunkIO {
    public static final ServerBlockService BLOCK_SERVICE = new ServerBlockService();
    private static final AsyncWorkerThread<ChunkCacheItem> chunkCacheAsyncWorkerThread = new AsyncWorkerThread<>(ChunkCacheHandler::cache, false);

    public static byte[] get(Position3D position3D) throws IOException {
        Path path = Path.of(ConstantServerSettings.CHUNK_SAVE_LOCATION + position3D.getPath());
        return Files.exists(path) ? Files.readAllBytes(Path.of(ConstantServerSettings.CHUNK_SAVE_LOCATION + position3D.getPath())) : null;
    }

    public static Chunk<ServerBlock> decode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);

        try {
            short paletteCount = byteBuf.getShort(20);
            ServerBlock[] palette = new ServerBlock[paletteCount];

            int index = 22;
            for (int i = 0; i < paletteCount; i++) {
                short paletteLength = byteBuf.getShort(index);
                index += 2;

                StringBuilder blockID = new StringBuilder();
                for (int j = 0; j < paletteLength; j++) {
                    byte b = byteBuf.getByte(index++);
                    blockID.append((char) b);
                }
                palette[i] = BLOCK_SERVICE.getBlock(blockID.toString());
            }

            Chunk<ServerBlock> chunk = new BiBlockChunk<>(ServerBlock.AIR);

            int W = ConstantCommonSettings.CHUNK_WIDTH;
            int H = ConstantCommonSettings.CHUNK_HEIGHT;
            int L = ConstantCommonSettings.CHUNK_LENGTH;

            int x = 0, y = 0, z = 0;

            int totalBlocks = ConstantCommonSettings.BLOCKS_IN_CHUNK;
            for (int i = 0; i < totalBlocks; ) {
                int blockID = byteBuf.getInt(index);
                int blockCount = byteBuf.getInt(index + 4);
                index += 8;

                ServerBlock block = palette[blockID];
                for (int j = 0; j < blockCount && i + j < totalBlocks; j++) {
                    if (x < W) {
                        chunk = chunk.setBlock(x, y, z, block);
                    }

                    y++;
                    if (y >= H) {
                        y = 0;
                        z++;
                        if (z >= L) {
                            z = 0;
                            x++;
                        }
                    }
                }
                i += blockCount;
            }

            return chunk;
        } finally {
            byteBuf.release();
        }
    }

    public static byte[] encode(Chunk<ServerBlock> chunk) {
        if (chunk == null) {
            return null;
        }

        ByteBuf byteBuf = Unpooled.buffer();

        try {
            byteBuf.writeLong(0L);
            byteBuf.writeLong(0L);
            byteBuf.writeInt(0);

            Map<ServerBlock, Integer> paletteMap = new LinkedHashMap<>();
            List<ServerBlock> paletteList = new ArrayList<>();

            int totalBlocks = ConstantCommonSettings.BLOCKS_IN_CHUNK;

            for (int i = 0; i < totalBlocks; i++) {

                int x = IndexCalculator.x(i);
                int y = IndexCalculator.y(i);
                int z = IndexCalculator.z(i);

                ServerBlock block = chunk.getBlock(x, y, z);

                if (!paletteMap.containsKey(block)) {
                    paletteMap.put(block, paletteList.size());
                    paletteList.add(block);
                }
            }

            // ------------------------------------------------------------
            // Palette
            // ------------------------------------------------------------

            byteBuf.writeShort(paletteList.size());

            for (ServerBlock block : paletteList) {

                String blockID = block.id();
                byte[] idBytes = blockID.getBytes(StandardCharsets.UTF_8);

                byteBuf.writeShort(idBytes.length);
                byteBuf.writeBytes(idBytes);
            }

            // ------------------------------------------------------------
            // RLE block stream
            // ------------------------------------------------------------

            int currentPaletteID = -1;
            int runLength = 0;

            for (int i = 0; i < totalBlocks; i++) {

                int x = IndexCalculator.x(i);
                int y = IndexCalculator.y(i);
                int z = IndexCalculator.z(i);

                ServerBlock block = chunk.getBlock(x, y, z);

                int paletteID = paletteMap.get(block);

                if (paletteID == currentPaletteID) {
                    runLength++;
                    continue;
                }

                if (runLength > 0) {
                    byteBuf.writeInt(currentPaletteID);
                    byteBuf.writeInt(runLength);
                }

                currentPaletteID = paletteID;
                runLength = 1;
            }

            // flush final run
            if (runLength > 0) {
                byteBuf.writeInt(currentPaletteID);
                byteBuf.writeInt(runLength);
            }

            byte[] out = new byte[byteBuf.readableBytes()];
            byteBuf.getBytes(0, out);

            return out;

        } finally {
            byteBuf.release();
        }
    }

    public static byte[] encodeIntegerChunk2D(Chunk2D<Integer> chunk2D) {
        int size = ConstantCommonSettings.BLOCKS_IN_CHUNK_2D;
        byte[] bytes = new byte[size * Integer.BYTES];

        int bx = 0, bz = 0;

        for (int i = 0; i < size; i++) {
            int value = chunk2D.getBlock(bx, bz);

            int offset = i * Integer.BYTES;
            ByteUtils.addInt(bytes, value, offset);

            bx++;
            if (bx >= ConstantCommonSettings.CHUNK_WIDTH) {
                bx = 0;
                bz++;
            }
        }

        return bytes;
    }

    public static void write(Position3D position3D, Chunk<ServerBlock> chunk) {
        chunkCacheAsyncWorkerThread.add(new ChunkCacheItem(position3D, chunk));
    }
}