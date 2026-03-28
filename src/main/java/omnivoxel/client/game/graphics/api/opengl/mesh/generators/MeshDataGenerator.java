package omnivoxel.client.game.graphics.api.opengl.mesh.generators;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.entity.ClientEntity;
import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.ShapeHelper;
import omnivoxel.client.game.graphics.api.opengl.mesh.definition.EntityMeshDataDefinition;
import omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting.ChunkMeshDataLightingGenerator;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.ChunkMeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.EntityMeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.TextureVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.UniqueVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.Vertex;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.common.face.BlockFace;
import omnivoxel.util.cache.IDCache;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.BlockService;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MeshDataGenerator {
    private final ChunkMeshDataGenerator chunkMeshDataGenerator;
    private final EntityMeshDataGenerator entityMeshDataGenerator;
    private final ClientWorld world;

    public MeshDataGenerator(ClientWorldDataService worldDataService, IDCache<String, EntityMeshDataDefinition> entityMeshDefinitionCache, Set<String> queuedEntityMeshData, ClientWorld world, BlockService blockService) {
        chunkMeshDataGenerator = new ChunkMeshDataGenerator(worldDataService, blockService, new ChunkMeshDataLightingGenerator(world, worldDataService), world);
        this.world = world;
        entityMeshDataGenerator = new EntityMeshDataGenerator(entityMeshDefinitionCache, queuedEntityMeshData);
    }

    public static void addPoint(List<Integer> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap, Vertex position, int tx, int ty, BlockFace normal, int r, int g, int b, int s, int type) {
        UniqueVertex vertex = new UniqueVertex(position, new TextureVertex(tx, ty), normal);

        if (!vertexIndexMap.containsKey(vertex)) {
            int[] vertexData = ShapeHelper.packVertexData(position, r, g, b, s, normal, tx, ty, type);
            vertexIndexMap.put(vertex, vertices.size());
            for (int data : vertexData) {
                vertices.add(data);
            }
        }
        indices.add(vertexIndexMap.get(vertex) / 3);
    }

    public static ByteBuffer createIntBuffer(List<Integer> data) {
        if (data.isEmpty()) {
            return null;
        }
        ByteBuffer buffer = MemoryUtil.memAlloc(data.size() * Integer.BYTES);
        try {
            for (int value : data) {
                buffer.putInt(value);
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

    public List<MeshDataTask> generateMeshData(MeshDataTask meshDataTask) {
        if (meshDataTask instanceof ChunkMeshDataTask(ByteBuf byteBuf, Position3D position3D)) {
            ChunkMeshDataGenerator.MeshDataAndTasks meshDataAndTasks = chunkMeshDataGenerator.generateMeshData(byteBuf, position3D);
            if (meshDataAndTasks != null) {
                world.add(position3D, meshDataAndTasks.meshData());
                return meshDataAndTasks.meshDataTasks();
            }
            return null;
        } else if (meshDataTask instanceof EntityMeshDataTask(ClientEntity entity)) {
            world.addEntity(entityMeshDataGenerator.generateMeshData(entity));
            return null;
        } else {
            throw new IllegalArgumentException(meshDataTask + " is an invalid input. Stop playing with things you CLEARLY don't know how to use...");
        }
    }
}