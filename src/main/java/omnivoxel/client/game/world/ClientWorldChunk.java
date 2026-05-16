package omnivoxel.client.game.world;

import omnivoxel.client.game.graphics.api.opengl.mesh.chunk.ChunkMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting.ChunkMeshDataLightingGenerator;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.light.ChunkLightingData;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.world.chunk.Chunk;

import java.util.concurrent.atomic.AtomicBoolean;

public class ClientWorldChunk {
    // TODO: Move to ChunkLightingData?
    private final short[][] neighborLightOverflow;
    private MeshData meshData;
    private ChunkMesh mesh;
    private Chunk<BlockWithMesh> chunkData;
    private int lastFetched;
    private ChunkLightingData chunkLightingData;
    private AtomicBoolean cleanLighting = new AtomicBoolean(false);

    private ClientWorldChunk(MeshData meshData, ChunkMesh mesh, Chunk<BlockWithMesh> chunkData, ChunkLightingData chunkLightingData) {
        this.meshData = meshData;
        this.mesh = mesh;
        this.chunkData = chunkData;
        this.chunkLightingData = chunkLightingData;
        this.neighborLightOverflow = new short[ChunkMeshDataLightingGenerator.Direction.VALUES.length * LightChannels.values().length][];
        for (int i = 0; i < neighborLightOverflow.length; i++) {
            this.neighborLightOverflow[i] = new short[0];
        }
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

    private static int getOverflowIndex(LightChannels channel, ChunkMeshDataLightingGenerator.Direction direction) {
        return channel.ordinal() * ChunkMeshDataLightingGenerator.Direction.VALUES.length + direction.ordinal();
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

    public Chunk<BlockWithMesh> getChunkData() {
        return chunkData;
    }

    public void setChunkData(Chunk<BlockWithMesh> chunkData) {
        this.chunkData = chunkData;
    }

    public void touch(int tick) {
        this.lastFetched = tick;
    }

    public int getLastFetchedTick() {
        return lastFetched;
    }

    public short[] getNeighborLightOverflow(LightChannels channel, ChunkMeshDataLightingGenerator.Direction direction) {
        return neighborLightOverflow[getOverflowIndex(channel, direction)];
    }

    public void setNeighborLightOverflow(LightChannels channel, ChunkMeshDataLightingGenerator.Direction direction, short[] overflow) {
        neighborLightOverflow[getOverflowIndex(channel, direction)] = overflow;
    }

    public ChunkLightingData getLightingData() {
        return chunkLightingData;
    }

    public void setChunkLightingData(ChunkLightingData chunkLightingData) {
        this.chunkLightingData = chunkLightingData;
    }

    public boolean isCleanLighting() {
        return cleanLighting.get();
    }

    public void setCleanLighting(boolean cleanLighting) {
        this.cleanLighting.set(cleanLighting);
    }
}