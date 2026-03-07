package omnivoxel.client.game.graphics.opengl.mesh.generators.meshShape;

import omnivoxel.client.game.graphics.opengl.mesh.vertex.UniqueVertex;

import java.util.List;
import java.util.Map;

public interface MeshShape {
    void generate(List<Float> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap);
}