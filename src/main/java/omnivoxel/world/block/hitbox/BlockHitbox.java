package omnivoxel.world.block.hitbox;

import omnivoxel.client.game.hitbox.Hitbox;

public interface BlockHitbox {
    boolean isColliding(int bx, int by, int bz, Hitbox hitbox);

    String getHitboxID();
}