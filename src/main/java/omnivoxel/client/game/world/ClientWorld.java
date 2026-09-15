package omnivoxel.client.game.world;

import omnivoxel.client.game.entity.EntityMeshWrapper;
import omnivoxel.client.game.graphics.api.opengl.OpenGLChecks;
import omnivoxel.client.game.graphics.api.opengl.mesh.EntityMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.chunk.ChunkMesh;
import omnivoxel.client.game.graphics.api.opengl.mesh.definition.EntityMeshDefinition;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.ChunkMeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.EntityMeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.meshData.MeshData;
import omnivoxel.client.game.graphics.api.opengl.mesh.util.ChunkMeshBuffer;
import omnivoxel.client.game.graphics.api.opengl.mesh.util.MeshGenerator;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.client.game.state.State;
import omnivoxel.client.network.Client;
import omnivoxel.client.network.request.ChunkRequest;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.ConstantNetworkSettings;
import omnivoxel.common.settings.Settings;
import omnivoxel.server.entity.Entity;
import omnivoxel.util.data.Direction;
import omnivoxel.util.math.Position2D;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;
import omnivoxel.world.chunk.ChunkShell;
import omnivoxel.world.chunk2d.Chunk2D;
import org.lwjgl.opengl.GL30C;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

// TODO: Make this only store world data, no queues, etc...
public class ClientWorld {
    // TODO: nonBufferizedMeshDataQueue is a messy solution, fix it
    private final Queue<MeshData> nonBufferizedMeshDataQueue;
    private final Set<Position3D> newChunks;
    private final State state;
    private final Map<Position3D, ClientWorldChunk> chunks;
    private final Map<String, EntityMeshData> entitiesMeshData;
    private final Map<String, EntityMeshReference> entityMeshes;
    private final Map<String, EntityMeshWrapper> entities;
    private final AtomicBoolean chunkKeysChanged = new AtomicBoolean(true);
    private final Set<Position3D> inPipelineChunks;
    private final Set<Position3D> inflightRequests;
    private final Map<Position2D, Chunk2D<Integer>> chunkHeights;
    private final Settings settings;
    private Position3D[] cachedKeys = null;
    private Client client;
    private int tick = 0;

    public ClientWorld(State state, Settings settings) {
        this.state = state;
        this.settings = settings;
        nonBufferizedMeshDataQueue = new ConcurrentLinkedDeque<>();
        this.chunks = new ConcurrentHashMap<>();

        newChunks = ConcurrentHashMap.newKeySet();
        entitiesMeshData = new ConcurrentHashMap<>();
        inPipelineChunks = ConcurrentHashMap.newKeySet();
        inflightRequests = ConcurrentHashMap.newKeySet();
        chunkHeights = new ConcurrentHashMap<>();
        entityMeshes = new HashMap<>();
        entities = new HashMap<>();
    }

    public void addEntity(Entity entity, String meshID) {
        if (!entityMeshes.containsKey(meshID)) {
            entityMeshes.put(meshID, new EntityMeshReference());
        }
        this.entities.put(entity.getEntityID(), new EntityMeshWrapper(entity, entityMeshes.get(meshID)));
    }

    public Chunk2D<Integer> getChunkHeights(Position2D position2D) {
        return chunkHeights.get(position2D);
    }

