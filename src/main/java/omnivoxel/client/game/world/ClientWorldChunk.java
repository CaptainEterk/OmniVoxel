package omnivoxel.client.game.world;

import omnivoxel.client.game.graphics.api.opengl.mesh.chunk.ChunkMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.light.ChunkLightingData;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.util.data.Direction;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.ChunkLODSampler;
import omnivoxel.world.chunk.ChunkShell;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ClientWorldChunk {
    private static final short[] EMPTY_OVERFLOW = new short[0];
    // TODO: Move to ChunkLightingData?
    private final short[][] neighborLightOverflow;
    private final AtomicBoolean[] cleanLighting;
    private final AtomicReference<Chunk<BlockWithMesh>> chunkData;
    private MeshData meshData;
    private ChunkMesh mesh;
    private int lastFetched;
    private ChunkLightingData chunkLightingData;

    private ClientWorldChunk(MeshData meshData, ChunkMesh mesh, Chunk<BlockWithMesh> chunkData, ChunkLightingData chunkLightingData) {
        this.meshData = meshData;
        this.mesh = mesh;
        this.chunkData = new AtomicReference<>(chunkData);
        this.chunkLightingData = chunkLightingData;
        this.neighborLightOverflow = new short[Direction.VALUES.length * LightChannels.values().length][];
        for (int i = 0; i < neighborLightOverflow.length; i++) {
            this.neighborLightOverflow[i] = EMPTY_OVERFLOW;
        }
        this.cleanLighting = new AtomicBoolean[LightChannels.values().length];
        for (int i = 0; i < cleanLighting.length; i++) {
            cleanLighting[i] = new AtomicBoolean(false);
        }
    }

    public ClientWorldChunk(LightChannels channel, Direction direction, short[] overflowLighting) {
        this(null, null, null, null);
        setNeighborLightOverflow(channel, direction, overflowLighting);
    }

    public ClientWorldChunk(MeshData meshData) {
        this(meshData, null, null, null);
    }

    public ClientWorldChunk(ChunkMesh mesh) {
        this(null, mesh, null, null);
    }

    public ClientWorldChunk(Chunk<BlockWithMesh> chunkData) {
        this(null, null, chunkData, null);
    }

    private static int getOverflowIndex(LightChannels channel, Direction direction) {
        return channel.ordinal() * Direction.VALUES.length + direction.ordinal();
    }

    public MeshData getMeshData() {
        return meshData;
    }

    public void setMeshData(MeshData meshData) {
        this.meshData = meshData;
    }

    public ChunkMesh getMesh() {
        return mesh;
    }

    public void setMesh(ChunkMesh mesh) {
        this.mesh = mesh;
    }

    public Chunk<BlockWithMesh> getChunkData(int lod) {
        return ChunkLODSampler.sample(chunkData.get(), lod);
    }

    public void setChunkData(Chunk<BlockWithMesh> chunkData) {
        while (true) {
            Chunk<BlockWithMesh> chunk = this.chunkData.get();
            Chunk<BlockWithMesh> nextChunk = chunkData;

            if (chunkData instanceof ChunkShell<BlockWithMesh> newChunkData) {
                if (chunk instanceof ChunkShell<BlockWithMesh> chunkShell) {
                    nextChunk = chunkShell.merge(newChunkData);
                } else {
                    return;
                }
            }

            if (nextChunk == chunk || this.chunkData.compareAndSet(chunk, nextChunk)) {
                break;
            }
        }
    }

    public void touch(int tick) {
        this.lastFetched = tick;
    }

    public int getLastFetchedTick() {
        return lastFetched;
    }

    public short[] getNeighborLightOverflow(LightChannels channel, Direction direction) {
        return neighborLightOverflow[getOverflowIndex(channel, direction)];
    }

    public void setNeighborLightOverflow(LightChannels channel, Direction direction, short[] overflow) {
        neighborLightOverflow[getOverflowIndex(channel, direction)] = overflow;
    }

    public ChunkLightingData getLightingData() {
        return chunkLightingData;
    }

    public void setChunkLightingData(ChunkLightingData chunkLightingData) {
        this.chunkLightingData = chunkLightingData;
    }

    public boolean isCleanLighting() {
        for (AtomicBoolean atomicBoolean : cleanLighting) {
            if (!atomicBoolean.get()) {
                return false;
            }
        }
        return true;
    }

    public void setCleanLighting(boolean cleanLighting) {
        for (AtomicBoolean atomicBoolean : this.cleanLighting) {
            atomicBoolean.set(cleanLighting);
        }
    }

    public void setCleanLighting(LightChannels channel, boolean cleanLighting) {
        if (channel == null) {
            setCleanLighting(cleanLighting);
        } else {
            this.cleanLighting[channel.ordinal()].set(cleanLighting);
        }
    }
}