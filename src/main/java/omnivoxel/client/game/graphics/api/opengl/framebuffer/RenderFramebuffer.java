package omnivoxel.client.game.graphics.api.opengl.framebuffer;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL30C;

import java.nio.ByteBuffer;

/**
 * Minimal OpenGL 3.3 framebuffer wrapper for rendering the scene at an internal resolution and blitting to the default framebuffer.
 */
public final class RenderFramebuffer {
    private int fboId;
    private int colorTexId;
    private int depthStencilRboId;
    private int width;
    private int height;
    private int filter;

    public void init(int width, int height, int filter) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.filter = filter;

        fboId = GL30C.glGenFramebuffers();
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fboId);

        colorTexId = GL11C.glGenTextures();
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, colorTexId);
        GL11C.glTexImage2D(
                GL11C.GL_TEXTURE_2D,
                0,
                GL11C.GL_RGBA8,
                this.width,
                this.height,
                0,
                GL11C.GL_RGBA,
                GL11C.GL_UNSIGNED_BYTE,
                (ByteBuffer) null
        );
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, filter);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, filter);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL12C.GL_CLAMP_TO_EDGE);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL12C.GL_CLAMP_TO_EDGE);
        GL30C.glFramebufferTexture2D(
                GL30C.GL_FRAMEBUFFER,
                GL30C.GL_COLOR_ATTACHMENT0,
                GL11C.GL_TEXTURE_2D,
                colorTexId,
                0
        );

        depthStencilRboId = GL30C.glGenRenderbuffers();
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, depthStencilRboId);
        GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH24_STENCIL8, this.width, this.height);
        // TODO: Be able to customize this
        GL30C.glFramebufferRenderbuffer(
                GL30C.GL_FRAMEBUFFER,
                GL30C.GL_DEPTH_STENCIL_ATTACHMENT,
                GL30C.GL_RENDERBUFFER,
                depthStencilRboId
        );

        int status = GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER);
        if (status != GL30C.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer incomplete: status=" + status);
        }

        // Unbind
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
    }

    public void resize(int width, int height) {
        int newWidth = Math.max(1, width);
        int newHeight = Math.max(1, height);
        if (newWidth == this.width && newHeight == this.height) {
            return;
        }

        this.width = newWidth;
        this.height = newHeight;

        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, colorTexId);
        GL11C.glTexImage2D(
                GL11C.GL_TEXTURE_2D,
                0,
                GL11C.GL_RGBA8,
                this.width,
                this.height,
                0,
                GL11C.GL_RGBA,
                GL11C.GL_UNSIGNED_BYTE,
                (ByteBuffer) null
        );
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, filter);
        GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, filter);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);

        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, depthStencilRboId);
        GL30C.glRenderbufferStorage(GL30C.GL_RENDERBUFFER, GL30C.GL_DEPTH24_STENCIL8, this.width, this.height);
        GL30C.glBindRenderbuffer(GL30C.GL_RENDERBUFFER, 0);
    }

    public void bindForDraw() {
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, fboId);
    }

    public void bindForRead() {
        GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, fboId);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void cleanup() {
        // Avoid deleting 0 if init never ran or partially failed.
        if (depthStencilRboId != 0) {
            GL30C.glDeleteRenderbuffers(depthStencilRboId);
            depthStencilRboId = 0;
        }
        if (colorTexId != 0) {
            GL11C.glDeleteTextures(colorTexId);
            colorTexId = 0;
        }
        if (fboId != 0) {
            GL30C.glDeleteFramebuffers(fboId);
            fboId = 0;
        }
        width = 0;
        height = 0;
    }

    public int colorTexture() {
        return colorTexId;
    }
}
