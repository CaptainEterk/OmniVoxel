package omnivoxel.client.game.graphics.api.opengl.mesh;

import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class FullscreenQuad {
    private int vao;
    private int vbo;
    private int ebo;

    public void init() {
        // Positions (NDC) + UV
        float[] vertices = {
                // pos      // uv
                -1f, -1f,   0f, 0f,
                 1f, -1f,   1f, 0f,
                 1f,  1f,   1f, 1f,
                -1f,  1f,   0f, 1f
        };

        int[] indices = {
                0, 1, 2,
                2, 3, 0
        };

        vao = GL30C.glGenVertexArrays();
        vbo = GL30C.glGenBuffers();
        ebo = GL30C.glGenBuffers();

        GL30C.glBindVertexArray(vao);

        // --- VBO ---
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();

        GL30C.glBindBuffer(GL30C.GL_ARRAY_BUFFER, vbo);
        GL30C.glBufferData(GL30C.GL_ARRAY_BUFFER, vertexBuffer, GL30C.GL_STATIC_DRAW);

        // --- EBO ---
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();

        GL30C.glBindBuffer(GL30C.GL_ELEMENT_ARRAY_BUFFER, ebo);
        GL30C.glBufferData(GL30C.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL30C.GL_STATIC_DRAW);

        // --- Attributes ---
        int stride = 4 * Float.BYTES;

        GL30C.glEnableVertexAttribArray(5);
        GL30C.glVertexAttribPointer(5, 2, GL30C.GL_FLOAT, false, stride, 0);

        GL30C.glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
    }

    public void render() {
        GL30C.glBindVertexArray(vao);
        GL30C.glDrawElements(GL30C.GL_TRIANGLES, 6, GL30C.GL_UNSIGNED_INT, 0);
        GL30C.glBindVertexArray(0);
    }

    public void cleanup() {
        if (vao != 0) GL30C.glDeleteVertexArrays(vao);
        if (vbo != 0) GL30C.glDeleteBuffers(vbo);
        if (ebo != 0) GL30C.glDeleteBuffers(ebo);

        vao = vbo = ebo = 0;
    }
}