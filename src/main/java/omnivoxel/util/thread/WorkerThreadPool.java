package omnivoxel.util.thread;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class WorkerThreadPool<T extends WorkerTask> {
    private final WorkerThread<T>[] workers;
    private final AtomicBoolean running;
    private final Set<T> pendingTasks;

    @SuppressWarnings("unchecked")
    public WorkerThreadPool(int threadCount, Supplier<BiFunction<T, Integer, List<T>>> taskHandlerSupplier, boolean daemon) {
        workers = new WorkerThread[threadCount];
        running = new AtomicBoolean(true);
        pendingTasks = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            WorkerThread<T> workerThread = new WorkerThread<>(new LinkedBlockingDeque<>(), taskHandlerSupplier.get(), running, pendingTasks);
            Thread thread = new Thread(workerThread, "Worker-" + i);
            thread.setDaemon(daemon);
            workers[i] = workerThread;
            thread.start();
        }
    }

    public void submit(T task) {
        try {
            if (running.get() && task != null) {
                if (pendingTasks.contains(task)) {
                    task.reject();
                    return;
                }
                BlockingDeque<T> smallestQueue = null;
                int smallestSize = Integer.MAX_VALUE;
                for (WorkerThread<T> workerThread : workers) {
                    int size = workerThread.size();
                    if (size < smallestSize) {
                        smallestQueue = workerThread.taskQueue();
                        smallestSize = size;
                        if (size == 0) {
                            break;
                        }
                    }
                }
                if (smallestQueue != null) {
                    smallestQueue.put(task);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        running.set(false);
    }

    public void awaitTermination() {
        for (WorkerThread<T> worker : workers) {
            try {
                worker.thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean hasTask(T task) {
        return pendingTasks.contains(task);
    }

    public static final class WorkerThread<V extends WorkerTask> implements Runnable {
        private final BlockingDeque<V> taskQueue;
        private final BiFunction<V, Integer, List<V>> taskHandler;
        private final AtomicBoolean running;
        private final Set<V> pendingTasks;
        private final Queue<V> priorityQueue;
        private Thread thread;

        public WorkerThread(BlockingDeque<V> taskQueue, BiFunction<V, Integer, List<V>> taskHandler, AtomicBoolean running, Set<V> pendingTasks) {
            this.taskQueue = taskQueue;
            this.taskHandler = taskHandler;
            this.running = running;
            this.pendingTasks = pendingTasks;
            this.priorityQueue = new ArrayDeque<>();
        }

        @Override
        public void run() {
            thread = Thread.currentThread();

            try {
                while (!Thread.currentThread().isInterrupted() && running.get()) {
                    int priorityBudget = 8;
                    while (!priorityQueue.isEmpty() && priorityBudget-- > 0) {
                        V task = priorityQueue.remove();
                        pendingTasks.remove(task);
                        List<V> moreTasks = taskHandler.apply(task, priorityQueue.size() + taskQueue.size());
                        if (moreTasks != null) {
                            moreTasks.forEach(t -> {
                                if (pendingTasks.add(t)) {
                                    priorityQueue.add(t);
                                } else {
                                    t.reject();
                                }
                            });
                        }
                    }

                    V task = priorityQueue.isEmpty()
                            ? taskQueue.poll(100, TimeUnit.MILLISECONDS)
                            : taskQueue.poll();

                    if (task != null) {
                        List<V> moreTasks = taskHandler.apply(task, priorityQueue.size() + taskQueue.size());
                        pendingTasks.remove(task);
                        if (moreTasks != null) {
                            moreTasks.forEach(t -> {
                                if (pendingTasks.add(t)) {
                                    priorityQueue.add(t);
                                } else {
                                    t.reject();
                                }
                            });
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public int size() {
            return priorityQueue.size() + taskQueue.size();
        }

        public BlockingDeque<V> taskQueue() {
            return taskQueue;
        }
    }
}