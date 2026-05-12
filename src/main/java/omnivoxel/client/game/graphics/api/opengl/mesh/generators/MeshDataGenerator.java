package omnivoxel.client.game.graphics.api.opengl.mesh.generators;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.entity.ClientEntity;
import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.ShapeHelper;
import omnivoxel.client.game.graphics.api.opengl.mesh.definition.EntityMeshDataDefinition;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.ChunkMeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.tasks.EntityMeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.TextureVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.UniqueLightVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.UniqueVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.Vertex;
import omnivoxel.client.game.graphics.block.BlockMesh;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.common.face.BlockFace;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.cache.IDCache;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.Block;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.ChunkShell;
import omnivoxel.world.chunk.SingleBlockChunk;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MeshDataGenerator {
    private static final BlockMesh[] blockMeshes = new BlockMesh[ConstantCommonSettings.BLOCKS_IN_CHUNK_PADDED];
    private static BlockWithMesh AIR = null;
    private final ChunkMeshDataGenerator chunkMeshDataGenerator;
    private final EntityMeshDataGenerator entityMeshDataGenerator;
    private final ClientWorld world;
    private final State state;

    public MeshDataGenerator(ClientWorldDataService worldDataService, IDCache<String, EntityMeshDataDefinition> entityMeshDefinitionCache, Set<String> queuedEntityMeshData, ClientWorld world, BlockService<BlockWithMesh> blockService, State state) {
        this.state = state;
        chunkMeshDataGenerator = new ChunkMeshDataGenerator(worldDataService, blockService, world);
        this.world = world;
        entityMeshDataGenerator = new EntityMeshDataGenerator(entityMeshDefinitionCache, queuedEntityMeshData);
    }

    public static void addPoint(List<Integer> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap, Vertex position, int tx, int ty, BlockFace normal, byte r, byte g, byte b, byte s, int type) {
        UniqueVertex vertex = new UniqueLightVertex(position, new TextureVertex(tx, ty), normal, r, g, b, s);

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

    public static BlockMesh[] unpackChunkPadded(ByteBuf byteBuf, Position3D pos, ClientWorldDataService worldDataService, BlockService<BlockWithMesh> blockService, ClientWorld world) {
        if (AIR == null) {
            AIR = new BlockWithMesh("omnivoxel:air", worldDataService.getBlock("omnivoxel:air"));
        }

        Block[] palette = new Block[byteBuf.getShort(24)];

        int index = 26;

        for (int i = 0; i < palette.length; i++) {
            short len = byteBuf.getShort(index);
            StringBuilder id = new StringBuilder();

            for (int j = 0; j < len; j++) {
                id.append((char) byteBuf.getByte(index + 2 + j));
            }

            palette[i] = new Block(id.toString());
            index += 2 + len;
        }

        Chunk<BlockWithMesh> center = new SingleBlockChunk<>(AIR);
        Chunk<BlockWithMesh> negX = new ChunkShell<>();
        Chunk<BlockWithMesh> posX = new ChunkShell<>();
        Chunk<BlockWithMesh> negY = new ChunkShell<>();
        Chunk<BlockWithMesh> posY = new ChunkShell<>();
        Chunk<BlockWithMesh> negZ = new ChunkShell<>();
        Chunk<BlockWithMesh> posZ = new ChunkShell<>();

        int x = -1, y = -1, z = -1;

        for (int i = 0; i < ConstantCommonSettings.BLOCKS_IN_CHUNK_PADDED; ) {
            int blockID = byteBuf.getInt(index);
            int blockCount = byteBuf.getInt(index + 4);
            index += 8;

            BlockMesh blockMesh = worldDataService.getBlock(palette[blockID].id());

            for (int j = 0; j < blockCount; j++) {
                int paddedIndex = i + j;
                blockMeshes[paddedIndex] = blockMesh;

                if (x >= 0 && x < ConstantCommonSettings.CHUNK_WIDTH &&
                        y >= 0 && y < ConstantCommonSettings.CHUNK_HEIGHT &&
                        z >= 0 && z < ConstantCommonSettings.CHUNK_LENGTH) {

                    center = center.setBlock(x, y, z,
                            blockService.getBlock(palette[blockID].id()));
                } else if (x == -1 && y >= 0 && y < ConstantCommonSettings.CHUNK_HEIGHT &&
                        z >= 0 && z < ConstantCommonSettings.CHUNK_LENGTH) {

                    negX = negX.setBlock(
                            ConstantCommonSettings.CHUNK_WIDTH - 1,
                            y,
                            z,
                            blockService.getBlock(palette[blockID].id()));
                } else if (x == ConstantCommonSettings.CHUNK_WIDTH &&
                        y >= 0 && y < ConstantCommonSettings.CHUNK_HEIGHT &&
                        z >= 0 && z < ConstantCommonSettings.CHUNK_LENGTH) {

                    posX = posX.setBlock(
                            0,
                            y,
                            z,
                            blockService.getBlock(palette[blockID].id()));
                } else if (z == -1 && x >= 0 && x < ConstantCommonSettings.CHUNK_WIDTH &&
                        y >= 0 && y < ConstantCommonSettings.CHUNK_HEIGHT) {

                    negZ = negZ.setBlock(
                            x,
                            y,
                            ConstantCommonSettings.CHUNK_LENGTH - 1,
                            blockService.getBlock(palette[blockID].id()));
                } else if (z == ConstantCommonSettings.CHUNK_LENGTH &&
                        x >= 0 && x < ConstantCommonSettings.CHUNK_WIDTH &&
                        y >= 0 && y < ConstantCommonSettings.CHUNK_HEIGHT) {

                    posZ = posZ.setBlock(
                            x,
                            y,
                            0,
                            blockService.getBlock(palette[blockID].id()));
                } else if (y == -1 && x >= 0 && x < ConstantCommonSettings.CHUNK_WIDTH &&
                        z >= 0 && z < ConstantCommonSettings.CHUNK_LENGTH) {

                    negY = negY.setBlock(
                            x,
                            ConstantCommonSettings.CHUNK_HEIGHT - 1,
                            z,
                            blockService.getBlock(palette[blockID].id()));
                } else if (y == ConstantCommonSettings.CHUNK_HEIGHT &&
                        x >= 0 && x < ConstantCommonSettings.CHUNK_WIDTH &&
                        z >= 0 && z < ConstantCommonSettings.CHUNK_LENGTH) {

                    posY = posY.setBlock(
                            x,
                            0,
                            z,
                            blockService.getBlock(palette[blockID].id()));
                }

                y++;
                if (y > ConstantCommonSettings.CHUNK_HEIGHT) {
                    y = -1;
                    z++;

                    if (z > ConstantCommonSettings.CHUNK_LENGTH) {
                        z = -1;
                        x++;
                    }
                }
            }

            i += blockCount;
        }

        byteBuf.release();

        world.addChunkData(pos, center, false);
        world.addChunkData(pos.add(-1, 0, 0), negX, true);
        world.addChunkData(pos.add(1, 0, 0), posX, true);
        world.addChunkData(pos.add(0, -1, 0), negY, true);
        world.addChunkData(pos.add(0, 1, 0), posY, true);
        world.addChunkData(pos.add(0, 0, -1), negZ, true);
        world.addChunkData(pos.add(0, 0, 1), posZ, true);

        return blockMeshes;
    }

    public List<MeshDataTask> generateMeshData(MeshDataTask meshDataTask, int queueSize) {
        state.setItem(Thread.currentThread().getName() + "_queue_size_mdg", queueSize);
        if (meshDataTask instanceof ChunkMeshDataTask(ByteBuf byteBuf, Position3D position3D)) {
            MeshData meshData = chunkMeshDataGenerator.generateMeshData(byteBuf, position3D);
            if (meshData != null) {
                world.add(position3D, meshData);
            } else {
                Logger.warn("Mesh data generation failed..." + position3D);
                return world.get(position3D, false, false) != null ? List.of(new ChunkMeshDataTask(null, position3D)) : null;
            }
        } else if (meshDataTask instanceof EntityMeshDataTask(ClientEntity entity)) {
            world.addEntity(entityMeshDataGenerator.generateMeshData(entity));
        } else {
            throw new IllegalArgumentException(meshDataTask + " is an invalid input. Stop playing with things you CLEARLY don't know how to use...");
        }
        return null;
    }
}