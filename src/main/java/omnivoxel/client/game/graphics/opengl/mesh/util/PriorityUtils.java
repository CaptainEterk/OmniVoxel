package omnivoxel.client.game.graphics.opengl.mesh.util;

import omnivoxel.client.game.camera.Camera;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.util.math.Position3D;

public final class PriorityUtils {
    private static Camera camera;

    public static double getPriority(Position3D position) {
        long distanceX = (long) position.x() * ConstantGameSettings.CHUNK_WIDTH - Math.round(camera.getX());
        long distanceY = (long) position.y() * ConstantGameSettings.CHUNK_HEIGHT - Math.round(camera.getY());
        long distanceZ = (long) position.z() * ConstantGameSettings.CHUNK_LENGTH - Math.round(camera.getZ());
        return (distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ) / 1000.0;
    }

    public static void setCamera(Camera camera) {
        PriorityUtils.camera = camera;
    }
}