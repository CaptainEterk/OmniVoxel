package omnivoxel.client.game.graphics.opengl.image;

import omnivoxel.client.game.settings.ConstantGameSettings;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ImageLoader {
    public static Image load(String path) {
        int width, height;
        ByteBuffer imageBuffer;

        // Load image
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer channelsBuffer = stack.mallocInt(1);

            imageBuffer = STBImage.stbi_load(ConstantGameSettings.DATA_LOCATION + path, widthBuffer, heightBuffer, channelsBuffer, 4);
            if (imageBuffer == null) {
                throw new RuntimeException(
                        "Failed to load texture file " + ConstantGameSettings.DATA_LOCATION + path + "\n" + STBImage.stbi_failure_reason());
            }

            width = widthBuffer.get();
            height = heightBuffer.get();
        }

        return new Image(imageBuffer, width, height);
    }
}