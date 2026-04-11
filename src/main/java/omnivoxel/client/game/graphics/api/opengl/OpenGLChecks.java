package omnivoxel.client.game.graphics.api.opengl;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

import java.util.ArrayList;
import java.util.List;

public final class OpenGLChecks {
    private OpenGLChecks() {
    }

    public static void checkError(String operation) {
        List<String> errors = new ArrayList<>();
        int error;
        while ((error = GL11C.glGetError()) != GL11C.GL_NO_ERROR) {
            errors.add(describe(error));
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("OpenGL error after " + operation + ": " + String.join(", ", errors));
        }
    }

    private static String describe(int error) {
        return switch (error) {
            case GL11C.GL_INVALID_ENUM -> "GL_INVALID_ENUM";
            case GL11C.GL_INVALID_VALUE -> "GL_INVALID_VALUE";
            case GL11C.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
            case GL11C.GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW";
            case GL11C.GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW";
            case GL11C.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
            case GL30C.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
            default -> "0x" + Integer.toHexString(error);
        };
    }
}
