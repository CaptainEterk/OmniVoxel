package omnivoxel.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private final Server server;

    public ServerHandler(Server server) {
        this.server = server;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object pack) throws InterruptedException {
        if (pack instanceof ByteBuf byteBuf) {
            int bPackageID = byteBuf.getInt(0);
            PackageID packageID = PackageID.values()[bPackageID];

            server.handlePackage(ctx, packageID, byteBuf);
        } else {
            System.out.println("Unknown package: " + pack);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}