package omnivoxel.util.thread;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

public class WorkerThreadPool<T> {
    private final WorkerThread<T>[] workers;
    private final AtomicBoolean running;

    @SuppressWarnings("unchecked")
    public WorkerThreadPool(int threadCount, Supplier<Function<T, List<T>>> taskHandlerSupplier, boolean daemon) {
        this.workers = new WorkerThread[threadCount];
        running = new AtomicBoolean(true);

        for (int i = 0; i < threadCount; i++) {
            WorkerThread<T> workerThread = new WorkerThread<>(new LinkedBlockingDeque<>(), taskHandlerSupplier.get(), running);
            Thread thread = new Thread(workerThread, "Worker-" + i);
            thread.setDaemon(daemon);
            workers[i] = workerThread;
            thread.start();
        }
    }

    public void submit(T task) throws InterruptedException {
        if (running.get()) {
            BlockingQueue<T> smallestQueue = null;
            int smallestSize = Integer.MAX_VALUE;
            for (WorkerThread<T> workerThread : workers) {
                BlockingQueue<T> queue = workerThread.taskQueue();
                int size = queue.size();
                if (size < smallestSize) {
                    smallestQueue = queue;
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

    public static final class WorkerThread<V> implements Runnable {
        private final BlockingQueue<V> taskQueue;
        private final Function<V, List<V>> taskHandler;
        private final Queue<V> localQueue;
        private final AtomicBoolean running;
        private Thread thread;

        public WorkerThread(BlockingQueue<V> taskQueue, Function<V, List<V>> taskHandler, AtomicBoolean running) {
            this.taskQueue = taskQueue;
            this.taskHandler = taskHandler;
            this.running = running;
            this.localQueue = new ArrayDeque<>();
        }

        @Override
        public void run() {
            thread = Thread.currentThread();
            try {
                while (!Thread.currentThread().isInterrupted() && running.get()) {
                    int taskCount = taskQueue.drainTo(localQueue);
                    if (taskCount > 0) {
                        while (!localQueue.isEmpty()) {
                            List<V> moreTasks = taskHandler.apply(localQueue.remove());
                            if (moreTasks != null) {
                                localQueue.addAll(moreTasks);
                            }
                        }
                    } else {
                        // TODO: Use blocking
                        Thread.sleep(10);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public BlockingQueue<V> taskQueue() {
            return taskQueue;
        }
    }
}