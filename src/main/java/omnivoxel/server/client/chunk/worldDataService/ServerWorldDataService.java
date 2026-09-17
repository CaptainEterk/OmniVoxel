package omnivoxel.server.client.chunk.worldDataService;

import omnivoxel.common.annotations.NotNull;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk2d.Chunk2D;
import omnivoxel.world.chunk2d.SingleBlockChunk2D;

public final class ServerWorldDataService {
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

    private double heightCalcHelper(double[] sparse, double[] densityCache, int sparseMinY, int chunkWorldMinY, int chunkHeight, int sx, int sy, int gx, int gz, double fx, double fz, int x, int z, int step, int worldY) {
        int yp = worldY - sparseMinY;

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

        int y = worldY - chunkWorldMinY;
        if (y >= -1 && y <= chunkHeight) {
            densityCache[IndexCalculator.calculateBlockIndexPadded(x, y, z)] = value;
        }

        return value;
    }

    public ChunkInfo getChunkInfo(ServerWorld world, Position3D position3D, int lod) {
        int chunkWidth = ConstantCommonSettings.CHUNK_WIDTH;
        int chunkHeight = ConstantCommonSettings.CHUNK_HEIGHT;
        int chunkLength = ConstantCommonSettings.CHUNK_LENGTH;

        int blockMinY = worldGenerator.getBlockMinY();
        int blockMaxY = worldGenerator.getBlockMaxY();

        int chunkMinY = worldGenerator.getChunkMinY();
        int chunkMaxY = worldGenerator.getChunkMaxY();

        int step = (1 << worldGenerator.getNoiseInterpolationScale()) << lod;

        int sparseMinY = blockMinY - 1;

        int sparseVerticalSize = blockMaxY - blockMinY + 2;

        int sx = Math.ceilDiv(chunkWidth + 2, step) + 1;

        int sy = Math.ceilDiv(sparseVerticalSize, step) + 1;

        int sz = Math.ceilDiv(chunkLength + 2, step) + 1;

        double[] sparse = new double[sx * sy * sz];

        for (int ix = 0; ix < sx; ix++) {
            int localX = ix * step - 1;

            int worldX = position3D.x() * chunkWidth + localX;

            for (int iz = 0; iz < sz; iz++) {
                int localZ = iz * step - 1;

                int worldZ = position3D.z() * chunkLength + localZ;

                for (int iy = 0; iy < sy; iy++) {
                    int worldY = sparseMinY + iy * step;

                    int index = ix + sx * (iy + sy * iz);

                    sparse[index] = worldGenerator
                            .getDensityFunction()
                            .evaluate(worldX, worldY, worldZ);
                }
            }
        }

        double[] densityCache = new double[ConstantCommonSettings.BLOCKS_IN_CHUNK_PADDED];

        Chunk2D<Integer> chunkHeights = world.getStoredChunkHeights(position3D.getPosition2D());

        int[] heights = new int[(chunkWidth + 2) * (chunkLength + 2)];

        boolean cachedHeights = chunkHeights != null;

        if (!cachedHeights) {
            chunkHeights = new SingleBlockChunk2D<>(blockMinY);
        }

        int chunkWorldMinY = position3D.y() * chunkHeight;

        int chunkWorldMaxY = chunkWorldMinY + chunkHeight - 1;

        if (chunkWorldMinY < blockMinY || chunkWorldMaxY > blockMaxY) {
            throw new IllegalStateException(
                    "Chunk outside generator vertical range: "
                            + "chunkY=" + position3D.y()
                            + ", chunkWorldMinY=" + chunkWorldMinY
                            + ", chunkWorldMaxY=" + chunkWorldMaxY
                            + ", blockMinY=" + blockMinY
                            + ", blockMaxY=" + blockMaxY
                            + ", chunkMinY=" + chunkMinY
                            + ", chunkMaxY=" + chunkMaxY);
        }

        for (int x = -1; x <= chunkWidth; x++) {
            int xp = x + 1;

            int gx = Math.floorDiv(xp, step);

            double fx = Math.floorMod(xp, step) / (double) step;

            for (int z = -1; z <= chunkLength; z++) {
                int zp = z + 1;

                int gz = Math.floorDiv(zp, step);

                double fz = Math.floorMod(zp, step) / (double) step;

                int height = blockMinY;

                for (int worldY = worldGenerator.getBlockMaxY(); worldY >= worldGenerator.getBlockMinY(); ) {
                    double value = heightCalcHelper(sparse, densityCache, sparseMinY, chunkWorldMinY, chunkHeight, sx, sy, gx, gz, fx, fz, x, z, step, worldY);

                    if (value > 0.0 && worldY > height) {
                        height = worldY;

                        if (worldY > chunkWorldMaxY + 1) {
                            worldY = chunkWorldMaxY + 1;
                        } else if (worldY <= chunkWorldMinY) {
                            break;
                        }
                    } else if (worldY <= chunkWorldMinY && height > worldY) {
                        break;
                    }

                    if (worldY > height && (worldY - worldGenerator.getHeightStep() > chunkWorldMaxY || worldY < chunkWorldMinY)) {
                        worldY -= worldGenerator.getHeightStep();
                        if (value > 0.0) {
                            height = worldY;
                            for (int i = worldGenerator.getHeightStep() - 1; i > 0; i--) {
                                if (heightCalcHelper(sparse, densityCache, sparseMinY, chunkWorldMinY, chunkHeight, sx, sy, gx, gz, fx, fz, x, z, step, worldY + i) > 0.0) {
                                    height = worldY + i;
                                    break;

                                }
                            }
                        }
                    } else {
                        worldY--;
                    }
                }

                int heightIndex = IndexCalculator.calculateBlockIndexPadded2D(x, z, chunkWidth + 2);

                heights[heightIndex] = height;

                if (!cachedHeights
                        && x >= 0 && x < chunkWidth
                        && z >= 0 && z < chunkLength
                ) {
                    chunkHeights = chunkHeights.setBlock(x, z, height);
                }
            }
        }

        if (!cachedHeights) {
            world.putChunkHeights(position3D.getPosition2D(), chunkHeights);
        }

        return new ChunkInfo(heights, densityCache);
    }

    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
}
