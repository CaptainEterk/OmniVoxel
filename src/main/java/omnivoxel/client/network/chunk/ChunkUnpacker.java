package omnivoxel.client.network.chunk;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.ChunkShell;
import omnivoxel.world.chunk.SingleBlockChunk;

public class ChunkUnpacker {
    public static void unpackChunkPadded(ByteBuf byteBuf, Position3D pos,
                                         BlockService<BlockWithMesh> blockService,
                                         ClientWorld world) {
        byteBuf.readerIndex(24);
        int lod = byteBuf.readInt();
        int step = 1 << lod;

        int width = ConstantCommonSettings.CHUNK_WIDTH;
        int height = ConstantCommonSettings.CHUNK_HEIGHT;
        int length = ConstantCommonSettings.CHUNK_LENGTH;

        int paddedWidth = (width >> lod) + 2;
        int paddedHeight = (height >> lod) + 2;
        int paddedLength = (length >> lod) + 2;
        int paddedBlocks = paddedWidth * paddedHeight * paddedLength;

        BlockWithMesh[] palette = new BlockWithMesh[byteBuf.readShort()];

        for (int i = 0; i < palette.length; i++) {
            short len = byteBuf.readShort();
            StringBuilder id = new StringBuilder();

            for (int j = 0; j < len; j++) {
                id.append((char) byteBuf.readByte());
            }

            palette[i] = blockService.getBlock(id.toString());
        }

        Chunk<BlockWithMesh> center = new SingleBlockChunk<>(palette[0], lod);
        Chunk<BlockWithMesh> negX = new ChunkShell<>(lod);
        Chunk<BlockWithMesh> posX = new ChunkShell<>(lod);
        Chunk<BlockWithMesh> negY = new ChunkShell<>(lod);
        Chunk<BlockWithMesh> posY = new ChunkShell<>(lod);
        Chunk<BlockWithMesh> negZ = new ChunkShell<>(lod);
        Chunk<BlockWithMesh> posZ = new ChunkShell<>(lod);

        int fullWidth = ConstantCommonSettings.CHUNK_WIDTH;
        int fullHeight = ConstantCommonSettings.CHUNK_HEIGHT;
        int fullLength = ConstantCommonSettings.CHUNK_LENGTH;

        int shellWidth = fullWidth >> lod;
        int shellHeight = fullHeight >> lod;
        int shellLength = fullLength >> lod;

        int x = -step;
        int y = -step;
        int z = -step;

        for (int i = 0; i < paddedBlocks; ) {
            int blockID = byteBuf.readInt();
            int blockCount = byteBuf.readInt();
            byte rotation = (byte) (byteBuf.readByte() & 3);

            BlockWithMesh block = palette[blockID];

            for (int j = 0; j < blockCount; j++) {
                int lx = x >> lod;
                int ly = y >> lod;
                int lz = z >> lod;

                if (x >= 0 && x < fullWidth &&
                        y >= 0 && y < fullHeight &&
                        z >= 0 && z < fullLength) {

                    center = center.setBlock(
                            lx,
                            ly,
                            lz,
                            block,
                            rotation
                    );
                } else if (x == -step &&
                        y >= 0 && y < fullHeight &&
                        z >= 0 && z < fullLength) {

                    negX = negX.setBlock(
                            shellWidth - 1,
                            y >> lod,
                            z >> lod,
                            block,
                            rotation
                    );

                } else if (x == fullWidth &&
                        y >= 0 && y < fullHeight &&
                        z >= 0 && z < fullLength) {

                    posX = posX.setBlock(
                            0,
                            y >> lod,
                            z >> lod,
                            block,
                            rotation
                    );

                } else if (y == -step &&
                        x >= 0 && x < fullWidth &&
                        z >= 0 && z < fullLength) {

                    negY = negY.setBlock(
                            x >> lod,
                            shellHeight - 1,
                            z >> lod,
                            block,
                            rotation
                    );

                } else if (y == fullHeight &&
                        x >= 0 && x < fullWidth &&
                        z >= 0 && z < fullLength) {

                    posY = posY.setBlock(
                            x >> lod,
                            0,
                            z >> lod,
                            block,
                            rotation
                    );

                } else if (z == -step &&
                        x >= 0 && x < fullWidth &&
                        y >= 0 && y < fullHeight) {

                    negZ = negZ.setBlock(
                            x >> lod,
                            y >> lod,
                            shellLength - 1,
                            block,
                            rotation
                    );

                } else if (z == fullLength &&
                        x >= 0 && x < fullWidth &&
                        y >= 0 && y < fullHeight) {

                    posZ = posZ.setBlock(
                            x >> lod,
                            y >> lod,
                            0,
                            block,
                            rotation
                    );
                }

                y += step;

                if (y > fullHeight) {
                    y = -step;
                    z += step;

                    if (z > fullLength) {
                        z = -step;
                        x += step;
                    }
                }
            }

            i += blockCount;
        }

        byteBuf.release();

        world.addChunkData(pos, center, false);
        world.addChunkData(pos.add(-1, 0, 0), negX, true);
        world.addChunkData(pos.add(1, 0, 0), posX, true);
        world.addChunkData(pos.add(0, -1, 0), negY, true);
        world.addChunkData(pos.add(0, 1, 0), posY, true);
        world.addChunkData(pos.add(0, 0, -1), negZ, true);
        world.addChunkData(pos.add(0, 0, 1), posZ, true);
    }
}