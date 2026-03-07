package omnivoxel.client.game.position;

import omnivoxel.util.math.Position3D;

import java.util.Objects;

public record DistanceChunk(int distance, Position3D pos) {
    @Override
    public boolean equals(Object o) {
        if (o instanceof Position3D position3D) {
            if (pos.equals(position3D)) {
                return true;
            }
        }
        if (o == null || getClass() != o.getClass()) return false;
        DistanceChunk that = (DistanceChunk) o;
        return distance == that.distance && Objects.equals(pos, that.pos);
    }
}