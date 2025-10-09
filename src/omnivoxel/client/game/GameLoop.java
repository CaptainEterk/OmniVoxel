package omnivoxel.client.game;

import omnivoxel.client.game.camera.Camera;
import omnivoxel.client.game.graphics.Renderer;
import omnivoxel.client.game.graphics.opengl.OpenGLRenderer;
import omnivoxel.client.game.graphics.opengl.window.Window;
import omnivoxel.client.game.settings.Settings;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.launcher.OmniVoxel;
import omnivoxel.client.network.Client;
import omnivoxel.util.log.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class GameLoop {
    private final Renderer renderer;
    private final Camera camera;
    private final ClientWorld world;
    private final AtomicBoolean gameRunning;
    private final BlockingQueue<Consumer<Window>> contextTasks;
    private final Client client;
    private final State state;
    private final Settings settings;

    public GameLoop(Camera camera, ClientWorld world, AtomicBoolean gameRunning, BlockingQueue<Consumer<Window>> contextTasks, Client client, State state, Settings settings) {
        this.camera = camera;
        this.world = world;
        this.gameRunning = gameRunning;
        this.contextTasks = contextTasks;
        this.client = client;
        this.state = state;
        this.settings = settings;
        this.renderer = new OpenGLRenderer();
    }

    public void init() {
        this.renderer.init(
                new Logger("Game Loop", OmniVoxel.SHOW_LOGS),
                state,
                settings,
                world,
                camera,
                client,
                gameRunning,
                contextTasks
        );
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void run() {
        while (!renderer.shouldClose()) {
            renderer.renderFrame();
        }
        renderer.cleanup();
    }
}