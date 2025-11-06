package omnivoxel.client.game.graphics.opengl.mesh.generators;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.entity.ClientEntity;
import omnivoxel.client.game.graphics.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.opengl.mesh.definition.EntityMeshDataDefinition;
import omnivoxel.client.game.graphics.opengl.mesh.tasks.ChunkMeshDataTask;
import omnivoxel.client.game.graphics.opengl.mesh.tasks.ChunkRemeshDataTask;
import omnivoxel.client.game.graphics.opengl.mesh.tasks.EntityMeshDataTask;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.util.cache.IDCache;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.BlockService;

import java.util.Set;

public final class MeshDataGenerator {
    private final ChunkMeshDataGenerator chunkMeshDataGenerator;
    private final EntityMeshDataGenerator entityMeshDataGenerator;
    private final ClientWorld world;

    public MeshDataGenerator(ClientWorldDataService worldDataService, IDCache<String, EntityMeshDataDefinition> entityMeshDefinitionCache, Set<String> queuedEntityMeshData, ClientWorld world, BlockService blockService) {
        chunkMeshDataGenerator = new ChunkMeshDataGenerator(worldDataService, blockService);
        this.world = world;
        entityMeshDataGenerator = new EntityMeshDataGenerator(entityMeshDefinitionCache, queuedEntityMeshData);
    }

    public void generateMeshData(MeshDataTask meshDataTask) {
        try {
            if (meshDataTask instanceof ChunkMeshDataTask(ByteBuf blocks, Position3D position3D)) {
                world.add(position3D, chunkMeshDataGenerator.generateMeshData(blocks, position3D, world));
            } else if (meshDataTask instanceof ChunkRemeshDataTask(ChunkBlockData chunk, Position3D position3D)) {
                world.add(position3D, chunkMeshDataGenerator.generateMeshData(chunk, position3D, world));
            } else if (meshDataTask instanceof EntityMeshDataTask(ClientEntity entity)) {
                world.addEntity(entityMeshDataGenerator.generateMeshData(entity));
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        } finally {
            meshDataTask.cleanup();
        }
    }
}