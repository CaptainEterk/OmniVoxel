package omnivoxel.client.game.graphics.api.opengl.mesh.generators;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.api.opengl.mesh.block.BlockMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting.ChunkMeshDataLightingGenerator;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.ChunkMeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.UniqueVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.Vertex;
import omnivoxel.client.game.graphics.light.ChunkLightingData;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.common.BlockShape;
import omnivoxel.common.face.BlockFace;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.Block;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.ChunkShell;
import omnivoxel.world.chunk.SingleBlockChunk;

import java.nio.ByteBuffer;
import java.util.*;

public class ChunkMeshDataGenerator {
    public static final Block air = new Block("omnivoxel:air");
    private final ClientWorldDataService worldDataService;
    private final BlockService blockService;
    private final ChunkMeshDataLightingGenerator chunkMeshDataLightingGenerator;
    private final BlockMesh[] blockMeshes = new BlockMesh[ConstantGameSettings.BLOCKS_IN_CHUNK_PADDED];
    private final ClientWorld world;

    public ChunkMeshDataGenerator(ClientWorldDataService worldDataService, BlockService blockService, ChunkMeshDataLightingGenerator chunkMeshDataLightingGenerator, ClientWorld world) {
        this.worldDataService = worldDataService;
        this.blockService = blockService;
        this.chunkMeshDataLightingGenerator = chunkMeshDataLightingGenerator;
        this.world = world;
    }

    private boolean calculateNeighborChunkLighting(Position3D position3D) {
        ClientWorldChunk clientWorldChunk = world.get(position3D, false, false);
        if (clientWorldChunk != null) {
            clientWorldChunk.setChunkLightingData(chunkMeshDataLightingGenerator.generateLighting(clientWorldChunk, position3D).chunkLightingData());
            return false;
        }
        return true;
    }

