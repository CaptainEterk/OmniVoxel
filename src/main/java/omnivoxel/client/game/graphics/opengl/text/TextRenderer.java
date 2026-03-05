package omnivoxel.client.game.graphics.opengl.text;

import omnivoxel.client.game.graphics.opengl.text.font.Font;
import org.lwjgl.opengl.*;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public class TextRenderer {
    private static final float TAB_SIZE = 40f;

    private static final int MAX_CHARS = 2048;

    private static final int INSTANCE_FLOATS = 8;
    private final FloatBuffer instanceBuffer =
            MemoryUtil.memAllocFloat(MAX_CHARS * INSTANCE_FLOATS);
    private int vaoID;
    private int quadVboID;
    private int instanceVboID;
    private int drawCount = 0;
    private Font currentFont;

    public void init() {
        float[] quadVertices = {
                0f, 0f, 0f, 0f,
                1f, 0f, 1f, 0f,
                1f, 1f, 1f, 1f,

                0f, 0f, 0f, 0f,
                1f, 1f, 1f, 1f,
                0f, 1f, 0f, 1f
        };

        vaoID = GL30C.glGenVertexArrays();
        GL30C.glBindVertexArray(vaoID);

        quadVboID = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, quadVboID);
        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, quadVertices, GL15C.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);

        instanceVboID = GL15C.glGenBuffers();
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, instanceVboID);
        GL15C.glBufferData(
                GL15C.GL_ARRAY_BUFFER,
                (long) MAX_CHARS * INSTANCE_FLOATS * Float.BYTES,
                GL15C.GL_DYNAMIC_DRAW
        );


        int stride = INSTANCE_FLOATS * Float.BYTES;

        GL20.glVertexAttribPointer(2, 2, GL11.GL_FLOAT, false, stride, 0);
        GL20.glEnableVertexAttribArray(2);
        GL33C.glVertexAttribDivisor(2, 1);

        GL20.glVertexAttribPointer(3, 2, GL11.GL_FLOAT, false, stride, 2 * Float.BYTES);
        GL20.glEnableVertexAttribArray(3);
        GL33C.glVertexAttribDivisor(3, 1);

        GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, 4 * Float.BYTES);
        GL20.glEnableVertexAttribArray(4);
        GL33C.glVertexAttribDivisor(4, 1);


        GL30C.glBindVertexArray(0);
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0);
    }

    public void queueText(Font font, String text, float x, float y, float scale, Alignment alignment) {
        if (currentFont == null) {
            currentFont = font;
        } else if (currentFont != font) {
            flush();
            currentFont = font;
        }

        String[] lines = text.split("[\r\n]");
        for (int i = 0; i < lines.length; i++) {
            accumulateLine(font, lines[i], x, y + 32 * (i + 2) * scale, scale, alignment);
        }
    }

    private void accumulateLine(Font font, String text, float x, float y, float scale, Alignment alignment) {
        float startX = x;

        if (alignment != Alignment.LEFT) {
            float totalWidth = 0;
            for (char c : text.toCharArray()) {
                if (c >= 32 && c <= 126) {
                    STBTTBakedChar charInfo = font.charData().get(c - 32);
                    totalWidth += getCharWidth(charInfo, scale);
                }
            }
            if (alignment == Alignment.CENTER) {
                startX -= totalWidth / 2f;
            } else if (alignment == Alignment.RIGHT) {
                startX -= totalWidth;
            }
        }

        for (char c : text.toCharArray()) {
            if (c == '\t') {
                startX += TAB_SIZE * scale;
                continue;
            }
            if (c < 32 || c > 126) continue;

            STBTTBakedChar charInfo = font.charData().get(c - 32);

            float xPos = startX + charInfo.xoff() * scale;
            float yPos = y - 32 * scale + charInfo.yoff() * scale;
            float w = (charInfo.x1() - charInfo.x0()) * scale;
            float h = (charInfo.y1() - charInfo.y0()) * scale;

            float sMin = (float) charInfo.x0() / font.bitmapWidth();
            float tMax = (float) charInfo.y0() / font.bitmapHeight();
            float sMax = (float) charInfo.x1() / font.bitmapWidth();
            float tMin = (float) charInfo.y1() / font.bitmapHeight();

            addInstance(xPos, yPos, w, h, sMin, tMin, sMax, tMax);

            startX += getCharWidth(charInfo, scale);
        }
    }

    private float getCharWidth(STBTTBakedChar charInfo, float scale) {
        return charInfo.xadvance() * scale;
    }

    private void addInstance(float x, float y, float w, float h,
                             float sMin, float tMin, float sMax, float tMax) {
        if (drawCount >= MAX_CHARS) return;
        instanceBuffer.put(x).put(y).put(w).put(h)
                .put(sMin).put(tMin).put(sMax).put(tMax);
        drawCount++;
    }

    public void flush() {
        if (drawCount == 0 || currentFont == null) return;

        instanceBuffer.flip();

        GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, currentFont.textureID());

        GL30C.glBindVertexArray(vaoID);

        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, instanceVboID);
        GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, instanceBuffer);

        GL31C.glDrawArraysInstanced(GL11C.GL_TRIANGLES, 0, 6, drawCount);

        GL30C.glBindVertexArray(0);

        instanceBuffer.clear();
        drawCount = 0;
        currentFont = null;
    }

    public void cleanup() {
        GL30C.glDeleteVertexArrays(vaoID);
        GL15C.glDeleteBuffers(quadVboID);
        GL15C.glDeleteBuffers(instanceVboID);
        MemoryUtil.memFree(instanceBuffer);
    }
}