package omnivoxel.client.game.graphics.camera;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.math.Position3D;

public final class CameraCullingService {
    private final Camera camera;
    private int ccx;
    private int ccy;
    private int ccz;

    public CameraCullingService(Camera camera) {
        this.camera = camera;
    }

    public void calculateChunkPosition() {
        this.ccx = (int) Math.floor(camera.getX() / ConstantCommonSettings.CHUNK_WIDTH);
        this.ccy = (int) Math.floor(camera.getY() / ConstantCommonSettings.CHUNK_HEIGHT);
        this.ccz = (int) Math.floor(camera.getZ() / ConstantCommonSettings.CHUNK_LENGTH);
    }

    public boolean shouldDistanceCullChunk(Position3D chunkPosition, int squaredRenderDistance) {
        int dx = chunkPosition.x() - ccx;
        int dy = chunkPosition.y() - ccy;
        int dz = chunkPosition.z() - ccz;
        int distance = dx * dx + dy * dy + dz * dz;

        return distance > squaredRenderDistance;
    }
}