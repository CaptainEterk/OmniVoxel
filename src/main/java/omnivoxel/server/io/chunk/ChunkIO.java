package omnivoxel.server.io.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.ConstantServerSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.io.CacheIO;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.bytes.ByteUtils;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.BiBlockChunk;
import omnivoxel.world.chunk.BytePaletteChunk;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.SingleBlockChunk;
import omnivoxel.world.chunk2d.Chunk2D;
import omnivoxel.world.chunk2d.SingleBlockChunk2D;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChunkIO {
    public static final ServerBlockService BLOCK_SERVICE = new ServerBlockService();
    private static final int ROTATION_CHUNK_MAGIC = 0x4F565231;

    public static byte[] get(Position3D position3D) throws IOException {
        Path path = Path.of(ConstantServerSettings.CHUNK_SAVE_LOCATION + position3D.getPath());
        return Files.exists(path) ? Files.readAllBytes(Path.of(ConstantServerSettings.CHUNK_SAVE_LOCATION + position3D.getPath())) : null;
    }

    public static Chunk2D<Integer> decodeChunk2D(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);

        try {
            Chunk2D<Integer> chunk2D = new SingleBlockChunk2D<>(0);

            int index = 0;

            for (int z = 0; z < ConstantCommonSettings.CHUNK_LENGTH; z++) {
                for (int x = 0; x < ConstantCommonSettings.CHUNK_WIDTH; x++) {

                    int value = byteBuf.getInt(index);
                    index += Integer.BYTES;

                    chunk2D = chunk2D.setBlock(x, z, value);
                }
            }

            return chunk2D;

        } finally {
            byteBuf.release();
        }
    }

    public static byte[] getChunk2D(Position2D position2D) throws IOException {
        Path path = Path.of(ConstantServerSettings.CHUNK_SAVE_LOCATION + position2D.getPath());

        return Files.exists(path)
                ? Files.readAllBytes(path)
                : null;
    }

    public static Chunk<ServerBlock> decode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);

        try {
            boolean hasRotations = byteBuf.readInt() == ROTATION_CHUNK_MAGIC;
            int lod = byteBuf.readInt();
            short paletteCount = byteBuf.readShort();
            ServerBlock[] palette = new ServerBlock[paletteCount];

            for (int i = 0; i < paletteCount; i++) {
                short paletteLength = byteBuf.readShort();

                StringBuilder blockID = new StringBuilder();
                for (int j = 0; j < paletteLength; j++) {
                    byte b = byteBuf.readByte();
                    blockID.append((char) b);
                }
                palette[i] = BLOCK_SERVICE.getBlock(blockID.toString());
            }

            Chunk<ServerBlock> chunk;
            if (palette.length == 1) {
                chunk = new SingleBlockChunk<>(palette[0], lod);
            } else {
                Chunk<ServerBlock> result;
                if (palette.length == 2) {
                    result = new BiBlockChunk<>(palette[0], lod);
                } else {
                    result = new BytePaletteChunk<>(lod);
                }
                chunk = result;

                int W = ConstantCommonSettings.CHUNK_WIDTH >> lod;
                int H = ConstantCommonSettings.CHUNK_HEIGHT >> lod;
                int L = ConstantCommonSettings.CHUNK_LENGTH >> lod;

                int x = 0, y = 0, z = 0;

                int totalBlocks = ConstantCommonSettings.BLOCKS_IN_CHUNK >> (lod * 3);
                for (int i = 0; i < totalBlocks; ) {
                    int blockID = byteBuf.readInt();
                    int blockCount = byteBuf.readInt();
                    byte rotation = hasRotations ? (byte) (byteBuf.readByte() & 3) : 0;

                    ServerBlock block = palette[blockID];
                    for (int j = 0; j < blockCount && i + j < totalBlocks; j++) {
                        if (x < W) {
                            chunk = chunk.setBlock(x, y, z, block, rotation);
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
            byteBuf.writeInt(ROTATION_CHUNK_MAGIC);
            byteBuf.writeInt(chunk.getLOD());

            Map<ServerBlock, Integer> paletteMap = new LinkedHashMap<>();
            List<ServerBlock> paletteList = new ArrayList<>();

            int totalBlocks = ConstantCommonSettings.BLOCKS_IN_CHUNK >> (chunk.getLOD() * 3);

            for (int i = 0; i < totalBlocks; i++) {
                int x = IndexCalculator.x(i, chunk.getLOD());
                int y = IndexCalculator.y(i, chunk.getLOD());
                int z = IndexCalculator.z(i, chunk.getLOD());

                ServerBlock block = chunk.getBlock(x, y, z);

                if (!paletteMap.containsKey(block)) {
                    paletteMap.put(block, paletteList.size());
                    paletteList.add(block);
                }
            }

            byteBuf.writeShort(paletteList.size());

            for (ServerBlock block : paletteList) {
                String blockID = block.id();
                byte[] idBytes = blockID.getBytes(StandardCharsets.UTF_8);

                byteBuf.writeShort(idBytes.length);
                byteBuf.writeBytes(idBytes);
            }

            int currentPaletteID = -1;
            byte currentRotation = 0;
            int runLength = 0;

            for (int i = 0; i < totalBlocks; i++) {
                int x = IndexCalculator.x(i, chunk.getLOD());
                int y = IndexCalculator.y(i, chunk.getLOD());
                int z = IndexCalculator.z(i, chunk.getLOD());

                ServerBlock block = chunk.getBlock(x, y, z);

                int paletteID = paletteMap.get(block);
                byte rotation = chunk.getBlockRotation(x, y, z);

                if (paletteID == currentPaletteID && rotation == currentRotation) {
                    runLength++;
                    continue;
                }

                if (runLength > 0) {
                    byteBuf.writeInt(currentPaletteID);
                    byteBuf.writeInt(runLength);
                    byteBuf.writeByte(currentRotation);
                }

                currentPaletteID = paletteID;
                currentRotation = rotation;
                runLength = 1;
            }

            if (runLength > 0) {
                byteBuf.writeInt(currentPaletteID);
                byteBuf.writeInt(runLength);
                byteBuf.writeByte(currentRotation);
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

    public static boolean writeChunk(Position3D position3D, Chunk<ServerBlock> chunk, boolean necessary) {
        return CacheIO.add(new Chunk3DCacheItem(position3D, chunk), necessary);
    }

    public static boolean writeChunk2D(Position2D position2D, Chunk2D<Integer> chunk, boolean necessary) {
        return CacheIO.add(new Chunk2DCacheItem(position2D, chunk), necessary);
    }
}
