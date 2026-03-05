package omnivoxel.util.game.nodes;

import omnivoxel.common.annotations.NotNull;

import java.util.Arrays;

public record ArrayGameNode(String key, GameNode[] nodes) implements GameNode {
    @Override
    public @NotNull String toString() {
        return "ArrayGameNode{" +
                "key='" + key + '\'' +
                ", nodes=" + Arrays.deepToString(nodes) +
                '}';
    }
}