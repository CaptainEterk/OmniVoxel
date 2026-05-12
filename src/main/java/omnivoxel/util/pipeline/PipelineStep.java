package omnivoxel.util.pipeline;

import omnivoxel.util.thread.WorkerTask;
import omnivoxel.util.thread.WorkerThreadPool;

import java.util.List;
import java.util.function.BiFunction;

public final class PipelineStep<T extends WorkerTask> {
    private final WorkerThreadPool<T> workerThreadPool;

    public PipelineStep(int threadCount, BiFunction<?, Integer, List<?>> process, boolean daemon) {
        this.workerThreadPool = new WorkerThreadPool<>(threadCount, () -> {
            return null;
        }, daemon);
    }

    public List<T> process(T pipelineStep, int queueSize) {
        return null;
    }
}