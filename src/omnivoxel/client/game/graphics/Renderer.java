package omnivoxel.client.game.graphics;

import omnivoxel.client.game.camera.Camera;
import omnivoxel.client.game.graphics.opengl.window.Window;
import omnivoxel.client.game.settings.Settings;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.network.Client;
import omnivoxel.util.log.Logger;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public interface Renderer {
    void init(Logger logger, State state, Settings settings, ClientWorld world, Camera camera, Client client, AtomicBoolean gameRunning, Queue<Consumer<Window>> contextTasks);

    boolean shouldClose();

    void addFrameAction(Runnable action);

    void renderFrame();

    void cleanup();

    Window getWindow();
}