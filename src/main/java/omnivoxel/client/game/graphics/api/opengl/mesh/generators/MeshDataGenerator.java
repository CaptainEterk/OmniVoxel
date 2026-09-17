package omnivoxel.client.game.graphics.api.opengl.mesh.generators;

import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.ShapeHelper;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.ChunkMeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.EntityMeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.TextureVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.UniqueLightVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.UniqueVertex;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.common.block.shape.BlockVertex;
import omnivoxel.common.face.BlockFace;
import omnivoxel.common.resource.GameResources;
import omnivoxel.common.settings.Settings;
import omnivoxel.server.entity.ServerEntityMesh;
import omnivoxel.util.IntegerDynamicList;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position3D;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public final class MeshDataGenerator {
    private final ChunkMeshDataGenerator chunkMeshDataGenerator;
    private final EntityMeshDataGenerator entityMeshDataGenerator;
    private final ClientWorld world;
    private final State state;

    public MeshDataGenerator(ClientWorld world, State state, Settings settings) {
        this.state = state;
        chunkMeshDataGenerator = new ChunkMeshDataGenerator(world, settings);
        this.world = world;
        entityMeshDataGenerator = new EntityMeshDataGenerator();
    }

    public static void addPoint(IntegerDynamicList vertices, IntegerDynamicList indices, Map<UniqueVertex, Integer> vertexIndexMap, BlockVertex position, int tx, int ty, BlockFace normal, byte r, byte g, byte b, byte s, boolean loose, int type, int[] vertexData) {
        UniqueVertex vertex = new UniqueLightVertex(position, new TextureVertex(tx, ty), normal, r, g, b, s);

        if (!vertexIndexMap.containsKey(vertex)) {
            ShapeHelper.packVertexData(position, r, g, b, s, normal.ordinal(), tx, ty, loose, type, vertexData);
            vertexIndexMap.put(vertex, vertices.size());
            for (int data : vertexData) {
                vertices.add(data);
            }
        }
        indices.add(vertexIndexMap.get(vertex) / 3);
    }

    public static ByteBuffer createIntBuffer(IntegerDynamicList data) {
        if (data.size() == 0) {
            return null;
        }
        ByteBuffer buffer = MemoryUtil.memAlloc(data.size() * Integer.BYTES);
        try {
            for (int i = 0; i < data.size(); i++) {
                buffer.putInt(data.get(i));
            }
            buffer.flip();
            return buffer;
        } catch (Exception e) {
            MemoryUtil.memFree(buffer);
            throw new RuntimeException("Error creating buffer", e);
        }
    }

    public static ByteBuffer createFloatBuffer(List<Float> data) {
        if (data.isEmpty()) {
            return null;
        }
        ByteBuffer buffer = MemoryUtil.memAlloc(data.size() * Float.BYTES);
        try {
            for (float value : data) {
                buffer.putFloat(value);
            }
            buffer.flip();
            return buffer;
        } catch (Exception e) {
            MemoryUtil.memFree(buffer);
            throw new RuntimeException("Error creating buffer", e);
        }
    }

    public List<MeshDataTask> generateMeshData(MeshDataTask meshDataTask, int queueSize) {
        state.setItem(Thread.currentThread().getName() + "_queue_size_mdg", queueSize);
        if (meshDataTask instanceof ChunkMeshDataTask(Position3D position3D)) {
            MeshData meshData = chunkMeshDataGenerator.generateMeshData(position3D);
            if (meshData != null) {
                world.add(position3D, meshData);
            } else {
                Logger.warn("Mesh data generation failed..." + position3D);
            }
        } else if (meshDataTask instanceof EntityMeshDataTask(
                ServerEntityMesh serverEntityMesh, GameResources gameResources
        )) {
            world.addEntity(entityMeshDataGenerator.generateMeshData(serverEntityMesh, gameResources));
        } else {
            throw new IllegalArgumentException(meshDataTask + " is an invalid input.");
        }
        return null;
    }
}
