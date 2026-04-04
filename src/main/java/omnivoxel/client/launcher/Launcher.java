package omnivoxel.client.launcher;

import io.netty.util.ResourceLeakDetector;
import omnivoxel.client.game.GameLoop;
import omnivoxel.client.game.graphics.api.opengl.window.Window;
import omnivoxel.client.game.graphics.block.BlockWithMesh;
import omnivoxel.client.game.graphics.camera.Camera;
import omnivoxel.client.game.graphics.camera.Frustum;
import omnivoxel.client.game.player.PlayerController;
import omnivoxel.client.game.settings.Settings;
import omnivoxel.client.game.state.State;
import omnivoxel.client.game.tick.TickLoop;
import omnivoxel.client.game.world.ClientWorld;
import omnivoxel.client.network.Client;
import omnivoxel.client.network.ClientLauncher;
import omnivoxel.client.network.chunk.worldDataService.ClientWorldDataService;
import omnivoxel.util.log.Logger;
import omnivoxel.world.block.BlockService;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Launcher {
    public static void main(String[] a) throws IOException, InterruptedException {
        Set<String> args = new HashSet<>(Arrays.asList(a));

        ClientInitializer.SHOW_LOGS = !args.contains("--no-logs");
        ClientInitializer.init();

        SecureRandom secureRandom = new SecureRandom();
        byte[] clientID = new byte[32];
        secureRandom.nextBytes(clientID);

        CountDownLatch connected = new CountDownLatch(1);

        ClientWorldDataService clientWorldDataService = new ClientWorldDataService();

        State state = new State();
        Settings settings = new Settings();
        settings.load();

        ClientWorld world = new ClientWorld(state);

        Logger logger = new Logger("Client", ClientInitializer.SHOW_LOGS);

        BlockService<BlockWithMesh> blockService = new BlockService<>((id -> new BlockWithMesh(id, clientWorldDataService.getBlock(id))));

        Client client = new Client(clientID, clientWorldDataService, logger, world, blockService);
        ClientLauncher clientLauncher = new ClientLauncher(logger, connected, client);
        Thread clientThread = new Thread(clientLauncher, "Client");
        clientThread.start();

        world.setClient(client);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        if (connected.await(5L, TimeUnit.SECONDS)) {
            client.setListeners(world.getEntityMeshDefinitionCache(), world.getQueuedEntityMeshData(), state);
            AtomicBoolean gameRunning = new AtomicBoolean(true);
            BlockingQueue<Consumer<Window>> contextTasks = new LinkedBlockingDeque<>();

            Camera camera = new Camera(new Frustum(), state);

            GameLoop gameLoop = new GameLoop(camera, world, gameRunning, contextTasks, client, state, settings);

            try {
                gameLoop.init();

                PlayerController playerController = new PlayerController(client, camera, settings, contextTasks, state, world, blockService, gameLoop.getRenderer().getWindow());

                Thread tickLoopThread = new Thread(new TickLoop(playerController, gameRunning, contextTasks, client), "Tick Loop");
                tickLoopThread.start();

                gameLoop.run();
            } catch (IOException e) {
                client.close();
                throw new RuntimeException(e);
            }
        }
    }
}