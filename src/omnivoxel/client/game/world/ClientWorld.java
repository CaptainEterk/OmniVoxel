package omnivoxel.client.game.world;

import omnivoxel.client.game.entity.ClientEntity;
import omnivoxel.client.game.graphics.opengl.mesh.chunk.ChunkMesh;
import omnivoxel.client.game.graphics.opengl.mesh.definition.EntityMeshDataDefinition;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.ChunkMeshData;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.GeneralEntityMeshData;
import omnivoxel.client.game.graphics.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.opengl.mesh.util.MeshGenerator;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.game.state.State;
import omnivoxel.client.network.Client;
import omnivoxel.client.network.request.ChunkRequest;
import omnivoxel.server.ConstantServerSettings;
import omnivoxel.util.cache.IDCache;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.block.Block;
import omnivoxel.world.chunk.Chunk;
import org.lwjgl.opengl.GL30C;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class ClientWorld {
    private final Set<Position3D> queuedChunks;
    private final AtomicInteger chunkRequestsSent;
    private final AtomicInteger chunkResponseGotten;
    private final Queue<MeshData> nonBufferizedChunks;
    private final Set<Position3D> newChunks;
    private final State state;
    private final Map<Position3D, ClientWorldChunk> chunks;
    private final Map<String, ClientEntity> entities;
    private final IDCache<String, EntityMeshDataDefinition> entityMeshDefinitionCache;
    private final Set<String> queuedEntityMeshData;
    private final AtomicBoolean chunksChanged = new AtomicBoolean(true);
    private Position3D[] cachedKeys = null;
    private Client client;
    private boolean requesting = true;

    public ClientWorld(State state) {
        this.state = state;
        queuedChunks = ConcurrentHashMap.newKeySet();
        nonBufferizedChunks = new ConcurrentLinkedDeque<>();
        this.chunks = new ConcurrentHashMap<>();
        this.entityMeshDefinitionCache = new IDCache<>();

        chunkRequestsSent = new AtomicInteger();
        chunkResponseGotten = new AtomicInteger();
        newChunks = ConcurrentHashMap.newKeySet();
        entities = new ConcurrentHashMap<>();
        queuedEntityMeshData = ConcurrentHashMap.newKeySet();
    }

    public int inflightRequests() {
        return chunkRequestsSent.get() - chunkResponseGotten.get();
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public int size() {
        return chunks.size();
    }

    public ClientWorldChunk get(Position3D position3D, boolean request) {
        ClientWorldChunk clientWorldChunk = chunks.get(position3D);
        if (clientWorldChunk != null) {
            return clientWorldChunk;
        } else if (requesting && request) {
            int inflightRequests = inflightRequests();
            if (inflightRequests < ConstantServerSettings.CHUNK_REQUEST_LIMIT) {
                client.sendRequest(new ChunkRequest(position3D));
                chunkRequestsSent.incrementAndGet();
            } else {
                requesting = false;
            }
        }
        return null;
    }

    public Position3D[] getKeys() {
        if (chunksChanged.get()) {
            chunksChanged.set(false);
            cachedKeys = chunks.keySet().toArray(new Position3D[0]);
        }
        return cachedKeys;
    }

    public ClientWorldChunk[] getValues() {
        return chunks.values().toArray(new ClientWorldChunk[0]);
    }

    public int bufferizeQueued(MeshGenerator meshGenerator, long endTime) {
        int count = 0;
        boolean bufferizing;
        do {
            bufferizing = bufferize(meshGenerator);
            if (bufferizing) {
                count++;
            }
        } while (bufferizing && count < ConstantGameSettings.BUFFERIZE_CHUNKS_PER_FRAME);
        state.setItem("bufferizing_queue_size", nonBufferizedChunks.size());
        return count;
    }

    public boolean bufferize(MeshGenerator meshGenerator) {
        if (!nonBufferizedChunks.isEmpty()) {
            MeshData meshData = nonBufferizedChunks.poll();
            if (meshData instanceof GeneralEntityMeshData entityMeshData) {
                entityMeshData.entity().setMesh(meshGenerator.bufferizeEntityMesh(entityMeshData));
                entityMeshDefinitionCache.put(entityMeshData.entity().getType().toString(), entityMeshData.entity().getMesh().getDefinition());
            } else if (meshData instanceof ChunkMeshData chunkMeshData) {
                ChunkMesh chunkMesh = meshGenerator.bufferizeChunkMesh(chunkMeshData);
                ClientWorldChunk clientWorldChunk = chunks.get(chunkMeshData.chunkPosition());
                if (clientWorldChunk == null) {
                    chunks.put(chunkMeshData.chunkPosition(), new ClientWorldChunk(chunkMesh));
                    chunksChanged.set(true);
                } else {
                    if (clientWorldChunk.getMesh() != null) {
                        freeChunk(clientWorldChunk.getMesh());
                    }
                    clientWorldChunk.setMesh(chunkMesh);
                }
                queuedChunks.remove(chunkMeshData.chunkPosition());
            }
            return true;
        }
        return false;
    }

    public void add(Position3D position3D, MeshData meshData) {
        ClientWorldChunk clientWorldChunk = chunks.get(position3D);
        if (clientWorldChunk == null) {
            chunks.put(position3D, new ClientWorldChunk(meshData));
            chunksChanged.set(true);
        } else {
            clientWorldChunk.setMeshData(meshData);
        }
        nonBufferizedChunks.add(meshData);
        chunkResponseGotten.incrementAndGet();
        newChunks.add(position3D);
        state.setItem("shouldCheckNewChunks", true);
    }

    public void addChunkData(Position3D position3D, Chunk<Block> chunk) {
        ClientWorldChunk clientWorldChunk = chunks.get(position3D);
        if (clientWorldChunk == null) {
            chunks.put(position3D, new ClientWorldChunk(chunk));
            chunksChanged.set(true);
        } else {
            clientWorldChunk.setChunkData(chunk);
        }
    }

    public void cleanup() {
        for (Position3D position : getKeys()) {
            freeChunk(chunks.get(position).getMesh());
        }
        chunks.clear();
    }

    public void tick() {
        int crs = chunkRequestsSent.get();
        int crg = chunkResponseGotten.get();
        int inflightRequests = crs - crg;
        state.setItem("inflight_requests", inflightRequests);
        state.setItem("chunk_requests_sent", crs);
        state.setItem("chunk_requests_received", crg);
        requesting = true;
    }

    public void freeAllChunksNotIn(Predicate<Position3D> predicate) {
        Position3D[] positions = getKeys();
        for (Position3D position : positions) {
            if (!predicate.test(position)) {
                freeChunk(chunks.remove(position).getMesh());
                chunksChanged.set(true);
            }
        }
    }

    private void freeChunk(ChunkMesh mesh) {
        if (mesh != null) {
            GL30C.glDeleteVertexArrays(mesh.solidVAO());
            GL30C.glDeleteBuffers(mesh.solidVBO());
            GL30C.glDeleteBuffers(mesh.solidEBO());

            GL30C.glDeleteVertexArrays(mesh.transparentVAO());
            GL30C.glDeleteBuffers(mesh.transparentVBO());
            GL30C.glDeleteBuffers(mesh.transparentEBO());

            mesh.meshData().cleanup();
        }
    }

    public void addEntity(ClientEntity entity) {
        entities.put(entity.getUUID(), entity);
        nonBufferizedChunks.add(entity.getMeshData());
    }

    public Map<String, ClientEntity> getEntities() {
        return entities;
    }

    public IDCache<String, EntityMeshDataDefinition> getEntityMeshDefinitionCache() {
        return this.entityMeshDefinitionCache;
    }

    public Set<String> getQueuedEntityMeshData() {
        return this.queuedEntityMeshData;
    }

    public void removeEntity(String entityID) {
        entities.remove(entityID);
    }
}