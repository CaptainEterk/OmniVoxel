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
import omnivoxel.common.block.hitbox.BlockHitbox;
import omnivoxel.common.network.NetworkHandler;
import omnivoxel.common.settings.ConstantCommonSettings;
import omnivoxel.common.settings.Settings;
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

    public static void main(String[] args) throws IOException {
        new ServerLauncher().run(100L);
    }

    public void run(long seed) throws IOException {
//        Logger.setMinPriority(Logger.Priority.NORMAL);

        Settings settings = new Settings();
        settings.load(ConstantCommonSettings.CONFIG_LOCATION);
        int port = settings.getIntSetting("port", 1515);
        String ip = settings.getSetting("ip", "0.0.0.0");

        ServerInitializer.init();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        Map<String, BlockShape> blockShapeCache = new HashMap<>();
        Map<String, BlockHitbox[]> blockHitboxCache = new HashMap<>();

        ServerWorld world = new ServerWorld();

        try {
            Map<String, ServerClient> clients = new ConcurrentHashMap<>();
            Server server = new Server(clients, seed, world, blockShapeCache, blockHitboxCache, ChunkIO.BLOCK_SERVICE, new ServerWorldHandler(world, clients), settings);
            Thread thread = new Thread(server::run, "Server Tick Loop");
            thread.start();

            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4),
                                    new NetworkHandler(server),
                                    new LengthFieldPrepender(4)
                            );
                        }
                    });

            ChannelFuture future = serverBootstrap.bind(ip, port).sync();
            Logger.info("Server started at " + ip + ":" + port);

            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
