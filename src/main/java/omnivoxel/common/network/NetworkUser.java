package omnivoxel.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import omnivoxel.server.PackageID;

public interface NetworkUser {
    void handlePackage(ChannelHandlerContext ctx, PackageID packageID, ByteBuf byteBuf);
}