    private MeshDataAndTasks generateChunkMeshDataLighting(BlockMesh[] blockMeshes, Position3D position3D) {
        ClientWorldChunk clientWorldChunk = world.get(position3D, false, false);
        if (clientWorldChunk == null || blockMeshes == null) {
            return null;
        }

        List<MeshDataTask> meshDataTasks = null;

        boolean failed = false;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (!(x == 0 && y == 0 && z == 0)) {
                        failed = failed || calculateNeighborChunkLighting(position3D.add(x, y, z));
                    }
                }
            }
        }

        if (failed) {
            calculateNeighborChunkLighting(position3D);
        }

        ChunkMeshDataLightingGenerator.ChunkLightingDataAndTasks chunkLightingDataAndTasks = chunkMeshDataLightingGenerator.generateLighting(clientWorldChunk, position3D);
        ChunkLightingData chunkLightingData = chunkLightingDataAndTasks.chunkLightingData();
        clientWorldChunk.setChunkLightingData(chunkLightingData);
        if (!failed) {
            meshDataTasks = chunkLightingDataAndTasks.meshDataTasks();
        }

        return new MeshDataAndTasks(generateChunkMeshData(blockMeshes, position3D, chunkLightingData), meshDataTasks);
    }

    private MeshData generateChunkMeshData(BlockMesh[] blockMeshes, Position3D position3D, ChunkLightingData chunkLightingData) {
        if (chunkLightingData == null) {
            return generateChunkMeshData(blockMeshes, position3D, ChunkLightingData.EMPTY);
        }

        List<Integer> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Integer> transparentVertices = new ArrayList<>();
        List<Integer> transparentIndices = new ArrayList<>();
        Map<UniqueVertex, Integer> vertexIndexMap = new HashMap<>();
        Map<UniqueVertex, Integer> transparentVertexIndexMap = new HashMap<>();

        for (int x = 0; x < ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantGameSettings.CHUNK_LENGTH; z++) {
                for (int y = 0; y < ConstantGameSettings.CHUNK_HEIGHT; y++) {
                    int index = IndexCalculator.calculateBlockIndexPadded(x, y, z);
                    BlockMesh blockMesh = blockMeshes[index];
                    if (blockMesh != null) {
                        int t_lightLevel = chunkLightingData.getSkylightChannel().getLighting(IndexCalculator.calculateBlockIndex(x, y, z));
                        if (blockMesh.shouldRenderTransparentMesh()) {
                            generateBlockMeshData(
                                    x,
                                    y,
                                    z,
                                    blockMesh,
                                    blockMeshes[index + BlockFace.TOP.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.BOTTOM.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.NORTH.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.SOUTH.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.EAST.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.WEST.getPaddedNeighborOffset()],
                                    transparentVertices,
                                    transparentIndices,
                                    transparentVertexIndexMap,
                                    t_lightLevel
                            );
                        } else {
                            generateBlockMeshData(
                                    x,
                                    y,
                                    z,
                                    blockMesh,
                                    blockMeshes[index + BlockFace.TOP.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.BOTTOM.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.NORTH.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.SOUTH.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.EAST.getPaddedNeighborOffset()],
                                    blockMeshes[index + BlockFace.WEST.getPaddedNeighborOffset()],
                                    vertices,
                                    indices,
                                    vertexIndexMap,
                                    t_lightLevel
                            );
                        }
                    }
                }
            }
        }

        ByteBuffer vertexBuffer = MeshDataGenerator.createIntBuffer(vertices);
        ByteBuffer indexBuffer = MeshDataGenerator.createIntBuffer(indices);
        ByteBuffer transparentVertexBuffer = MeshDataGenerator.createIntBuffer(transparentVertices);
        ByteBuffer transparentIndexBuffer = MeshDataGenerator.createIntBuffer(transparentIndices);

        return new ChunkMeshData(vertexBuffer, indexBuffer, transparentVertexBuffer, transparentIndexBuffer, position3D);
    }

    private void generateBlockMeshData(
            int x, int y, int z,
            BlockMesh blockMesh, BlockMesh top, BlockMesh bottom, BlockMesh north, BlockMesh south, BlockMesh east, BlockMesh west,
            List<Integer> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap, int t_lightLevel) {

        BlockShape shape = blockMesh.getShape(top, bottom, north, south, east, west);

        boolean renderTop = shouldRenderFaceCached(blockMesh, shape, top, BlockFace.TOP, top, bottom, north, south, east, west);
        boolean renderBottom = shouldRenderFaceCached(blockMesh, shape, bottom, BlockFace.BOTTOM, top, bottom, north, south, east, west);
        boolean renderNorth = shouldRenderFaceCached(blockMesh, shape, north, BlockFace.NORTH, top, bottom, north, south, east, west);
        boolean renderSouth = shouldRenderFaceCached(blockMesh, shape, south, BlockFace.SOUTH, top, bottom, north, south, east, west);
        boolean renderEast = shouldRenderFaceCached(blockMesh, shape, east, BlockFace.EAST, top, bottom, north, south, east, west);
        boolean renderWest = shouldRenderFaceCached(blockMesh, shape, west, BlockFace.WEST, top, bottom, north, south, east, west);

        if (renderTop)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.TOP, vertices, indices, vertexIndexMap, t_lightLevel);
        if (renderBottom)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.BOTTOM, vertices, indices, vertexIndexMap, t_lightLevel);
        if (renderNorth)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.NORTH, vertices, indices, vertexIndexMap, t_lightLevel);
        if (renderSouth)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.SOUTH, vertices, indices, vertexIndexMap, t_lightLevel);
        if (renderEast)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.EAST, vertices, indices, vertexIndexMap, t_lightLevel);
        if (renderWest)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.WEST, vertices, indices, vertexIndexMap, t_lightLevel);
    }

    private boolean shouldRenderFaceCached(BlockMesh originalBlockMesh, BlockShape originalShape, BlockMesh adjacentBlockMesh, BlockFace face,
                                           BlockMesh top, BlockMesh bottom, BlockMesh north, BlockMesh south, BlockMesh east, BlockMesh west) {
        if (adjacentBlockMesh == null) {
            return true;
        }

        if (adjacentBlockMesh.isTransparent() && !Objects.equals(adjacentBlockMesh.getModID(), originalBlockMesh.getModID())) {
            return true;
        }
        BlockShape adjBlockShape = adjacentBlockMesh.getShape(top, bottom, north, south, east, west);
        return !(originalShape.solid()[face.ordinal()] && adjBlockShape.solid()[face.ordinal()])
                && originalBlockMesh.shouldRenderFace(face, adjacentBlockMesh);
    }

    private void addFacePrecomputedShape(
            int x,
            int y,
            int z,
            BlockMesh blockMesh,
            BlockShape shape,
            BlockFace blockFace,
            List<Integer> vertices,
            List<Integer> indices,
            Map<UniqueVertex, Integer> vertexIndexMap,
            int t_lightLevel
    ) {
        int[] uvCoordinates = blockMesh.getUVCoordinates(blockFace);
        Vertex[] faceVertices = shape.vertices()[blockFace.ordinal()];
        int[] faceIndices = shape.indices()[blockFace.ordinal()];

        // TODO: Remove all hardcoding
        int blockType = Objects.equals(blockMesh.getModID(), "core:water_source_block") ? 1 : 0;

        for (int idx : faceIndices) {
            Vertex pointPosition = faceVertices[idx];
            Vertex position = pointPosition.add(x, y, z);
            MeshDataGenerator.addPoint(
                    vertices,
                    indices,
                    vertexIndexMap,
                    position,
                    uvCoordinates[idx * 2],
                    uvCoordinates[idx * 2 + 1],
                    blockFace,
                    0,
                    0,
                    0,
                    t_lightLevel,
                    blockType
            );
        }
    }

    private BlockMesh[] unpackChunkPadded(ByteBuf byteBuf, Position3D pos) {
        Block[] palette = new Block[byteBuf.getShort(20)];

        int index = 22;

        for (int i = 0; i < palette.length; i++) {
            short len = byteBuf.getShort(index);
            StringBuilder id = new StringBuilder();

            for (int j = 0; j < len; j++) {
                id.append((char) byteBuf.getByte(index + 2 + j));
            }

            palette[i] = new Block(id.toString());
            index += 2 + len;
        }

        Chunk<Block> center = new SingleBlockChunk<>(air);
        Chunk<Block> negX = new ChunkShell<>();
        Chunk<Block> posX = new ChunkShell<>();
        Chunk<Block> negY = new ChunkShell<>();
        Chunk<Block> posY = new ChunkShell<>();
        Chunk<Block> negZ = new ChunkShell<>();
        Chunk<Block> posZ = new ChunkShell<>();

        int x = -1, y = -1, z = -1;

        for (int i = 0; i < ConstantGameSettings.BLOCKS_IN_CHUNK_PADDED; ) {
            int blockID = byteBuf.getInt(index);
            int blockCount = byteBuf.getInt(index + 4);
            index += 8;

            BlockMesh blockMesh = worldDataService.getBlock(palette[blockID].id());

            for (int j = 0; j < blockCount; j++) {
                int paddedIndex = i + j;
                blockMeshes[paddedIndex] = blockMesh;

                if (x >= 0 && x < ConstantGameSettings.CHUNK_WIDTH &&
                        y >= 0 && y < ConstantGameSettings.CHUNK_HEIGHT &&
                        z >= 0 && z < ConstantGameSettings.CHUNK_LENGTH) {

                    center = center.setBlock(x, y, z,
                            blockService.getBlock(palette[blockID].id()));
                } else if (x == -1 && y >= 0 && y < ConstantGameSettings.CHUNK_HEIGHT &&
                        z >= 0 && z < ConstantGameSettings.CHUNK_LENGTH) {

                    negX = negX.setBlock(
                            ConstantGameSettings.CHUNK_WIDTH - 1,
                            y,
                            z,
                            blockService.getBlock(palette[blockID].id()));
                } else if (x == ConstantGameSettings.CHUNK_WIDTH &&
                        y >= 0 && y < ConstantGameSettings.CHUNK_HEIGHT &&
                        z >= 0 && z < ConstantGameSettings.CHUNK_LENGTH) {

                    posX = posX.setBlock(
                            0,
                            y,
                            z,
                            blockService.getBlock(palette[blockID].id()));
                } else if (z == -1 && x >= 0 && x < ConstantGameSettings.CHUNK_WIDTH &&
                        y >= 0 && y < ConstantGameSettings.CHUNK_HEIGHT) {

                    negZ = negZ.setBlock(
                            x,
                            y,
                            ConstantGameSettings.CHUNK_LENGTH - 1,
                            blockService.getBlock(palette[blockID].id()));
                } else if (z == ConstantGameSettings.CHUNK_LENGTH &&
                        x >= 0 && x < ConstantGameSettings.CHUNK_WIDTH &&
                        y >= 0 && y < ConstantGameSettings.CHUNK_HEIGHT) {

                    posZ = posZ.setBlock(
                            x,
                            y,
                            0,
                            blockService.getBlock(palette[blockID].id()));
                } else if (y == -1 && x >= 0 && x < ConstantGameSettings.CHUNK_WIDTH &&
                        z >= 0 && z < ConstantGameSettings.CHUNK_LENGTH) {

                    negY = negY.setBlock(
                            x,
                            ConstantGameSettings.CHUNK_HEIGHT - 1,
                            z,
                            blockService.getBlock(palette[blockID].id()));
                } else if (y == ConstantGameSettings.CHUNK_HEIGHT &&
                        x >= 0 && x < ConstantGameSettings.CHUNK_WIDTH &&
                        z >= 0 && z < ConstantGameSettings.CHUNK_LENGTH) {

                    posY = posY.setBlock(
                            x,
                            0,
                            z,
                            blockService.getBlock(palette[blockID].id()));
                }

                y++;
                if (y > ConstantGameSettings.CHUNK_HEIGHT) {
                    y = -1;
                    z++;

                    if (z > ConstantGameSettings.CHUNK_LENGTH) {
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

    public MeshDataAndTasks generateMeshData(ByteBuf blocks, Position3D position3D) {
        if (blocks == null) {
            return generateChunkMeshDataLighting(unpackChunkPadded(position3D, world.get(position3D, false, false)), position3D);
        }
        return generateChunkMeshDataLighting(unpackChunkPadded(blocks, position3D), position3D);
    }

    private BlockMesh[] unpackChunkPadded(Position3D position3D, ClientWorldChunk centerChunk) {
        Chunk<Block> center = centerChunk == null ? null : centerChunk.getChunkData();
        if (center == null) {
            // TODO: Make these logs call Logger at a really low priority
            System.out.println("Center is null: " + position3D + " " + centerChunk);
            return null;
        }

        ClientWorldChunk negXChunk = world.get(position3D.add(-1, 0, 0), false, true);
        ClientWorldChunk posXChunk = world.get(position3D.add(1, 0, 0), false, true);
        ClientWorldChunk negYChunk = world.get(position3D.add(0, -1, 0), false, true);
        ClientWorldChunk posYChunk = world.get(position3D.add(0, 1, 0), false, true);
        ClientWorldChunk negZChunk = world.get(position3D.add(0, 0, -1), false, true);
        ClientWorldChunk posZChunk = world.get(position3D.add(0, 0, 1), false, true);

        if (negXChunk == null ||
                posXChunk == null ||
                negYChunk == null ||
                posYChunk == null ||
                negZChunk == null ||
                posZChunk == null) {
            System.out.println("Shells are null");
            return null;
        }

        Chunk<Block> negX = negXChunk.getChunkData();
        Chunk<Block> posX = posXChunk.getChunkData();
        Chunk<Block> negY = negYChunk.getChunkData();
        Chunk<Block> posY = posYChunk.getChunkData();
        Chunk<Block> negZ = negZChunk.getChunkData();
        Chunk<Block> posZ = posZChunk.getChunkData();

        int W = ConstantGameSettings.CHUNK_WIDTH;
        int H = ConstantGameSettings.CHUNK_HEIGHT;
        int L = ConstantGameSettings.CHUNK_LENGTH;

        BlockMesh[] blockMeshes = new BlockMesh[ConstantGameSettings.BLOCKS_IN_CHUNK_PADDED];

        for (int x = -1; x <= ConstantGameSettings.CHUNK_WIDTH; x++) {
            for (int y = -1; y <= ConstantGameSettings.CHUNK_HEIGHT; y++) {
                for (int z = -1; z <= ConstantGameSettings.CHUNK_LENGTH; z++) {
                    int outOfBounds = 0;
                    if (x < 0 || x == ConstantGameSettings.CHUNK_WIDTH) outOfBounds++;
                    if (y < 0 || y == ConstantGameSettings.CHUNK_HEIGHT) outOfBounds++;
                    if (z < 0 || z == ConstantGameSettings.CHUNK_LENGTH) outOfBounds++;

                    if (outOfBounds > 1) {
                        continue;
                    }

                    Chunk<Block> chunk;
                    int lx = x, ly = y, lz = z;

                    if (x < 0) {
                        chunk = negX;
                        lx = W - 1;
                    } else if (x == W) {
                        chunk = posX;
                        lx = 0;
                    } else if (y < 0) {
                        chunk = negY;
                        ly = H - 1;
                    } else if (y == H) {
                        chunk = posY;
                        ly = 0;
                    } else if (z < 0) {
                        chunk = negZ;
                        lz = L - 1;
                    } else if (z == L) {
                        chunk = posZ;
                        lz = 0;
                    } else {
                        chunk = center;
                    }

                    Block block = chunk.getBlock(lx, ly, lz);
                    BlockMesh blockMesh = worldDataService.getBlock(
                            block == null ? "omnivoxel:air" : block.id()
                    );

                    int index = IndexCalculator.calculateBlockIndexPadded(x, y, z);

                    blockMeshes[index] = blockMesh;
                }
            }
        }

        return blockMeshes;
    }

    public record MeshDataAndTasks(MeshData meshData, List<MeshDataTask> meshDataTasks) {
    }

}