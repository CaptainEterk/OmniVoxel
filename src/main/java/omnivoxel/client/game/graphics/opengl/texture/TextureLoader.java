package omnivoxel.client.game.graphics.opengl.texture;

import omnivoxel.client.game.graphics.opengl.image.Image;
import omnivoxel.client.game.graphics.opengl.image.ImageLoader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.stb.STBImage;

public class TextureLoader {
    public static int loadTexture(String path) {
        String filePath = "assets/textures/" + path;

        Image image = ImageLoader.load(filePath);

        int textureId = GL11.glGenTextures();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

        GL11.glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                GL11.GL_RGBA,
                image.width(),
                image.height(),
                0,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                image.image()
        );

        GL30C.glGenerateMipmap(GL11.GL_TEXTURE_2D);

        STBImage.stbi_image_free(image.image());

        return textureId;
    }
}