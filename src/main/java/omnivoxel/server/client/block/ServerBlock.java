package omnivoxel.server.client.block;

import omnivoxel.common.BlockShape;
import omnivoxel.common.annotations.NotNull;
import omnivoxel.server.client.ServerItem;

import java.nio.ByteBuffer;
import java.util.Objects;

public record ServerBlock(
        String id,
        String blockShape,
        double[][] uvCoords,
        boolean transparent,
        boolean transparentMesh
) implements ServerItem {
    public static final double[][] emptyUVCoords = new double[6][0];

    public static final ServerBlock VOID = new ServerBlock("omnivoxel:void/default", BlockShape.EMPTY_BLOCK_SHAPE_STRING, emptyUVCoords, true, false);
    // TODO: Don't hardcore omnivoxel:air/default
    public static final ServerBlock AIR = new ServerBlock("omnivoxel:air/default", BlockShape.EMPTY_BLOCK_SHAPE_STRING, emptyUVCoords, true, false);

    public ServerBlock {
        if (uvCoords.length != 6) {
            throw new IllegalArgumentException("uvCoords must have 6 faces");
        }
    }

    public static String createID(String id, String blockState) {
        return id + "/" + blockState;
    }

    @Override
    public byte @NotNull [] getBytes() {
        byte[] idBytes = id.getBytes();
        byte[] shapeBytes = blockShape == null ? new byte[0] : blockShape.getBytes();

        int uvCoordByteCount = 0;
        for (double[] uvCoords : this.uvCoords) {
            uvCoordByteCount += 2;
            uvCoordByteCount += uvCoords.length * Double.BYTES;
        }

        int size = 2 + idBytes.length
                + 2 + shapeBytes.length
                + 1 + 1
                + uvCoordByteCount;

        ByteBuffer buffer = ByteBuffer.allocate(size);

        buffer.putShort((short) idBytes.length);
        buffer.put(idBytes);

        buffer.putShort((short) shapeBytes.length);
        buffer.put(shapeBytes);

        buffer.put((byte) (transparent ? 1 : 0));

        buffer.put((byte) (transparentMesh ? 1 : 0));

        for (double[] uvCoords : this.uvCoords) {
            buffer.putShort((short) uvCoords.length);
            for (double uv : uvCoords) {
                buffer.putDouble(uv);
            }
        }

        return buffer.array();
    }

    public byte[] getBlockBytes() {
        byte[] idBytes = id.getBytes();

        int size = 2 + idBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(size);

        buffer.putShort((short) idBytes.length);
        buffer.put(idBytes);

        return buffer.array();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ServerBlock that = (ServerBlock) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}