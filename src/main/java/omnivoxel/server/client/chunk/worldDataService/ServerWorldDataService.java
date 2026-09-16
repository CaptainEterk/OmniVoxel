package omnivoxel.server.client.chunk.worldDataService;

import omnivoxel.common.annotations.NotNull;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk2d.Chunk2D;
import omnivoxel.world.chunk2d.SingleBlockChunk2D;

public final class ServerWorldDataService {
    private static final int HEIGHT_STEP = 8;
    private final ServerBlockService blockService;
    private final WorldGenerator worldGenerator;

    public ServerWorldDataService(ServerBlockService blockService, WorldGenerator worldGenerator) {
        this.blockService = blockService;
        this.worldGenerator = worldGenerator;
    }

    public boolean shouldGenerateChunk(Position3D position3D) {
        boolean withinX = (worldGenerator.getChunkMinX() == null || worldGenerator.getChunkMaxX() == null) ||
                (position3D.x() >= worldGenerator.getChunkMinX() && position3D.x() <= worldGenerator.getChunkMaxX());
        boolean withinY = (worldGenerator.getChunkMinY() == null || worldGenerator.getChunkMaxY() == null) ||
                (position3D.y() >= worldGenerator.getChunkMinY() && position3D.y() <= worldGenerator.getChunkMaxY());
        boolean withinZ = (worldGenerator.getChunkMinZ() == null || worldGenerator.getChunkMaxZ() == null) ||
                (position3D.z() >= worldGenerator.getChunkMinZ() && position3D.z() <= worldGenerator.getChunkMaxZ());

        return withinX && withinY && withinZ;
    }

    public boolean shouldGenerateBlock(int worldX, int worldY, int worldZ) {
        boolean withinX = (worldGenerator.getBlockMinX() == null || worldGenerator.getBlockMaxX() == null) ||
                (worldX >= worldGenerator.getBlockMinX() && worldX < worldGenerator.getBlockMaxX());
        boolean withinY = (worldGenerator.getBlockMinY() == null || worldGenerator.getBlockMaxY() == null) ||
                (worldY >= worldGenerator.getBlockMinY() && worldY < worldGenerator.getBlockMaxY());
        boolean withinZ = (worldGenerator.getBlockMinZ() == null || worldGenerator.getBlockMaxZ() == null) ||
                (worldZ >= worldGenerator.getBlockMinZ() && worldZ < worldGenerator.getBlockMaxZ());

        return withinX && withinY && withinZ;
    }

    @NotNull
    public ServerBlock getBlockAt(int x, int y, int z,
                                  int worldX, int worldY, int worldZ,
                                  ChunkInfo chunkInfo) {
        if (!shouldGenerateBlock(worldX, worldY, worldZ)) {
            return ServerBlock.AIR;
        }

        double density = chunkInfo.densityCache()[IndexCalculator.calculateBlockIndexPadded(x, y, z)];
        double ncFloor = chunkInfo.densityCache()[IndexCalculator.calculateBlockIndexPadded(x, y - 1, z)];
        double ncCeiling = chunkInfo.densityCache()[IndexCalculator.calculateBlockIndexPadded(x, y + 1, z)];

        boolean isFloor = ncFloor > 0;
        boolean isCeiling = ncCeiling > 0;

        String result = (density >= 0 ? worldGenerator.getBlockFunction() : worldGenerator.getNegDensityBlockFunction()).evaluate(
                density, null,
                isFloor, isCeiling,
                chunkInfo.heights()[IndexCalculator.calculateBlockIndexPadded2D(x, z)] - worldY,
                worldX, worldY, worldZ
        );

        return blockService.getBlock(result);
    }

