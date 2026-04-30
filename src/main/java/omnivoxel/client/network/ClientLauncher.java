package omnivoxel.client.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import omnivoxel.common.network.NetworkHandler;
import omnivoxel.common.network.NetworkService;
import omnivoxel.server.PackageID;
import omnivoxel.util.bytes.ByteUtils;
import omnivoxel.util.log.Logger;

import java.util.concurrent.CountDownLatch;

public class ClientLauncher implements Runnable {
    private static final int VERSION_ID = 0;
    private static final String HOST = "localhost";
    private static final int PORT = 1515;

    private final CountDownLatch connected;
    private final byte[] clientID;

    private final Client client;

    public ClientLauncher(CountDownLatch connected, Client client) {
        this.connected = connected;
        this.client = client;
        clientID = client.getClientID();
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
                                    new NetworkHandler(client)
                            );
                        }
                    }).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

            Logger.info("Connecting to " + HOST + ":" + PORT);

            ChannelFuture future = bootstrap.connect(HOST, PORT).sync();
            client.setChannel(future.channel());
            client.setGroup(group);
            connected.countDown();

            Logger.info("Connected to server at " + HOST + ":" + PORT + " with clientID " + ByteUtils.bytesToHex(clientID));

            NetworkService.sendBytes(future.channel(), PackageID.VERSION_HANDSHAKE, clientID, String.format("%-8s", VERSION_ID).getBytes());

            future.channel().closeFuture().sync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            group.shutdownGracefully();
        }
    }
}
