package omnivoxel.client.game.graphics.opengl.text.font;

import omnivoxel.client.game.settings.ConstantGameSettings;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

public record Font(STBTTBakedChar.Buffer charData, int textureID, int bitmapWidth, int bitmapHeight) {
    private static final int BITMAP_WIDTH = 512;
    private static final int BITMAP_HEIGHT = 512;
    private static final int FONT_SIZE = 32;

    public static Font create(String fontName) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(ConstantGameSettings.DATA_LOCATION + "assets/fonts/" + fontName));
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc(96);
        // Allocate memory for the bitmap
        ByteBuffer bitmap = MemoryUtil.memAlloc(BITMAP_WIDTH * BITMAP_HEIGHT);

        // Bake the font bitmap into a 2D texture
        STBTruetype.stbtt_BakeFontBitmap(
                buffer, FONT_SIZE, bitmap, BITMAP_WIDTH, BITMAP_HEIGHT, 32, charData
        ); // Baking characters 32-126 (ASCII)

        // Create an OpenGL texture
        int textureID = GL11C.glGenTextures();
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, textureID);

        // Upload the bitmap to the texture
        GL11C.glTexImage2D(
                GL11C.GL_TEXTURE_2D,
                0,
                GL11C.GL_RED,
                BITMAP_WIDTH,
                BITMAP_HEIGHT,
                0,
                GL11C.GL_RED,
                GL11C.GL_UNSIGNED_BYTE,
                bitmap
        );

        // Set texture filtering
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);

        // Free the bitmap memory
        MemoryUtil.memFree(bitmap);

        return new Font(charData, textureID, BITMAP_WIDTH, BITMAP_HEIGHT);
    }

    public float getCharWidth(char c) {
        if (c < 32 || c > 126) {
            return 0; // Skip non-printable characters
        }
        return charData.get(c - 32).xadvance();
    }

    public void cleanup() {
        charData.free();
        GL11C.glDeleteTextures(textureID);
    }
}