package omnivoxel.client.game.graphics.api.opengl.image;

import java.nio.ByteBuffer;

public record Image(ByteBuffer image, int width, int height) {
}
