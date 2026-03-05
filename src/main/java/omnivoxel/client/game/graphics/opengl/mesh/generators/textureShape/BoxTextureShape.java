package omnivoxel.client.game.graphics.opengl.mesh.generators.textureShape;

import omnivoxel.common.face.BlockFace;

import java.util.Arrays;

public class BoxTextureShape {
    private final int tw;
    private final int th;
    private final float[][] uvCoords;

    public BoxTextureShape(int tw, int th) {
        this.tw = tw;
        this.th = th;
        uvCoords = new float[6][4];
    }

    private BoxTextureShape(int tw, int th, float[][] uvCoords) {
        this.tw = tw;
        this.th = th;
        this.uvCoords = uvCoords;
    }

    public BoxTextureShape setCoords(BlockFace face, int x, int y, int w, int h) {
        float u0 = (float) x / tw;
        float u1 = (float) (x + w) / tw;

        float v0 = 1 - ((float) y / th);
        float v1 = 1 - ((float) (y + h) / th);

        uvCoords[face.ordinal()] = new float[]{
                u0, v0,
                u1, v0,
                u1, v1,
                u0, v1
        };

        return this;
    }

    public float[] getCoords(BlockFace face) {
        return uvCoords[face.ordinal()];
    }

    public BoxTextureShape copy() {
        float[][] copiedCoords = new float[uvCoords.length][];
        for (int i = 0; i < uvCoords.length; i++) {
            if (uvCoords[i] != null) {
                copiedCoords[i] = Arrays.copyOf(uvCoords[i], uvCoords[i].length);
            }
        }
        return new BoxTextureShape(tw, th, copiedCoords);
    }
}