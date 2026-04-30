package omnivoxel.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import omnivoxel.server.PackageID;
import omnivoxel.util.log.Logger;

@ChannelHandler.Sharable
public class NetworkHandler extends ChannelInboundHandlerAdapter {
    private final NetworkUser user;

    public NetworkHandler(NetworkUser user) {
        this.user = user;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object pack) {
        if (pack instanceof ByteBuf byteBuf) {
            int bPackageID = byteBuf.getInt(4);
            if (bPackageID < 0 || bPackageID > PackageID.values().length) {
                Logger.error(Logger.Priority.HIGH, "Received package with invalid ID: " + bPackageID);
                byteBuf.release();
                return;
            }

            PackageID packageID = PackageID.values()[bPackageID];

            user.handlePackage(ctx, packageID, byteBuf);
        } else {
            Logger.error(Logger.Priority.HIGH, "Unknown package: " + pack);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}