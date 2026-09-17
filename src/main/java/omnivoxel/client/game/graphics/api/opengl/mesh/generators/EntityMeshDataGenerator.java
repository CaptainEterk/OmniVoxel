package omnivoxel.client.game.graphics.api.opengl.mesh.generators;

import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.EntityMeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.GeneralEntityMeshData;
import omnivoxel.common.entity.EntityVertex;
import omnivoxel.common.resource.GameResources;
import omnivoxel.server.entity.ServerEntityMesh;
import omnivoxel.server.entity.ServerEntityShape;
import omnivoxel.util.IntegerDynamicList;

import java.nio.ByteBuffer;
import java.util.*;

public class EntityMeshDataGenerator {
    public EntityMeshData generateMeshData(ServerEntityMesh serverEntityMesh, GameResources gameResources) {
        List<Float> vertices = new ArrayList<>();
        IntegerDynamicList indices = new IntegerDynamicList();
        Map<EntityVertex, Integer> vertexIndexMap = new HashMap<>();

        ServerEntityShape serverEntityShape = gameResources.serverEntityShapes().get(serverEntityMesh.shapeID());

        int face = 0;
        for (EntityVertex[] entityVertices : serverEntityShape.entityVertices()) {
            int[] faceIndices = serverEntityShape.indices()[face++];
            int[] vertexMap = new int[entityVertices.length];

            int i = 0;
            for (EntityVertex entityVertex : entityVertices) {
                int index;
                if (vertexIndexMap.containsKey(entityVertex)) {
                    index = vertexIndexMap.get(entityVertex);
                } else {
                    index = vertexIndexMap.size();
                    vertexIndexMap.put(entityVertex, index);
                    vertices.add(entityVertex.x());
                    vertices.add(entityVertex.y());
                    vertices.add(entityVertex.z());
                    vertices.add(entityVertex.u());
                    vertices.add(entityVertex.v());
                }

                vertexMap[i++] = index;
            }

            for (int faceIndex : faceIndices) {
                indices.add(vertexMap[faceIndex]);
            }
        }

        ByteBuffer vertexBuffer = MeshDataGenerator.createFloatBuffer(vertices);
        ByteBuffer indexBuffer = MeshDataGenerator.createIntBuffer(indices);

        EntityMeshData[] children = new EntityMeshData[serverEntityMesh.childrenIDs().length];
        for (int i = 0; i < children.length; i++) {
            children[i] = generateMeshData(gameResources.serverEntityMeshes().get(serverEntityMesh.childrenIDs()[i]), gameResources);
        }

        return new GeneralEntityMeshData(vertexBuffer, indexBuffer, children, serverEntityShape.id());
    }
}