package omnivoxel.client.game.graphics.renderer;

public final class ChunkBuffer {

    private int chunkBuffer;
    private int chunkBufferCapacity;

    public ChunkBuffer(int chunkBuffer, int chunkBufferCapacity) {
        this.chunkBuffer = chunkBuffer;
        this.chunkBufferCapacity = chunkBufferCapacity;
    }

    public int chunkBuffer() {
        return chunkBuffer;
    }

    public void setChunkBuffer(int chunkBuffer) {
        this.chunkBuffer = chunkBuffer;
    }

    public int chunkBufferCapacity() {
        return chunkBufferCapacity;
    }

    public void setChunkBufferCapacity(int chunkBufferCapacity) {
        this.chunkBufferCapacity = chunkBufferCapacity;
    }
}