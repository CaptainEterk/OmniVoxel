package omnivoxel.client.game.graphics.opengl.mesh.generators;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.graphics.opengl.mesh.ShapeHelper;
import omnivoxel.client.game.graphics.opengl.mesh.block.Block;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.ChunkMeshData;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.opengl.mesh.vertex.TextureVertex;
import omnivoxel.client.game.graphics.opengl.mesh.vertex.UniqueVertex;
import omnivoxel.client.game.graphics.opengl.mesh.vertex.Vertex;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.common.BlockShape;
import omnivoxel.common.face.BlockFace;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.BlockService;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.ChunkShell;
import omnivoxel.world.chunk.SingleBlockChunk;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;

public class ChunkMeshDataGenerator {
    public static final omnivoxel.world.block.Block air = new omnivoxel.world.block.Block("omnivoxel:air");
    private final ClientWorldDataService worldDataService;
    private final BlockService blockService;

    public ChunkMeshDataGenerator(ClientWorldDataService worldDataService, BlockService blockService) {
        this.worldDataService = worldDataService;
        this.blockService = blockService;
    }

    private void addPoint(List<Integer> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap, Vertex position, int tx, int ty, BlockFace normal, float r, float g, float b, int type) {
        UniqueVertex vertex = new UniqueVertex(position, new TextureVertex(tx, ty), normal);

        if (!vertexIndexMap.containsKey(vertex)) {
            int[] vertexData = ShapeHelper.packVertexData(position, 0, r, g, b, normal, tx, ty, type);
            vertexIndexMap.put(vertex, vertices.size());
            for (int data : vertexData) {
                vertices.add(data);
            }
        }
        indices.add(vertexIndexMap.get(vertex) / 3);
    }

