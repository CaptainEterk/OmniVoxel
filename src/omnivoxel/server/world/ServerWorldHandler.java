package omnivoxel.server.world;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.server.PackageID;
import omnivoxel.server.client.ServerClient;
import omnivoxel.server.client.block.ServerBlock;
import omnivoxel.util.math.Position3D;
import omnivoxel.world.chunk.Chunk;

public class ServerWorldHandler {
    private final ServerWorld world;

    public ServerWorldHandler(ServerWorld world) {
        this.world = world;
    }

    private static void sendBytes(ChannelHandlerContext ctx, PackageID id, byte[]... bytes) {
        ByteBuf buffer = Unpooled.buffer();
        int length = 4;
        for (byte[] bites : bytes) {
            length += bites.length;
        }
        buffer.writeInt(length);
        buffer.writeInt(id.ordinal());
        for (byte[] bites : bytes) {
            buffer.writeBytes(bites);
        }
        ctx.channel().writeAndFlush(buffer);
    }

    public void removeBlock(int worldX, int worldY, int worldZ, ServerClient client) {
        boolean success = false;
        if (canModify(worldX, worldY, worldZ, client)) {
            int chunkX = worldX / ConstantGameSettings.CHUNK_WIDTH;
            int chunkY = worldY / ConstantGameSettings.CHUNK_HEIGHT;
            int chunkZ = worldZ / ConstantGameSettings.CHUNK_LENGTH;
            int x = worldX % ConstantGameSettings.CHUNK_WIDTH;
            int y = worldX % ConstantGameSettings.CHUNK_HEIGHT;
            int z = worldX % ConstantGameSettings.CHUNK_LENGTH;
            Position3D position3D = new Position3D(chunkX, chunkY, chunkZ);
            Chunk<ServerBlock> chunk = world.get(position3D);
            if (chunk != null) {
                chunk.setBlock(x, y, z, ServerBlock.AIR);
                success = true;
            } else {
//                ServerLogger.logger.debug("Unable to set block (%d, %d, %d)".formatted(worldX, worldY, worldZ));
            }
        }
        if (client != null) {
//            sendBytes(client.getCTX(), success ? PackageID.REMOVE_BLOCK_SUCCESS : PackageID.REMOVE_BLOCK_FAILURE);
        }
    }

    public void addBlock(int worldX, int worldY, int worldZ, ServerClient client) {

    }

    private boolean canModify(int worldX, int worldY, int worldZ, ServerClient client) {
        // TODO: Sometimes the region might check the name of the player
        return true;
    }
}