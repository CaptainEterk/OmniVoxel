package omnivoxel.util.time;

import omnivoxel.util.executor.Executor;
import org.lwjgl.glfw.GLFW;

public class PeriodicTimeExecutor implements Executor {
    private final Runnable task;
    private final double period;
    private double time;

    public PeriodicTimeExecutor(Runnable task, double period) {
        this.task = task;
        this.period = period;
        this.time = GLFW.glfwGetTime();
    }

    @Override
    public void execute() {
        double newTime = GLFW.glfwGetTime();
        if (newTime - this.time > period) {
            task.run();
            this.time += period;
        }
    }
}