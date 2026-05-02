package omnivoxel.client.game.graphics.api.opengl.mesh.generators.meshShape;

import omnivoxel.client.game.graphics.api.opengl.mesh.generators.textureShape.BoxTextureShape;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.TextureVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.UniqueVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.Vertex;
import omnivoxel.common.face.BlockFace;

import java.util.List;
import java.util.Map;

public record BoxMeshShape(float x, float y, float z, float width, float height, float length,
                           BoxTextureShape textureShape) implements MeshShape {
    @Override
    public void generate(List<Float> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap) {
        float hx = width / 2f;
        float hy = height / 2f;
        float hz = length / 2f;

        BlockFace[] faces = {
                BlockFace.TOP,
                BlockFace.BOTTOM,
                BlockFace.NORTH,
                BlockFace.SOUTH,
                BlockFace.EAST,
                BlockFace.WEST
        };

        int[] quadIndices = {0, 2, 1, 0, 3, 2}; // counter-clockwise

        for (BlockFace face : faces) {
            float[][] corners = switch (face) {
                case NORTH -> new float[][]{
                        {-hx, -hy, -hz}, {hx, -hy, -hz}, {hx, hy, -hz}, {-hx, hy, -hz}
                };
                case SOUTH -> new float[][]{
                        {hx, -hy, hz}, {-hx, -hy, hz}, {-hx, hy, hz}, {hx, hy, hz}
                };
                case WEST -> new float[][]{
                        {-hx, -hy, hz}, {-hx, -hy, -hz}, {-hx, hy, -hz}, {-hx, hy, hz}
                };
                case EAST -> new float[][]{
                        {hx, -hy, -hz}, {hx, -hy, hz}, {hx, hy, hz}, {hx, hy, -hz}
                };
                case TOP -> new float[][]{
                        {-hx, hy, -hz}, {hx, hy, -hz}, {hx, hy, hz}, {-hx, hy, hz}
                };
                case BOTTOM -> new float[][]{
                        {-hx, -hy, hz}, {hx, -hy, hz}, {hx, -hy, -hz}, {-hx, -hy, -hz}
                };
                default -> new float[][]{};
            };

            for (int i : quadIndices) {
                float[] pos = corners[i];
                float[] uvs = textureShape().getCoords(face);

                Vertex vertex = new Vertex(
                        pos[0] + x,
                        pos[1] + y,
                        pos[2] + z
                );

                TextureVertex textureVertex = new TextureVertex(
                        uvs[i * 2],
                        uvs[i * 2 + 1]
                );

                UniqueVertex unique = new UniqueVertex(vertex, textureVertex, face);

                int index;
                if (vertexIndexMap.containsKey(unique)) {
                    index = vertexIndexMap.get(unique);
                } else {
                    index = vertexIndexMap.size();
                    vertexIndexMap.put(unique, index);

                    vertices.add(vertex.px());
                    vertices.add(vertex.py());
                    vertices.add(vertex.pz());
                    vertices.add(textureVertex.tx());
                    vertices.add(textureVertex.ty());
                }

                indices.add(index);
            }
        }
    }
}