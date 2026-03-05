package omnivoxel.client.network.util;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.graphics.opengl.mesh.block.Block;
import omnivoxel.client.game.graphics.opengl.mesh.vertex.Vertex;
import omnivoxel.common.BlockShape;
import omnivoxel.common.face.BlockFace;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ByteBufUtils {
    private static final Map<String, BlockShape> shapeCache = new HashMap<>();

    public static void cacheBlockShapeFromByteBuf(ByteBuf byteBuf) {
        byteBuf.skipBytes(8);
        int idLen = byteBuf.readUnsignedShort();
        byte[] idBytes = new byte[idLen];
        byteBuf.readBytes(idBytes);
        String id = new String(idBytes, StandardCharsets.UTF_8);

        Vertex[][] vertices = new Vertex[6][];
        int[][] indices = new int[6][];
        boolean[] solid = new boolean[6];

        for (int face = 0; face < 6; face++) {
            int vCount = byteBuf.readUnsignedShort();
            Vertex[] verts = new Vertex[vCount];
            for (int i = 0; i < vCount; i++) {
                float x = byteBuf.readFloat();
                float y = byteBuf.readFloat();
                float z = byteBuf.readFloat();
                verts[i] = new Vertex(x, y, z);
            }
            vertices[face] = verts;

            int iCount = byteBuf.readUnsignedShort();
            int[] idx = new int[iCount];
            for (int i = 0; i < iCount; i++) {
                idx[i] = byteBuf.readInt();
            }
            indices[face] = idx;

            solid[face] = byteBuf.readByte() != 0;
        }

        BlockShape blockShape = new BlockShape(id, vertices, indices, solid);

        shapeCache.put(id, blockShape);
    }

    public static Block registerBlockFromByteBuf(ByteBuf byteBuf) {
        int readerIndex = 8;

        int idLength = byteBuf.getShort(readerIndex);
        readerIndex += 2;

        byte[] idBytes = new byte[idLength];
        byteBuf.getBytes(readerIndex, idBytes);
        readerIndex += idLength;

        String blockIDState = new String(idBytes);

        String[] ids = blockIDState.split("/");
        String modID = ids[0];

        final String blockID = modID.contains(":") ? modID.split(":", 2)[1] : modID;

        int shapeIDLength = byteBuf.getShort(readerIndex);
        readerIndex += 2;

        byte[] shapeIDBytes = new byte[shapeIDLength];
        byteBuf.getBytes(readerIndex, shapeIDBytes);
        readerIndex += shapeIDLength;

        final String shapeID = new String(shapeIDBytes);
        final BlockShape blockShape = shapeCache.getOrDefault(shapeID, BlockShape.DEFAULT_BLOCK_SHAPE);

        boolean transparent = byteBuf.getByte(readerIndex++) == 1;
        boolean transparentMesh = byteBuf.getByte(readerIndex++) == 1;

        int[][] allUVCoords = new int[6][];
        for (int f = 0; f < 6; f++) {
            short uvCoordCount = byteBuf.getShort(readerIndex);
            readerIndex += 2;
            int[] uvCoords = new int[uvCoordCount];
            for (int i = 0; i < uvCoordCount; i++) {
                uvCoords[i] = (int) byteBuf.getDouble(readerIndex);
                readerIndex += Double.BYTES;
            }
            allUVCoords[f] = uvCoords;
        }

        shapeCache.put(blockShape.id(), blockShape);

        return new Block(ids[1]) {
            @Override
            public String getID() {
                return blockID;
            }

            @Override
            public String getModID() {
                return modID;
            }

            @Override
            public BlockShape getShape(Block top, Block bottom, Block north, Block south, Block east, Block west) {
                return blockShape;
            }

            @Override
            public boolean shouldRenderFace(BlockFace face, Block adjacentBlock) {
                return !modID.equals("omnivoxel:air") && !adjacentBlock.getModID().equals(modID) && adjacentBlock.isTransparent();
            }

            @Override
            public int[] getUVCoordinates(BlockFace blockFace) {
                return allUVCoords[blockFace.ordinal()];
            }

            @Override
            public boolean isTransparent() {
                return transparent;
            }

            @Override
            public boolean shouldRenderTransparentMesh() {
                return transparentMesh;
            }
        };
    }
}