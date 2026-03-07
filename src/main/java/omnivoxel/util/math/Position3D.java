package omnivoxel.util.math;

public record Position3D(int x, int y, int z) {
    @Override
    public int hashCode() {
        int hash = x;
        hash = 31 * hash + y;
        hash = 31 * hash + z;
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position3D that = (Position3D) o;
        return x == that.x && y == that.y && z == that.z;
    }

    public Position3D add(int x, int y, int z) {
        return new Position3D(this.x + x, this.y + y, this.z + z);
    }

    public Position3D add(Position3D position3D) {
        return add(position3D.x(), position3D.y(), position3D.z());
    }

    public String getPath() {
        return x + "_" + y + "_" + z + ".chunk";
    }

    public Position2D getPosition2D() {
        return new Position2D(x, z);
    }
}