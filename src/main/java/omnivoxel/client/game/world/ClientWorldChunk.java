package omnivoxel.client.game.world;

import omnivoxel.client.game.graphics.api.opengl.mesh.chunk.ChunkMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting.ChunkMeshDataLightingGenerator;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.light.ChunkLightingData;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.world.chunk.Chunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientWorldChunk {
    // TODO: Move to ChunkLightingData?
    private final Map<LightChannels, Map<ChunkMeshDataLightingGenerator.Direction, Map<Integer, Byte>>> neighborLightOverflowMap;
    private MeshData meshData;
    private ChunkMesh mesh;
    private Chunk<BlockWithMesh> chunkData;
    private int lastFetched;
    private ChunkLightingData chunkLightingData;

    private ClientWorldChunk(MeshData meshData, ChunkMesh mesh, Chunk<BlockWithMesh> chunkData, ChunkLightingData chunkLightingData) {
        this.meshData = meshData;
        this.mesh = mesh;
        this.chunkData = chunkData;
        this.chunkLightingData = chunkLightingData;
        this.neighborLightOverflowMap = new ConcurrentHashMap<>();
        for (LightChannels channel : LightChannels.values()) {
            neighborLightOverflowMap.put(channel, new ConcurrentHashMap<>());
            for (ChunkMeshDataLightingGenerator.Direction direction : ChunkMeshDataLightingGenerator.Direction.values()) {
                neighborLightOverflowMap.get(channel).put(direction, new ConcurrentHashMap<>());
            }
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

    public ClientWorldChunk(ChunkLightingData chunkLightingData) {
        this(null, null, null, chunkLightingData);
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

    public Map<Integer, Byte> getNeighborLightOverflowMap(LightChannels channel, ChunkMeshDataLightingGenerator.Direction direction) {
        return neighborLightOverflowMap.get(channel).get(direction);
    }

    public ChunkLightingData getLightingData() {
        return chunkLightingData;
    }

    public void setChunkLightingData(ChunkLightingData chunkLightingData) {
        this.chunkLightingData = chunkLightingData;
    }
}