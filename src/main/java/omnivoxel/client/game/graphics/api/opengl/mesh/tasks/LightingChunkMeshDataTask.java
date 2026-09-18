package omnivoxel.client.game.graphics.api.opengl.mesh.tasks;

import io.netty.buffer.ByteBuf;
import omnivoxel.client.game.graphics.api.opengl.mesh.MeshDataTask;
import omnivoxel.client.game.graphics.light.channel.LightChannels;
import omnivoxel.util.math.Position3D;

import java.util.Objects;

public record LightingChunkMeshDataTask(ByteBuf blocks, Position3D position3D,
                                        LightChannels channel, boolean priority) implements MeshDataTask {
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LightingChunkMeshDataTask that = (LightingChunkMeshDataTask) o;
        return Objects.equals(position3D, that.position3D) && channel == that.channel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position3D, channel);
    }

    @Override
    public void reject() {
    }
}