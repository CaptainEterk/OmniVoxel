package omnivoxel.client.game.graphics.api.opengl.mesh.vertex;

import omnivoxel.common.annotations.NotNull;
import omnivoxel.common.face.BlockFace;

import java.util.Objects;


public class UniqueVertex {
    private final @NotNull Vertex vertex;
    private final @NotNull TextureVertex textureVertex;
    private final @NotNull BlockFace blockFace;
    private final int cachedHash;

    public UniqueVertex(@NotNull Vertex vertex, @NotNull TextureVertex textureVertex, @NotNull BlockFace blockFace) {
        this.vertex = vertex;
        this.textureVertex = textureVertex;
        this.blockFace = blockFace;
        this.cachedHash = computeHash(vertex, textureVertex, blockFace);
    }

    protected int computeHash(@NotNull Vertex vertex, @NotNull TextureVertex textureVertex, @NotNull BlockFace blockFace) {
        int result = 17;
        result = 31 * result + Float.hashCode(vertex.px());
        result = 31 * result + Float.hashCode(vertex.py());
        result = 31 * result + Float.hashCode(vertex.pz());
        result = 31 * result + Float.hashCode(textureVertex.tx());
        result = 31 * result + Float.hashCode(textureVertex.ty());
        result = 31 * result + blockFace.hashCode();
        return result;
    }

    @Override
    public int hashCode() {
        return cachedHash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UniqueVertex that = (UniqueVertex) o;
        return cachedHash == that.cachedHash && Objects.equals(vertex, that.vertex) && Objects.equals(textureVertex, that.textureVertex) && blockFace == that.blockFace;
    }

    public Vertex vertex() {
        return vertex;
    }

    public TextureVertex textureVertex() {
        return textureVertex;
    }

    public BlockFace blockFace() {
        return blockFace;
    }
}