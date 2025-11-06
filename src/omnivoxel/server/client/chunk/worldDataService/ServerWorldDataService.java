package omnivoxel.server.client.chunk.worldDataService;

import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.common.BlockShape;
import omnivoxel.common.annotations.NotNull;
import omnivoxel.server.ConstantServerSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.worldDataService.block.BlockFunction;
import omnivoxel.server.client.chunk.worldDataService.block.functions.ConditionBlockFunction;
import omnivoxel.server.client.chunk.worldDataService.block.functions.OneBlockFunction;
import omnivoxel.server.client.chunk.worldDataService.block.functions.SequenceBlockFunction;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.client.chunk.worldDataService.density.functions.*;
import omnivoxel.server.games.Game;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.cache.NoiseCache;
import omnivoxel.util.config.Config;
import omnivoxel.util.game.nodes.*;
import omnivoxel.util.math.Position3D;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public final class ServerWorldDataService {
    private static final Map<String, Class<? extends DensityFunction>> densityFunctionCache = new HashMap<>();
    private static final Map<String, Class<? extends BlockFunction>> blockFunctionCache = new HashMap<>();
    private final ServerBlockService blockService;
    private final DensityFunction densityFunction;
    private final BlockFunction blockFunction;
    private final DensityFunction heightFunction;

    private final Integer chunkMinX;
    private final Integer chunkMinY;
    private final Integer chunkMinZ;
    private final Integer chunkMaxX;
    private final Integer chunkMaxY;
    private final Integer chunkMaxZ;

    private final Integer blockMinX;
    private final Integer blockMinY;
    private final Integer blockMinZ;
    private final Integer blockMaxX;
    private final Integer blockMaxY;
    private final Integer blockMaxZ;
    private final Integer depthSections;
    private final NoiseCache noiseCache = new NoiseCache();

    public ServerWorldDataService(ServerBlockService blockService, Map<String, BlockShape> blockShapeCache, GameNode gameNode, long seed) {
        this.blockService = blockService;

        addDensityFunction(Noise3DDensityFunction.class);
        addDensityFunction(ValueDensityFunction.class);

        addDensityFunction(XClampedGradientDensityFunction.class);
        addDensityFunction(YClampedGradientDensityFunction.class);
        addDensityFunction(ZClampedGradientDensityFunction.class);

        addDensityFunction(XDensityFunction.class);
        addDensityFunction(YDensityFunction.class);
        addDensityFunction(ZDensityFunction.class);

        addDensityFunction(AddDensityFunction.class);
        addDensityFunction(MulDensityFunction.class);
        addDensityFunction(MinDensityFunction.class);
        addDensityFunction(MaxDensityFunction.class);
        addDensityFunction(AbsDensityFunction.class);
        addDensityFunction(RangeChoiceDensityFunction.class);
        addDensityFunction(InterpolatedDensityFunction.class);
        addDensityFunction(SqueezeDensityFunction.class);
        addDensityFunction(QuarterNegativeDensityFunction.class);
        addDensityFunction(SquareDensityFunction.class);
        addDensityFunction(FlatCacheDensityFunction.class);
        addDensityFunction(Cache2DDensityFunction.class);
        addDensityFunction(CacheOnceDensityFunction.class);
        addDensityFunction(SplineDensityFunction.class);
        addDensityFunction(ShiftedNoiseDensityFunction.class);
        addDensityFunction(ShiftADensityFunction.class);
        addDensityFunction(ShiftBDensityFunction.class);
        addDensityFunction(HalfNegativeDensityFunction.class);
        addDensityFunction(OldBlendedNoiseDensityFunction.class);
        addDensityFunction(ClampDensityFunction.class);
        addDensityFunction(WeirdScaledSamplerDensityFunction.class);
        addDensityFunction(CubeDensityFunction.class);

        addBlockFunction(OneBlockFunction.class);
        addBlockFunction(SequenceBlockFunction.class);
        addBlockFunction(ConditionBlockFunction.class);

        ObjectGameNode worldGeneratorNode = Game.checkGameNodeType(gameNode, ObjectGameNode.class);

        Config gameProperties = new Config(ConstantServerSettings.GAME_LOCATION + "game.properties");

        Game.loadNoises(Game.checkGameNodeType(worldGeneratorNode.object().get("noises"), ArrayGameNode.class), seed);
        Game.loadBlocks(worldGeneratorNode, blockService);
        Game.loadBlockShapes(gameProperties.get("id"), worldGeneratorNode, blockService, blockShapeCache);

        DoubleGameNode chunkMinXNode = Game.checkGameNodeType(worldGeneratorNode.object().get("chunk_min_x"), DoubleGameNode.class);
        DoubleGameNode chunkMinYNode = Game.checkGameNodeType(worldGeneratorNode.object().get("chunk_min_y"), DoubleGameNode.class);
        DoubleGameNode chunkMinZNode = Game.checkGameNodeType(worldGeneratorNode.object().get("chunk_min_z"), DoubleGameNode.class);

        DoubleGameNode chunkMaxXNode = Game.checkGameNodeType(worldGeneratorNode.object().get("chunk_max_x"), DoubleGameNode.class);
        DoubleGameNode chunkMaxYNode = Game.checkGameNodeType(worldGeneratorNode.object().get("chunk_max_y"), DoubleGameNode.class);
        DoubleGameNode chunkMaxZNode = Game.checkGameNodeType(worldGeneratorNode.object().get("chunk_max_z"), DoubleGameNode.class);

        this.chunkMinX = chunkMinXNode == null ? null : (int) chunkMinXNode.value();
        this.chunkMinY = chunkMinYNode == null ? null : (int) chunkMinYNode.value();
        this.chunkMinZ = chunkMinZNode == null ? null : (int) chunkMinZNode.value();

        this.chunkMaxX = chunkMaxXNode == null ? null : (int) chunkMaxXNode.value();
        this.chunkMaxY = chunkMaxYNode == null ? null : (int) chunkMaxYNode.value();
        this.chunkMaxZ = chunkMaxZNode == null ? null : (int) chunkMaxZNode.value();

        blockMinX = chunkMinX == null ? null : chunkMinX * ConstantGameSettings.CHUNK_WIDTH;
        blockMinY = chunkMinY == null ? null : chunkMinY * ConstantGameSettings.CHUNK_HEIGHT;
        blockMinZ = chunkMinZ == null ? null : chunkMinZ * ConstantGameSettings.CHUNK_LENGTH;

        blockMaxX = chunkMaxX == null ? null : (chunkMaxX + 1) * ConstantGameSettings.CHUNK_WIDTH;
        blockMaxY = chunkMaxY == null ? null : (chunkMaxY + 1) * ConstantGameSettings.CHUNK_HEIGHT;
        blockMaxZ = chunkMaxZ == null ? null : (chunkMaxZ + 1) * ConstantGameSettings.CHUNK_LENGTH;

        DoubleGameNode depthSectionsNode = Game.checkGameNodeType(worldGeneratorNode.object().get("depth_sections"), DoubleGameNode.class);

        this.depthSections = depthSectionsNode == null ? null : (int) depthSectionsNode.value();

        densityFunction = getDensityFunction(Game.checkGameNodeType(worldGeneratorNode.object().get("density"), ObjectGameNode.class), seed);
        blockFunction = getBlockFunction(Game.checkGameNodeType(worldGeneratorNode.object().get("surface"), ObjectGameNode.class), seed);
        heightFunction = getDensityFunction(Game.checkGameNodeType(worldGeneratorNode.object().get("heights"), ObjectGameNode.class), seed);
    }

    private static void addDensityFunction(Class<? extends DensityFunction> densityFunctionClass) {
        Function[] annotations = densityFunctionClass.getAnnotationsByType(Function.class);
        if (annotations.length == 0) {
            throw new IllegalArgumentException("Density functions must have the @Function annotation");
        }
        densityFunctionCache.put(annotations[0].id(), densityFunctionClass);
    }

    private static void addBlockFunction(Class<? extends BlockFunction> blockFunctionClass) {
        Function[] annotations = blockFunctionClass.getAnnotationsByType(Function.class);
        if (annotations.length == 0) {
            throw new IllegalArgumentException(blockFunctionClass + " must have the @Function annotation");
        }
        blockFunctionCache.put(annotations[0].id(), blockFunctionClass);
    }

    public static DensityFunction getDensityFunction(GameNode args, long seed) {
        try {
            String type;
            long i;
            if (args instanceof DoubleGameNode doubleGameNode) {
                type = "value";
                i = Double.doubleToLongBits(doubleGameNode.value());
            } else {
                ObjectGameNode objectGameNode = Game.checkGameNodeType(args, ObjectGameNode.class);
                type = Game.checkGameNodeType(objectGameNode.object().get("type"), StringGameNode.class).value();
                i = seed;
            }
            Class<? extends DensityFunction> dfClass = densityFunctionCache.get(type);
            if (dfClass == null) {
                throw new IllegalArgumentException(String.format("%s is not a valid type for a density function", type));
            }
            return dfClass.getConstructor(GameNode.class, long.class).newInstance(args, i);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static BlockFunction getBlockFunction(GameNode args, long seed) {
        try {
            String type = Game.checkGameNodeType(Game.checkGameNodeType(args, ObjectGameNode.class).object().get("type"), StringGameNode.class).value();
            Class<? extends BlockFunction> dfClass = blockFunctionCache.get(type);
            if (dfClass == null) {
                throw new IllegalArgumentException(String.format("%s is not a valid type for a density function", type));
            }
            return dfClass.getConstructor(GameNode.class, long.class).newInstance(args, seed);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean shouldGenerateChunk(Position3D position3D) {
        boolean withinX = (chunkMinX == null || chunkMaxX == null) ||
                (position3D.x() >= chunkMinX && position3D.x() <= chunkMaxX);
        boolean withinY = (chunkMinY == null || chunkMaxY == null) ||
                (position3D.y() >= chunkMinY && position3D.y() <= chunkMaxY);
        boolean withinZ = (chunkMinZ == null || chunkMaxZ == null) ||
                (position3D.z() >= chunkMinZ && position3D.z() <= chunkMaxZ);

        return withinX && withinY && withinZ;
    }

    public boolean shouldGenerateBlock(int worldX, int worldY, int worldZ) {
        boolean withinX = (blockMinX == null || blockMaxX == null) ||
                (worldX >= blockMinX && worldX < blockMaxX);
        boolean withinY = (blockMinY == null || blockMaxY == null) ||
                (worldY >= blockMinY && worldY < blockMaxY);
        boolean withinZ = (blockMinZ == null || blockMaxZ == null) ||
                (worldZ >= blockMinZ && worldZ < blockMaxZ);

        return withinX && withinY && withinZ;
    }

    @NotNull
    public ServerBlock getBlockAt(int x, int y, int z,
                                  int worldX, int worldY, int worldZ,
                                  ChunkInfo chunkInfo) {
        if (!shouldGenerateBlock(worldX, worldY, worldZ)) {
            return ServerBlock.VOID;
        }

        Double ncDensity = noiseCache.getCached(worldX, worldY, worldZ);
        Double ncFloor = noiseCache.getCached(worldX, worldY - 1, worldZ);
        Double ncCeiling = noiseCache.getCached(worldX, worldY + 1, worldZ);

        double density;
        if (ncDensity == null) {
            density = densityFunction.evaluate(worldX, worldY, worldZ);
            noiseCache.cache(worldX, worldY, worldZ, density);
        } else {
            density = ncDensity;
        }

        boolean isFloor = (ncFloor == null ? densityFunction.evaluate(worldX, worldY - 1, worldZ) : ncFloor) > 0;
        boolean isCeiling = (ncCeiling == null ? densityFunction.evaluate(worldX, worldY + 1, worldZ) : ncCeiling) > 0;

        String result = blockFunction.evaluate(
                density, null,
                isFloor, isCeiling,
                chunkInfo.heights()[IndexCalculator.calculateBlockIndexPadded2D(x, z)] - worldY,
                worldX, worldY, worldZ
        );

        return blockService.getBlock(result);
    }

    public ChunkInfo getChunkInfo(Position3D position3D) {
        int[] heights = new int[ConstantGameSettings.PADDED_WIDTH * ConstantGameSettings.PADDED_LENGTH];
        if (chunkMaxY != null && chunkMinY != null) {
            for (int x = -1; x <= ConstantGameSettings.CHUNK_WIDTH; x++) {
                int worldX = position3D.x() * ConstantGameSettings.CHUNK_WIDTH + x;
                for (int z = -1; z <= ConstantGameSettings.CHUNK_LENGTH; z++) {
                    int worldZ = position3D.z() * ConstantGameSettings.CHUNK_LENGTH + z;
                    heights[IndexCalculator.calculateBlockIndexPadded2D(x, z)] = 0;
                    for (int worldY = blockMaxY; worldY > blockMinY; worldY--) {
                        Double height = noiseCache.getCached(worldX, worldY, worldZ);
                        if ((height == null ? heightFunction.evaluate(worldX, worldY, worldZ) : height) > 0) {
                            heights[IndexCalculator.calculateBlockIndexPadded2D(x, z)] = worldY;
                            break;
                        }
                    }
                }
            }
        }
        return new ChunkInfo(heights);
    }
}