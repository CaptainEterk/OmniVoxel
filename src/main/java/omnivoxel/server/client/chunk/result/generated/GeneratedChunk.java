package omnivoxel.server.client.chunk.result.generated;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.annotations.NotNull;
import omnivoxel.common.network.NetworkService;
import omnivoxel.server.BlockIDCount;
import omnivoxel.server.PackageID;
import omnivoxel.server.client.ServerClient;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.result.ChunkResult;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.SingleBlockChunk;

import java.util.ArrayList;
import java.util.List;

public abstract class GeneratedChunk {
    private static ChunkResult emptyChunk = null;

    // TODO: Move this to something similar to ChunkIO
    public static ChunkResult getResult(GeneratedChunk generatedChunk, ServerClient client) {
        if (generatedChunk instanceof EmptyGeneratedChunk && client != null) {
            if (emptyChunk == null) {
                emptyChunk = getResult(new EmptyGeneratedChunk(), null);
            }
            return emptyChunk;
        }

        Chunk<ServerBlock> chunkOut = new SingleBlockChunk<>(ServerBlock.AIR);
        List<ServerBlock> palette = new ArrayList<>();
        int[] chunk = new int[ConstantCommonSettings.BLOCKS_IN_CHUNK_PADDED];
        int chunkByteOffset = 0;
        for (int x = -1; x < ConstantCommonSettings.CHUNK_WIDTH + 1; x++) {
            for (int z = -1; z < ConstantCommonSettings.CHUNK_LENGTH + 1; z++) {
                for (int y = -1; y < ConstantCommonSettings.CHUNK_HEIGHT + 1; y++) {
                    ServerBlock block = generatedChunk.getBlock(x, y, z);
                    if (!palette.contains(block)) {
                        palette.add(block);
                    }
                    if (x > 0 && x < ConstantCommonSettings.CHUNK_WIDTH &&
                            y > 0 && y < ConstantCommonSettings.CHUNK_HEIGHT &&
                            z > 0 && z < ConstantCommonSettings.CHUNK_LENGTH) {
                        chunkOut = chunkOut.setBlock(x, y, z, block);
                    }
                    chunk[chunkByteOffset] = palette.indexOf(block);
                    chunkByteOffset++;
                }
            }
        }

        if (client != null) {
            // TODO: This shouldn't handle anything server/client related
            palette.forEach(serverBlock -> {
                if (client.registerBlockID(serverBlock.id())) {
                    NetworkService.sendBytes(client.getCTX().channel(), PackageID.REGISTER_BLOCK, null, serverBlock.getBytes());
                }
            });
        }

        List<BlockIDCount> chunkData = new ArrayList<>();
        int count = 0;
        int currentID = 0;
        for (int id : chunk) {
            if (currentID != id) {
                chunkData.add(new BlockIDCount(currentID, count));
                currentID = id;
                count = 1;
            } else {
                count++;
            }
        }
        chunkData.add(new BlockIDCount(currentID, count));

        byte[] chunkBytes = new byte[chunkData.size() * 8];
        for (int i = 0; i < chunkData.size(); i++) {
            BlockIDCount blockIDCount = chunkData.get(i);
            chunkBytes[i * 8] = (byte) (blockIDCount.blockID() >> 24);
            chunkBytes[i * 8 + 1] = (byte) (blockIDCount.blockID() >> 16);
            chunkBytes[i * 8 + 2] = (byte) (blockIDCount.blockID() >> 8);
            chunkBytes[i * 8 + 3] = (byte) (blockIDCount.blockID());
            chunkBytes[i * 8 + 4] = (byte) (blockIDCount.count() >> 24);
            chunkBytes[i * 8 + 5] = (byte) (blockIDCount.count() >> 16);
            chunkBytes[i * 8 + 6] = (byte) (blockIDCount.count() >> 8);
            chunkBytes[i * 8 + 7] = (byte) (blockIDCount.count());
        }

        List<byte[]> paletteBytesList = new ArrayList<>();
        int paletteLength = 0;
        for (ServerBlock block : palette) {
            byte[] blockBytes = block.getBlockBytes();
            byte[] bytes = new byte[blockBytes.length];
            System.arraycopy(blockBytes, 0, bytes, 0, blockBytes.length);
            paletteBytesList.add(bytes);
            paletteLength += bytes.length;
        }

        byte[] paletteBytes = new byte[2 + paletteLength];
        paletteBytes[0] = (byte) (palette.size() >> 8);
        paletteBytes[1] = (byte) palette.size();
        int paletteIndex = 2;
        for (byte[] paletteBites : paletteBytesList) {
            System.arraycopy(paletteBites, 0, paletteBytes, paletteIndex, paletteBites.length);
            paletteIndex += paletteBites.length;
        }

        byte[] out = new byte[chunkBytes.length + paletteBytes.length];
        System.arraycopy(paletteBytes, 0, out, 0, paletteBytes.length);
        System.arraycopy(chunkBytes, 0, out, paletteBytes.length, chunkBytes.length);

        return new ChunkResult(out, chunkOut);
    }

    abstract public ServerBlock getBlock(int x, int y, int z);

    abstract public GeneratedChunk setBlock(int x, int y, int z, @NotNull ServerBlock block);
}