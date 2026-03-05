package omnivoxel.client.game.entity;

import omnivoxel.client.game.graphics.opengl.mesh.EntityMesh;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.EntityMeshData;
import omnivoxel.server.entity.EntityType;

public class ClientEntity {
    private final String name;
    private final String uuid;
    private final EntityType type;
    private double x;
    private double y = 200;
    private double z;
    private double yaw;
    private double pitch;
    private EntityMesh mesh;
    private EntityMeshData meshData;

    public ClientEntity(String name, String uuid, EntityType type) {
        this.name = name;
        this.uuid = uuid;
        this.type = type;
    }

    public void set(double x, double y, double z, double pitch, double yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public String getName() {
        return name;
    }

    public String getUUID() {
        return uuid;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public EntityMesh getMesh() {
        return mesh;
    }

    public void setMesh(EntityMesh mesh) {
        this.mesh = mesh;
    }

    public EntityMeshData getMeshData() {
        return meshData;
    }

    public void setMeshData(EntityMeshData meshData) {
        this.meshData = meshData;
    }

    public EntityType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ClientEntity{" +
                "name='" + name + '\'' +
                ", uuid='" + uuid + '\'' +
                ", type=" + type +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", mesh=" + mesh +
                ", meshData=" + meshData +
                '}';
    }
}