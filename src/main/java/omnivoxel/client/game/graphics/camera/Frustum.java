package omnivoxel.client.game.graphics.camera;

import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.math.Position3D;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class Frustum {

    private final FrustumIntersection frustumIntersection =
            new FrustumIntersection();

    private final Vector4f[] planes = {
            new Vector4f(),
            new Vector4f(),
            new Vector4f(),
            new Vector4f(),
            new Vector4f(),
            new Vector4f()
    };

    public void updateFrustum(
            Matrix4f projectionMatrix,
            Matrix4f viewMatrix
    ) {
        Matrix4f viewProjection =
                new Matrix4f(projectionMatrix).mul(viewMatrix);

        frustumIntersection.set(viewProjection);

        /*
         * JOML plane indices:
         *
         * 0 = left
         * 1 = right
         * 2 = bottom
         * 3 = top
         * 4 = near
         * 5 = far
         */
        for (int i = 0; i < 6; i++) {
            viewProjection.frustumPlane(i, planes[i]);
            planes[i].normalize3();
        }
    }

    public Vector4f[] getPlanes() {
        return planes;
    }

    public boolean isChunkInFrustum(Position3D position3D) {
        int x = IndexCalculator.blockX(position3D.x());
        int y = IndexCalculator.blockY(position3D.y());
        int z = IndexCalculator.blockZ(position3D.z());

        return frustumIntersection.testAab(
                x,
                y,
                z,
                x + ConstantCommonSettings.CHUNK_WIDTH,
                y + ConstantCommonSettings.CHUNK_HEIGHT,
                z + ConstantCommonSettings.CHUNK_LENGTH
        );
    }
}