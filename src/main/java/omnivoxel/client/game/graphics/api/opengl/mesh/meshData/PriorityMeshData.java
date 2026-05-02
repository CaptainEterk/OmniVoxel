package omnivoxel.client.game.graphics.api.opengl.mesh.meshData;

import omnivoxel.client.game.graphics.api.opengl.mesh.util.PriorityUtils;
import omnivoxel.common.annotations.NotNull;
import omnivoxel.util.math.Position3D;


public record PriorityMeshData(Position3D position, MeshData meshData) implements Comparable<PriorityMeshData> {
    public Double getPriority() {
        return PriorityUtils.getPriority(position);
    }

    @Override
    public int compareTo(@NotNull PriorityMeshData priorityMeshData) {
        return Double.compare(getPriority(), priorityMeshData.getPriority());
    }

    @Override
    public String toString() {
        return "PriorityMeshData[" +
                "position=" + position + ", " +
                "meshData=" + meshData + ']';
    }
}