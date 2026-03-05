package omnivoxel.client.game.graphics.opengl.image;

import java.nio.ByteBuffer;

public record Image(ByteBuffer image, int width, int height) {
}
