package omnivoxel.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import omnivoxel.common.BlockShape;
import omnivoxel.server.client.ServerClient;
import omnivoxel.server.client.chunk.ChunkIO;
import omnivoxel.server.world.ServerWorld;
import omnivoxel.server.world.ServerWorldHandler;
import omnivoxel.util.log.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerLauncher {
    // TODO: Use a config file
    private static final int PORT = 1515;
    private static final String IP = "0.0.0.0";

    public static void main(String[] args) throws IOException {
        new ServerLauncher().run(100L);
    }

    public void run(long seed) throws IOException {
        ServerInitializer.init();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Map<String, BlockShape> blockShapeCache = new HashMap<>();

        ServerWorld world = new ServerWorld();

        try {
            // TODO: Make a wrapper class around clients
            Map<String, ServerClient> clients = new ConcurrentHashMap<>();
            Server server = new Server(clients, seed, world, blockShapeCache, ChunkIO.BLOCK_SERVICE, new ServerWorldHandler(world, clients));
            Thread thread = new Thread(server::run, "Server Tick Loop");
            thread.start();
            ServerHandler serverHandler = new ServerHandler(server);

            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4),
                                    serverHandler,
                                    new LengthFieldPrepender(4)
                            );
                        }
                    });

            ChannelFuture future = serverBootstrap.bind(IP, PORT).sync();
            Logger.info("Server started at " + IP + ":" + PORT);

            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
