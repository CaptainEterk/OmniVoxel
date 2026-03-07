package omnivoxel.util.executor;

import java.util.ArrayDeque;
import java.util.Queue;

public class ExecutorCollection<E extends Executor> implements Executor {
    private final Queue<E> executors;

    public ExecutorCollection() {
        executors = new ArrayDeque<>();
    }

    public void add(E executor) {
        executors.add(executor);
    }

    @Override
    public void execute() {
        executors.forEach(Executor::execute);
    }
}