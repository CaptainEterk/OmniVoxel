package omnivoxel.client.game.world;

import omnivoxel.client.game.graphics.opengl.mesh.chunk.ChunkMesh;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.MeshData;
import omnivoxel.world.block.Block;
import omnivoxel.world.chunk.Chunk;

public class ClientWorldChunk {
    private MeshData meshData;
    private ChunkMesh mesh;
    private Chunk<Block> chunkData;

    public ClientWorldChunk(MeshData meshData) {
        this.meshData = meshData;
    }

    public ClientWorldChunk(ChunkMesh mesh) {
        this.mesh = mesh;
    }

    public ClientWorldChunk(Chunk<Block> chunkData) {
        this.chunkData = chunkData;
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
}