    public void setChunkHeights(Position2D position2D, Chunk2D<Integer> chunk2D) {
        chunkHeights.put(position2D, chunk2D);
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public int size() {
        return chunks.size();
    }

    public ClientWorldChunk get(Position3D position3D, boolean request, boolean shell) {
        return get(position3D, request, shell, -1);
    }

    public ClientWorldChunk get(Position3D position3D, boolean request, boolean shell, int lod) {
        ClientWorldChunk clientWorldChunk = chunks.get(position3D);
        ClientWorldChunk out = null;
        if (clientWorldChunk != null) {
            if (shell || !(clientWorldChunk.getChunkData(-1) instanceof ChunkShell<BlockWithMesh>)) {
                out = clientWorldChunk;
                clientWorldChunk.touch(tick);
            }
        }
        if (request) {
            if (clientWorldChunk == null || clientWorldChunk.getChunkData(-1).getLOD() > lod || clientWorldChunk.getChunkData(-1) instanceof ChunkShell<BlockWithMesh>) {
                if (inflightRequests.size() < ConstantNetworkSettings.INFLIGHT_REQUESTS_MAXIMUM && !inPipelineChunks.contains(position3D)) {
                    inPipelineChunks.add(position3D);
                    inflightRequests.add(position3D);
                    client.sendRequest(new ChunkRequest(position3D, Math.clamp(lod, 0, 5)));
                }
            }
        }
        return out;
    }

    public ClientWorldChunk get(Position3D position3D) {
        return chunks.get(position3D);
    }

    public Position3D[] getKeys() {
        if (chunkKeysChanged.get()) {
            chunkKeysChanged.set(false);
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
        } while (bufferizing && count < settings.getIntSetting("bufferize_chunks_per_frame", Integer.MAX_VALUE) && System.nanoTime() < endTime);
        state.setItem("bufferizing_queue_size", nonBufferizedMeshDataQueue.size());
        return count;
    }

    public void receivedChunk(Position3D position3D) {
        inflightRequests.remove(position3D);
    }

    public int inflightRequestCount() {
        return inflightRequests.size();
    }

    public boolean bufferize(MeshGenerator meshGenerator) {
        if (!nonBufferizedMeshDataQueue.isEmpty()) {
            MeshData meshData = nonBufferizedMeshDataQueue.poll();
            if (meshData instanceof EntityMeshData entityMeshData) {
                EntityMeshReference entityMeshReference = entityMeshes.computeIfAbsent(entityMeshData.id(), k -> new EntityMeshReference());
                if (entityMeshReference.getEntityMesh() != null) {
                    freeEntityMesh(entityMeshReference.getEntityMesh());
                }
                entityMeshReference.setEntityMesh(meshGenerator.bufferizeEntityMesh(entityMeshData));
            } else if (meshData instanceof ChunkMeshData chunkMeshData) {
                ChunkMesh chunkMesh = meshGenerator.bufferizeChunkMesh(chunkMeshData);
                ClientWorldChunk clientWorldChunk = chunks.putIfAbsent(chunkMeshData.chunkPosition(), new ClientWorldChunk(chunkMesh));
                if (clientWorldChunk == null) {
                    chunkKeysChanged.set(true);
                } else {
                    if (clientWorldChunk.getMesh() != null) {
                        freeChunk(clientWorldChunk.getMesh(), meshGenerator.getChunkMeshBuffer());
                    }
                    clientWorldChunk.setMesh(chunkMesh);
                }
                inPipelineChunks.remove(chunkMeshData.chunkPosition());
            }
            return true;
        }
        return false;
    }

    public void add(Position3D position3D, Direction direction, short[] overflowLighting, LightChannels channel) {
        ClientWorldChunk clientWorldChunk = chunks.putIfAbsent(position3D, new ClientWorldChunk(channel, direction, overflowLighting));
        if (clientWorldChunk == null) {
            chunkKeysChanged.set(true);
        } else {
            clientWorldChunk.setNeighborLightOverflow(channel, direction, overflowLighting);
        }
    }

    public void add(Position3D position3D, MeshData meshData) {
        ClientWorldChunk clientWorldChunk = chunks.putIfAbsent(position3D, new ClientWorldChunk(meshData));
        if (clientWorldChunk == null) {
            chunkKeysChanged.set(true);
        } else {
            clientWorldChunk.setMeshData(meshData);
        }
        nonBufferizedMeshDataQueue.add(meshData);
        newChunks.add(position3D);
        state.setItem("shouldCheckNewChunks", true);
    }

    public void addChunkData(Position3D position3D, Chunk<BlockWithMesh> chunk, boolean shell) {
        ClientWorldChunk existing = chunks.putIfAbsent(position3D, new ClientWorldChunk(chunk));

        if (existing == null) {
            if (!shell) {
                chunkKeysChanged.set(true);
            }
            return;
        }

        if (!shell) {
            existing.setChunkData(chunk);
            return;
        }

        if (existing.getChunkData(-1) instanceof ChunkShell<BlockWithMesh> existingShell) {
            if (chunk instanceof ChunkShell<BlockWithMesh> newShell) {
                existing.setChunkData(existingShell.mergeDown(newShell));
            } else {
                existing.setChunkData(chunk);
            }
        }
    }

    public void cleanup(ChunkMeshBuffer chunkMeshBuffer) {
        for (Position3D position : getKeys()) {
            freeChunk(chunks.get(position).getMesh(), chunkMeshBuffer);
        }
        chunks.clear();
    }

    public void tick() {
        tick++;
    }

    public void freeAllChunksNotInAndNotRecentlyAccessed(Predicate<Position3D> predicate, ChunkMeshBuffer chunkMeshBuffer, int maxChunksProcessed) {
        Position3D[] positions = getKeys();

        if (maxChunksProcessed == 0 || maxChunksProcessed > positions.length) {
            if (maxChunksProcessed > 0) {
                freeAllChunksNotInAndNotRecentlyAccessed(predicate, chunkMeshBuffer, positions.length);
            }
            return;
        }

        boolean changed = false;

        int start = (int) Math.floor(Math.random() * (positions.length - maxChunksProcessed));

        for (int positionsLength = positions.length, i = start; i < positionsLength && i < maxChunksProcessed + start; i++) {
            Position3D pos = positions[i];
            if (predicate.test(pos)) continue;

            if (inPipelineChunks.contains(pos)) continue;

            ClientWorldChunk chunk = chunks.get(pos);

            if (tick - chunk.getLastFetchedTick() < ConstantCommonSettings.CHUNK_TICK_TIMEOUT) continue;

            if (neighborRecentlyFetched(pos)) continue;

            freeChunk(chunk.getMesh(), chunkMeshBuffer);
            chunks.remove(pos);

            changed = true;
        }

        if (changed) {
            chunkKeysChanged.set(true);
        }
    }

    private void freeEntityMesh(EntityMesh entityMesh) {
        if (entityMesh != null) {
            EntityMeshDefinition entityDefinition = entityMesh.getDefinition();
            GL30C.glDeleteVertexArrays(entityDefinition.solidVAO());
            GL30C.glDeleteBuffers(entityDefinition.solidVBO());
            GL30C.glDeleteBuffers(entityDefinition.solidEBO());
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
        return neighbor != null && tick - neighbor.getLastFetchedTick() < (long) ConstantCommonSettings.CHUNK_TICK_TIMEOUT || inPipelineChunks.contains(pos);
    }

    private void freeChunk(ChunkMesh mesh, ChunkMeshBuffer chunkMeshBuffer) {
        if (mesh != null) {
            mesh.cleanup(chunkMeshBuffer);

            mesh.meshData().cleanup();
            OpenGLChecks.checkError("delete chunk mesh");
        }
    }

    public void addEntity(EntityMeshData entityMeshData) {
        this.entitiesMeshData.put(entityMeshData.id(), entityMeshData);
        nonBufferizedMeshDataQueue.add(entityMeshData);
    }

    public Map<String, EntityMeshWrapper> getEntityMeshes() {
        return entities;
    }

    public void removeEntity(String entityID) {
        entitiesMeshData.remove(entityID);
    }

    public int inPipelineChunkCount() {
        return inPipelineChunks.size();
    }

    public EntityMeshWrapper getEntity(String entityID) {
        return entities.get(entityID);
    }
}
