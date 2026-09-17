package omnivoxel.client.game.graphics.api.opengl.mesh.util;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class ChunkMeshBuffer {
    private static final int VERTEX_STRIDE =
            3 * Integer.BYTES;

    private final ChunkIndirectMemoryManager memoryManager =
            new ChunkIndirectMemoryManager();

    /*
     * Allocations that are no longer referenced by the CPU-side
     * chunk mesh, but may still be referenced by commands already
     * submitted to the GPU.
     */
    private final List<ChunkMeshAllocation> pendingFrees =
            new ArrayList<>();

    /*
     * Groups allocations behind a GPU fence.
     */
    private final ArrayDeque<RetiredFrame> retiredFrames =
            new ArrayDeque<>();

    private int vertexBuffer;
    private int indexBuffer;
    private int vao;

    private long vertexCapacity;
    private long indexCapacity;

    public int vao() {
        return vao;
    }

    public int vertexBuffer() {
        return vertexBuffer;
    }

    public int indexBuffer() {
        return indexBuffer;
    }

    public long vertexCapacity() {
        return vertexCapacity;
    }

    public long indexCapacity() {
        return indexCapacity;
    }

    public long remainingVertexBytes() {
        return memoryManager.remainingVertexBytes();
    }

    public long remainingIndexBytes() {
        return memoryManager.remainingIndexBytes();
    }

    public void init(
            long vertexCapacity,
            long indexCapacity
    ) {
        if (vertexCapacity <= 0 ||
                indexCapacity <= 0) {

            throw new IllegalArgumentException(
                    "Chunk mesh buffer capacities must be greater than zero"
            );
        }

        this.vertexCapacity = vertexCapacity;
        this.indexCapacity = indexCapacity;

        memoryManager.init(
                vertexCapacity,
                indexCapacity
        );

        vertexBuffer = GL45C.glCreateBuffers();
        indexBuffer = GL45C.glCreateBuffers();
        vao = GL45C.glCreateVertexArrays();

        GL45C.glNamedBufferStorage(
                vertexBuffer,
                vertexCapacity,
                GL45C.GL_DYNAMIC_STORAGE_BIT
        );

        GL45C.glNamedBufferStorage(
                indexBuffer,
                indexCapacity,
                GL45C.GL_DYNAMIC_STORAGE_BIT
        );

        /*
         * Vertex format:
         *
         * uint data1;
         * uint data2;
         * uint data3;
         */

        GL45C.glVertexArrayVertexBuffer(
                vao,
                0,
                vertexBuffer,
                0,
                VERTEX_STRIDE
        );

        GL45C.glVertexArrayElementBuffer(
                vao,
                indexBuffer
        );

        /*
         * location = 0
         * uint data1
         */
        GL45C.glEnableVertexArrayAttrib(
                vao,
                0
        );

        GL45C.glVertexArrayAttribIFormat(
                vao,
                0,
                1,
                GL11C.GL_UNSIGNED_INT,
                0
        );

        GL45C.glVertexArrayAttribBinding(
                vao,
                0,
                0
        );

        /*
         * location = 1
         * uint data2
         */
        GL45C.glEnableVertexArrayAttrib(
                vao,
                1
        );

        GL45C.glVertexArrayAttribIFormat(
                vao,
                1,
                1,
                GL11C.GL_UNSIGNED_INT,
                Integer.BYTES
        );

        GL45C.glVertexArrayAttribBinding(
                vao,
                1,
                0
        );

        /*
         * location = 2
         * uint data3
         */
        GL45C.glEnableVertexArrayAttrib(
                vao,
                2
        );

        GL45C.glVertexArrayAttribIFormat(
                vao,
                2,
                1,
                GL11C.GL_UNSIGNED_INT,
                2 * Integer.BYTES
        );

        GL45C.glVertexArrayAttribBinding(
                vao,
                2,
                0
        );
    }

    public synchronized ChunkMeshAllocation allocate(
            int vertexSize,
            int indexSize
    ) {
        return memoryManager.allocate(
                vertexSize,
                indexSize
        );
    }

    public void upload(
            ChunkMeshAllocation allocation,
            ByteBuffer vertices,
            ByteBuffer indices
    ) {
        if (allocation == null) {
            throw new IllegalArgumentException(
                    "Allocation cannot be null"
            );
        }

        if (vertices.remaining() !=
                allocation.vertexSize()) {

            throw new IllegalArgumentException(
                    "Vertex data size does not match allocation: expected=" +
                            allocation.vertexSize() +
                            ", actual=" +
                            vertices.remaining()
            );
        }

        if (indices.remaining() !=
                allocation.indexSize()) {

            throw new IllegalArgumentException(
                    "Index data size does not match allocation: expected=" +
                            allocation.indexSize() +
                            ", actual=" +
                            indices.remaining()
            );
        }

        GL45C.glNamedBufferSubData(
                vertexBuffer,
                allocation.vertexOffset(),
                vertices
        );

        GL45C.glNamedBufferSubData(
                indexBuffer,
                allocation.indexOffset(),
                indices
        );
    }

    /*
     * Marks an allocation as no longer needed.
     *
     * The allocation is NOT immediately returned to the allocator.
     * It remains valid until the GPU has completed the frame that
     * could still reference it.
     */
    public synchronized void freeLater(
            ChunkMeshAllocation allocation
    ) {
        if (allocation == null) {
            return;
        }

        if (allocation.vertexSize() <= 0 &&
                allocation.indexSize() <= 0) {

            return;
        }

        pendingFrees.add(allocation);
    }

    public long usedVertexBytes() {
        return memoryManager.usedVertexBytes();
    }

    public long usedIndexBytes() {
        return memoryManager.usedIndexBytes();
    }

    public int pendingFreeCount() {
        synchronized (this) {
            return pendingFrees.size();
        }
    }

    public int retiredFrameCount() {
        synchronized (this) {
            return retiredFrames.size();
        }
    }

    public long retiredAllocationCount() {
        synchronized (this) {
            long count = 0;

            for (RetiredFrame frame : retiredFrames) {
                count += frame.allocations().size();
            }

            return count;
        }
    }

    /*
     * Called after all rendering commands for the frame have been
     * submitted.
     *
     * The fence represents completion of all GPU commands submitted
     * before this point.
     */
    public synchronized void endFrame() {
        if (pendingFrees.isEmpty()) {
            return;
        }

        long fence = GL32C.glFenceSync(
                GL32C.GL_SYNC_GPU_COMMANDS_COMPLETE,
                0
        );

        if (fence == 0L) {
            throw new IllegalStateException(
                    "Failed to create GPU synchronization fence"
            );
        }

        List<ChunkMeshAllocation> allocations =
                new ArrayList<>(pendingFrees);

        pendingFrees.clear();

        retiredFrames.addLast(
                new RetiredFrame(
                        fence,
                        allocations
                )
        );
    }

    /*
     * Checks previously submitted GPU fences.
     *
     * Allocations are returned to the allocator only after the GPU
     * has finished every command protected by their fence.
     *
     * This method never blocks.
     */
    public synchronized void collectFreedMemory() {
        while (!retiredFrames.isEmpty()) {

            RetiredFrame retiredFrame =
                    retiredFrames.peekFirst();

            int result = GL32C.glClientWaitSync(
                    retiredFrame.fence(),
                    0,
                    0L
            );

            if (result == GL32C.GL_TIMEOUT_EXPIRED) {
                break;
            }

            if (result == GL32C.GL_WAIT_FAILED) {
                throw new IllegalStateException(
                        "glClientWaitSync failed"
                );
            }

            GL32C.glDeleteSync(
                    retiredFrame.fence()
            );

            for (ChunkMeshAllocation allocation
                    : retiredFrame.allocations()) {

                memoryManager.free(allocation);
            }

            retiredFrames.removeFirst();
        }
    }

    /*
     * Resets the allocator.
     *
     * WARNING:
     * This invalidates every existing ChunkMeshAllocation.
     *
     * Do not call while chunks are still rendered.
     */
    public synchronized void reset() {
        for (RetiredFrame retiredFrame : retiredFrames) {
            GL32C.glDeleteSync(
                    retiredFrame.fence()
            );
        }

        retiredFrames.clear();
        pendingFrees.clear();

        memoryManager.reset();
    }

    public synchronized void cleanup() {
        /*
         * Release allocations waiting behind fences.
         */
        for (RetiredFrame retiredFrame : retiredFrames) {
            GL32C.glDeleteSync(
                    retiredFrame.fence()
            );
        }

        retiredFrames.clear();
        pendingFrees.clear();

        if (vao != 0) {
            GL45C.glDeleteVertexArrays(vao);
            vao = 0;
        }

        if (vertexBuffer != 0) {
            GL45C.glDeleteBuffers(vertexBuffer);
            vertexBuffer = 0;
        }

        if (indexBuffer != 0) {
            GL45C.glDeleteBuffers(indexBuffer);
            indexBuffer = 0;
        }

        memoryManager.reset();

        vertexCapacity = 0;
        indexCapacity = 0;
    }

    private record RetiredFrame(
            long fence,
            List<ChunkMeshAllocation> allocations
    ) {
    }
}