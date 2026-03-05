package omnivoxel.util.math;

public record DoublePosition3D(double x, double y, double z) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoublePosition3D that = (DoublePosition3D) o;
        return x == that.x && y == that.y && z == that.z;
    }

    public DoublePosition3D add(double x, double y, double z) {
        return new DoublePosition3D(this.x + x, this.y + y, this.z + z);
    }

    public DoublePosition3D add(DoublePosition3D position3D) {
        return add(position3D.x(), position3D.y(), position3D.z());
    }
}