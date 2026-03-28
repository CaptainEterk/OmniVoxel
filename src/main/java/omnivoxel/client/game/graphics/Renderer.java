package omnivoxel.client.game.graphics;

import omnivoxel.client.game.graphics.api.opengl.window.Window;

import java.io.IOException;

public interface Renderer {
    void init() throws IOException;

    boolean shouldClose();

    void addFrameAction(Runnable action);

    void renderFrame();

    void cleanup();

    Window getWindow();
}