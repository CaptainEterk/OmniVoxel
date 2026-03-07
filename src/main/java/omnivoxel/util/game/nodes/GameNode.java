package omnivoxel.util.game.nodes;

public sealed interface GameNode permits ArrayGameNode, BooleanGameNode, DoubleGameNode, NullGameNode, ObjectGameNode, StringGameNode {
    String key();
}