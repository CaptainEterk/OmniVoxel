package omnivoxel.client.game.graphics.api.opengl.mesh.generators;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.ChunkMeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.UniqueVertex;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.Vertex;
import omnivoxel.client.game.graphics.block.BlockMesh;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.light.ChunkLightingData;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.common.BlockShape;
import omnivoxel.common.face.BlockFace;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.Block;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;

import java.nio.ByteBuffer;
import java.util.*;

public class ChunkMeshDataGenerator {
    private final ClientWorldDataService worldDataService;
    private final BlockService<BlockWithMesh> blockService;
    private final ClientWorld world;

    public ChunkMeshDataGenerator(ClientWorldDataService worldDataService, BlockService<BlockWithMesh> blockService, ClientWorld world) {
        // TODO: Remove hardcoding
        this.worldDataService = worldDataService;
        this.blockService = blockService;
        this.world = world;
    }

    private MeshData generateChunkMeshData(BlockMesh[] blockMeshes, Position3D position3D) {
        if (blockMeshes == null) {
            Logger.warn("blockMeshes is null");
            return null;
        }

        ClientWorldChunk clientWorldChunk = world.get(position3D, false, false);

        if (clientWorldChunk == null) {
            Logger.warn("clientWorldChunk is null");
            return null;
        }

        ChunkLightingData chunkLightingData = clientWorldChunk.getLightingData();

        if (chunkLightingData == null) {
            Logger.warn("chunkLightingData is null");
            return null;
        }

        List<Integer> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Integer> transparentVertices = new ArrayList<>();
        List<Integer> transparentIndices = new ArrayList<>();
        Map<UniqueVertex, Integer> vertexIndexMap = new HashMap<>();
        Map<UniqueVertex, Integer> transparentVertexIndexMap = new HashMap<>();

        for (int x = 0; x < ConstantCommonSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantCommonSettings.CHUNK_LENGTH; z++) {
                for (int y = 0; y < ConstantCommonSettings.CHUNK_HEIGHT; y++) {
                    int index = IndexCalculator.calculateBlockIndexPadded(x, y, z);
                    BlockMesh blockMesh = blockMeshes[index];
                    if (blockMesh != null) {
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
                                    chunkLightingData
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
                                    chunkLightingData
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
            List<Integer> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap, ChunkLightingData chunkLightingData) {

        BlockShape shape = blockMesh.getShape(top, bottom, north, south, east, west);

        boolean renderTop = shouldRenderFaceCached(blockMesh, shape, top, BlockFace.TOP, top, bottom, north, south, east, west);
        boolean renderBottom = shouldRenderFaceCached(blockMesh, shape, bottom, BlockFace.BOTTOM, top, bottom, north, south, east, west);
        boolean renderNorth = shouldRenderFaceCached(blockMesh, shape, north, BlockFace.NORTH, top, bottom, north, south, east, west);
        boolean renderSouth = shouldRenderFaceCached(blockMesh, shape, south, BlockFace.SOUTH, top, bottom, north, south, east, west);
        boolean renderEast = shouldRenderFaceCached(blockMesh, shape, east, BlockFace.EAST, top, bottom, north, south, east, west);
        boolean renderWest = shouldRenderFaceCached(blockMesh, shape, west, BlockFace.WEST, top, bottom, north, south, east, west);

        if (renderTop)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.TOP, vertices, indices, vertexIndexMap, chunkLightingData);
        if (renderBottom)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.BOTTOM, vertices, indices, vertexIndexMap, chunkLightingData);
        if (renderNorth)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.NORTH, vertices, indices, vertexIndexMap, chunkLightingData);
        if (renderSouth)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.SOUTH, vertices, indices, vertexIndexMap, chunkLightingData);
        if (renderEast)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.EAST, vertices, indices, vertexIndexMap, chunkLightingData);
        if (renderWest)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.WEST, vertices, indices, vertexIndexMap, chunkLightingData);
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
            ChunkLightingData chunkLightingData
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
                    sampleVertexLight(x, y, z, blockFace, pointPosition, chunkLightingData, LightChannels.RED),
                    sampleVertexLight(x, y, z, blockFace, pointPosition, chunkLightingData, LightChannels.GREEN),
                    sampleVertexLight(x, y, z, blockFace, pointPosition, chunkLightingData, LightChannels.BLUE),
                    sampleVertexLight(x, y, z, blockFace, pointPosition, chunkLightingData, LightChannels.SKYLIGHT),
                    blockType
            );
        }
    }

    private byte sampleVertexLight(
            int bx, int by, int bz,
            BlockFace face,
            Vertex vertex,
            ChunkLightingData lighting,
            LightChannels channel
    ) {
        return lighting.getChannel(channel).getLighting(IndexCalculator.calculateBlockIndex(bx, by, bz));
    }

    private BlockMesh[] unpackChunkPadded(Position3D position3D, ClientWorldChunk centerChunk) {
        Chunk<BlockWithMesh> center = centerChunk == null ? null : centerChunk.getChunkData();
        if (center == null) {
            Logger.warn(Logger.Priority.LOW, "The center chunk is null");
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
            Logger.warn(Logger.Priority.LOW, "One or more shell chunk is null");
            return null;
        }

        Chunk<BlockWithMesh> negX = negXChunk.getChunkData();
        Chunk<BlockWithMesh> posX = posXChunk.getChunkData();
        Chunk<BlockWithMesh> negY = negYChunk.getChunkData();
        Chunk<BlockWithMesh> posY = posYChunk.getChunkData();
        Chunk<BlockWithMesh> negZ = negZChunk.getChunkData();
        Chunk<BlockWithMesh> posZ = posZChunk.getChunkData();

        if (negX == null ||
                posX == null ||
                negY == null ||
                posY == null ||
                negZ == null ||
                posZ == null) {
            Logger.warn(Logger.Priority.LOW, "One or more shell chunk data is null");
            return null;
        }

        int W = ConstantCommonSettings.CHUNK_WIDTH;
        int H = ConstantCommonSettings.CHUNK_HEIGHT;
        int L = ConstantCommonSettings.CHUNK_LENGTH;

        BlockMesh[] blockMeshes = new BlockMesh[ConstantCommonSettings.BLOCKS_IN_CHUNK_PADDED];

        for (int x = -1; x <= ConstantCommonSettings.CHUNK_WIDTH; x++) {
            for (int y = -1; y <= ConstantCommonSettings.CHUNK_HEIGHT; y++) {
                for (int z = -1; z <= ConstantCommonSettings.CHUNK_LENGTH; z++) {
                    int outOfBounds = 0;
                    if (x < 0 || x == ConstantCommonSettings.CHUNK_WIDTH) outOfBounds++;
                    if (y < 0 || y == ConstantCommonSettings.CHUNK_HEIGHT) outOfBounds++;
                    if (z < 0 || z == ConstantCommonSettings.CHUNK_LENGTH) outOfBounds++;

                    if (outOfBounds > 1) {
                        continue;
                    }

                    Chunk<BlockWithMesh> chunk;
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

    public MeshData generateMeshData(ByteBuf blocks, Position3D position3D) {
        if (blocks == null) {
            return generateChunkMeshData(unpackChunkPadded(position3D, world.get(position3D, false, false)), position3D);
        }
        return generateChunkMeshData(MeshDataGenerator.unpackChunkPadded(blocks, position3D, worldDataService, blockService, world), position3D);
    }
}