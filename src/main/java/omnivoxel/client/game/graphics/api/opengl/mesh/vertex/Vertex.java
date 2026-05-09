package omnivoxel.client.game.graphics.api.opengl.mesh.vertex;

public record Vertex(float px, float py, float pz) {
    public Vertex add(float x, float y, float z) {
        return new Vertex(x + px, y + py, z + pz);
    }
}