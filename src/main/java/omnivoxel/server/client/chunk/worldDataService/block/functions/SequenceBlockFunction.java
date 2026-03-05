package omnivoxel.server.client.chunk.worldDataService.block.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.ServerWorldDataService;
import omnivoxel.server.client.chunk.worldDataService.block.BlockFunction;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.ArrayGameNode;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;

@Function(id = "sequence")
public class SequenceBlockFunction extends BlockFunction {
    private final static String air = "omnivoxel:air/default";
    private final BlockFunction[] blockFunctions;

    public SequenceBlockFunction(GameNode node, long i) {
        super(node, i);
        ArrayGameNode args = Game.checkGameNodeType(Game.checkGameNodeType(node, ObjectGameNode.class).object().get("args"), ArrayGameNode.class);
        blockFunctions = new BlockFunction[args.nodes().length];
        for (int j = 0; j < blockFunctions.length; j++) {
            blockFunctions[j] = ServerWorldDataService.getBlockFunction(args.nodes()[j], i);
        }
    }

    @Override
    public String evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z) {
        for (BlockFunction blockFunction : blockFunctions) {
            String result = blockFunction.evaluate(density, info, floor, ceiling, depth, x, y, z);
            if (result != null) {
                return result;
            }
        }
        return air;
    }
}