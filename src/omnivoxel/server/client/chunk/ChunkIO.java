package omnivoxel.server.client.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.server.ConstantServerSettings;
import omnivoxel.server.PackageID;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.BiBlockChunk;
import omnivoxel.world.chunk.Chunk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChunkIO {
    public static final ServerBlockService BLOCK_SERVICE = new ServerBlockService();

    public static void sendBlock(ChannelHandlerContext ctx, ServerBlock block) {
        ByteBuf buffer = Unpooled.buffer();
        byte[] bytes = block.getBytes();
        buffer.writeInt(4 + bytes.length);
        buffer.writeInt(PackageID.REGISTER_BLOCK.ordinal());
        buffer.writeBytes(bytes);
        ctx.channel().writeAndFlush(buffer);
    }

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

            int W = ConstantGameSettings.CHUNK_WIDTH;
            int H = ConstantGameSettings.CHUNK_HEIGHT;
            int L = ConstantGameSettings.CHUNK_LENGTH;

            int x = 0, y = 0, z = 0;

            int totalBlocks = ConstantGameSettings.BLOCKS_IN_CHUNK;
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
}