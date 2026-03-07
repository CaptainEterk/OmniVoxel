package omnivoxel.util.math;

import java.util.Objects;

public record Position2D(int x, int z) {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Position2D that = (Position2D) o;
        return x() == that.x() && z() == that.z();
    }

    @Override
    public int hashCode() {
        return Objects.hash(x(), z());
    }

    public Position2D add(int x, int y, int z) {
        return new Position2D(this.x + x, this.z + z);
    }

    public Position2D add(Position3D position3D) {
        return add(position3D.x(), position3D.y(), position3D.z());
    }
}