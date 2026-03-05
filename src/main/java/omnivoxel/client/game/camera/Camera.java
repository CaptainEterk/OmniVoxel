package omnivoxel.client.game.camera;

import omnivoxel.client.game.state.State;
import org.joml.Math;
import org.joml.Matrix4f;

public class Camera {
    protected final Frustum frustum;
    // State
    private final State state;
    // Position
    protected double x = 0f;
    protected double y = 0f;
    protected double z = 0f;
    // Rotation
    protected double pitch = 0f; // Up-down
    protected double yaw = 0f; // Left-right

    // TODO: Move this to settings
    protected double fov = 90d;

    public Camera(Frustum frustum, State state) {
        this.frustum = frustum;
        this.state = state;
    }

    public void rotateX(float angle) {
        // Clamp the up-down rotation to looking straight up and looking straight down
        if (angle != pitch) {
            pitch = org.joml.Math.clamp(-org.joml.Math.PI_OVER_2_f, Math.PI_OVER_2_f, pitch + angle);
        }
    }

    public void rotateY(float angle) {
        if (angle != yaw) {
            yaw += angle;
            yaw %= (float) (org.joml.Math.PI * 2);
            if (yaw < 0) {
                yaw += (float) (Math.PI * 2);
            }
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getPitch() {
        return pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public double getFOV() {
        return fov;
    }

    public void setFOV(double fov) {
        this.fov = fov;
        if (this.fov < 1) {
            this.fov = 1;
        } else if (this.fov > 179) {
            this.fov = 179;
        }
    }

    public float getNear() {
        return 0.1f;
    }

    public float getFar() {
        return 1000f;
    }

    public void updateFrustum(Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        frustum.updateFrustum(projectionMatrix, viewMatrix);
    }

    public Frustum getFrustum() {
        return frustum;
    }

    public void setPosition(double x, double y, double z) {
        if (x != this.x || y != this.y || z != this.z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}