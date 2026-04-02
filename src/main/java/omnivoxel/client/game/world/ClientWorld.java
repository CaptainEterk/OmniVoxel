package omnivoxel.client.game.world;

import omnivoxel.client.game.entity.ClientEntity;
import omnivoxel.client.game.graphics.api.opengl.mesh.chunk.ChunkMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.definition.EntityMeshDataDefinition;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.ChunkMeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.GeneralEntityMeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.util.MeshGenerator;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.game.state.State;
import omnivoxel.client.network.Client;
import omnivoxel.client.network.request.ChunkRequest;
import omnivoxel.server.ConstantServerSettings;
import omnivoxel.util.cache.IDCache;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.ChunkShell;
import omnivoxel.world.chunk2d.Chunk2D;
import org.lwjgl.opengl.GL30C;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class ClientWorld {
    private final Set<Position3D> queuedChunks;
    private final Queue<MeshData> nonBufferizedChunks;
    private final Set<Position3D> newChunks;
    private final State state;
    private final Map<Position3D, ClientWorldChunk> chunks;
    private final Map<String, ClientEntity> entities;
    private final IDCache<String, EntityMeshDataDefinition> entityMeshDefinitionCache;
    private final Set<String> queuedEntityMeshData;
    private final AtomicBoolean chunksChanged = new AtomicBoolean(true);
    private final Set<Position3D> chunkRequests;
    private final Map<Position2D, Chunk2D<Integer>> skylights;
    private Position3D[] cachedKeys = null;
    private Client client;
    private boolean requesting = true;
    private int tick = 0;

    public ClientWorld(State state) {
        this.state = state;
        queuedChunks = ConcurrentHashMap.newKeySet();
        nonBufferizedChunks = new ConcurrentLinkedDeque<>();
        this.chunks = new ConcurrentHashMap<>();
        this.entityMeshDefinitionCache = new IDCache<>();

        newChunks = ConcurrentHashMap.newKeySet();
        entities = new ConcurrentHashMap<>();
        queuedEntityMeshData = ConcurrentHashMap.newKeySet();
        chunkRequests = ConcurrentHashMap.newKeySet();
        skylights = new ConcurrentHashMap<>();
    }

    public Chunk2D<Integer> getSkylightChunk(Position2D position2D) {
        return skylights.get(position2D);
    }

    public void setSkylightChunk(Position2D position2D, Chunk2D<Integer> chunk2D) {
        this.skylights.put(position2D, chunk2D);
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public int size() {
        return chunks.size();
    }

    public ClientWorldChunk get(Position3D position3D, boolean request, boolean shell) {
        ClientWorldChunk clientWorldChunk = chunks.get(position3D);
        if (clientWorldChunk != null) {
            if (!shell) {
                clientWorldChunk.touch(tick);
            }
            boolean isShell = clientWorldChunk.getChunkData() instanceof ChunkShell<BlockWithMesh>;
            boolean expired = tick - clientWorldChunk.getLastFetchedTick() > ConstantGameSettings.CHUNK_TICK_TIMEOUT;

            if (shell || (!isShell && !expired)) {
                return clientWorldChunk;
            }
        }
        if (requesting && request) {
            if (chunkRequests.size() < ConstantServerSettings.INFLIGHT_REQUESTS_MAXIMUM && chunkRequests.add(position3D)) {
                client.sendRequest(new ChunkRequest(position3D));
            }
        } else {
            requesting = false;

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

    public int bufferizeQueued(MeshGenerator meshGenerator, long endTime) {
        int count = 0;
        boolean bufferizing;
        do {
            bufferizing = bufferize(meshGenerator);
            if (bufferizing) {
                count++;
            }
        } while (bufferizing && count < ConstantGameSettings.BUFFERIZE_CHUNKS_PER_FRAME && System.nanoTime() < endTime);
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
                ClientWorldChunk clientWorldChunk = chunks.putIfAbsent(chunkMeshData.chunkPosition(), new ClientWorldChunk(chunkMesh));
                if (clientWorldChunk == null) {
                    chunksChanged.set(true);
                } else {
                    if (clientWorldChunk.getMesh() != null) {
                        freeChunk(clientWorldChunk.getMesh());
                    }
                    clientWorldChunk.setMesh(chunkMesh);
                }
                chunkRequests.remove(chunkMeshData.chunkPosition());
            }
            return true;
        }
        return false;
    }

    public void add(Position3D position3D, MeshData meshData) {
        if (meshData == null) {
            return;
        }
        ClientWorldChunk clientWorldChunk = chunks.putIfAbsent(position3D, new ClientWorldChunk(meshData));
        if (clientWorldChunk == null) {
            chunksChanged.set(true);
        } else {
            clientWorldChunk.setMeshData(meshData);
        }
        nonBufferizedChunks.add(meshData);
        newChunks.add(position3D);
        state.setItem("shouldCheckNewChunks", true);
    }

    public void addChunkData(Position3D position3D, Chunk<BlockWithMesh> chunk, boolean shell) {
        ClientWorldChunk existing = chunks.putIfAbsent(position3D, new ClientWorldChunk(chunk));

        if (existing == null) {
            if (!shell) {
                chunksChanged.set(true);
            }
            return;
        }

        Chunk<BlockWithMesh> existingData = existing.getChunkData();

        if (!shell) {
            existing.setChunkData(chunk);
            return;
        }

        if (existingData instanceof ChunkShell<BlockWithMesh> existingShell &&
                chunk instanceof ChunkShell<BlockWithMesh> newShell) {

            existingShell.merge(newShell);
        }
    }

    public void cleanup() {
        for (Position3D position : getKeys()) {
            freeChunk(chunks.get(position).getMesh());
        }
        chunks.clear();
    }

    public void tick() {
        requesting = true;
        tick++;
    }

    public void freeAllChunksNotInAndNotRecentlyAccessed(Predicate<Position3D> predicate) {
        Position3D[] positions = getKeys();
        boolean changed = false;

        for (Position3D pos : positions) {

            ClientWorldChunk chunk = chunks.get(pos);
            if (chunk == null) continue;

            // Only consider chunks OUTSIDE the predicate
            if (predicate.test(pos)) continue;

            // Skip chunks still queued for mesh generation
            if (queuedChunks.contains(pos)) continue;

            // Skip if recently accessed
            if (tick - chunk.getLastFetchedTick() < ConstantGameSettings.CHUNK_TICK_TIMEOUT) continue;

            // Skip if any neighbor was recently accessed
            if (neighborRecentlyFetched(pos)) continue;

            freeChunk(chunk.getMesh());
            chunks.remove(pos);
            chunkRequests.remove(pos);

            changed = true;
        }

        if (changed) {
            chunksChanged.set(true);
        }
    }

    private boolean neighborRecentlyFetched(Position3D pos) {
        return recentlyFetched(pos.add(-1, 0, 0)) ||
                recentlyFetched(pos.add(1, 0, 0)) ||
                recentlyFetched(pos.add(0, -1, 0)) ||
                recentlyFetched(pos.add(0, 1, 0)) ||
                recentlyFetched(pos.add(0, 0, -1)) ||
                recentlyFetched(pos.add(0, 0, 1));
    }

    private boolean recentlyFetched(Position3D pos) {
        ClientWorldChunk neighbor = chunks.get(pos);
        return neighbor != null && tick - neighbor.getLastFetchedTick() < (long) ConstantGameSettings.CHUNK_TICK_TIMEOUT;
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

    public int chunkRequestCount() {
        return chunkRequests.size();
    }
}