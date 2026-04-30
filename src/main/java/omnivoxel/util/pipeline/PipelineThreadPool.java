package omnivoxel.util.pipeline;

import java.util.List;

public class PipelineThreadPool {
    private final List<PipelineStep<?>> pipeline;

    public PipelineThreadPool(int threadCount, boolean daemon, PipelineStep<?>... pipeline) {
        this.pipeline = List.of(pipeline);
    }
}