    public ChunkInfo getChunkInfo(ServerWorld world, Position3D position3D, int lod) {
        int chunkWidth = ConstantCommonSettings.CHUNK_WIDTH;
        int chunkHeight = ConstantCommonSettings.CHUNK_HEIGHT;
        int chunkLength = ConstantCommonSettings.CHUNK_LENGTH;

        int step = (1 << worldGenerator.getNoiseInterpolationScale()) << lod;

        int sx = Math.ceilDiv(chunkWidth + 2, step) + 1;
        int sy = Math.ceilDiv(chunkHeight + 2, step) + 1;
        int sz = Math.ceilDiv(chunkLength + 2, step) + 1;

        double[] sparse = new double[sx * sy * sz];

        for (int ix = 0; ix < sx; ix++) {
            int x = ix * step - 1;
            int worldX =
                    position3D.x() * chunkWidth + x;

            for (int iz = 0; iz < sz; iz++) {
                int z = iz * step - 1;
                int worldZ = position3D.z() * chunkLength + z;

                for (int iy = 0; iy < sy; iy++) {
                    int y = iy * step - 1;
                    int worldY = position3D.y() * chunkHeight + y;

                    int index = ix + sx * (iy + sy * iz);

                    sparse[index] =
                            worldGenerator.getDensityFunction().evaluate(worldX, worldY, worldZ);
                }
            }
        }

        double[] densityCache = new double[ConstantCommonSettings.BLOCKS_IN_CHUNK_PADDED];

        for (int x = -1; x <= chunkWidth; x++) {
            int xp = x + 1;

            int gx = Math.floorDiv(xp, step);
            double fx = Math.floorMod(xp, step) / (double) step;

            for (int z = -1; z <= chunkLength; z++) {
                int zp = z + 1;

                int gz = Math.floorDiv(zp, step);
                double fz = Math.floorMod(zp, step) / (double) step;

                for (int y = -1; y <= chunkHeight; y++) {
                    int yp = y + 1;

                    int gy = Math.floorDiv(yp, step);
                    double fy = Math.floorMod(yp, step) / (double) step;

                    int base000 = gx + sx * (gy + sy * gz);

                    int base010 = gx + sx * ((gy + 1) + sy * gz);

                    int base001 = gx + sx * (gy + sy * (gz + 1));

                    int base011 = gx + sx * ((gy + 1) + sy * (gz + 1));

                    double c000 = sparse[base000];
                    double c100 = sparse[base000 + 1];

                    double c010 = sparse[base010];
                    double c110 = sparse[base010 + 1];

                    double c001 = sparse[base001];
                    double c101 = sparse[base001 + 1];

                    double c011 = sparse[base011];
                    double c111 = sparse[base011 + 1];

                    double x00 = c000 + fx * (c100 - c000);

                    double x10 = c010 + fx * (c110 - c010);

                    double x01 = c001 + fx * (c101 - c001);

                    double x11 = c011 + fx * (c111 - c011);

                    double y0 = x00 + fy * (x10 - x00);

                    double y1 = x01 + fy * (x11 - x01);

                    double value = y0 + fz * (y1 - y0);

                    densityCache[IndexCalculator.calculateBlockIndexPadded(x, y, z)] = value;
                }
            }
        }

        Position2D position2D = position3D.getPosition2D();

        int[] heights = new int[(chunkWidth + 2) * (chunkLength + 2)];

        Chunk2D<Integer> chunkHeights = world.getStoredChunkHeights(position2D);

        boolean cachedHeights = chunkHeights != null;

        if (!cachedHeights) {
            chunkHeights = new SingleBlockChunk2D<>(0);
        }

        if (worldGenerator.getChunkMaxY() != null && worldGenerator.getChunkMinY() != null) {
            for (int x = -1; x <= chunkWidth; x++) {
                int worldX = position3D.x() * chunkWidth + x;

                for (int z = -1; z <= chunkLength; z++) {
                    int worldZ = position3D.z() * chunkLength + z;

                    boolean insideChunk =
                            x >= 0
                                    && x < chunkWidth
                                    && z >= 0
                                    && z < chunkLength;


                    if (cachedHeights && insideChunk) {
                        heights[IndexCalculator.calculateBlockIndexPadded2D(x, z, chunkWidth + 2)] = chunkHeights.getBlock(x, z);

                        continue;
                    }

                    int height = worldGenerator.getBlockMinY();

                    for (
                            int worldY = worldGenerator.getBlockMaxY() - 1;
                            worldY >= worldGenerator.getBlockMinY();
                            worldY -= HEIGHT_STEP
                    ) {
                        double density =
                                worldGenerator
                                        .getHeightFunction()
                                        .evaluate(
                                                worldX,
                                                worldY,
                                                worldZ
                                        );

                        if (density >= 0.0) {
                            height = worldY;

                            for (int i = HEIGHT_STEP - 1; i > 0; i--) {
                                if (worldGenerator.getHeightFunction().evaluate(worldX, worldY + i, worldZ) >= 0.0) {
                                    height = worldY + i;
                                    break;
                                }
                            }

                            break;
                        }
                    }

                    heights[IndexCalculator.calculateBlockIndexPadded2D(x, z, chunkWidth + 2)] = height;

                    if (!cachedHeights && insideChunk) {
                        chunkHeights = chunkHeights.setBlock(x, z, height);
                    }
                }
            }
        }

        if (!cachedHeights) {
            world.putChunkHeights(position2D, chunkHeights);
        }

        return new ChunkInfo(heights, densityCache);
    }

    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
}
