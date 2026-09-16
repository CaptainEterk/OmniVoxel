package omnivoxel.server.client.chunk.worldDataService;

import omnivoxel.common.block.hitbox.BlockHitbox;
import omnivoxel.common.block.shape.BlockShape;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.ConstantServerSettings;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.worldDataService.block.BlockFunction;
import omnivoxel.server.client.chunk.worldDataService.block.functions.ConditionBlockFunction;
import omnivoxel.server.client.chunk.worldDataService.block.functions.OneBlockFunction;
import omnivoxel.server.client.chunk.worldDataService.block.functions.SequenceBlockFunction;
import omnivoxel.server.client.chunk.worldDataService.density.DensityFunction;
import omnivoxel.server.client.chunk.worldDataService.density.functions.*;
import omnivoxel.server.games.Game;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.util.config.Config;
import omnivoxel.util.game.nodes.*;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk2d.Chunk2D;
import omnivoxel.world.chunk2d.SingleBlockChunk2D;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class WorldGenerator {
    private static final Map<String, Class<? extends DensityFunction>> densityFunctionCache = new HashMap<>();
    private static final Map<String, Class<? extends BlockFunction>> blockFunctionCache = new HashMap<>();
    private final DensityFunction densityFunction;
    private final BlockFunction blockFunction;
    private final BlockFunction negDensityBlockFunction;
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
    // TODO: Add functionality for this
    private final Integer depthSections;
    private final boolean heightIsDensityFunction;
    private final Integer noiseInterpolationScale;

    public WorldGenerator(GameNode gameNode, Map<String, BlockShape> blockShapeCache, Map<String, BlockHitbox[]> blockHitboxCache, long seed, ServerBlockService blockService) {
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
        Game.loadBlockShapes(gameProperties.get("id"), worldGeneratorNode, blockShapeCache);
        Game.loadBlockHitboxes(gameProperties.get("id"), worldGeneratorNode, blockHitboxCache);

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

        blockMinX = chunkMinX == null ? null : chunkMinX * ConstantCommonSettings.CHUNK_WIDTH;
        blockMinY = chunkMinY == null ? null : chunkMinY * ConstantCommonSettings.CHUNK_HEIGHT;
        blockMinZ = chunkMinZ == null ? null : chunkMinZ * ConstantCommonSettings.CHUNK_LENGTH;

        blockMaxX = chunkMaxX == null ? null : (chunkMaxX + 1) * ConstantCommonSettings.CHUNK_WIDTH;
        blockMaxY = chunkMaxY == null ? null : (chunkMaxY + 1) * ConstantCommonSettings.CHUNK_HEIGHT;
        blockMaxZ = chunkMaxZ == null ? null : (chunkMaxZ + 1) * ConstantCommonSettings.CHUNK_LENGTH;

        DoubleGameNode depthSectionsNode = Game.checkGameNodeType(worldGeneratorNode.object().get("depth_sections"), DoubleGameNode.class);

        this.depthSections = depthSectionsNode == null ? null : (int) depthSectionsNode.value();

        DoubleGameNode noiseInterpolationNode = Game.checkGameNodeType(worldGeneratorNode.object().get("noise_interpolation"), DoubleGameNode.class);

        this.noiseInterpolationScale = noiseInterpolationNode == null ? null : (int) noiseInterpolationNode.value();

        densityFunction = getDensityFunction(Game.checkGameNodeType(worldGeneratorNode.object().get("density"), ObjectGameNode.class), seed);
        blockFunction = getBlockFunction(Game.checkGameNodeType(Game.checkGameNodeType(worldGeneratorNode.object().get("surface"), ObjectGameNode.class).object().get("block"), ObjectGameNode.class), seed);
        negDensityBlockFunction = getBlockFunction(Game.checkGameNodeType(Game.checkGameNodeType(worldGeneratorNode.object().get("surface"), ObjectGameNode.class).object().get("neg_density"), ObjectGameNode.class), seed);
        if (worldGeneratorNode.object().containsKey("heights")) {
            heightFunction = getDensityFunction(Game.checkGameNodeType(worldGeneratorNode.object().get("heights"), ObjectGameNode.class), seed);
            heightIsDensityFunction = false;
        } else {
            heightFunction = densityFunction;
            heightIsDensityFunction = true;
        }
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

    // TODO: Move to a more relevant class
    // TODO: Make it actually work correctly (if this is called, and the chunk with the highest block in it isn't generated yet, it won't return the correct answer
    public Chunk2D<Integer> rebuildChunkHeights(ServerWorld world, Position2D position2D) {
        Chunk2D<Integer> chunkHeights = new SingleBlockChunk2D<>(0);

        if (chunkMinY == null || chunkMaxY == null) {
            throw new IllegalStateException("Cannot rebuild heights without chunk Y bounds");
        }

        for (int x = 0; x < ConstantCommonSettings.CHUNK_WIDTH; x++) {
            for (int z = 0; z < ConstantCommonSettings.CHUNK_LENGTH; z++) {
                int highestY = blockMinY;

                boolean found = false;

                for (int chunkY = chunkMaxY - 1; chunkY >= chunkMinY; chunkY--) {
                    Chunk<ServerBlock> chunk = world.get(new Position3D(position2D.x(), chunkY, position2D.z()));

                    if (chunk == null || chunk.getLOD() > 0) {
                        continue;
                    }

                    for (int localY = ConstantCommonSettings.CHUNK_HEIGHT - 1; localY >= 0; localY--) {
                        ServerBlock block = chunk.getBlock(x, localY, z);

                        if (block != null && block.partOfGround()) {
                            highestY = chunkY * ConstantCommonSettings.CHUNK_HEIGHT + localY;
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        break;
                    }
                }

                chunkHeights = chunkHeights.setBlock(x, z, highestY);
            }
        }

        world.putChunkHeights(position2D, chunkHeights);

        return chunkHeights;
    }

    public DensityFunction getDensityFunction() {
        return densityFunction;
    }

    public BlockFunction getBlockFunction() {
        return blockFunction;
    }

    public BlockFunction getNegDensityBlockFunction() {
        return negDensityBlockFunction;
    }

    public DensityFunction getHeightFunction() {
        return heightFunction;
    }

    public Integer getChunkMinX() {
        return chunkMinX;
    }

    public Integer getChunkMinY() {
        return chunkMinY;
    }

    public Integer getChunkMinZ() {
        return chunkMinZ;
    }

    public Integer getChunkMaxX() {
        return chunkMaxX;
    }

    public Integer getChunkMaxY() {
        return chunkMaxY;
    }

    public Integer getChunkMaxZ() {
        return chunkMaxZ;
    }

    public Integer getBlockMinX() {
        return blockMinX;
    }

    public Integer getBlockMinY() {
        return blockMinY;
    }

    public Integer getBlockMinZ() {
        return blockMinZ;
    }

    public Integer getBlockMaxX() {
        return blockMaxX;
    }

    public Integer getBlockMaxY() {
        return blockMaxY;
    }

    public Integer getBlockMaxZ() {
        return blockMaxZ;
    }

    public Integer getDepthSections() {
        return depthSections;
    }

    public int getNoiseInterpolationScale() {
        return noiseInterpolationScale;
    }
}
