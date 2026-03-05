package omnivoxel.util.boundingBox;

public record WorldBoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public boolean intersects(WorldBoundingBox other) {
        return this.maxX >= other.minX && this.minX <= other.maxX &&
                this.maxY >= other.minY && this.minY <= other.maxY &&
                this.maxZ >= other.minZ && this.minZ <= other.maxZ;
    }
}