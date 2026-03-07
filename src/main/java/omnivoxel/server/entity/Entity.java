package omnivoxel.server.entity;

import omnivoxel.client.game.graphics.opengl.mesh.EntityMesh;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.hitbox.Hitbox;
import omnivoxel.server.client.ServerItem;
import omnivoxel.util.math.DoublePosition3D;

import java.security.SecureRandom;

public abstract class Entity implements ServerItem {
    protected final float friction = getFriction();
    protected final byte[] entityID;
    private final Hitbox hitbox;
    protected double x;
    protected double y;
    protected double z;
    protected double velocityX;
    protected double velocityY;
    protected double velocityZ;
    protected EntityMesh mesh;
    private MeshData meshData;

    protected Entity(Hitbox hitbox) {
        this.hitbox = hitbox;
        entityID = new byte[32];
        new SecureRandom().nextBytes(entityID);
    }

    protected float getFriction() {
        return 0.1f;
    }

    public void tick(float deltaTime) {
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
        z += velocityZ * deltaTime;

        double frictionFactor = Math.pow(friction, deltaTime);
        velocityX *= frictionFactor;
        velocityY *= frictionFactor;
        velocityZ *= frictionFactor;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public void setVelocityX(float velocityX) {
        this.velocityX = velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public void setVelocityY(float velocityY) {
        this.velocityY = velocityY;
    }

    public double getVelocityZ() {
        return velocityZ;
    }

    public void setVelocityZ(float velocityZ) {
        this.velocityZ = velocityZ;
    }

    public MeshData getMeshData() {
        return meshData;
    }

    public void setMeshData(MeshData meshData) {
        this.meshData = meshData;
    }

    public Hitbox getHitbox() {
        return hitbox;
    }

    public EntityMesh getMesh() {
        return mesh;
    }

    public void setMesh(EntityMesh mesh) {
        this.mesh = mesh;
    }

    public DoublePosition3D getPosition() {
        return new DoublePosition3D(x, y, z);
    }
}