package omnivoxel.world.block.hitbox;

import omnivoxel.client.game.hitbox.Hitbox;

public class FullBlockHitbox implements BlockHitbox {
    @Override
    public boolean isColliding(int bx, int by, int bz, Hitbox hitbox) {
        return hitboxIntersectsAABB(hitbox, bx, by, bz, bx + 1, by + 1, bz + 1);
    }

    @Override
    public String getHitboxID() {
        return "omnivoxel:hitbox/full_block";
    }

    private boolean hitboxIntersectsAABB(Hitbox h, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return h.maxX() > minX && h.minX() < maxX &&
                h.maxY() > minY && h.minY() < maxY &&
                h.maxZ() > minZ && h.minZ() < maxZ;
    }
}