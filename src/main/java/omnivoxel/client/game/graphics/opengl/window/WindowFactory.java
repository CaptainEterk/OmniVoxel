package omnivoxel.client.game.graphics.opengl.window;

import omnivoxel.client.game.graphics.opengl.image.Image;
import omnivoxel.client.game.graphics.opengl.image.ImageLoader;
import omnivoxel.util.log.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.util.Queue;
import java.util.function.Consumer;

public final class WindowFactory {
    public static Window createWindow(int width, int height, String title, Logger logger, Queue<Consumer<Window>> contextTasks) throws RuntimeException {
        // Set up an error callback. The default implementation will print the error message in System.err.
        GLFW.glfwSetErrorCallback((error, description) -> {
            String msg = GLFWErrorCallback.getDescription(description);
            logger.error(String.format("GLFW Error %d: %s", error, msg));
        });

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Get the resolution of the primary monitor
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        assert vidmode != null;

        int x = (vidmode.width() - width) / 2;
        int y = (vidmode.height() - height) / 2;

        // Set window hints before window creation
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_POSITION_X, x);
        GLFW.glfwWindowHint(GLFW.GLFW_POSITION_Y, y);
        GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, 24);
        GLFW.glfwWindowHint(GLFW.GLFW_DOUBLEBUFFER, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);


        // Create the window
        long window = GLFW.glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);

        // TODO: Make it so you can turn V-Sync on and off in Settings
        // Enable v-sync
        GLFW.glfwSwapInterval(0);

        /*
         This line is critical for LWJGL's interoperation with GLFW's OpenGL context,
         or any context that is managed externally.
         LWJGL detects the context that is current in the current thread,
         creates the GLCapabilities instance and makes the OpenGL bindings available for use.
        */
        GL.createCapabilities();

        // Mouse motion
        if (GLFW.glfwRawMouseMotionSupported()) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
        }

        if (logger != null) {
            logger.debug(String.format("OpenGL/GLFW window created - title \"%s\" - position (%d, %d) - size (%d, %d) - key %d", title, x, y, width, height, window));
        }

        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("mac")) {
            Image img = ImageLoader.load("assets/icons/favicon.png");

            GLFWImage image = GLFWImage.malloc();
            GLFWImage.Buffer imagebf = GLFWImage.malloc(1);
            image.set(img.width(), img.height(), img.image());
            imagebf.put(0, image);

            GLFW.glfwSetWindowIcon(window, imagebf);

            imagebf.free();
            image.free();
        }

        String version = GL11.glGetString(GL11.GL_VERSION);

        return new Window(window, version, contextTasks);
    }
}