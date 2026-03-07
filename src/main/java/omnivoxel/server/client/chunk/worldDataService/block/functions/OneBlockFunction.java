package omnivoxel.server.client.chunk.worldDataService.block.functions;

import omnivoxel.server.client.chunk.worldDataService.Function;
import omnivoxel.server.client.chunk.worldDataService.block.BlockFunction;
import omnivoxel.server.games.Game;
import omnivoxel.util.game.nodes.GameNode;
import omnivoxel.util.game.nodes.ObjectGameNode;
import omnivoxel.util.game.nodes.StringGameNode;

@Function(id = "block")
public class OneBlockFunction extends BlockFunction {
    private final String result;

    public OneBlockFunction(GameNode args, long seed) {
        super(args, seed);

        if (args instanceof ObjectGameNode objectGameNode) {
            this.result = Game.checkGameNodeType(objectGameNode.object().get("id"), StringGameNode.class).value();
        } else {
            throw new IllegalArgumentException("GameNode must be an ObjectGameNode, not " + args.getClass());
        }
    }

    @Override
    public String evaluate(double density, int[] info, boolean floor, boolean ceiling, int depth, double x, double y, double z) {
        return result;
    }
}
