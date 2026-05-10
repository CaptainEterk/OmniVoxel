package omnivoxel.util.thread;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class AsyncWorkerThread<T> {
    private final Consumer<T> consumer;
    private final BlockingQueue<T> queue;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public AsyncWorkerThread(Consumer<T> consumer, boolean daemon) {
        this.consumer = consumer;
        queue = new LinkedBlockingDeque<>();
        Thread thread = new Thread(this::run);
        thread.setDaemon(daemon);
        thread.start();
    }

    private void run() {
        try {
            while (!Thread.interrupted() && running.get()) {
                T item = queue.poll(100L, TimeUnit.MILLISECONDS);
                if (item != null) {
                    consumer.accept(item);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(T task) {
        queue.add(task);
    }

    public void addAll(Collection<T> tasks) {
        queue.addAll(tasks);
    }

    public void stop() {

    }
}