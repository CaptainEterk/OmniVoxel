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
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.common.BlockShape;
import omnivoxel.common.face.BlockFace;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.Settings;
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
    private final Settings settings;

    public ChunkMeshDataGenerator(ClientWorldDataService worldDataService, BlockService<BlockWithMesh> blockService, ClientWorld world, Settings settings) {
        this.worldDataService = worldDataService;
        this.blockService = blockService;
        this.world = world;
        this.settings = settings;
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
                                    chunkLightingData,
                                    blockMeshes,
                                    position3D
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
                                    chunkLightingData,
                                    blockMeshes,
                                    position3D
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
            List<Integer> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap, ChunkLightingData chunkLightingData,
            BlockMesh[] blockMeshes,
            Position3D chunkPosition) {

        BlockShape shape = blockMesh.getShape(top, bottom, north, south, east, west);

        boolean renderTop = shouldRenderFaceCached(blockMesh, shape, top, BlockFace.TOP, top, bottom, north, south, east, west);
        boolean renderBottom = shouldRenderFaceCached(blockMesh, shape, bottom, BlockFace.BOTTOM, top, bottom, north, south, east, west);
        boolean renderNorth = shouldRenderFaceCached(blockMesh, shape, north, BlockFace.NORTH, top, bottom, north, south, east, west);
        boolean renderSouth = shouldRenderFaceCached(blockMesh, shape, south, BlockFace.SOUTH, top, bottom, north, south, east, west);
        boolean renderEast = shouldRenderFaceCached(blockMesh, shape, east, BlockFace.EAST, top, bottom, north, south, east, west);
        boolean renderWest = shouldRenderFaceCached(blockMesh, shape, west, BlockFace.WEST, top, bottom, north, south, east, west);

        if (renderTop)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.TOP, vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        if (renderBottom)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.BOTTOM, vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        if (renderNorth)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.NORTH, vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        if (renderSouth)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.SOUTH, vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        if (renderEast)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.EAST, vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        if (renderWest)
            addFacePrecomputedShape(x, y, z, blockMesh, shape, BlockFace.WEST, vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
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
            ChunkLightingData chunkLightingData,
            BlockMesh[] blockMeshes,
            Position3D chunkPosition
    ) {
        int[] uvCoordinates = blockMesh.getUVCoordinates(blockFace);
        Vertex[] faceVertices = shape.vertices()[blockFace.ordinal()];
        int[] faceIndices = shape.indices()[blockFace.ordinal()];

        // TODO: Remove all hardcoding
        int blockType = Objects.equals(blockMesh.getModID() + "/" + blockMesh.getState(), "core:water_source_block/top") ? 1 : 0;
        boolean ambientOcclusion = settings.getBooleanSetting("ambient_occlusion", true);
        boolean smoothLighting = settings.getBooleanSetting("smooth_lighting", false);

        for (int idx : faceIndices) {
            Vertex pointPosition = faceVertices[idx];
            Vertex position = pointPosition.add(x, y, z);
            int ambientOcclusionLevel = ambientOcclusion ? sampleAmbientOcclusion(x, y, z, blockFace, pointPosition, blockMeshes) : 4;
            MeshDataGenerator.addPoint(
                    vertices,
                    indices,
                    vertexIndexMap,
                    position,
                    uvCoordinates[idx * 2],
                    uvCoordinates[idx * 2 + 1],
                    blockFace,
                    applyAmbientOcclusion(sampleVertexLight(x, y, z, blockFace, pointPosition, chunkLightingData, chunkPosition, LightChannels.RED, smoothLighting), ambientOcclusionLevel),
                    applyAmbientOcclusion(sampleVertexLight(x, y, z, blockFace, pointPosition, chunkLightingData, chunkPosition, LightChannels.GREEN, smoothLighting), ambientOcclusionLevel),
                    applyAmbientOcclusion(sampleVertexLight(x, y, z, blockFace, pointPosition, chunkLightingData, chunkPosition, LightChannels.BLUE, smoothLighting), ambientOcclusionLevel),
                    applyAmbientOcclusion(sampleVertexLight(x, y, z, blockFace, pointPosition, chunkLightingData, chunkPosition, LightChannels.SKYLIGHT, smoothLighting), ambientOcclusionLevel),
                    blockType
            );
        }
    }

    private byte sampleVertexLight(
            int bx, int by, int bz,
            BlockFace face,
            Vertex vertex,
            ChunkLightingData lighting,
            Position3D chunkPosition,
            LightChannels channel,
            boolean smoothLighting
    ) {
        int sampleX = bx;
        int sampleY = by;
        int sampleZ = bz;

        if (face == BlockFace.TOP && isAtMaxBlockEdge(vertex.py())) {
            sampleY++;
        } else if (face == BlockFace.BOTTOM && isAtMinBlockEdge(vertex.py())) {
            sampleY--;
        } else if (face == BlockFace.NORTH && isAtMaxBlockEdge(vertex.pz())) {
            sampleZ++;
        } else if (face == BlockFace.SOUTH && isAtMinBlockEdge(vertex.pz())) {
            sampleZ--;
        } else if (face == BlockFace.EAST && isAtMaxBlockEdge(vertex.px())) {
            sampleX++;
        } else if (face == BlockFace.WEST && isAtMinBlockEdge(vertex.px())) {
            sampleX--;
        }

        if (smoothLighting) {
            return sampleSmoothVertexLight(sampleX, sampleY, sampleZ, bx, by, bz, face, vertex, lighting, chunkPosition, channel);
        }

        return sampleLight(sampleX, sampleY, sampleZ, bx, by, bz, lighting, chunkPosition, channel);
    }

    private byte sampleSmoothVertexLight(
            int sampleX, int sampleY, int sampleZ,
            int fallbackX, int fallbackY, int fallbackZ,
            BlockFace face,
            Vertex vertex,
            ChunkLightingData lighting,
            Position3D chunkPosition,
            LightChannels channel
    ) {
        int tx1 = 0, ty1 = 0, tz1 = 0;
        int tx2 = 0, ty2 = 0, tz2 = 0;

        switch (face) {
            case TOP, BOTTOM -> {
                tx1 = vertexSign(vertex.px());
                tz2 = vertexSign(vertex.pz());
            }
            case NORTH, SOUTH -> {
                tx1 = vertexSign(vertex.px());
                ty2 = vertexSign(vertex.py());
            }
            case EAST, WEST -> {
                ty1 = vertexSign(vertex.py());
                tz2 = vertexSign(vertex.pz());
            }
            case NONE -> {
                return sampleLight(sampleX, sampleY, sampleZ, fallbackX, fallbackY, fallbackZ, lighting, chunkPosition, channel);
            }
        }

        int light = 0;
        light += sampleLight(sampleX, sampleY, sampleZ, fallbackX, fallbackY, fallbackZ, lighting, chunkPosition, channel) & 0xFF;
        light += sampleLight(sampleX + tx1, sampleY + ty1, sampleZ + tz1, fallbackX, fallbackY, fallbackZ, lighting, chunkPosition, channel) & 0xFF;
        light += sampleLight(sampleX + tx2, sampleY + ty2, sampleZ + tz2, fallbackX, fallbackY, fallbackZ, lighting, chunkPosition, channel) & 0xFF;
        light += sampleLight(sampleX + tx1 + tx2, sampleY + ty1 + ty2, sampleZ + tz1 + tz2, fallbackX, fallbackY, fallbackZ, lighting, chunkPosition, channel) & 0xFF;

        return (byte) Math.round(light / 4.0f);
    }

    private byte sampleLight(
            int sampleX, int sampleY, int sampleZ,
            int fallbackX, int fallbackY, int fallbackZ,
            ChunkLightingData currentLighting,
            Position3D currentChunkPosition,
            LightChannels channel
    ) {
        if (IndexCalculator.checkBounds(sampleX, sampleY, sampleZ)) {
            return currentLighting.getChannel(channel).getLighting(IndexCalculator.calculateBlockIndex(sampleX, sampleY, sampleZ));
        }

        int chunkOffsetX = chunkOffset(sampleX, ConstantCommonSettings.CHUNK_WIDTH);
        int chunkOffsetY = chunkOffset(sampleY, ConstantCommonSettings.CHUNK_HEIGHT);
        int chunkOffsetZ = chunkOffset(sampleZ, ConstantCommonSettings.CHUNK_LENGTH);
        ClientWorldChunk sampleChunk = world.get(currentChunkPosition.add(chunkOffsetX, chunkOffsetY, chunkOffsetZ), false, true);

        if (sampleChunk == null || sampleChunk.getLightingData() == null) {
            return currentLighting.getChannel(channel).getLighting(IndexCalculator.calculateBlockIndex(fallbackX, fallbackY, fallbackZ));
        }

        int localX = wrapCoordinate(sampleX, ConstantCommonSettings.CHUNK_WIDTH);
        int localY = wrapCoordinate(sampleY, ConstantCommonSettings.CHUNK_HEIGHT);
        int localZ = wrapCoordinate(sampleZ, ConstantCommonSettings.CHUNK_LENGTH);
        return sampleChunk.getLightingData().getChannel(channel).getLighting(IndexCalculator.calculateBlockIndex(localX, localY, localZ));
    }

    private int chunkOffset(int coordinate, int size) {
        if (coordinate < 0) {
            return -1;
        }

        if (coordinate >= size) {
            return 1;
        }

        return 0;
    }

    private int wrapCoordinate(int coordinate, int size) {
        if (coordinate < 0) {
            return size - 1;
        }

        if (coordinate >= size) {
            return 0;
        }

        return coordinate;
    }

    private boolean isAtMinBlockEdge(float coordinate) {
        return coordinate <= 0.0f;
    }

    private boolean isAtMaxBlockEdge(float coordinate) {
        return coordinate >= 1.0f;
    }

    private int sampleAmbientOcclusion(
            int bx, int by, int bz,
            BlockFace face,
            Vertex vertex,
            BlockMesh[] blockMeshes
    ) {
        int nx = 0;
        int ny = 0;
        int nz = 0;
        int tx1 = 0, ty1 = 0, tz1 = 0;
        int tx2 = 0, ty2 = 0, tz2 = 0;

        switch (face) {
            case TOP, BOTTOM -> {
                ny = face == BlockFace.TOP ? 1 : -1;
                tx1 = vertexSign(vertex.px());
                tz2 = vertexSign(vertex.pz());
            }
            case NORTH, SOUTH -> {
                nz = face == BlockFace.NORTH ? 1 : -1;
                tx1 = vertexSign(vertex.px());
                ty2 = vertexSign(vertex.py());
            }
            case EAST, WEST -> {
                nx = face == BlockFace.EAST ? 1 : -1;
                ty1 = vertexSign(vertex.py());
                tz2 = vertexSign(vertex.pz());
            }
            case NONE -> {
                return 4;
            }
        }

        int sampleX = bx + nx;
        int sampleY = by + ny;
        int sampleZ = bz + nz;

        boolean side1 = isAmbientOccluder(blockMeshes, sampleX + tx1, sampleY + ty1, sampleZ + tz1);
        boolean side2 = isAmbientOccluder(blockMeshes, sampleX + tx2, sampleY + ty2, sampleZ + tz2);
        boolean corner = isAmbientOccluder(blockMeshes, sampleX + tx1 + tx2, sampleY + ty1 + ty2, sampleZ + tz1 + tz2);

        if (side1 && side2) {
            return 1;
        }

        return 4 - (booleanValue(side1) + booleanValue(side2) + booleanValue(corner));
    }

    private int vertexSign(float coordinate) {
        return coordinate < 0.5f ? -1 : 1;
    }

    private boolean isAmbientOccluder(BlockMesh[] blockMeshes, int x, int y, int z) {
        if (x < -1 || x > ConstantCommonSettings.CHUNK_WIDTH ||
                y < -1 || y > ConstantCommonSettings.CHUNK_HEIGHT ||
                z < -1 || z > ConstantCommonSettings.CHUNK_LENGTH) {
            return false;
        }

        BlockMesh blockMesh = blockMeshes[IndexCalculator.calculateBlockIndexPadded(x, y, z)];
        return blockMesh != null && !blockMesh.isTransparent();
    }

    private int booleanValue(boolean value) {
        return value ? 1 : 0;
    }

    private byte applyAmbientOcclusion(byte light, int ambientOcclusion) {
        return (byte) Math.round((light & 0xFF) * (ambientOcclusion / 4.0f));
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
