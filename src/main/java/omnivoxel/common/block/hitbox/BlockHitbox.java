package omnivoxel.common.block.hitbox;

import omnivoxel.client.game.hitbox.Hitbox;
import omnivoxel.util.bytes.ByteUtils;

public record BlockHitbox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                          BlockHitboxVolumeProperties volumeProperties) {
    public static final String EMPTY_BLOCK_HITBOX_STRING = "omnivoxel:empty";
    public static final BlockHitbox[] EMPTY_BLOCK_HITBOX = new BlockHitbox[0];

    public byte[] getBytes() {
        byte[] volumeBytes = volumeProperties.getBytes();
        byte[] bytes = new byte[Float.BYTES * 6 + volumeBytes.length];
        ByteUtils.addFloats(bytes, 0, minX, minY, minZ, maxX, maxY, maxZ);
        System.arraycopy(volumeBytes, 0, bytes, Float.BYTES * 6, volumeBytes.length);
        return bytes;
    }

    public boolean isColliding(Hitbox hitbox, float ox, float oy, float oz) {
        return hitboxIntersectsAABB(
                hitbox.minX() + ox,
                hitbox.minY() + oy,
                hitbox.minZ() + oz,

                hitbox.maxX() + ox,
                hitbox.maxY() + oy,
                hitbox.maxZ() + oz,

                minX,
                minY,
                minZ,

                maxX,
                maxY,
                maxZ
        );
    }

    public boolean intersectsRay(double originX, double originY, double originZ,
                                 double dirX, double dirY, double dirZ,
                                 int x, int y, int z) {

        double minX = x + this.minX;
        double minY = y + this.minY;
        double minZ = z + this.minZ;

        double maxX = x + this.maxX;
        double maxY = y + this.maxY;
        double maxZ = z + this.maxZ;

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

    private boolean hitboxIntersectsAABB(
            float minAX, float minAY, float minAZ,
            float maxAX, float maxAY, float maxAZ,

            float minBX, float minBY, float minBZ,
            float maxBX, float maxBY, float maxBZ
    ) {
        return maxAX > minBX && minAX < maxBX &&
                maxAY > minBY && minAY < maxBY &&
                maxAZ > minBZ && minAZ < maxBZ;
    }

    public record BlockHitboxVolumeProperties(boolean isVolume, float speed, boolean isGround) {
        public byte[] getBytes() {
            byte[] bytes = new byte[2 + Float.BYTES];
            bytes[0] = (byte) (isVolume ? 1 : 0);
            bytes[1] = (byte) (isGround ? 1 : 0);
            ByteUtils.addFloat(bytes, speed, 2);
            return bytes;
        }
    }
}