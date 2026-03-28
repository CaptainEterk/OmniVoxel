package omnivoxel.client.game.tick;

import omnivoxel.client.game.graphics.api.opengl.mesh.util.PriorityUtils;
import omnivoxel.client.game.graphics.api.opengl.window.Window;
import omnivoxel.client.game.input.KeyInput;
import omnivoxel.client.game.input.MouseButtonInput;
import omnivoxel.client.game.input.MouseInput;
import omnivoxel.client.game.player.PlayerController;
import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.client.network.Client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class TickLoop implements Runnable {
    private final PlayerController playerController;
    private final AtomicBoolean gameRunning;
    private final BlockingQueue<Consumer<Window>> contextTasks;
    private final Client client;

    public TickLoop(PlayerController playerController, AtomicBoolean gameRunning, BlockingQueue<Consumer<Window>> contextTasks, Client client) {
        this.playerController = playerController;
        this.gameRunning = gameRunning;
        this.contextTasks = contextTasks;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            Thread.currentThread().setName("TickLoop");

            KeyInput keyInput = new KeyInput();
            MouseButtonInput mouseButtonInput = new MouseButtonInput();
            MouseInput mouseInput = new MouseInput();

            contextTasks.add(keyInput::init);
            contextTasks.add(mouseButtonInput::init);
            contextTasks.add(mouseInput::init);

            playerController.setKeyInput(keyInput);
            playerController.setMouseButtonInput(mouseButtonInput);
            playerController.setMouseInput(mouseInput);

            PriorityUtils.setCamera(playerController.getCamera());

            long deltaTime = ConstantGameSettings.TICK_LENGTH_NS;
            long nextTickTime = System.nanoTime();

            while (gameRunning.get()) {
                long now = System.nanoTime();
                if (now >= nextTickTime) {
                    playerController.tick(deltaTime / 1_000_000_000.0);
                    nextTickTime += deltaTime;
                } else {
                    long sleepTime = nextTickTime - now;
                    if (sleepTime > 1_000_000) {
                        // Sleep most of the time
                        LockSupport.parkNanos(sleepTime - 500_000);
                    }
                    // Short busy-wait for precision
                    while (System.nanoTime() < nextTickTime) {
                        Thread.onSpinWait();
                    }
                }
            }
        } catch (Exception e) {
            gameRunning.set(false);
            throw e;
        }
    }
}