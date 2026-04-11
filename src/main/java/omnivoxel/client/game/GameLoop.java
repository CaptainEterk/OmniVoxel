package omnivoxel.client.game;

import omnivoxel.client.game.graphics.Renderer;
import omnivoxel.client.game.graphics.api.opengl.OpenGLRenderer;
import omnivoxel.client.game.graphics.api.opengl.text.TextRenderer;
import omnivoxel.client.game.graphics.api.opengl.window.Window;
import omnivoxel.client.game.graphics.camera.Camera;
import omnivoxel.client.game.graphics.menu.MenuRenderer;
import omnivoxel.client.game.graphics.menu.MenuSystem;
import omnivoxel.client.game.graphics.menu.components.LayoutComponent;
import omnivoxel.client.game.graphics.menu.position.ComponentPositionOrigin;
import omnivoxel.client.game.settings.Settings;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.network.Client;

import java.io.IOException;
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
    private final LayoutComponent mainComponent;

    public GameLoop(Camera camera, ClientWorld world, AtomicBoolean gameRunning, BlockingQueue<Consumer<Window>> contextTasks, Client client, State state, Settings settings) {
        this.camera = camera;
        this.world = world;
        this.gameRunning = gameRunning;
        this.contextTasks = contextTasks;
        this.client = client;
        this.state = state;
        this.settings = settings;
        this.mainComponent = new LayoutComponent(ComponentPositionOrigin.TOP_LEFT);
        TextRenderer textRenderer = new TextRenderer();
        this.renderer = new OpenGLRenderer(
                state,
                settings,
                textRenderer,
                world,
                camera,
                client,
                gameRunning,
                contextTasks,
                new MenuSystem(new MenuRenderer(mainComponent), textRenderer)
        );
    }

    public void init() throws IOException {
        this.renderer.init();
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void run() {
        while (!renderer.shouldClose() && gameRunning.get() && client.isClientRunning()) {
            renderer.renderFrame();
        }
        renderer.cleanup();
    }
}