    private MeshData generateChunkMeshData(Block[] blocks, Position3D position3D) {
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
                    Block block = blocks[index];
                    if (block != null) {
                        if (block.shouldRenderTransparentMesh()) {
                            generateBlockMeshData(
                                    x,
                                    y,
                                    z,
                                    block,
                                    blocks[index + BlockFace.TOP.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.BOTTOM.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.NORTH.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.SOUTH.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.EAST.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.WEST.getPaddedNeighborOffset()],
                                    transparentVertices,
                                    transparentIndices,
                                    transparentVertexIndexMap
                            );
                        } else {
                            generateBlockMeshData(
                                    x,
                                    y,
                                    z,
                                    block,
                                    blocks[index + BlockFace.TOP.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.BOTTOM.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.NORTH.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.SOUTH.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.EAST.getPaddedNeighborOffset()],
                                    blocks[index + BlockFace.WEST.getPaddedNeighborOffset()],
                                    vertices,
                                    indices,
                                    vertexIndexMap
                            );
                        }
                    }
                }
            }
        }

        ByteBuffer vertexBuffer = createBuffer(vertices);
        ByteBuffer indexBuffer = createBuffer(indices);
        ByteBuffer transparentVertexBuffer = createBuffer(transparentVertices);
        ByteBuffer transparentIndexBuffer = createBuffer(transparentIndices);

        return new ChunkMeshData(vertexBuffer, indexBuffer, transparentVertexBuffer, transparentIndexBuffer, position3D);
    }

    private void generateBlockMeshData(
            int x, int y, int z,
            Block block, Block top, Block bottom, Block north, Block south, Block east, Block west,
            List<Integer> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap) {

        BlockShape shape = block.getShape(top, bottom, north, south, east, west);

        boolean renderTop = shouldRenderFaceCached(block, shape, top, BlockFace.TOP, top, bottom, north, south, east, west);
        boolean renderBottom = shouldRenderFaceCached(block, shape, bottom, BlockFace.BOTTOM, top, bottom, north, south, east, west);
        boolean renderNorth = shouldRenderFaceCached(block, shape, north, BlockFace.NORTH, top, bottom, north, south, east, west);
        boolean renderSouth = shouldRenderFaceCached(block, shape, south, BlockFace.SOUTH, top, bottom, north, south, east, west);
        boolean renderEast = shouldRenderFaceCached(block, shape, east, BlockFace.EAST, top, bottom, north, south, east, west);
        boolean renderWest = shouldRenderFaceCached(block, shape, west, BlockFace.WEST, top, bottom, north, south, east, west);

        if (renderTop)
            addFacePrecomputedShape(x, y, z, block, shape, BlockFace.TOP, vertices, indices, vertexIndexMap);
        if (renderBottom)
            addFacePrecomputedShape(x, y, z, block, shape, BlockFace.BOTTOM, vertices, indices, vertexIndexMap);
        if (renderNorth)
            addFacePrecomputedShape(x, y, z, block, shape, BlockFace.NORTH, vertices, indices, vertexIndexMap);
        if (renderSouth)
            addFacePrecomputedShape(x, y, z, block, shape, BlockFace.SOUTH, vertices, indices, vertexIndexMap);
        if (renderEast)
            addFacePrecomputedShape(x, y, z, block, shape, BlockFace.EAST, vertices, indices, vertexIndexMap);
        if (renderWest)
            addFacePrecomputedShape(x, y, z, block, shape, BlockFace.WEST, vertices, indices, vertexIndexMap);
    }

    private boolean shouldRenderFaceCached(Block originalBlock, BlockShape originalShape, Block adjacentBlock, BlockFace face,
                                           Block top, Block bottom, Block north, Block south, Block east, Block west) {
        if (adjacentBlock == null) {
            return true;
        }

        if (adjacentBlock.isTransparent() && !Objects.equals(adjacentBlock.getModID(), originalBlock.getModID())) {
            return true;
        }
        BlockShape adjBlockShape = adjacentBlock.getShape(top, bottom, north, south, east, west);
        return !(originalShape.solid()[face.ordinal()] && adjBlockShape.solid()[face.ordinal()])
                && originalBlock.shouldRenderFace(face, adjacentBlock);
    }

    private void addFacePrecomputedShape(int x, int y, int z, Block block, BlockShape shape, BlockFace blockFace,
                                         List<Integer> vertices, List<Integer> indices, Map<UniqueVertex, Integer> vertexIndexMap) {
        int[] uvCoordinates = block.getUVCoordinates(blockFace);
        Vertex[] faceVertices = shape.vertices()[blockFace.ordinal()];
        int[] faceIndices = shape.indices()[blockFace.ordinal()];

        // TODO: Remove all hardcoding
        int blockType = Objects.equals(block.getModID(), "core:water_source_block") ? 1 : 0;

        for (int idx : faceIndices) {
            Vertex pointPosition = faceVertices[idx];
            Vertex position = pointPosition.add(x, y, z);
            addPoint(
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
                    blockType
            );
        }
    }

    private ByteBuffer createBuffer(List<Integer> data) {
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

    private ChunkBlockData unpackChunk(ByteBuf byteBuf, Position3D pos, ClientWorld world) {
        omnivoxel.world.block.Block[] palette = new omnivoxel.world.block.Block[byteBuf.getShort(20)];

        int index = 22;

        for (int i = 0; i < palette.length; i++) {
            short len = byteBuf.getShort(index);
            StringBuilder id = new StringBuilder();

            for (int j = 0; j < len; j++) {
                id.append((char) byteBuf.getByte(index + 2 + j));
            }

            palette[i] = new omnivoxel.world.block.Block(id.toString());
            index += 2 + len;
        }

        Chunk<omnivoxel.world.block.Block> center = new SingleBlockChunk<>(air);
        Chunk<omnivoxel.world.block.Block> negX = new ChunkShell<>();
        Chunk<omnivoxel.world.block.Block> posX = new ChunkShell<>();
        Chunk<omnivoxel.world.block.Block> negY = new ChunkShell<>();
        Chunk<omnivoxel.world.block.Block> posY = new ChunkShell<>();
        Chunk<omnivoxel.world.block.Block> negZ = new ChunkShell<>();
        Chunk<omnivoxel.world.block.Block> posZ = new ChunkShell<>();

        Block[] blocks = new Block[ConstantGameSettings.BLOCKS_IN_CHUNK_PADDED];

        int x = -1, y = -1, z = -1;

        for (int i = 0; i < ConstantGameSettings.BLOCKS_IN_CHUNK_PADDED; ) {
            int blockID = byteBuf.getInt(index);
            int blockCount = byteBuf.getInt(index + 4);
            index += 8;

            Block block = worldDataService.getBlock(palette[blockID].id());

            for (int j = 0; j < blockCount; j++) {
                int paddedIndex = i + j;
                blocks[paddedIndex] = block;

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

        return new ChunkBlockData(center, blocks);
    }

    public MeshData generateMeshData(ByteBuf blocks, Position3D position3D, ClientWorld world) {
        if (blocks == null) {
            return generateMeshData(position3D, world);
        }
        ChunkBlockData chunk = unpackChunk(blocks, position3D, world);
        return generateChunkMeshData(chunk.blocks(), position3D);
    }

    public MeshData generateMeshData(Position3D position3D, ClientWorld world) {
        ClientWorldChunk centerChunk = world.get(position3D, false, false);
        Chunk<omnivoxel.world.block.Block> center = centerChunk == null ? null : centerChunk.getChunkData();
        if (center == null) {
            System.out.println("Center is null");
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

        Chunk<omnivoxel.world.block.Block> negX = negXChunk.getChunkData();
        Chunk<omnivoxel.world.block.Block> posX = posXChunk.getChunkData();
        Chunk<omnivoxel.world.block.Block> negY = negYChunk.getChunkData();
        Chunk<omnivoxel.world.block.Block> posY = posYChunk.getChunkData();
        Chunk<omnivoxel.world.block.Block> negZ = negZChunk.getChunkData();
        Chunk<omnivoxel.world.block.Block> posZ = posZChunk.getChunkData();

        int W = ConstantGameSettings.CHUNK_WIDTH;
        int H = ConstantGameSettings.CHUNK_HEIGHT;
        int L = ConstantGameSettings.CHUNK_LENGTH;

        Block[] blocks = new Block[ConstantGameSettings.BLOCKS_IN_CHUNK_PADDED];

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

                    Chunk<omnivoxel.world.block.Block> chunk;
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

                    Block block;
                    try {
                        block = worldDataService.getBlock(
                                chunk.getBlock(lx, ly, lz).id()
                        );
                    } catch (Exception e) {
                        System.out.println(negX + " " + posX + " " + negY + " " + posY + " " + posZ + " " + negZ + " " + position3D);
                        System.out.println(lx + " " + ly + " " + lz + " " + x + " " + y + " " + z);
                        System.out.println(chunk.getBlock(lx, ly, lz) + " " + chunk);
                        throw e;
                    }

                    int index = IndexCalculator.calculateBlockIndexPadded(x, y, z);

                    blocks[index] = block;
                }
            }
        }

        return generateChunkMeshData(blocks, position3D);
    }
}