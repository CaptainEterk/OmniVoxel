package omnivoxel.client.game.graphics.opengl;

import omnivoxel.client.game.camera.Camera;
import omnivoxel.client.game.entity.ClientEntity;
import omnivoxel.client.game.graphics.Renderer;
import omnivoxel.client.game.graphics.opengl.mesh.EntityMesh;
import omnivoxel.client.game.graphics.opengl.mesh.util.MeshGenerator;
import omnivoxel.client.game.graphics.opengl.shader.ShaderProgram;
import omnivoxel.client.game.graphics.opengl.shader.ShaderProgramHandler;
import omnivoxel.client.game.graphics.opengl.text.Alignment;
import omnivoxel.client.game.graphics.opengl.text.TextRenderer;
import omnivoxel.client.game.graphics.opengl.text.font.Font;
import omnivoxel.client.game.graphics.opengl.texture.TextureLoader;
import omnivoxel.client.game.graphics.opengl.window.Window;
import omnivoxel.client.game.graphics.opengl.window.WindowFactory;
import omnivoxel.client.game.position.DistanceChunk;
import omnivoxel.client.game.position.PositionedChunk;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.game.settings.Settings;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.game.world.ClientWorldChunk;
import omnivoxel.client.network.Client;
import omnivoxel.common.annotations.NotNull;
import omnivoxel.server.ConstantServerSettings;
import omnivoxel.util.executor.ExecutorCollection;
import omnivoxel.util.log.Logger;
import omnivoxel.util.math.Position3D;
import omnivoxel.util.time.PeriodicTimeExecutor;
import omnivoxel.util.time.Timer;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class OpenGLRenderer implements Renderer {
    private static final Matrix4f IDENTITY_MATRIX = new Matrix4f().identity();
    private static final int FPS_SAMPLES = 60;
    private final Queue<Runnable> frameActions = new ArrayDeque<>();
    private final List<PositionedChunk> solidRenderedChunksInFrustum = new ArrayList<>();
    private final List<PositionedChunk> transparentRenderedChunksInFrustum = new ArrayList<>();
    // Client
    private Client client;
    // TODO: Remove all TEMP
    // Window
    private Window window;
    // Shader program
    private ShaderProgram shaderProgram;
    private ShaderProgram zppShaderProgram;
    private ShaderProgram textShaderProgram;
    // State
    private Settings settings;
    private State state;
    private ExecutorCollection<PeriodicTimeExecutor> periodicTimeExecutorCollection;
    // Renderer
    private TextRenderer textRenderer;
    // Resources
    private Font font;
    private MeshGenerator meshGenerator;
    private int texture;
    private int TEMP_texture;
    private List<DistanceChunk> solidRenderedChunks;
    private List<DistanceChunk> transparentRenderedChunks;
    private String TEMP_rightDebugText = "";
    private Camera camera;
    private ClientWorld world;
    private Timer timer;
    private AtomicBoolean gameRunning;
    private Queue<Consumer<Window>> contextTasks;

    @Override
    public void init(Logger logger, State state, Settings settings, ClientWorld world, Camera camera, Client client, AtomicBoolean gameRunning, Queue<Consumer<Window>> contextTasks) {
        this.state = state;
        this.settings = settings;
        this.world = world;
        this.camera = camera;
        this.client = client;
        this.gameRunning = gameRunning;
        this.contextTasks = contextTasks;

        try {
            // Creates an OpenGL window
            this.window = WindowFactory.createWindow(settings.getIntSetting("width", 500), settings.getIntSetting("height", 500), ConstantGameSettings.DEFAULT_WINDOW_TITLE, logger, contextTasks);

            // Create renderers
            this.textRenderer = new TextRenderer();

            initShader();

            window.addResizingCallback(w -> {
                GL11C.glViewport(0, 0, w.getWidth(), w.getHeight());
                textShaderProgram.bind();
                textShaderProgram.setUniform("projection", new Matrix4f().ortho(0.0f, w.getWidth(), w.getHeight(), 0.0f, -1.0f, 1.0f));
                shaderProgram.bind();

                state.setItem("shouldUpdateView", true);
            });

            initState();

            this.textRenderer.init();

            initResources();

            this.window.init(settings.getIntSetting("width", 500), settings.getIntSetting("height", 500));
            this.window.show();

            initOpenGL();

            initFrameActions();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initShader() throws IOException {
        // Initializes the default shader
        // TODO: Make the player able to use their shaders instead.
        ShaderProgramHandler shaderProgramHandler = new ShaderProgramHandler();
        shaderProgramHandler.addShaderProgram("default", Map.of("assets/shaders/default.vert", GL20.GL_VERTEX_SHADER, "assets/shaders/default.frag", GL20.GL_FRAGMENT_SHADER));
        shaderProgramHandler.addShaderProgram("zpp", Map.of("assets/shaders/zpp.vert", GL20.GL_VERTEX_SHADER, "assets/shaders/zpp.frag", GL20.GL_FRAGMENT_SHADER));
        shaderProgramHandler.addShaderProgram("text", Map.of("assets/shaders/text.vert", GL20.GL_VERTEX_SHADER, "assets/shaders/text.frag", GL20.GL_FRAGMENT_SHADER));
        this.shaderProgram = shaderProgramHandler.getShaderProgram("default");
        this.zppShaderProgram = shaderProgramHandler.getShaderProgram("zpp");
        this.textShaderProgram = shaderProgramHandler.getShaderProgram("text");
        this.shaderProgram.bind();
        this.shaderProgram.setUniform("fogColor", 0.0f, 0.61568627451f, 1.0f, 1.0f);
        this.shaderProgram.setUniform("fogFar", settings.getFloatSetting("render_distance", 100) - ConstantGameSettings.CHUNK_SIZE);
        this.shaderProgram.setUniform("fogNear", (settings.getFloatSetting("render_distance", 100) - ConstantGameSettings.CHUNK_SIZE) / 10 * 9);
        this.shaderProgram.setUniform("blockTexture", 0);
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

        state.setItem("shouldRenderWireframe", false);
        state.setItem("seeDebug", true);
        state.setItem("bufferizing_queue_size", 0);
        state.setItem("missing_chunks", 0);

        state.setItem("z-prepass", false);

        state.setItem("inflight_requests", 0);
        state.setItem("chunk_requests_sent", 0);
        state.setItem("chunk_requests_received", 0);

        state.setItem("total_rendered_chunks", 1);

        solidRenderedChunks = new ArrayList<>();
        transparentRenderedChunks = new ArrayList<>();

        periodicTimeExecutorCollection = new ExecutorCollection<>();
        periodicTimeExecutorCollection.add(new PeriodicTimeExecutor(this::TEMP_updateRightText, 0.5));
        periodicTimeExecutorCollection.add(new PeriodicTimeExecutor(() -> state.setItem("attemptFreeChunksTime", true), 2.0));
    }

    private void initResources() throws IOException {
        // TODO: This should be able to use other fonts
        this.font = Font.create("Minecraft.ttf");

        this.meshGenerator = new MeshGenerator();

        // TODO: Make this stitch textures together and save texture coordinates in a string->(x, y) map.
        // TODO: Make the user be able to use texture packs instead (by loading it and stitching it together)
        this.texture = TextureLoader.loadTexture("texture_atlas.png");
        this.TEMP_texture = TextureLoader.loadTexture("player_texture.png");

        this.timer = new Timer(FPS_SAMPLES);
        this.timer.start();
    }

    private void initOpenGL() {
        GL11C.glClearColor(0.0f, 0.61568627451f, 1.0f, 1.0f);
        GL11C.glClearDepth(1.0f);
//        GL11C.glCullFace(GL11C.GL_BACK);
    }

    private void initFrameActions() {
        addFrameAction(periodicTimeExecutorCollection::execute);

        addFrameAction(this::start);
        addFrameAction(this::update);

        addFrameAction(this::renderEntities);

        addFrameAction(this::calculateFrustumChunks);

        addFrameAction(this::renderSolidChunks);
        addFrameAction(this::renderTransparentChunks);

        addFrameAction(this::bufferizeChunks);

        addFrameAction(this::renderDebugText);
        addFrameAction(this::openGLStateReset);

        addFrameAction(this::cleanupOpenGL);

        addFrameAction(this::updateState);

        addFrameAction(client::tick);
        addFrameAction(world::tick);
    }

    private void TEMP_updateRightText() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        this.TEMP_rightDebugText = String.format("""
                        GPU: %s
                        Java Version: %s
                        OpenGL Version: %s
                        Operating System: %s %s
                        Memory Usage: %,d/%,d (%.2f%%)
                        """,
                GL11.glGetString(GL11.GL_RENDERER),
                System.getProperty("java.version"),
                window.getVersion(),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                usedMemory,
                totalMemory,
                (double) usedMemory * 100d / totalMemory
        );
    }

    private void start() {
        // Clear the framebuffer
        GL11.glClear(GL11C.GL_COLOR_BUFFER_BIT | GL11C.GL_DEPTH_BUFFER_BIT);

        // Bind the shader program and set the time uniform
        shaderProgram.bind();
        shaderProgram.setUniform("time", (float) GLFW.glfwGetTime());

        // Bind the texture
        GL13C.glActiveTexture(GL13C.GL_TEXTURE0);
    }

    private void update() {
        if (state.getItem("shouldRenderWireframe", Boolean.class)) {
            GL11C.glPolygonMode(GL11C.GL_FRONT_AND_BACK, GL11C.GL_LINE);
        }

        shaderProgram.setUniform("cameraPosition", camera.getX(), camera.getY(), camera.getZ());

        if (state.getItem("shouldUpdateView", Boolean.class)) {
            Matrix4f projectionMatrix = new Matrix4f().setPerspective((float) Math.toRadians(camera.getFOV()), window.getAspectRatio(), camera.getNear(), camera.getFar());
            Matrix4f viewMatrix = new Matrix4f().rotate((float) camera.getPitch(), 1, 0, 0).rotate((float) camera.getYaw(), 0, 1, 0);
            Matrix4f cameraViewMatrix = new Matrix4f(viewMatrix).translate((float) -camera.getX(), (float) -camera.getY(), (float) -camera.getZ());

            camera.updateFrustum(projectionMatrix, new Matrix4f(viewMatrix).translate((float) -camera.getX(), (float) -camera.getY(), (float) -camera.getZ()));
            shaderProgram.setUniform("projection", projectionMatrix);
            shaderProgram.setUniform("view", cameraViewMatrix);
            shaderProgram.setUniform("cameraView", cameraViewMatrix);

            zppShaderProgram.bind();
            zppShaderProgram.setUniform("projection", projectionMatrix);
            zppShaderProgram.setUniform("view", viewMatrix);
            shaderProgram.bind();

            state.setItem("shouldUpdateView", false);
        }

        if (world.inflightRequests() < ConstantServerSettings.INFLIGHT_REQUESTS_MINIMUM) {
            state.setItem("shouldUpdateVisibleMeshes", true);
        }

        List<DistanceChunk> chunks;
        if (state.getItem("shouldUpdateVisibleMeshes", Boolean.class)) {
            solidRenderedChunks.clear();
            transparentRenderedChunks.clear();

            int renderDistance = settings.getIntSetting("render_distance", 100);

            chunks = calculateRenderedChunks(renderDistance);

            attemptFreeChunks();

            for (DistanceChunk chunk : chunks) {
                ClientWorldChunk clientWorldChunk = world.get(chunk.pos(), true, false);
                if (clientWorldChunk != null && clientWorldChunk.getMesh() != null) {
                    if (clientWorldChunk.getMesh().solidIndexCount() > 0) {
                        solidRenderedChunks.add(chunk);
                    }
                    if (clientWorldChunk.getMesh().transparentIndexCount() > 0) {
                        transparentRenderedChunks.add(chunk);
                    }
                }
            }

            transparentRenderedChunks.sort(Comparator.comparingInt(DistanceChunk::distance));

            state.setItem("total_rendered_chunks", chunks.size());

            state.setItem("shouldUpdateVisibleMeshes", false);
        }

        if (state.getItem("shouldAttemptFreeChunks", Boolean.class)) {
            attemptFreeChunks();
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
        shaderProgram.setUniform("meshType", 1);
        Map<String, ClientEntity> entityMeshes = world.getEntities();

        entityMeshes.forEach((id, clientEntity) -> {
//            if (camera.getFrustum().isEntityInFrustum(clientEntity, camera)) {
            renderEntityMesh(clientEntity.getMesh(), IDENTITY_MATRIX);
//            }
        });
    }

    private void calculateFrustumChunks() {
        solidRenderedChunksInFrustum.clear();
        for (DistanceChunk solidRenderedChunk : solidRenderedChunks) {
            if (camera.getFrustum().isChunkInFrustum(solidRenderedChunk.pos())) {
                solidRenderedChunksInFrustum.add(new PositionedChunk(solidRenderedChunk.pos(), world.get(solidRenderedChunk.pos(), false, false)));
            }
        }

        transparentRenderedChunksInFrustum.clear();
        for (DistanceChunk transparentRenderedChunk : transparentRenderedChunks) {
            if (camera.getFrustum().isChunkInFrustum(transparentRenderedChunk.pos())) {
                transparentRenderedChunksInFrustum.add(new PositionedChunk(transparentRenderedChunk.pos(), world.get(transparentRenderedChunk.pos(), false, false)));
            }
        }
    }

    private void renderSolidChunks() {
        shaderProgram.setUniform("meshType", 0);
        shaderProgram.setUniform("model", IDENTITY_MATRIX);

        GL11C.glEnable(GL11C.GL_DEPTH_TEST);
        GL11C.glDepthFunc(GL11C.GL_LEQUAL);
        GL11C.glDepthMask(true);

        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, texture);
        int occuluded = 0;

        if (state.getItem("z-prepass", Boolean.class)) {
            System.out.println("z-prepass");
            zppShaderProgram.bind();
            zppShaderProgram.setUniform("meshType", 0);
            zppShaderProgram.setUniform("model", IDENTITY_MATRIX);

            GL11C.glColorMask(false, false, false, false);

            for (PositionedChunk positionedChunk : solidRenderedChunksInFrustum) {
                Position3D position3D = positionedChunk.pos();
                if (positionedChunk.chunk().getMesh().solidVAO() > 0 && positionedChunk.chunk().getMesh().solidIndexCount() > 0) {
                    shaderProgram.setUniform("chunkPosition", position3D.x(), position3D.y(), position3D.z());
                    renderVAO(positionedChunk.chunk().getMesh().solidVAO(), positionedChunk.chunk().getMesh().solidIndexCount());
                }
            }

            GL11C.glDepthFunc(GL11C.GL_EQUAL);

            GL11C.glColorMask(true, true, true, true);
        }

        for (PositionedChunk positionedChunk : solidRenderedChunksInFrustum) {
            Position3D position3D = positionedChunk.pos();
            if (positionedChunk.chunk().getMesh().solidVAO() > 0 && positionedChunk.chunk().getMesh().solidIndexCount() > 0) {
                shaderProgram.setUniform("chunkPosition", position3D.x(), position3D.y(), position3D.z());
                renderVAO(positionedChunk.chunk().getMesh().solidVAO(), positionedChunk.chunk().getMesh().solidIndexCount());
            } else {
                occuluded++;
            }
        }

        state.setItem("geometry_culled_chunks", occuluded);
    }

    private void renderTransparentChunks() {
        GL11C.glDepthFunc(GL11C.GL_LESS);
        GL11C.glDisable(GL11C.GL_CULL_FACE);
        GL11C.glDepthMask(false);
        GL11C.glEnable(GL11C.GL_BLEND);
        GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA);
        int occuluded = 0;
        for (int i = transparentRenderedChunksInFrustum.size() - 1; i >= 0; i--) {
            PositionedChunk positionedChunk = transparentRenderedChunksInFrustum.get(i);
            Position3D position3D = positionedChunk.pos();
            shaderProgram.setUniform("chunkPosition", position3D.x(), position3D.y(), position3D.z());
            if (positionedChunk.chunk().getMesh().transparentVAO() > 0 && positionedChunk.chunk().getMesh().transparentIndexCount() > 0) {
                renderVAO(positionedChunk.chunk().getMesh().transparentVAO(), positionedChunk.chunk().getMesh().transparentIndexCount());
            } else {
                occuluded++;
            }
        }
        state.setItem("geometry_culled_chunks", state.getItem("geometry_culled_chunks", Integer.class) + occuluded);
    }

    private void bufferizeChunks() {
        state.setItem("bufferizing_chunk_count", world.bufferizeQueued(meshGenerator, System.nanoTime() + ConstantGameSettings.BUFFERIZE_END_TIME_LIMIT_MS * 1_000_000));
    }

    private void cleanupOpenGL() {
        GL30C.glBindVertexArray(0);
        GL11C.glBindTexture(GL11C.GL_TEXTURE_2D, 0);
    }

    private void updateState() {
        timer.stop();
        timer.start();
        double deltaTime = timer.averageTimes();
        state.setItem("fps", (int) (1_000_000_000 / deltaTime));
    }

    private void renderDebugText() {
        if (state.getItem("seeDebug", Boolean.class)) {
            String leftDebugText = ConstantGameSettings.DEFAULT_WINDOW_TITLE + "\n" + String.format(
                    """
                            FPS: %d
                            Position: %.2f %.2f %.2f
                            Delta Time: %.4f
                            Chunks:
                            \t- Rendered: %d/%d/%d
                            \t- Loaded: %d
                            \t- Should be loaded: %d
                            \t- Bufferized Chunks: %d
                            \t- Non-Bufferized Chunks: %d
                            \t- Missing Chunks: %d
                            Culling:
                            \t- Total: %d,
                            \t- Frustum: %d,
                            \t- Geometry: %d,
                            Network:
                            \t- Inflight Requests: %d
                            \t- Chunk Requests Sent: %d
                            \t- Chunk Requests Received: %d
                            Player:
                            \t- Velocity X: %.2f
                            \t- Velocity Y: %.2f
                            \t- Velocity Z: %.2f
                            \t- On Ground: %b
                            \t- Friction Factor: %.2f
                            \t- Movement Mode: %s
                            Pipelines:
                            \t- Queued Meshes: %d
                            \t- Queued Mesh Data's: %d
                            \t- Bufferizing Chunks: %d
                            """,
                    state.getItem("fps", Integer.class),
                    camera.getX(),
                    camera.getY(),
                    camera.getZ(),
                    state.getItem("deltaTime", Double.class),
                    solidRenderedChunksInFrustum.size() + transparentRenderedChunksInFrustum.size(),
                    solidRenderedChunksInFrustum.size(),
                    transparentRenderedChunksInFrustum.size(),
                    world.size(),
                    state.getItem("total_rendered_chunks", Integer.class),
                    state.getItem("bufferizing_chunk_count", Integer.class),
                    state.getItem("bufferizing_queue_size", Integer.class),
                    state.getItem("missing_chunks", Integer.class),
                    world.size() - solidRenderedChunks.size() + transparentRenderedChunks.size(),
                    (solidRenderedChunks.size() - solidRenderedChunksInFrustum.size()) + (transparentRenderedChunks.size() - transparentRenderedChunksInFrustum.size()),
                    state.getItem("geometry_culled_chunks", Integer.class),
                    state.getItem("inflight_requests", Integer.class),
                    state.getItem("chunk_requests_sent", Integer.class),
                    state.getItem("chunk_requests_received", Integer.class),
                    state.getItem("velocity_x", Double.class),
                    state.getItem("velocity_y", Double.class),
                    state.getItem("velocity_z", Double.class),
                    state.getItem("on_ground", Boolean.class),
                    state.getItem("friction_factor", Double.class),
                    state.getItem("movement_mode", String.class),
                    0,
                    0,
                    0
            );

            GL11C.glPolygonMode(GL11C.GL_FRONT_AND_BACK, GL11C.GL_FILL);
            textShaderProgram.bind();

            GL11.glDisable(GL11.GL_DEPTH_TEST);

            textRenderer.queueText(font, leftDebugText, 4, 4, 0.6f, Alignment.LEFT);
            textRenderer.queueText(font, TEMP_rightDebugText, window.getWidth() - 4, 4, 0.6f, Alignment.RIGHT);

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
        // Bind the VAO
        GL30C.glBindVertexArray(vao);

        // Draw the elements using indices in the VAO
        GL30C.glDrawElements(GL11C.GL_TRIANGLES, indexCount, GL11C.GL_UNSIGNED_INT, 0);
    }

    private @NotNull List<DistanceChunk> calculateRenderedChunks(int renderDistance) {
        int frustumBias = settings.getIntSetting("frustum_bias", 10);

        int chunkX = Math.round((float) renderDistance / ConstantGameSettings.CHUNK_WIDTH) + 1;
        int chunkY = Math.round((float) renderDistance / ConstantGameSettings.CHUNK_HEIGHT) + 1;
        int chunkZ = Math.round((float) renderDistance / ConstantGameSettings.CHUNK_LENGTH) + 1;

        int rdChunks = renderDistance / ConstantGameSettings.CHUNK_SIZE + 1;
        int squaredRenderDistance = rdChunks * rdChunks;
        Map<Integer, Set<DistanceChunk>> positionedChunks = new HashMap<>();
        int highestBucketDistance = 0;

        int ccx = (int) -Math.floor(camera.getX() / ConstantGameSettings.CHUNK_WIDTH);
        int ccy = (int) -Math.floor(camera.getY() / ConstantGameSettings.CHUNK_HEIGHT);
        int ccz = (int) -Math.floor(camera.getZ() / ConstantGameSettings.CHUNK_LENGTH);
        int count = 0;

        for (int x = -chunkX; x <= chunkX; x++) {
            for (int y = -chunkY; y <= chunkY; y++) {
                for (int z = -chunkZ; z <= chunkZ; z++) {
                    int dx = x - ccx;
                    int dy = y - ccy;
                    int dz = z - ccz;
                    int distance = x * x + y * y + z * z;

                    if (distance < squaredRenderDistance) {
                        Position3D position3D = new Position3D(dx, dy, dz);

                        if (!camera.getFrustum().isChunkInFrustum(position3D)) {
                            distance *= frustumBias;
                        }

                        if (distance > highestBucketDistance) {
                            highestBucketDistance = distance;
                        }

                        positionedChunks.computeIfAbsent(distance, i -> new HashSet<>()).add(new DistanceChunk(distance, position3D));
                        count++;
                    }
                }
            }
        }

        List<DistanceChunk> out = new ArrayList<>(count);

        for (int i = 0; i < highestBucketDistance; i++) {
            Set<DistanceChunk> posChunks = positionedChunks.get(i);
            if (posChunks != null) {
                out.addAll(posChunks);
            }
        }

        return out;
    }

    private void attemptFreeChunks() {
        int ccx = (int) Math.floor(camera.getX() / ConstantGameSettings.CHUNK_WIDTH);
        int ccy = (int) Math.floor(camera.getY() / ConstantGameSettings.CHUNK_HEIGHT);
        int ccz = (int) Math.floor(camera.getZ() / ConstantGameSettings.CHUNK_LENGTH);

        int renderDistance = settings.getIntSetting("render_distance", 100);
        int rdChunks = renderDistance / ConstantGameSettings.CHUNK_SIZE + 1;
        int squaredRenderDistance = rdChunks * rdChunks;

        world.freeAllChunksNotIn(position3D -> {
            int dx = position3D.x() - ccx;
            int dy = position3D.y() - ccy;
            int dz = position3D.z() - ccz;
            int distance = dx * dx + dy * dy + dz * dz;

            return distance < squaredRenderDistance;
        });
        state.setItem("shouldAttemptFreeChunks", false);
    }

    @Override
    public void addFrameAction(Runnable action) {
        frameActions.add(action);
    }

    @Override
    public boolean shouldClose() {
        return window.shouldClose();
    }

    @Override
    public void renderFrame() {
        // Run the context tasks
        Consumer<Window> task;
        while ((task = contextTasks.poll()) != null) {
            task.accept(window);
        }

        // Run all frameActions
        frameActions.forEach(Runnable::run);

        // Swap the framebuffers
        GLFW.glfwSwapBuffers(window.window());

        // Poll for window events.
        GLFW.glfwPollEvents();
    }

    @Override
    public void cleanup() {
        world.cleanup();
        textRenderer.cleanup();
        font.cleanup();

        gameRunning.set(false);

        client.close();

        GLFW.glfwDestroyWindow(window.window());

        GLFW.glfwTerminate();
    }

    @Override
    public Window getWindow() {
        return window;
    }
}