package omnivoxel.client.game.graphics.api.opengl;

import omnivoxel.client.game.entity.EntityMeshWrapper;
import omnivoxel.client.game.graphics.RenderType;
import omnivoxel.client.game.graphics.RendererAPI;
import omnivoxel.client.game.graphics.api.opengl.framebuffer.RenderFramebuffer;
import omnivoxel.client.game.graphics.api.opengl.mesh.EntityMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.FullscreenQuad;
import omnivoxel.client.game.graphics.api.opengl.mesh.RenderMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.util.MeshGenerator;
import omnivoxel.client.game.graphics.api.opengl.mesh.wireframe.WireframeMesh;
import omnivoxel.client.game.graphics.api.opengl.shader.ShaderProgram;
import omnivoxel.client.game.graphics.api.opengl.shader.ShaderProgramHandler;
import omnivoxel.client.game.graphics.api.opengl.text.Alignment;
import omnivoxel.client.game.graphics.api.opengl.text.TextRenderer;
import omnivoxel.client.game.graphics.api.opengl.texture.TextureLoader;
import omnivoxel.client.game.graphics.api.opengl.window.Window;
import omnivoxel.client.game.graphics.api.opengl.window.WindowFactory;
import omnivoxel.client.game.graphics.block.BlockMesh;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.camera.Camera;
import omnivoxel.client.game.graphics.camera.CameraCullingService;
import omnivoxel.client.game.graphics.menu.MenuSystem;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.Client;
import omnivoxel.common.block.shape.BlockShape;
import omnivoxel.common.settings.ConstantClientSettings;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.ConstantNetworkSettings;
import omnivoxel.common.settings.Settings;
import omnivoxel.util.IndexCalculator;
import omnivoxel.util.executor.ExecutorCollection;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.time.PeriodicTimeExecutor;
import omnivoxel.util.time.Timer;
import omnivoxel.world.chunk.Chunk;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class OpenGLRendererAPI implements RendererAPI {
    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f().identity();
    private static final int FPS_SAMPLES = 60;
    // Client
    private final Client client;
    // State
    private final Settings settings;
    private final State state;
    // Renderer
    private final TextRenderer textRenderer;
    private final Camera camera;
    private final ClientWorld world;
    private final AtomicBoolean gameRunning;
    private final Queue<Consumer<Window>> contextTasks;
    private final MenuSystem menuSystem;
    private final CameraCullingService cameraCullingService;
    private final Map<String, WireframeMesh> wireframeShapeMeshes = new HashMap<>();
    private final ShaderProgramHandler shaderProgramHandler = new ShaderProgramHandler();
    // TODO: Remove all TEMP
    // Window
    private Window window;
    // Shader program
    private ShaderProgram shaderProgram;
    private ShaderProgram textShaderProgram;
    private ExecutorCollection<PeriodicTimeExecutor> periodicTimeExecutorCollection;
    // Resources
    private MeshGenerator meshGenerator;
    private int TEMP_texture;
    private RenderFramebuffer renderFramebuffer;
    private RenderFramebuffer skyFramebuffer;
    private int renderWidth;
    private int renderHeight;
    private boolean renderTracksWindowSize;
    private int renderFilter;
    private Timer timer;
    private FullscreenQuad fullscreenQuad;

    public OpenGLRendererAPI(State state, Settings settings, TextRenderer textRenderer, ClientWorld world, Camera camera, Client client, AtomicBoolean gameRunning, Queue<Consumer<Window>> contextTasks, MenuSystem menuSystem, CameraCullingService cameraCullingService) {
        this.state = state;
        this.settings = settings;
        this.textRenderer = textRenderer;
        this.world = world;
        this.camera = camera;
        this.client = client;
        this.gameRunning = gameRunning;
        this.contextTasks = contextTasks;
        this.menuSystem = menuSystem;
        this.cameraCullingService = cameraCullingService;
    }

    @Override
    public void init(MeshGenerator meshGenerator) throws IOException {
        this.meshGenerator = meshGenerator;
        this.window = WindowFactory.createWindow(settings.getIntSetting("width", 500), settings.getIntSetting("height", 500), ConstantClientSettings.DEFAULT_WINDOW_TITLE, contextTasks, settings.getBooleanSetting("vsync", true));

        initShader();

        window.addResizingCallback(w -> {
            GL11C.glViewport(0, 0, w.getWidth(), w.getHeight());

            if (renderFramebuffer != null && renderTracksWindowSize) {
                updateInternalRenderSizeFromSettings();
                renderFramebuffer.resize(renderWidth, renderHeight);
            }

            if (skyFramebuffer != null && renderTracksWindowSize) {
                updateInternalRenderSizeFromSettings();
                skyFramebuffer.resize(renderWidth, renderHeight);
            }

            textShaderProgram.bind();
            textShaderProgram.setUniform("projection", new Matrix4f().ortho(0.0f, w.getWidth(), w.getHeight(), 0.0f, -1.0f, 1.0f));
            shaderProgram.bind();

            state.setItem("shouldUpdateView", true);
        });

        initState();

        this.textRenderer.init();

        initResources();
        menuSystem.init();

        this.window.init(settings.getIntSetting("width", 500), settings.getIntSetting("height", 500));
        this.window.show();

        initOpenGL();
        initRenderTarget();
    }

    private void initShader() throws IOException {
        // TODO: Make the player able to use their shaders instead.
        shaderProgramHandler.addShaderProgram("default", Map.of("assets/shaders/default.vert", GL20.GL_VERTEX_SHADER, "assets/shaders/default.frag", GL20.GL_FRAGMENT_SHADER));
        shaderProgramHandler.addShaderProgram("text", Map.of("assets/shaders/text.vert", GL20.GL_VERTEX_SHADER, "assets/shaders/text.frag", GL20.GL_FRAGMENT_SHADER));
        String shaderProgramID = settings.getSetting("shader", "default");
        this.shaderProgram = shaderProgramHandler.getShaderProgram(shaderProgramID) == null ? shaderProgramHandler.getShaderProgram("default") : shaderProgramHandler.getShaderProgram(shaderProgramID);
        this.textShaderProgram = shaderProgramHandler.getShaderProgram("text");
        this.shaderProgram.bind();
        this.shaderProgram.setUniform("fogColor", 0.0f, 0.0f, 0.0f, 0.0f);
        this.shaderProgram.setUniform("fogFar", settings.getFloatSetting("render_distance", 100) - ConstantCommonSettings.CHUNK_SIZE);
        this.shaderProgram.setUniform("fogNear", (settings.getFloatSetting("render_distance", 100) - ConstantCommonSettings.CHUNK_SIZE) / 10 * 9);
        this.shaderProgram.setUniform("renderDistance", settings.getFloatSetting("render_distance", 100));
        this.shaderProgram.setUniform("blockTexture", 0);
        this.shaderProgram.setUniform("highlightColor", 0.0f, 0.0f, 0.0f, 0.7f);
        this.shaderProgram.unbind();

        this.textShaderProgram.bind();

        this.textShaderProgram.setUniform("textColor", 1f, 1f, 1f);
        this.textShaderProgram.setUniform("textTexture", 0);

        this.textShaderProgram.unbind();
    }

    private void initState() {
        state.setItem("shouldUpdateTextView", true);
        state.setItem("shouldUpdateView", true);
        state.setItem("shouldUpdateVisibleMeshes", true);
        state.setItem("shouldCheckNewChunks", false);
        state.setItem("shouldAttemptFreeChunks", false);
        state.setItem("shouldFreeAll", false);
        state.setItem("shouldToggleWindowFullscreen", false);
        state.setItem("has_observed_block", false);
        // TODO: Remove in_water hardcoding
        state.setItem("in_water", false);
        state.setItem("prev_in_water", false);

        state.setItem("shouldRenderWireframe", false);
        state.setItem("seeDebug", true);
        state.setItem("bufferizing_queue_size", 0);
        state.setItem("missing_chunks", 0);

        state.setItem("inflight_requests", 0);
        state.setItem("chunk_requests_sent", 0);
        state.setItem("chunk_requests_received", 0);

        state.setItem("total_rendered_chunks", 1);

        for (int i = 0; i < Math.max(settings.getIntSetting("max_mesh_generator_threads", Runtime.getRuntime().availableProcessors()), settings.getIntSetting("max_lighting_generator_threads", Runtime.getRuntime().availableProcessors())); i++) {
            state.setItem("Worker-" + i + "_queue_size_cmdlg", 0);
            state.setItem("Worker-" + i + "_queue_size_mdg", 0);
        }
        state.setItem("indirect_buffer_size", 0L);
        state.setItem("indirect_buffer_used_percentage", 0);

        periodicTimeExecutorCollection = new ExecutorCollection<>();
        periodicTimeExecutorCollection.add(new PeriodicTimeExecutor(() -> state.setItem("attemptFreeChunksTime", true), 2.0));
//        periodicTimeExecutorCollection.add(new PeriodicTimeExecutor(() -> System.out.println(state.getItem("fps", Integer.class)), 0.25));
    }

    private void initResources() {
        // TODO: Make this stitch textures together and save texture coordinates in a string->(x, y) map.
        // TODO: Make the user be able to use texture packs instead (by loading it and stitching it together)
        // TODO: Make this also be able to use texture arrays instead of texture atlases depending on OpenGL version (keep atlases around for older hardware).
        this.TEMP_texture = TextureLoader.loadTexture("player_texture.png");

        this.timer = new Timer(FPS_SAMPLES);
        this.timer.start();

        this.fullscreenQuad = new FullscreenQuad();
        this.fullscreenQuad.init();
    }

    private void initOpenGL() {
        GL11C.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        GL11C.glClearDepth(1.0f);
        GL11C.glCullFace(GL11C.GL_BACK);
        OpenGLChecks.checkError("initialize OpenGL state");
    }

    private void initRenderTarget() {
        updateInternalRenderSizeFromSettings();
        renderFramebuffer = new RenderFramebuffer();
        renderFramebuffer.init(renderWidth, renderHeight, renderFilter);
        skyFramebuffer = new RenderFramebuffer();
        skyFramebuffer.init(renderWidth, renderHeight, renderFilter);
        OpenGLChecks.checkError("initialize render targets");
    }

    private void updateTime() {
        shaderProgram.bind();
        shaderProgram.setUniform("time", (float) GLFW.glfwGetTime());
    }

    private void clearSkyFramebuffer() {
        if (skyFramebuffer != null) {
            skyFramebuffer.bindForDraw();
            shaderProgram.setUniform("screenResolution", skyFramebuffer.width(), skyFramebuffer.height());
            GL11C.glViewport(0, 0, skyFramebuffer.width(), skyFramebuffer.height());
        }

        GL11.glClear(GL11C.GL_COLOR_BUFFER_BIT);
    }

    private void renderSkyToCurrentFramebuffer() {
        GL11C.glDepthMask(false);
        GL11C.glDisable(GL11C.GL_CULL_FACE);
        GL11C.glDisable(GL11C.GL_DEPTH_TEST);

        shaderProgram.bind();
        shaderProgram.setUniformUnsigned("meshType", 2);
        float angle = (float) (GLFW.glfwGetTime() / 60 * Math.PI);
        float sunY = (float) Math.sin(angle);

        float skyIntensity = Math.max(0f, sunY);
        shaderProgram.setUniform("skyIntensity", skyIntensity);
        fullscreenQuad.render();

        GL11C.glEnable(GL11C.GL_CULL_FACE);
        GL11C.glEnable(GL11C.GL_DEPTH_TEST);
        GL11C.glDepthMask(true);
    }

    private void renderSky() {
        skyFramebuffer.bindForDraw();
        GL11C.glViewport(0, 0, skyFramebuffer.width(), skyFramebuffer.height());
        GL11C.glClear(GL11C.GL_COLOR_BUFFER_BIT);
        renderSkyToCurrentFramebuffer();
        shaderProgram.setUniform("skyTexture", 1);

        clearRenderFramebuffer();
        GL11C.glViewport(0, 0, renderFramebuffer.width(), renderFramebuffer.height());
        renderSkyToCurrentFramebuffer();

        GL13C.glActiveTexture(GL13C.GL_TEXTURE1);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, skyFramebuffer.colorTexture());
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
    }

    private void clearRenderFramebuffer() {
        if (renderFramebuffer != null) {
            renderFramebuffer.bindForDraw();
            shaderProgram.setUniform("screenResolution", renderFramebuffer.width(), renderFramebuffer.height());
            GL11C.glViewport(0, 0, renderFramebuffer.width(), renderFramebuffer.height());
        }

        GL11.glClear(GL11C.GL_DEPTH_BUFFER_BIT);

        GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
    }

    private void update() {
        boolean inWater = state.getItem("in_water", Boolean.class);
        if (inWater != state.getItem("prev_in_water", Boolean.class)) {
            if (inWater) {
                this.shaderProgram.setUniform("fogColor", 0.0f, 0.21f, 0.21f, 0.0f);
                this.shaderProgram.setUniform("fogFar", (float) ConstantCommonSettings.CHUNK_SIZE / 2f);
                this.shaderProgram.setUniform("fogNear", 0f);
            } else {
                this.shaderProgram.setUniform("fogColor", 0.0f, 0.0f, 0.0f, 1.0f);
            }
        }
        state.setItem("prev_in_water", inWater);
        if (state.getItem("shouldRenderWireframe", Boolean.class)) {
            GL11C.glPolygonMode(GL11C.GL_FRONT_AND_BACK, GL11C.GL_LINE);
        }

        shaderProgram.setUniform("cameraPosition", camera.getX(), camera.getY(), camera.getZ());

        if (state.getItem("shouldUpdateView", Boolean.class)) {
            Matrix4f projectionMatrix = new Matrix4f().setPerspective((float) Math.toRadians(camera.getFOV()), window.getAspectRatio(), camera.getNear(), camera.getFar());
            Matrix4f viewMatrix = new Matrix4f().rotate((float) camera.getPitch(), 1, 0, 0).rotate((float) camera.getYaw(), 0, 1, 0);
            Matrix4f cameraViewMatrix = new Matrix4f(viewMatrix).translate((float) -camera.getX(), (float) -camera.getY(), (float) -camera.getZ());

            camera.updateFrustum(projectionMatrix, cameraViewMatrix);
            shaderProgram.setUniform("projection", projectionMatrix);
            shaderProgram.setUniform("view", cameraViewMatrix);
            shaderProgram.setUniform("cameraView", cameraViewMatrix);

            Matrix4f invProjection = new Matrix4f(projectionMatrix).invert();
            Matrix4f invView = new Matrix4f(viewMatrix).invert();

            shaderProgram.setUniform("invProjection", invProjection);
            shaderProgram.setUniform("invView", invView);

            state.setItem("shouldUpdateView", false);
        }

        if (world.inflightRequestCount() < ConstantNetworkSettings.INFLIGHT_REQUESTS_MINIMUM) {
            state.setItem("shouldUpdateVisibleMeshes", true);
        }

        if (state.getItem("shouldFreeAll", Boolean.class)) {
            world.freeAllChunksNotInAndNotRecentlyAccessed((position3D) -> false, meshGenerator.getChunkMeshBuffer(), Integer.MAX_VALUE);
        }

        attemptFreeChunks();

        if (state.getItem("shouldToggleWindowFullscreen", Boolean.class)) {
            window.toggleFullscreen();
            state.setItem("shouldToggleWindowFullscreen", false);
        }
    }

    private void openGLStateReset() {
        GL11C.glEnable(GL11C.GL_DEPTH_TEST);
        GL11C.glEnable(GL11C.GL_CULL_FACE);
        GL11C.glDepthMask(true);
        GL11C.glDepthFunc(GL11C.GL_LESS);
        GL11C.glClearDepth(1.0f);
        GL11C.glDisable(GL11C.GL_BLEND);
    }

    private void renderEntities() {
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, TEMP_texture);
        shaderProgram.setUniformUnsigned("meshType", 1);
        Map<String, EntityMeshWrapper> entityMeshes = world.getEntityMeshes();

        entityMeshes.forEach((id, entityMeshWrapper) -> {
//            if (camera.getFrustum().isEntityInFrustum(clientEntity, camera)) {
            renderEntityMesh(entityMeshWrapper.entityMeshReference().getEntityMesh(), IDENTITY_MATRIX);
//            }
        });
    }

    private void renderBlockHighlight() {
        if (!Boolean.TRUE.equals(state.getItem("has_observed_block", Boolean.class))) {
            return;
        }

        Position3D observedBlock = state.getItem("observed_block", Position3D.class);
        if (observedBlock == null) {
            return;
        }

        int chunkX = IndexCalculator.chunkX(observedBlock.x());
        int chunkY = IndexCalculator.chunkY(observedBlock.y());
        int chunkZ = IndexCalculator.chunkZ(observedBlock.z());
        int localX = IndexCalculator.localX(observedBlock.x());
        int localY = IndexCalculator.localY(observedBlock.y());
        int localZ = IndexCalculator.localZ(observedBlock.z());

        ClientWorldChunk clientWorldChunk = world.get(new Position3D(chunkX, chunkY, chunkZ), false, false);
        if (clientWorldChunk == null) {
            return;
        }

        Chunk<BlockWithMesh> chunk = clientWorldChunk.getChunkData(-1);
        if (chunk == null) {
            return;
        }

        BlockWithMesh block = chunk.getBlock(localX, localY, localZ);
        if (block == null) {
            return;
        }

        BlockMesh blockMesh = block.blockMesh();
        if (blockMesh == null || BlockShape.EMPTY_BLOCK_SHAPE_STRING.equals(blockMesh.getShape().id())) {
            return;
        }

        WireframeMesh wireframeMesh = wireframeShapeMeshes.computeIfAbsent(blockMesh.getShape().id(), ignored -> WireframeMesh.create(blockMesh.getShape()));
        if (wireframeMesh.indexCount() == 0) {
            return;
        }

        byte rotation = blockMesh.isRotatable() ? chunk.getBlockRotation(localX, localY, localZ) : 0;
        Matrix4f model = new Matrix4f()
                .translate(observedBlock.x(), observedBlock.y(), observedBlock.z())
                .translate(0.5f, 0.0f, 0.5f)
                .rotateY((float) ((rotation & 3) * Math.PI / 2.0))
                .translate(-0.5f, 0.0f, -0.5f);

        shaderProgram.setUniformUnsigned("meshType", 3);
        shaderProgram.setUniform("model", model);
        shaderProgram.setUniform("highlightColor", 0.0f, 0.0f, 0.0f, 0.5f);

        GL11C.glEnable(GL11C.GL_DEPTH_TEST);
        GL11C.glDepthFunc(GL11C.GL_LEQUAL);
        GL11C.glDepthMask(false);
        GL11C.glDisable(GL11C.GL_CULL_FACE);
        GL11C.glEnable(GL11C.GL_BLEND);
        GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA);
        GL11C.glLineWidth(2.0f);

        GL30C.glBindVertexArray(wireframeMesh.vao());
        GL11C.glDrawElements(GL11C.GL_LINES, wireframeMesh.indexCount(), GL11C.GL_UNSIGNED_INT, 0);
        GL30C.glBindVertexArray(0);

        GL11C.glLineWidth(1.0f);
        GL11C.glDepthMask(true);
    }

    private void bufferizeChunks() {
        state.setItem("bufferizing_chunk_count", world.bufferizeQueued(meshGenerator, System.nanoTime() + ConstantClientSettings.BUFFERIZE_END_TIME_LIMIT_MS * 1_000_000));
    }

    private void cleanupOpenGL() {
        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
        GL30C.glBindVertexArray(0);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
        OpenGLChecks.checkError("cleanup OpenGL bindings");
    }

    private void blitToWindowFramebuffer() {
        if (renderFramebuffer == null) {
            return;
        }

        renderFramebuffer.bindForRead();
        GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, 0);
        GL30C.glBlitFramebuffer(
                0, 0, renderFramebuffer.width(), renderFramebuffer.height(),
                0, 0, window.getWidth(), window.getHeight(),
                GL11C.GL_COLOR_BUFFER_BIT,
                renderFilter
        );
        GL30C.glBlitFramebuffer(
                0, 0, renderFramebuffer.width(), renderFramebuffer.height(),
                0, 0, window.getWidth(), window.getHeight(),
                GL11C.GL_DEPTH_BUFFER_BIT,
                GL11C.GL_NEAREST
        );

        GL30C.glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
        GL11C.glViewport(0, 0, window.getWidth(), window.getHeight());
    }

    private void updateInternalRenderSizeFromSettings() {
        int explicitW = settings.getIntSetting("render_width", 0);
        int explicitH = settings.getIntSetting("render_height", 0);

        if (explicitW > 0 && explicitH > 0) {
            renderWidth = explicitW;
            renderHeight = explicitH;
            renderTracksWindowSize = false;
        } else {
            float scale = settings.getFloatSetting("render_scale", 0.0f);
            if (scale > 0.0f) {
                renderWidth = Math.max(1, Math.round(window.getWidth() * scale));
                renderHeight = Math.max(1, Math.round(window.getHeight() * scale));
            } else {
                renderWidth = Math.max(1, window.getWidth());
                renderHeight = Math.max(1, window.getHeight());
            }
            renderTracksWindowSize = true;
        }

        String filterSetting = settings.getSetting("render_filter", "nearest");
        renderFilter = filterSetting != null && filterSetting.equalsIgnoreCase("linear")
                ? GL11C.GL_LINEAR
                : GL11C.GL_NEAREST;
    }

    private void prepareGuiRendering() {
        GL11C.glPolygonMode(GL11C.GL_FRONT_AND_BACK, GL11C.GL_FILL);
        textShaderProgram.bind();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private void resetGuiRendering() {
        textRenderer.flush();

        textShaderProgram.unbind();
    }

    private final long[] frameTimes = new long[1000];
    private int frameIndex = 0;

    private void updateState() {
        timer.stop();
        timer.start();

        double deltaTime = timer.getLastDuration();

        frameTimes[frameIndex++ % frameTimes.length] = (long) deltaTime;

        long[] sorted = frameTimes.clone();
        Arrays.sort(sorted);

        double p99  = sorted[(int) (sorted.length * 0.99)];
        double p999 = sorted[(int) (sorted.length * 0.999)];
        double p9999 = sorted[(int) (sorted.length * 0.9999)];

        state.setItem("fps", (int) (1_000_000_000 / deltaTime));
        state.setItem("low1", (int) (1_000_000_000 / p99));
        state.setItem("low.1", (int) (1_000_000_000 / p999));
        state.setItem("low.01", (int) (1_000_000_000 / p9999));
    }

    // TODO: Remove this replace it with GUI rendering
    private void renderDebugText() {
        if (state.getItem("seeDebug", Boolean.class)) {
            String leftDebugText = ConstantClientSettings.DEFAULT_WINDOW_TITLE + "\n" + String.format(
                    """
                            FPS: %d %d/%d/%d
                            Position: %.2f %.2f %.2f
                            Delta Time: %.4f
                            Chunks:
                            \t- Loaded: %d
                            \t- Should be loaded: %d
                            \t- Bufferized Chunks: %d
                            \t- Non-Bufferized Chunks: %d
                            Rendering:
                            \t- Indirect vertex buffer size: %d
                            \t- Indirect vertex buffer usage: %.2f%%
                            Network:
                            \t- Inflight Requests: %d
                            \t- Chunk Requests Sent: %d
                            \t- Chunk Requests Received: %d
                            Player:
                            \t- Velocity X: %.2f
                            \t- Velocity Y: %.2f
                            \t- Velocity Z: %.2f
                            \t- Pitch: %.2f
                            \t- Yaw: %.2f
                            \t- On Ground: %b
                            \t- Friction Factor: %.2f
                            \t- Movement Mode: %s
                            \t- Selected Block: %s
                            \t- Observed Block: %s
                            \t- In Water: %b
                            Pipelines:
                            \t- Total: %d
                            \t- Lighting: %d
                            \t- Meshing: %d
                            """,
                    state.getItem("fps", Integer.class),
                    state.getItem("low1", Integer.class),
                    state.getItem("low.1", Integer.class),
                    state.getItem("low.01", Integer.class),
                    camera.getX(),
                    camera.getY(),
                    camera.getZ(),
                    state.getItem("deltaTime", Double.class),
                    world.size(),
                    state.getItem("total_rendered_chunks", Integer.class),
                    state.getItem("bufferizing_chunk_count", Integer.class),
                    state.getItem("bufferizing_queue_size", Integer.class),
                    state.getItem("indirect_buffer_size", Long.class),
                    state.getItem("indirect_buffer_used_percentage", Double.class),
                    world.inflightRequestCount(),
                    state.getItem("chunk_requests_sent", Integer.class),
                    state.getItem("chunk_requests_received", Integer.class),
                    state.getItem("velocity_x", Double.class),
                    state.getItem("velocity_y", Double.class),
                    state.getItem("velocity_z", Double.class),
                    state.getItem("pitch", Double.class),
                    state.getItem("yaw", Double.class),
                    state.getItem("on_ground", Boolean.class),
                    state.getItem("friction_factor", Double.class),
                    state.getItem("movement_mode", String.class),
                    state.getItem("selected_block", String.class),
                    state.getItem("observed_block_id", String.class),
                    state.getItem("in_water", Boolean.class),
                    world.inPipelineChunkCount(),
                    state.getItem("Worker-0_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-1_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-2_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-3_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-4_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-5_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-6_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-7_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-8_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-9_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-10_queue_size_cmdlg", Integer.class) +
                            state.getItem("Worker-11_queue_size_cmdlg", Integer.class),
                    state.getItem("Worker-0_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-1_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-2_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-3_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-4_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-5_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-6_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-7_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-8_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-9_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-10_queue_size_mdg", Integer.class) +
                            state.getItem("Worker-11_queue_size_mdg", Integer.class)
            );

            GL11C.glPolygonMode(GL11C.GL_FRONT_AND_BACK, GL11C.GL_FILL);
            textShaderProgram.bind();

            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11C.glDepthMask(false);
            GL11C.glDisable(GL11C.GL_CULL_FACE);
            GL11C.glEnable(GL11C.GL_BLEND);
            GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA);

            textRenderer.queueText(this.menuSystem.getFont(), leftDebugText, 4, 4, 0.6f, Alignment.LEFT);
            textRenderer.queueText(this.menuSystem.getFont(), "+", window.getWidth() / 2f, window.getHeight() / 2f, 0.6f, Alignment.CENTER);

            textRenderer.flush();

            textShaderProgram.unbind();
        }
    }

    private void renderEntityMesh(EntityMesh entityMesh, Matrix4f parentTransform) {
        if (entityMesh != null) {
            Matrix4f currentTransform = new Matrix4f(parentTransform).mul(entityMesh.getMeshData().getModel());
            shaderProgram.setUniform("model", currentTransform);
            renderVAO(entityMesh.getDefinition().solidVAO(), entityMesh.getDefinition().solidIndexCount());
            entityMesh.getChildren().forEach(mesh -> renderEntityMesh(mesh, currentTransform));
        }
    }

    private void renderVAO(int vao, int indexCount) {
        GL30C.glBindVertexArray(vao);

        GL30C.glDrawElements(GL11C.GL_TRIANGLES, indexCount, GL11C.GL_UNSIGNED_INT, 0);
    }

    // TODO: Move to ChunkRenderer
    private void attemptFreeChunks() {
        int renderDistance = settings.getIntSetting("render_distance", 100);
        int rdChunks = renderDistance / ConstantCommonSettings.CHUNK_SIZE + 1;
        int squaredRenderDistance = rdChunks * rdChunks;

        cameraCullingService.calculateChunkPosition();
        world.freeAllChunksNotInAndNotRecentlyAccessed(position3D -> !cameraCullingService.shouldDistanceCullChunk(position3D, squaredRenderDistance), meshGenerator.getChunkMeshBuffer(), settings.getIntSetting("free_chunk_max", 100));
        state.setItem("shouldAttemptFreeChunks", false);
    }

    @Override
    public boolean shouldClose() {
        return window.shouldClose();
    }

    @Override
    public void beginFrame() {
        Consumer<Window> task;
        while ((task = contextTasks.poll()) != null) {
            task.accept(window);
        }

        periodicTimeExecutorCollection.execute();

        updateTime();
        clearSkyFramebuffer();
        renderSky();

        update();

        renderEntities();

    }

    @Override
    public void endFrame() {
        renderBlockHighlight();

        bufferizeChunks();

        blitToWindowFramebuffer();

//        prepareGuiRendering();
//        menuSystem.tick();
//        resetGuiRendering();
        renderDebugText();
        openGLStateReset();

        cleanupOpenGL();
        OpenGLChecks.checkError("finish frame");

        updateState();

        client.tick();
        world.tick();

        GLFW.glfwSwapBuffers(window.window());

        GLFW.glfwPollEvents();
    }

    @Override
    public void cleanup() {
        textRenderer.cleanup();
        menuSystem.cleanup();

        if (fullscreenQuad != null) {
            fullscreenQuad.cleanup();
        }

        wireframeShapeMeshes.clear();

        if (renderFramebuffer != null) {
            renderFramebuffer.cleanup();
            renderFramebuffer = null;
        }

        gameRunning.set(false);

        client.close();

        world.cleanup(meshGenerator.getChunkMeshBuffer());

        shaderProgram.cleanup();

        textShaderProgram.cleanup();

        GLFW.glfwDestroyWindow(window.window());

        GLFW.glfwTerminate();
    }

    @Override
    public void setMipmapping(boolean mipmapped) {
        if (mipmapped) {
            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D,
                    GL11C.GL_TEXTURE_MIN_FILTER,
                    GL11C.GL_NEAREST_MIPMAP_LINEAR);

            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D,
                    GL11C.GL_TEXTURE_MAG_FILTER,
                    GL11C.GL_NEAREST);

            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D,
                    GL12C.GL_TEXTURE_MAX_LEVEL,
                    1000);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                    GL11.GL_TEXTURE_MIN_FILTER,
                    GL11.GL_NEAREST);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
                    GL11.GL_TEXTURE_MAG_FILTER,
                    GL11.GL_NEAREST);

            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D,
                    GL12C.GL_TEXTURE_BASE_LEVEL,
                    0);

            GL11C.glTexParameteri(GL11C.GL_TEXTURE_2D,
                    GL12C.GL_TEXTURE_MAX_LEVEL,
                    0);
        }
    }

    @Override
    public Window getWindow() {
        return window;
    }

    @Override
    public void setRenderType(RenderType renderType) {
        shaderProgram.setUniformUnsigned("meshType", renderType.ordinal());
    }

    @Override
    public void setShaderIVec3(String id, int x, int y, int z) {
        shaderProgram.setUniform(id, x, y, z);
    }

    @Override
    public void setShaderInt(String id, int i) {
        shaderProgram.setUniform(id, i);
    }

    @Override
    public void render(RenderMesh mesh) {
//        renderVAO(mesh.vao(), mesh.indexCount());
    }

    @Override
    public ShaderProgramHandler getShaderProgramHandler() {
        return shaderProgramHandler;
    }
}
