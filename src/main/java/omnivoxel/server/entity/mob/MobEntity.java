package omnivoxel.server.entity.mob;

import omnivoxel.client.game.hitbox.Hitbox;

public abstract class MobEntity extends GravityEntity {
    private final String name;
    protected double yaw;
    protected double pitch;

    protected MobEntity(String name, Hitbox hitbox) {
        super(hitbox);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void set(double x, double y, double z, double pitch, double yaw) {
        super.set(x, y, z);
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}