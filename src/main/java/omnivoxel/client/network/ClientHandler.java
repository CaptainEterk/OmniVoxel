package omnivoxel.client.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import omnivoxel.server.PackageID;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private final Client client;

    public ClientHandler(Client client) {
        this.client = client;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        if (msg instanceof ByteBuf byteBuf) {
            int bPackageID = byteBuf.getInt(4);
            PackageID packageID = PackageID.values()[bPackageID];

            client.handlePackage(ctx, packageID, byteBuf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}