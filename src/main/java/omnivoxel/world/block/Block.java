package omnivoxel.world.block;

import java.util.Objects;

public class Block {
    private final String id;

    public Block(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return Objects.equals(id, block.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String id() {
        return id;
    }
}