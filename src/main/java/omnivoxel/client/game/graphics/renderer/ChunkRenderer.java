package omnivoxel.client.game.graphics.renderer;

import omnivoxel.client.game.graphics.RendererAPI;
import omnivoxel.client.game.graphics.api.opengl.OpenGLChecks;
import omnivoxel.client.game.graphics.api.opengl.mesh.RenderMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.util.ChunkIndirectBuffer;
import omnivoxel.client.game.graphics.api.opengl.mesh.util.ChunkMeshBuffer;
import omnivoxel.client.game.graphics.api.opengl.shader.ShaderProgram;
import omnivoxel.client.game.graphics.api.opengl.shader.ShaderProgramHandler;
import omnivoxel.client.game.graphics.api.opengl.texture.TextureLoader;
import omnivoxel.client.game.graphics.camera.Camera;
import omnivoxel.client.game.graphics.chunk.RenderedChunkProvider;
import omnivoxel.client.game.position.DistanceChunk;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.Settings;
import omnivoxel.util.math.Position3D;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ChunkRenderer {
    private static final int CHUNK_GPU_SIZE = 64;
    private static final int INDIRECT_COMMAND_SIZE = 5 * Integer.BYTES;
    private final RendererAPI rendererAPI;
    private final State state;
    private final Settings settings;
    private final List<DistanceChunk> solidRenderedChunks = new ArrayList<>();
    private final List<DistanceChunk> decorationRenderedChunks = new ArrayList<>();
    private final List<DistanceChunk> transparentRenderedChunks = new ArrayList<>();
    private final Camera camera;
    private final ClientWorld world;
    private final RenderedChunkProvider renderedChunkProvider;
    private final ShaderProgramHandler shaderProgramHandler;
    private int texture;
    private ShaderProgram chunkCullingComputeShaderProgram;
    private int chunkBuffer;
    private int chunkBufferCapacity;
    private ChunkMeshBuffer chunkMeshBuffer;
    private ChunkIndirectBuffer chunkIndirectBuffer;
    private int indirectDrawCount;

    public ChunkRenderer(RendererAPI rendererAPI, State state, Settings settings, Camera camera, ClientWorld world, RenderedChunkProvider renderedChunkProvider) {
        this(rendererAPI, state, settings, camera, world, renderedChunkProvider, rendererAPI.getShaderProgramHandler());
    }

    public ChunkRenderer(RendererAPI rendererAPI, State state, Settings settings, Camera camera, ClientWorld world, RenderedChunkProvider renderedChunkProvider, ShaderProgramHandler shaderProgramHandler) {
        this.rendererAPI = rendererAPI;
        this.state = state;
        this.settings = settings;
        this.camera = camera;
        this.world = world;
        this.renderedChunkProvider = renderedChunkProvider;
        this.shaderProgramHandler = shaderProgramHandler;
    }

    public void initResources(ChunkMeshBuffer chunkMeshBuffer, ChunkIndirectBuffer chunkIndirectBuffer) throws IOException {
        this.chunkMeshBuffer = chunkMeshBuffer;
        this.chunkIndirectBuffer = chunkIndirectBuffer;
        this.texture = TextureLoader.loadTexture("texture_atlas.png");
        this.chunkCullingComputeShaderProgram = shaderProgramHandler.addShaderProgram("chunk_indirect", Map.of("assets/shaders/chunk_indirect.comp", GL43C.GL_COMPUTE_SHADER));
    }

    private void ensureChunkBuffer(int chunkCount) {
        if (chunkCount == 0) {
            return;
        }

        if (chunkCount <= chunkBufferCapacity && chunkBuffer != 0) {
            return;
        }

        if (chunkBuffer != 0) {
            GL45C.glDeleteBuffers(chunkBuffer);
        }

        chunkBufferCapacity = chunkCount;
        chunkBuffer = GL45C.glCreateBuffers();

        GL45C.glNamedBufferStorage(
                chunkBuffer,
                (long) chunkBufferCapacity * CHUNK_GPU_SIZE,
                GL45C.GL_DYNAMIC_STORAGE_BIT
        );
    }

    public void cleanup() {
        chunkCullingComputeShaderProgram.cleanup();
    }

    public void render() {
        chunkMeshBuffer.collectFreedMemory();

        if (state.getItem("shouldUpdateVisibleMeshes", Boolean.class)) {
            solidRenderedChunks.clear();
            decorationRenderedChunks.clear();
            transparentRenderedChunks.clear();

            int renderDistance = settings.getIntSetting("render_distance", 100);
            float distanceLod0 = settings.getFloatSetting("distance_lod0", 1.0f);
            float distanceLod1 = settings.getFloatSetting("distance_lod1", 1.0f);
            float distanceLod2 = settings.getFloatSetting("distance_lod2", 1.0f);
            float distanceLod3 = settings.getFloatSetting("distance_lod3", 1.0f);
            float distanceLod4 = settings.getFloatSetting("distance_lod4", 1.0f);

            renderedChunkProvider.update(settings.getIntSetting("frustum_bias", 10), renderDistance, camera);
            List<DistanceChunk> chunks = renderedChunkProvider.getOutput();

            if (!chunks.isEmpty()) {
                int rdChunks = renderDistance / ConstantCommonSettings.CHUNK_SIZE + 1;
                float squaredRenderDistance = rdChunks * rdChunks;

                ensureChunkBuffer(chunks.size());
                int requiredIndirectCommands = Math.multiplyExact(chunks.size(), 3);
                if (requiredIndirectCommands > chunkIndirectBuffer.capacity()) {
                    throw new IllegalStateException(
                            "Chunk indirect buffer is too small: required=" +
                                    requiredIndirectCommands +
                                    ", capacity=" +
                                    chunkIndirectBuffer.capacity()
                    );
                }

                ByteBuffer chunkData = MemoryUtil.memAlloc(chunks.size() * CHUNK_GPU_SIZE);

                try {
                    for (DistanceChunk chunk : chunks) {
                        int lod;

                        float d = chunk.distance() / squaredRenderDistance;

                        if (d < distanceLod0) {
                            lod = 0;
                        } else if (d < distanceLod1) {
                            lod = 1;
                        } else if (d < distanceLod2) {
                            lod = 2;
                        } else if (d < distanceLod3) {
                            lod = 3;
                        } else if (d < distanceLod4) {
                            lod = 4;
                        } else {
                            lod = 5;
                        }

                        Position3D position = chunk.pos();

                        var clientChunk = world.get(position, true, false, lod);

                        chunkData.putInt(position.x());
                        chunkData.putInt(position.y());
                        chunkData.putInt(position.z());
                        chunkData.putInt(0);

                        if (clientChunk == null || clientChunk.getMesh() == null) {
                            for (int i = 0; i < 12; i++) {
                                chunkData.putInt(0);
                            }
                        } else {
                            RenderMesh solidMesh = clientChunk.getMesh().solid();
                            RenderMesh transparentMesh = clientChunk.getMesh().transparent();
                            RenderMesh decorationMesh = clientChunk.getMesh().decoration();

                            chunkData.putInt(clientChunk.getMesh().lod());

                            putMesh(chunkData, solidMesh);
                            putMesh(chunkData, transparentMesh);
                            putMesh(chunkData, decorationMesh);
                            chunkData.putInt(0);
                            chunkData.putInt(0);
                        }

                    }

                    chunkData.flip();

                    GL45C.glNamedBufferSubData(
                            chunkBuffer,
                            0,
                            chunkData
                    );
                } finally {
                    MemoryUtil.memFree(chunkData);
                }

                indirectDrawCount = chunks.size();

                chunkCullingComputeShaderProgram.bind();

                chunkCullingComputeShaderProgram.setUniformUnsigned("chunkCount", chunks.size());

                GL43C.glBindBufferBase(
                        GL43C.GL_SHADER_STORAGE_BUFFER,
                        0,
                        chunkBuffer
                );

                GL43C.glBindBufferBase(
                        GL43C.GL_SHADER_STORAGE_BUFFER,
                        1,
                        chunkIndirectBuffer.buffer()
                );

                int workGroups = (chunks.size() + 63) / 64;

                GL43C.glDispatchCompute(
                        workGroups,
                        1,
                        1
                );

                GL43C.glMemoryBarrier(
                        GL43C.GL_COMMAND_BARRIER_BIT |
                                GL43C.GL_SHADER_STORAGE_BARRIER_BIT
                );

                transparentRenderedChunks.sort(Comparator.comparingInt(DistanceChunk::distance));

                OpenGLChecks.checkError("indirect");
            }
        }

        state.setItem("total_rendered_chunks", indirectDrawCount);
        state.setItem("shouldUpdateVisibleMeshes", false);
        ShaderProgram shaderProgram = shaderProgramHandler.getShaderProgram("default");
        shaderProgram.bind();
        shaderProgram.setUniformUnsigned("meshType", 0);

        GL11C.glBindTexture(
                GL11C.GL_TEXTURE_2D,
                texture
        );

        GL45C.glBindVertexArray(
                chunkMeshBuffer.vao()
        );

        GL43C.glBindBufferBase(
                GL43C.GL_SHADER_STORAGE_BUFFER,
                0,
                chunkBuffer
        );

        GL45C.glBindBuffer(
                GL43C.GL_DRAW_INDIRECT_BUFFER,
                chunkIndirectBuffer.buffer()
        );

        renderIndirectMeshes(shaderProgram, 0, true, true, false);
        renderIndirectMeshes(shaderProgram, 2, false, true, false);
        renderIndirectMeshes(shaderProgram, 1, false, false, true);

        chunkMeshBuffer.endFrame();

        state.setItem("indirect_buffer_size", chunkMeshBuffer.usedVertexBytes() + chunkMeshBuffer.remainingVertexBytes());
        state.setItem("indirect_buffer_used_percentage", (double) chunkMeshBuffer.usedVertexBytes() / (chunkMeshBuffer.usedVertexBytes() + chunkMeshBuffer.remainingVertexBytes()) * 100.0);

        OpenGLChecks.checkError("chunks");
    }

    private static void putMesh(ByteBuffer chunkData, RenderMesh mesh) {
        chunkData.putInt(mesh.indexCount());
        chunkData.putInt(mesh.firstIndex());
        chunkData.putInt(mesh.baseVertex());
    }

    private void renderIndirectMeshes(
            ShaderProgram shaderProgram,
            int meshType,
            boolean cullFace,
            boolean depthMask,
            boolean blend
    ) {
        if (indirectDrawCount == 0) {
            return;
        }

        // All chunk categories use the chunk shader path; meshType selects
        // unrelated entity and sky shader paths.
        shaderProgram.setUniformUnsigned("meshType", 0);

        GL11C.glEnable(GL11C.GL_DEPTH_TEST);
        GL11C.glDepthFunc(GL11C.GL_LEQUAL);

        if (cullFace) {
            GL11C.glEnable(GL11C.GL_CULL_FACE);
            GL11C.glCullFace(GL11C.GL_BACK);
        } else {
            GL11C.glDisable(GL11C.GL_CULL_FACE);
        }

        GL11C.glDepthMask(depthMask);

        if (blend) {
            GL11C.glEnable(GL11C.GL_BLEND);
            GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            GL11C.glDisable(GL11C.GL_BLEND);
        }

        long offset = (long) meshType * indirectDrawCount * INDIRECT_COMMAND_SIZE;
        GL45C.glMultiDrawElementsIndirect(
                GL11C.GL_TRIANGLES,
                GL11C.GL_UNSIGNED_INT,
                offset,
                indirectDrawCount,
                0
        );
    }
}