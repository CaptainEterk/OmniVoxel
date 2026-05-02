package omnivoxel.common.settings;

public class ConstantNetworkSettings {
    public static final int CHUNK_GENERATOR_INDIVIDUAL_TASK_COUNT = 5;
    public static final int INFLIGHT_REQUESTS_MAXIMUM = ConstantServerSettings.CHUNK_GENERATOR_THREAD_LIMIT * CHUNK_GENERATOR_INDIVIDUAL_TASK_COUNT * 5;
    public static final int INFLIGHT_REQUESTS_MINIMUM = INFLIGHT_REQUESTS_MAXIMUM / 2;
    public static final long CHUNK_REQUEST_BATCHING_TIME = 50;
    public static final int CHUNK_REQUEST_BATCHING_LIMIT = 1000;
}