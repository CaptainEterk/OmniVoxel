package omnivoxel.common;

import omnivoxel.client.game.graphics.opengl.mesh.vertex.Vertex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public record BlockShape(String id, Vertex[][] vertices, int[][] indices, boolean[] solid) {
    public static final String DEFAULT_BLOCK_SHAPE_STRING = "omnivoxel:default_block_shape";
    public static final String EMPTY_BLOCK_SHAPE_STRING = "omnivoxel:empty_block_shape";
    // Default unit cube vertices
    private static final Vertex[][] CUBE_VERTICES = {
            // Top (+Y)
            {
                    new Vertex(0, 1, 0),
                    new Vertex(0, 1, 1),
                    new Vertex(1, 1, 1),
                    new Vertex(1, 1, 0)
            },
            // Bottom (-Y)
            {
                    new Vertex(0, 0, 0),
                    new Vertex(1, 0, 0),
                    new Vertex(1, 0, 1),
                    new Vertex(0, 0, 1)
            },
            // North (+Z)
            {
                    new Vertex(0, 0, 1),
                    new Vertex(1, 0, 1),
                    new Vertex(1, 1, 1),
                    new Vertex(0, 1, 1)
            },
            // South (-Z)
            {
                    new Vertex(0, 1, 0),
                    new Vertex(1, 1, 0),
                    new Vertex(1, 0, 0),
                    new Vertex(0, 0, 0)
            },
            // East (+X)
            {
                    new Vertex(1, 1, 0),
                    new Vertex(1, 1, 1),
                    new Vertex(1, 0, 1),
                    new Vertex(1, 0, 0)
            },
            // West (-X)
            {
                    new Vertex(0, 0, 0),
                    new Vertex(0, 0, 1),
                    new Vertex(0, 1, 1),
                    new Vertex(0, 1, 0)
            }
    };
    private static final int[][] CUBE_INDICES = {
            {0, 1, 2, 2, 3, 0},
            {0, 1, 2, 2, 3, 0},
            {0, 1, 2, 2, 3, 0},
            {0, 1, 2, 2, 3, 0},
            {0, 1, 2, 2, 3, 0},
            {0, 1, 2, 2, 3, 0},
    };
    private static final boolean[] CUBE_SOLID = {true, true, true, true, true, true};
    public static final BlockShape DEFAULT_BLOCK_SHAPE = new BlockShape(BlockShape.DEFAULT_BLOCK_SHAPE_STRING, CUBE_VERTICES, CUBE_INDICES, CUBE_SOLID);
    private static final Vertex[][] EMPTY_VERTICES = new Vertex[6][0];
    private static final int[][] EMPTY_INDICES = new int[6][0];
    private static final boolean[] EMPTY_SOLID = new boolean[6];
    public static final BlockShape EMPTY_BLOCK_SHAPE =
            new BlockShape(BlockShape.EMPTY_BLOCK_SHAPE_STRING, EMPTY_VERTICES, EMPTY_INDICES, EMPTY_SOLID);

    public byte[] getBytes() {
        // Encode key
        byte[] idBytes = id == null ? new byte[0] : id.getBytes(StandardCharsets.UTF_8);
        int idLen = idBytes.length;

        // First, compute required capacity
        int capacity = 2 + idLen; // key length + key bytes
        for (int face = 0; face < 6; face++) {
            capacity += Short.BYTES; // vertex count
            capacity += vertices[face].length * (3 * Float.BYTES); // vertex data
            capacity += Short.BYTES; // index count
            capacity += indices[face].length * Integer.BYTES; // indices
            capacity += 1; // solid flag
        }

        ByteBuffer buffer = ByteBuffer.allocate(capacity).order(ByteOrder.BIG_ENDIAN);

        // Write key
        buffer.putShort((short) idLen);
        buffer.put(idBytes);

        // Write shape data
        for (int face = 0; face < 6; face++) {
            // vertices
            buffer.putShort((short) vertices[face].length);
            for (Vertex v : vertices[face]) {
                buffer.putFloat(v.px());
                buffer.putFloat(v.py());
                buffer.putFloat(v.pz());
            }

            // indices
            buffer.putShort((short) indices[face].length);
            for (int idx : indices[face]) {
                buffer.putInt(idx);
            }

            // solid
            buffer.put((byte) (solid[face] ? 1 : 0));
        }

        return buffer.array();
    }
}