package omnivoxel.client.game.graphics.api.opengl.mesh.vertex;

import omnivoxel.common.annotations.NotNull;
import omnivoxel.common.face.BlockFace;

public class UniqueLightVertex extends UniqueVertex {
    private final byte r;
    private final byte g;
    private final byte b;
    private final byte s;

    public UniqueLightVertex(@NotNull Vertex vertex, @NotNull TextureVertex textureVertex, @NotNull BlockFace blockFace, byte r, byte g, byte b, byte s) {
        super(vertex, textureVertex, blockFace);
        this.r = r;
        this.g = g;
        this.b = b;
        this.s = s;
    }

    @Override
    protected int computeHash(@NotNull Vertex vertex, @NotNull TextureVertex textureVertex, @NotNull BlockFace blockFace) {
        int result = super.computeHash(vertex, textureVertex, blockFace);
        result = 31 * result + Byte.hashCode(r);
        result = 31 * result + Byte.hashCode(g);
        result = 31 * result + Byte.hashCode(b);
        result = 31 * result + Byte.hashCode(s);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UniqueLightVertex that = (UniqueLightVertex) o;
        return r == that.r && g == that.g && b == that.b && s == that.s;
    }

    public byte r() {
        return r;
    }

    public byte g() {
        return g;
    }

    public byte b() {
        return b;
    }

    public byte s() {
        return s;
    }
}