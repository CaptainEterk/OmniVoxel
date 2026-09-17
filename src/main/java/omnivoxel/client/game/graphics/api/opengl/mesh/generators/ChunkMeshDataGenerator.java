package omnivoxel.client.game.graphics.api.opengl.mesh.generators;

import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.ChunkMeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.vertex.UniqueVertex;
import omnivoxel.client.game.graphics.block.BlockMesh;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.light.ChunkLightingData;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.common.block.shape.BlockShape;
import omnivoxel.common.block.shape.BlockVertex;
import omnivoxel.common.face.BlockFace;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.Settings;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.IntegerDynamicList;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class ChunkMeshDataGenerator {
    private static final BlockFace[] UNROTATE = {
            BlockFace.TOP,
            BlockFace.BOTTOM,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.TOP,
            BlockFace.BOTTOM,
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.TOP,
            BlockFace.BOTTOM,
            BlockFace.SOUTH,
            BlockFace.NORTH,
            BlockFace.WEST,
            BlockFace.EAST,
            BlockFace.TOP,
            BlockFace.BOTTOM,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.NORTH
    };
    private final ClientWorld world;
    private final IntegerDynamicList solidVertices = new IntegerDynamicList();
    private final IntegerDynamicList solidIndices = new IntegerDynamicList();
    private final IntegerDynamicList transparentVertices = new IntegerDynamicList();
    private final IntegerDynamicList transparentIndices = new IntegerDynamicList();
    private final IntegerDynamicList decorationVertices = new IntegerDynamicList();
    private final IntegerDynamicList decorationIndices = new IntegerDynamicList();
    private final Map<UniqueVertex, Integer> vertexIndexMap = new HashMap<>();
    private final Map<UniqueVertex, Integer> transparentVertexIndexMap = new HashMap<>();
    private final Map<UniqueVertex, Integer> decorationVertexIndexMap = new HashMap<>();
    private final int[] vertexData = new int[3];
    private boolean smoothLighting;
    private boolean ambientOcclusion;
    private BlockMesh[] blockMeshes;
    private byte[] rotations;
    private int lod;
    private int chunkWidth;
    private int chunkHeight;
    private int chunkLength;

    public ChunkMeshDataGenerator(ClientWorld world, Settings settings) {
        this.world = world;
        this.ambientOcclusion = settings.getBooleanSetting("ambient_occlusion", true);
        this.smoothLighting = settings.getBooleanSetting("smooth_lighting", false);
        settings.addSettingListener("ambient_occlusion", v -> ambientOcclusion = Boolean.parseBoolean(v));
        settings.addSettingListener("smooth_lighting", v -> smoothLighting = Boolean.parseBoolean(v));
    }

    private int getPaddedNeighborOffset(BlockFace face) {
        int paddedLength = chunkLength + 2;
        int paddedHeight = chunkHeight + 2;

        return switch (face) {
            case EAST -> paddedLength * paddedHeight;  // +X
            case WEST -> -paddedLength * paddedHeight; // -X

            case NORTH -> paddedLength;                 // +Z
            case SOUTH -> -paddedLength;                // -Z

            case TOP -> 1;                            // +Y
            case BOTTOM -> -1;                          // -Y

            default -> 0;
        };
    }

    private MeshData generateChunkMeshData(Position3D position3D) {
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

        solidVertices.clear();
        solidIndices.clear();
        transparentVertices.clear();
        transparentIndices.clear();
        decorationVertices.clear();
        decorationIndices.clear();
        vertexIndexMap.clear();
        transparentVertexIndexMap.clear();
        decorationVertexIndexMap.clear();

        for (int x = 0; x < chunkWidth; x++) {
            for (int z = 0; z < chunkLength; z++) {
                for (int y = 0; y < chunkHeight; y++) {
                    int index = IndexCalculator.calculateBlockIndexPadded(x, y, z, chunkWidth, chunkHeight, chunkLength);
                    BlockMesh blockMesh = blockMeshes[index];
                    if (blockMesh != null) {
                        if (blockMesh.shouldRenderTransparentMesh()) {
                            generateBlockMeshData(
                                    x,
                                    y,
                                    z,
                                    blockMesh,
                                    index + getPaddedNeighborOffset(BlockFace.TOP),
                                    index + getPaddedNeighborOffset(BlockFace.BOTTOM),
                                    index + getPaddedNeighborOffset(BlockFace.NORTH),
                                    index + getPaddedNeighborOffset(BlockFace.SOUTH),
                                    index + getPaddedNeighborOffset(BlockFace.EAST),
                                    index + getPaddedNeighborOffset(BlockFace.WEST),
                                    transparentVertices,
                                    transparentIndices,
                                    transparentVertexIndexMap,
                                    chunkLightingData,
                                    blockMeshes,
                                    rotations,
                                    position3D
                            );
                        } else if (blockMesh.shouldRenderDecorationMesh()) {
                            generateBlockMeshData(
                                    x,
                                    y,
                                    z,
                                    blockMesh,
                                    index + getPaddedNeighborOffset(BlockFace.TOP),
                                    index + getPaddedNeighborOffset(BlockFace.BOTTOM),
                                    index + getPaddedNeighborOffset(BlockFace.NORTH),
                                    index + getPaddedNeighborOffset(BlockFace.SOUTH),
                                    index + getPaddedNeighborOffset(BlockFace.EAST),
                                    index + getPaddedNeighborOffset(BlockFace.WEST),
                                    decorationVertices,
                                    decorationIndices,
                                    decorationVertexIndexMap,
                                    chunkLightingData,
                                    blockMeshes,
                                    rotations,
                                    position3D
                            );
                        } else {
                            generateBlockMeshData(
                                    x,
                                    y,
                                    z,
                                    blockMesh,
                                    index + getPaddedNeighborOffset(BlockFace.TOP),
                                    index + getPaddedNeighborOffset(BlockFace.BOTTOM),
                                    index + getPaddedNeighborOffset(BlockFace.NORTH),
                                    index + getPaddedNeighborOffset(BlockFace.SOUTH),
                                    index + getPaddedNeighborOffset(BlockFace.EAST),
                                    index + getPaddedNeighborOffset(BlockFace.WEST),
                                    solidVertices,
                                    solidIndices,
                                    vertexIndexMap,
                                    chunkLightingData,
                                    blockMeshes,
                                    rotations,
                                    position3D
                            );
                        }
                    }
                }
            }
        }

        ByteBuffer vertexBuffer = MeshDataGenerator.createIntBuffer(solidVertices);
        ByteBuffer indexBuffer = MeshDataGenerator.createIntBuffer(solidIndices);
        ByteBuffer transparentVertexBuffer = MeshDataGenerator.createIntBuffer(transparentVertices);
        ByteBuffer transparentIndexBuffer = MeshDataGenerator.createIntBuffer(transparentIndices);
        ByteBuffer decorationVertexBuffer = MeshDataGenerator.createIntBuffer(decorationVertices);
        ByteBuffer decorationIndexBuffer = MeshDataGenerator.createIntBuffer(decorationIndices);

        return new ChunkMeshData(vertexBuffer, indexBuffer, transparentVertexBuffer, transparentIndexBuffer, decorationVertexBuffer, decorationIndexBuffer, lod, position3D);
    }

    private void generateBlockMeshData(
            int x, int y, int z,
            BlockMesh blockMesh, int topIndex, int bottomIndex, int northIndex, int southIndex, int eastIndex, int westIndex,
            IntegerDynamicList vertices, IntegerDynamicList indices, Map<UniqueVertex, Integer> vertexIndexMap, ChunkLightingData chunkLightingData,
            BlockMesh[] blockMeshes,
            byte[] rotations,
            Position3D chunkPosition
    ) {
        BlockShape shape = blockMesh.getShape();
        int index = IndexCalculator.calculateBlockIndexPadded(x, y, z, chunkWidth, chunkHeight, chunkLength);
        byte rotation = blockMesh.isRotatable() ? rotations[index] : 0;
        byte rotationOffset = (byte) ((rotation & 3) * 6);

        addFaceIfVisible(x, y, z, blockMesh, shape, 0, rotationOffset, blockMeshes[topIndex], getRotation(blockMeshes[topIndex], rotations, topIndex), vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        addFaceIfVisible(x, y, z, blockMesh, shape, 1, rotationOffset, blockMeshes[bottomIndex], getRotation(blockMeshes[bottomIndex], rotations, bottomIndex), vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        addFaceIfVisible(x, y, z, blockMesh, shape, 2, rotationOffset, blockMeshes[northIndex], getRotation(blockMeshes[northIndex], rotations, northIndex), vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        addFaceIfVisible(x, y, z, blockMesh, shape, 3, rotationOffset, blockMeshes[southIndex], getRotation(blockMeshes[southIndex], rotations, southIndex), vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        addFaceIfVisible(x, y, z, blockMesh, shape, 4, rotationOffset, blockMeshes[eastIndex], getRotation(blockMeshes[eastIndex], rotations, eastIndex), vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        addFaceIfVisible(x, y, z, blockMesh, shape, 5, rotationOffset, blockMeshes[westIndex], getRotation(blockMeshes[westIndex], rotations, westIndex), vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
    }

    private byte getRotation(BlockMesh blockMesh, byte[] rotations, int index) {
        return blockMesh != null && blockMesh.isRotatable() ? rotations[index] : 0;
    }

    private void addFaceIfVisible(
            int x, int y, int z,
            BlockMesh blockMesh,
            BlockShape shape,
            int worldFace,
            byte rotationOffset,
            BlockMesh adjacent,
            byte adjacentRotation,
            IntegerDynamicList vertices,
            IntegerDynamicList indices,
            Map<UniqueVertex, Integer> vertexIndexMap,
            ChunkLightingData chunkLightingData,
            BlockMesh[] blockMeshes,
            Position3D chunkPosition
    ) {
        BlockFace sourceFace = UNROTATE[rotationOffset + worldFace];
        BlockFace face = BlockFace.NORMAL_VALUES[worldFace];
        if (shouldRenderFace(blockMesh, shape, adjacent, sourceFace.ordinal(), worldFace, adjacentRotation)) {
            addFace(x, y, z, blockMesh, shape, sourceFace, face, (byte) (rotationOffset / 6), vertices, indices, vertexIndexMap, chunkLightingData, blockMeshes, chunkPosition);
        }
    }

    private boolean shouldRenderFace(BlockMesh originalBlockMesh, BlockShape originalShape, BlockMesh adjacentBlockMesh, int sourceFaceOrdinal, int worldFaceOrdinal, byte adjacentRotation) {
        if (adjacentBlockMesh == null) {
            Logger.warn("Adjacent block mesh is null");
            return true;
        }

        if (!originalShape.coverable()[sourceFaceOrdinal]) {
            return true;
        }

        BlockFace adjacentSourceFace = UNROTATE[((adjacentRotation & 3) * 6) + (worldFaceOrdinal ^ 1)];
        if (adjacentBlockMesh.getShape().solid()[adjacentSourceFace.ordinal()]) {
            return false;
        }

        if (originalBlockMesh.isSelfOccluded() && originalBlockMesh.getModID().equals(adjacentBlockMesh.getModID())) {
            return false;
        }

        return !originalShape.coversOppositeSelfFace()[sourceFaceOrdinal ^ 1]
                || !originalShape.id().equals(adjacentBlockMesh.getShape().id());
    }

    private void addFace(
            int x,
            int y,
            int z,
            BlockMesh blockMesh,
            BlockShape shape,
            BlockFace blockFace,
            BlockFace worldFace,
            byte rotation,
            IntegerDynamicList vertices,
            IntegerDynamicList indices,
            Map<UniqueVertex, Integer> vertexIndexMap,
            ChunkLightingData chunkLightingData,
            BlockMesh[] blockMeshes,
            Position3D chunkPosition
    ) {
        int[] uvCoordinates = blockMesh.getUVCoordinates(blockFace);
        BlockVertex[] faceVertices = shape.vertices()[blockFace.ordinal()];
        int[] faceIndices = shape.indices()[blockFace.ordinal()];

        for (int idx : faceIndices) {
            BlockVertex pointPosition = faceVertices[idx].rotate(rotation);
            BlockVertex position = pointPosition.add(x, y, z);
            int ambientOcclusionLevel = ambientOcclusion ? sampleAmbientOcclusion(x, y, z, worldFace, pointPosition, blockMeshes) : 4;
            MeshDataGenerator.addPoint(
                    vertices,
                    indices,
                    vertexIndexMap,
                    position,
                    uvCoordinates[idx * 2],
                    uvCoordinates[idx * 2 + 1],
                    worldFace,
                    applyAmbientOcclusion(sampleVertexLight(x, y, z, worldFace, pointPosition, chunkLightingData, chunkPosition, LightChannels.RED, smoothLighting), ambientOcclusionLevel),
                    applyAmbientOcclusion(sampleVertexLight(x, y, z, worldFace, pointPosition, chunkLightingData, chunkPosition, LightChannels.GREEN, smoothLighting), ambientOcclusionLevel),
                    applyAmbientOcclusion(sampleVertexLight(x, y, z, worldFace, pointPosition, chunkLightingData, chunkPosition, LightChannels.BLUE, smoothLighting), ambientOcclusionLevel),
                    applyAmbientOcclusion(sampleVertexLight(x, y, z, worldFace, pointPosition, chunkLightingData, chunkPosition, LightChannels.SKYLIGHT, smoothLighting), ambientOcclusionLevel),
                    ambientOcclusionLevel == 4,
                    blockMesh.getShaderType(),
                    vertexData
            );
        }
    }

    private byte sampleVertexLight(
            int bx, int by, int bz,
            BlockFace face,
            BlockVertex vertex,
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
            BlockVertex vertex,
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

        if (sampleChunk == null || sampleChunk.getLightingData() == null || sampleChunk.getLightingData().getChannel(channel) == null) {
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
            BlockVertex vertex,
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

        boolean side1 = isAmbientOccluder(blockMeshes, sampleX + tx1, sampleY + ty1, sampleZ + tz1, face);
        boolean side2 = isAmbientOccluder(blockMeshes, sampleX + tx2, sampleY + ty2, sampleZ + tz2, face);
        boolean corner = isAmbientOccluder(blockMeshes, sampleX + tx1 + tx2, sampleY + ty1 + ty2, sampleZ + tz1 + tz2, face);

        if (side1 && side2) {
            return 1;
        }

        return 4 - (booleanValue(side1) + booleanValue(side2) + booleanValue(corner));
    }

    private int vertexSign(float coordinate) {
        return coordinate < 0.5f ? -1 : 1;
    }

    private boolean isAmbientOccluder(BlockMesh[] blockMeshes, int x, int y, int z, BlockFace face) {
        if (x < -1 || x > ConstantCommonSettings.CHUNK_WIDTH ||
                y < -1 || y > ConstantCommonSettings.CHUNK_HEIGHT ||
                z < -1 || z > ConstantCommonSettings.CHUNK_LENGTH) {
            return false;
        }

        BlockMesh blockMesh = blockMeshes[IndexCalculator.calculateBlockIndexPadded(x, y, z, chunkWidth, chunkHeight, chunkLength)];
        return blockMesh != null && blockMesh.getShape().solid()[face.ordinal()] && blockMesh.getShape().coverable()[face.ordinal()];
    }

    private int booleanValue(boolean value) {
        return value ? 1 : 0;
    }

    private byte applyAmbientOcclusion(byte light, int ambientOcclusion) {
        return (byte) Math.round((light & 0xFF) * (ambientOcclusion / 4.0f));
    }

    private void setupLOD(Chunk<BlockWithMesh> chunk, Chunk<BlockWithMesh> negXChunkData, Chunk<BlockWithMesh> posXChunkData, Chunk<BlockWithMesh> negYChunkData, Chunk<BlockWithMesh> posYChunkData, Chunk<BlockWithMesh> negZChunkData, Chunk<BlockWithMesh> posZChunkData) {
        lod = Math.min(chunk.getLOD(), Math.min(negXChunkData.getLOD(), Math.min(posXChunkData.getLOD(), Math.min(negYChunkData.getLOD(), Math.min(posYChunkData.getLOD(), Math.min(negZChunkData.getLOD(), posZChunkData.getLOD()))))));

        chunkWidth = ConstantCommonSettings.CHUNK_WIDTH >> lod;
        chunkHeight = ConstantCommonSettings.CHUNK_HEIGHT >> lod;
        chunkLength = ConstantCommonSettings.CHUNK_LENGTH >> lod;

        int paddedSize =
                (chunkWidth + 2) *
                        (chunkHeight + 2) *
                        (chunkLength + 2);

        blockMeshes = new BlockMesh[paddedSize];
        rotations = new byte[paddedSize];
    }

    private boolean unpackChunkPadded(Position3D position3D) {
        ClientWorldChunk centerChunk = world.get(position3D, false, false);

        if (centerChunk == null) {
            Logger.warn(Logger.Priority.LOW, "The center chunk is null");
            return true;
        }

        ClientWorldChunk negXChunk = world.get(position3D.add(-1, 0, 0), false, true);
        ClientWorldChunk posXChunk = world.get(position3D.add(1, 0, 0), false, true);
        ClientWorldChunk negYChunk = world.get(position3D.add(0, -1, 0), false, true);
        ClientWorldChunk posYChunk = world.get(position3D.add(0, 1, 0), false, true);
        ClientWorldChunk negZChunk = world.get(position3D.add(0, 0, -1), false, true);
        ClientWorldChunk posZChunk = world.get(position3D.add(0, 0, 1), false, true);

        if (negXChunk == null || posXChunk == null ||
                negYChunk == null || posYChunk == null ||
                negZChunk == null || posZChunk == null) {
            Logger.warn(Logger.Priority.LOW, "One or more shell chunks are null");
            return true;
        }

        setupLOD(centerChunk.getChunkData(-1), negXChunk.getChunkData(-1), posXChunk.getChunkData(-1), negYChunk.getChunkData(-1), posYChunk.getChunkData(-1), negZChunk.getChunkData(-1), posZChunk.getChunkData(-1));

        Chunk<BlockWithMesh> center = centerChunk.getChunkData(lod);

        Chunk<BlockWithMesh> negX = negXChunk.getChunkData(lod);
        Chunk<BlockWithMesh> posX = posXChunk.getChunkData(lod);
        Chunk<BlockWithMesh> negY = negYChunk.getChunkData(lod);
        Chunk<BlockWithMesh> posY = posYChunk.getChunkData(lod);
        Chunk<BlockWithMesh> negZ = negZChunk.getChunkData(lod);
        Chunk<BlockWithMesh> posZ = posZChunk.getChunkData(lod);

        if (negX == null || posX == null ||
                negY == null || posY == null ||
                negZ == null || posZ == null) {
            Logger.warn(Logger.Priority.LOW, "One or more shell chunk data are null");
            return true;
        }

        for (int x = -1; x <= chunkWidth; x++) {
            for (int y = -1; y <= chunkHeight; y++) {
                for (int z = -1; z <= chunkLength; z++) {
                    int outside = 0;

                    if (x < 0 || x >= chunkWidth) outside++;
                    if (y < 0 || y >= chunkHeight) outside++;
                    if (z < 0 || z >= chunkLength) outside++;

                    if (outside > 1) {
                        continue;
                    }

                    Chunk<BlockWithMesh> chunk = center;

                    int lx = x;
                    int ly = y;
                    int lz = z;

                    if (x < 0) {
                        chunk = negX;
                        lx = chunkWidth - 1;
                    } else if (x >= chunkWidth) {
                        chunk = posX;
                        lx = 0;
                    } else if (y < 0) {
                        chunk = negY;
                        ly = chunkHeight - 1;
                    } else if (y >= chunkHeight) {
                        chunk = posY;
                        ly = 0;
                    } else if (z < 0) {
                        chunk = negZ;
                        lz = chunkLength - 1;
                    } else if (z >= chunkLength) {
                        chunk = posZ;
                        lz = 0;
                    }

                    int index = IndexCalculator.calculateBlockIndexPadded(x, y, z, chunkWidth, chunkHeight, chunkLength);

                    blockMeshes[index] = chunk.getBlock(lx, ly, lz).blockMesh();
                    rotations[index] = chunk.getBlockRotation(lx, ly, lz);
                }
            }
        }

        return false;
    }

    public MeshData generateMeshData(Position3D position3D) {
        if (unpackChunkPadded(position3D)) {
            return null;
        }
        return generateChunkMeshData(position3D);
    }
}
