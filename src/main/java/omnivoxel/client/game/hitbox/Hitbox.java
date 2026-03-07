package omnivoxel.client.game.hitbox;

public record Hitbox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int blocksX, int blocksY,
                     int blocksZ) {

    public boolean intersects(Hitbox other) {
        return this.maxX > other.minX && this.minX < other.maxX && this.maxY > other.minY && this.minY < other.maxY && this.maxZ > other.minZ && this.minZ < other.maxZ;
    }
}