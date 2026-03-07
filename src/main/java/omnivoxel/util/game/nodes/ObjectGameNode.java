package omnivoxel.util.game.nodes;

import java.util.Map;

public record ObjectGameNode(String key, Map<String, GameNode> object) implements GameNode {
}