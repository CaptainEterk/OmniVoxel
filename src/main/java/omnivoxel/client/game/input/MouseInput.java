package omnivoxel.client.game.input;

import omnivoxel.client.game.graphics.api.opengl.window.Window;

import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;

public class MouseInput {
    private double oldX;
    private double oldY;

    private double deltaX;
    private double deltaY;

    public void init(Window window) {
        glfwSetCursorPosCallback(window.window(), this::mouseCallback);
    }

    private void mouseCallback(long window, double x, double y) {
        deltaX = x - oldX;
        deltaY = y - oldY;
        oldX = x;
        oldY = y;
    }

    public double getDeltaX() {
        return deltaX;
    }

    public double getDeltaY() {
        return deltaY;
    }

    public void clearDelta() {
        deltaX = 0;
        deltaY = 0;
    }
}