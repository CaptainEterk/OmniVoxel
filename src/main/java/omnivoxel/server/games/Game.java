package omnivoxel.server.games;

import omnivoxel.client.game.graphics.opengl.mesh.vertex.Vertex;
import omnivoxel.common.BlockShape;
import omnivoxel.server.ServerLogger;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.server.client.chunk.blockService.ServerBlockService;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3D;
import omnivoxel.server.client.chunk.worldDataService.noise.Noise3DProvider;
import omnivoxel.util.game.nodes.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public final class Game {
    @SuppressWarnings("unchecked")
    public static <T extends GameNode> T checkGameNodeType(GameNode gameNode, Class<T> clazz) {
        if (gameNode == null) {
            return null;
        }
        if (clazz.isInstance(gameNode)) {
            return (T) gameNode;
        }
        if (gameNode instanceof StringGameNode stringGameNode) {
            throw new IllegalArgumentException("Expected " + clazz.getSimpleName() + " but got \"" + stringGameNode.value() + "\"");
        }
        throw new IllegalArgumentException("Expected " + clazz.getSimpleName() + " but got " + gameNode.getClass().getSimpleName());
    }

    public static void loadNoises(ArrayGameNode noises, long seed) {
        for (GameNode node : noises.nodes()) {
            loadNoise(checkGameNodeType(node, ObjectGameNode.class), seed);
        }
    }

    private static void loadNoise(ObjectGameNode noise, long seed) {
        String id = checkGameNodeType(noise.object().get("id"), StringGameNode.class).value();
        double[] octaves = Arrays.stream(checkGameNodeType(noise.object().get("octaves"), ArrayGameNode.class).nodes())
                .mapToDouble(gameNode -> checkGameNodeType(gameNode, DoubleGameNode.class).value())
                .toArray();
        double firstOctave = checkGameNodeType(noise.object().get("first_octave"), DoubleGameNode.class).value();
        ServerLogger.logger.debug("Registered noise: " + id);
        Noise3DProvider.registerNoise(id, new Noise3D(octaves, (int) firstOctave, seed));
    }

    public static void loadBlocks(ObjectGameNode worldGeneratorNode, ServerBlockService blockService) {
        ArrayGameNode blocks = Game.checkGameNodeType(worldGeneratorNode.object().get("blocks"), ArrayGameNode.class);
        for (GameNode node : blocks.nodes()) {
            ObjectGameNode blockObjectGameNode = Game.checkGameNodeType(node, ObjectGameNode.class);
            String id = Game.checkGameNodeType(blockObjectGameNode.object().get("id"), StringGameNode.class).value();
            ArrayGameNode blockStates = Game.checkGameNodeType(blockObjectGameNode.object().get("block_states"), ArrayGameNode.class);
            for (GameNode stateNode : blockStates.nodes()) {
                ObjectGameNode objectStateNode = Game.checkGameNodeType(stateNode, ObjectGameNode.class);
                String blockState = Game.checkGameNodeType(objectStateNode.object().get("id"), StringGameNode.class).value();
                String blockShape = Game.checkGameNodeType(objectStateNode.object().get("block_shape"), StringGameNode.class).value();
                boolean transparent = Game.checkGameNodeType(objectStateNode.object().get("transparent"), BooleanGameNode.class).value();
                boolean transparentMesh = Game.checkGameNodeType(objectStateNode.object().get("transparent_mesh"), BooleanGameNode.class).value();
                ObjectGameNode texture = Game.checkGameNodeType(objectStateNode.object().get("texture"), ObjectGameNode.class);
                String uvMapping = Game.checkGameNodeType(texture.object().get("uv_mapping"), StringGameNode.class).value();
                double[][] uvCoords = new double[6][];
                if (Objects.equals(uvMapping, "face")) {
                    double[] coords = Arrays.stream(Game.checkGameNodeType(texture.object().get("uv_coords"), ArrayGameNode.class).nodes()).mapToDouble((gn) -> Game.checkGameNodeType(gn, DoubleGameNode.class).value()).toArray();
                    for (int i = 0; i < 6; i++) {
                        uvCoords[i] = coords;
                    }
                } else if (Objects.equals(uvMapping, "cube")) {
                    ArrayGameNode faceNodes = Game.checkGameNodeType(texture.object().get("uv_coords"), ArrayGameNode.class);
                    if (faceNodes.nodes().length != 6) {
                        throw new IllegalArgumentException("Cube mapping must provide 6 UV arrays, one per face");
                    }

                    for (int i = 0; i < 6; i++) {
                        ArrayGameNode faceArray = Game.checkGameNodeType(faceNodes.nodes()[i], ArrayGameNode.class);
                        double[] faceCoords = Arrays.stream(faceArray.nodes())
                                .mapToDouble(gn -> Game.checkGameNodeType(gn, DoubleGameNode.class).value())
                                .toArray();
                        uvCoords[i] = faceCoords;
                    }
                } else {
                    throw new IllegalArgumentException("\"" + uvMapping + "\" is not a valid uv_mapping");
                }

                blockService.registerServerBlock(new ServerBlock(ServerBlock.createID(id, blockState), blockShape, uvCoords, transparent, transparentMesh));
            }
        }
    }

    public static void loadBlockShapes(String gameID, ObjectGameNode worldGeneratorNode, ServerBlockService blockService, Map<String, BlockShape> blockShapeCache) {
        ArrayGameNode blocks_shapes = Game.checkGameNodeType(worldGeneratorNode.object().get("block_shapes"), ArrayGameNode.class);
        for (GameNode node : blocks_shapes.nodes()) {
            ObjectGameNode blockShapeObjectGameNode = Game.checkGameNodeType(node, ObjectGameNode.class);
            String id = Game.checkGameNodeType(blockShapeObjectGameNode.object().get("id"), StringGameNode.class).value();
            ArrayGameNode verticesNode = Game.checkGameNodeType(blockShapeObjectGameNode.object().get("vertices"), ArrayGameNode.class);
            Vertex[][] vertices = new Vertex[6][];
            for (int i = 0; i < 6; i++) {
                Vertex[] vs = Arrays.stream(Game.checkGameNodeType(verticesNode.nodes()[i], ArrayGameNode.class).nodes()).map((gn) -> {
                    double[] vertexArray = Arrays.stream(Game.checkGameNodeType(gn, ArrayGameNode.class).nodes()).mapToDouble((vgn) -> Game.checkGameNodeType(vgn, DoubleGameNode.class).value()).toArray();
                    return new Vertex((float) vertexArray[0], (float) vertexArray[1], (float) vertexArray[2]);
                }).toArray(Vertex[]::new);
                vertices[i] = vs;
            }

            ArrayGameNode indicesNode = Game.checkGameNodeType(blockShapeObjectGameNode.object().get("indices"), ArrayGameNode.class);
            int[][] indices = new int[6][];
            for (int i = 0; i < 6; i++) {
                int[] vs = Arrays.stream(Game.checkGameNodeType(indicesNode.nodes()[i], ArrayGameNode.class).nodes()).mapToInt((gn) -> (int) Game.checkGameNodeType(gn, DoubleGameNode.class).value()).toArray();
                indices[i] = vs;
            }

            ArrayGameNode solidNode = Game.checkGameNodeType(blockShapeObjectGameNode.object().get("solid"), ArrayGameNode.class);
            GameNode[] nodes = solidNode.nodes();
            boolean[] solid = new boolean[nodes.length];
            for (int i = 0; i < nodes.length; i++) {
                solid[i] = Game.checkGameNodeType(nodes[i], BooleanGameNode.class).value();
            }

            blockShapeCache.put(gameID + ":" + id, new BlockShape(gameID + ":" + id, vertices, indices, solid));
        }

        blockShapeCache.put(BlockShape.EMPTY_BLOCK_SHAPE_STRING, BlockShape.EMPTY_BLOCK_SHAPE);

        blockService.registerServerBlock(ServerBlock.AIR);
        blockService.registerServerBlock(ServerBlock.VOID);
    }
}