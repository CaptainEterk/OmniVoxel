package omnivoxel.server.client.chunk;

import io.netty.buffer.ByteBuf;
import omnivoxel.server.client.ServerClient;

public record ChunkTask(ServerClient serverClient, int x, int y, int z, ByteBuf byteBuf) {
    public ChunkTask(int x, int y, int z) {
        this(null, x, y, z, null);
    }
}