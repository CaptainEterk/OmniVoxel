package omnivoxel.client.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import omnivoxel.server.PackageID;
import omnivoxel.util.bytes.ByteUtils;
import omnivoxel.util.log.Logger;

import java.util.concurrent.CountDownLatch;

public class ClientLauncher implements Runnable {
    private static final int VERSION_ID = 0;
    private static final String HOST = "172.28.217.111";
    private static final int PORT = 1515;

    private final CountDownLatch connected;
    private final byte[] clientID;

    private final Client client;

    public ClientLauncher(CountDownLatch connected, Client client) {
        this.connected = connected;
        this.client = client;
        clientID = client.getClientID();
    }

    public static void sendBytes(ChannelFuture future, PackageID id, byte[]... bytes) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(id.ordinal());
        for (byte[] bites : bytes) {
            buffer.writeBytes(bites);
        }
        future.channel().writeAndFlush(buffer);
    }

    @Override
    public void run() {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldPrepender(4),
                                    new LengthFieldBasedFrameDecoder(
                                            1048576,
                                            0,
                                            4,
                                            0,
                                            4
                                    ),
                                    new ClientHandler(client)
                            );
                        }
                    });

            ChannelFuture future = bootstrap.connect(HOST, PORT).sync();
            client.setChannel(future.channel());
            client.setGroup(group);
            connected.countDown();

            Logger.info("Connected to server at " + HOST + ":" + PORT + " with clientID " + ByteUtils.bytesToHex(clientID));

            sendBytes(future, PackageID.REGISTER_CLIENT, String.format("%-8s", VERSION_ID).getBytes(), clientID);

            future.channel().closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
