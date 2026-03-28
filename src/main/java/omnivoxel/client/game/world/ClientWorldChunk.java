package omnivoxel.client.game.world;

import omnivoxel.client.game.graphics.api.opengl.mesh.chunk.ChunkMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.generators.lighting.ChunkMeshDataLightingGenerator;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.light.ChunkLightingData;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.Block;
import omnivoxel.world.chunk.Chunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientWorldChunk {
    // TODO: Move to ChunkLightingData?
    private final Map<ChunkMeshDataLightingGenerator.Direction, Map<Position3D, ChunkMeshDataLightingGenerator.LightNode>> neighborLightOverflowQueue;
    private MeshData meshData;
    private ChunkMesh mesh;
    private Chunk<Block> chunkData;
    private int lastFetched;
    private ChunkLightingData chunkLightingData;

    private ClientWorldChunk(MeshData meshData, ChunkMesh mesh, Chunk<Block> chunkData, ChunkLightingData chunkLightingData) {
        this.meshData = meshData;
        this.mesh = mesh;
        this.chunkData = chunkData;
        this.chunkLightingData = chunkLightingData;
        this.neighborLightOverflowQueue = new ConcurrentHashMap<>();
        for (ChunkMeshDataLightingGenerator.Direction value : ChunkMeshDataLightingGenerator.Direction.values()) {
            neighborLightOverflowQueue.put(value, new ConcurrentHashMap<>());
        }
    }

    public ClientWorldChunk(MeshData meshData) {
        this(meshData, null, null, null);
    }

    public ClientWorldChunk(ChunkMesh mesh) {
        this(null, mesh, null, null);
    }

    public ClientWorldChunk(Chunk<Block> chunkData) {
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

    public Chunk<Block> getChunkData() {
        return chunkData;
    }

    public void setChunkData(Chunk<Block> chunkData) {
        this.chunkData = chunkData;
    }

    public void touch(int tick) {
        this.lastFetched = tick;
    }

    public int getLastFetchedTick() {
        return lastFetched;
    }

    public Map<Position3D, ChunkMeshDataLightingGenerator.LightNode> getNeighborLightOverflowQueue(ChunkMeshDataLightingGenerator.Direction direction) {
        return neighborLightOverflowQueue.get(direction);
    }

    public ChunkLightingData getLightingData() {
        return chunkLightingData;
    }

    public void setChunkLightingData(ChunkLightingData chunkLightingData) {
        this.chunkLightingData = chunkLightingData;
    }
}