package omnivoxel.client.game.graphics.opengl.mesh.vertex;

public record Vertex(float px, float py, float pz) {
    public Vertex(float px, float py, float pz) {
        this.px = px;
        this.py = py;
        this.pz = pz;
    }

    public Vertex add(float x, float y, float z) {
        return new Vertex(x + px, y + py, z + pz);
    }
}