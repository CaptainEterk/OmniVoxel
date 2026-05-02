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

    @Override
    public boolean intersectsRay(double originX, double originY, double originZ,
                                 double dirX, double dirY, double dirZ,
                                 int x, int y, int z) {

        double minX = x;
        double minY = y;
        double minZ = z;

        double maxX = x + 1.0;
        double maxY = y + 1.0;
        double maxZ = z + 1.0;

        double tmin = Double.NEGATIVE_INFINITY;
        double tmax = Double.POSITIVE_INFINITY;

        // X slab
        if (Math.abs(dirX) < 1e-8) {
            if (originX < minX || originX > maxX) return false;
        } else {
            double tx1 = (minX - originX) / dirX;
            double tx2 = (maxX - originX) / dirX;
            if (tx1 > tx2) {
                double t = tx1;
                tx1 = tx2;
                tx2 = t;
            }
            tmin = Math.max(tmin, tx1);
            tmax = Math.min(tmax, tx2);
            if (tmax < tmin) return false;
        }

        // Y slab
        if (Math.abs(dirY) < 1e-8) {
            if (originY < minY || originY > maxY) return false;
        } else {
            double ty1 = (minY - originY) / dirY;
            double ty2 = (maxY - originY) / dirY;
            if (ty1 > ty2) {
                double t = ty1;
                ty1 = ty2;
                ty2 = t;
            }
            tmin = Math.max(tmin, ty1);
            tmax = Math.min(tmax, ty2);
            if (tmax < tmin) return false;
        }

        // Z slab
        if (Math.abs(dirZ) < 1e-8) {
            if (originZ < minZ || originZ > maxZ) return false;
        } else {
            double tz1 = (minZ - originZ) / dirZ;
            double tz2 = (maxZ - originZ) / dirZ;
            if (tz1 > tz2) {
                double t = tz1;
                tz1 = tz2;
                tz2 = t;
            }
            tmin = Math.max(tmin, tz1);
            tmax = Math.min(tmax, tz2);
            if (tmax < tmin) return false;
        }

        // Intersection exists if the interval is in front of the ray
        return tmax >= Math.max(tmin, 0.0);
    }

    private boolean hitboxIntersectsAABB(Hitbox h, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return h.maxX() > minX && h.minX() < maxX &&
                h.maxY() > minY && h.minY() < maxY &&
                h.maxZ() > minZ && h.minZ() < maxZ;
    }
}