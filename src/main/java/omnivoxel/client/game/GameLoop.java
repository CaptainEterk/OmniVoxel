package omnivoxel.client.game;

import omnivoxel.client.game.graphics.RendererAPI;
import omnivoxel.client.game.graphics.api.opengl.OpenGLRendererAPI;
import omnivoxel.client.game.graphics.api.opengl.mesh.util.ChunkIndirectBuffer;
import omnivoxel.client.game.graphics.api.opengl.mesh.util.ChunkMeshBuffer;
import omnivoxel.client.game.graphics.api.opengl.mesh.util.MeshGenerator;
import omnivoxel.client.game.graphics.api.opengl.text.TextRenderer;
import omnivoxel.client.game.graphics.api.opengl.window.Window;
import omnivoxel.client.game.graphics.camera.Camera;
import omnivoxel.client.game.graphics.camera.CameraCullingService;
import omnivoxel.client.game.graphics.chunk.RenderedChunkProvider;
import omnivoxel.client.game.graphics.menu.MenuRenderer;
import omnivoxel.client.game.graphics.menu.MenuSystem;
import omnivoxel.client.game.graphics.menu.components.LayoutComponent;
import omnivoxel.client.game.graphics.menu.position.ComponentPositionOrigin;
import omnivoxel.client.game.graphics.renderer.ChunkRenderer;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.network.Client;
import omnivoxel.common.settings.Settings;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class GameLoop {
    private final RendererAPI rendererAPI;
    private final Camera camera;
    private final ClientWorld world;
    private final AtomicBoolean gameRunning;
    private final BlockingQueue<Consumer<Window>> contextTasks;
    private final Client client;
    private final State state;
    private final Settings settings;
    private final LayoutComponent mainComponent;
    private final ChunkRenderer chunkRenderer;

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
        this.rendererAPI = new OpenGLRendererAPI(
                state,
                settings,
                textRenderer,
                world,
                camera,
                client,
                gameRunning,
                contextTasks,
                new MenuSystem(new MenuRenderer(mainComponent), textRenderer),
                new CameraCullingService(camera)
        );
        chunkRenderer = new ChunkRenderer(rendererAPI, state, settings, camera, world, new RenderedChunkProvider(), rendererAPI.getShaderProgramHandler());
    }

    public void init() throws IOException {
        ChunkMeshBuffer chunkMeshBuffer = new ChunkMeshBuffer();

        ChunkIndirectBuffer chunkIndirectBuffer = new ChunkIndirectBuffer();

        rendererAPI.init(new MeshGenerator(chunkMeshBuffer));

        chunkMeshBuffer.init(
                256L * 1024L * 1024L,
                1024L * 1024L * 1024L
        );

        chunkIndirectBuffer.init(300_000);

        chunkRenderer.initResources(chunkMeshBuffer, chunkIndirectBuffer);
    }

    public RendererAPI getRenderer() {
        return rendererAPI;
    }

    public void run() {
        while (!rendererAPI.shouldClose() && gameRunning.get() && client.isClientRunning()) {
            rendererAPI.beginFrame();
            chunkRenderer.render();
            rendererAPI.endFrame();
        }
        rendererAPI.cleanup();
    }
}
