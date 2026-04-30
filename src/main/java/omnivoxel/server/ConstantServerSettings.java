package omnivoxel.server;

// TODO: Move to Settings
public final class ConstantServerSettings {
    public static final int CHUNK_GENERATOR_INDIVIDUAL_TASK_COUNT = 20;
    public static final int CHUNK_GENERATOR_THREAD_LIMIT = Runtime.getRuntime().availableProcessors();
    public static final int INFLIGHT_REQUESTS_MAXIMUM = CHUNK_GENERATOR_THREAD_LIMIT * CHUNK_GENERATOR_INDIVIDUAL_TASK_COUNT * 5;
    public static final int INFLIGHT_REQUESTS_MINIMUM = INFLIGHT_REQUESTS_MAXIMUM / 2;
    public static final int MODIFICATION_GENERALIZATION_LIMIT = 10;
    public static final long CHUNK_REQUEST_BATCHING_TIME = 50;
    public static final int CHUNK_REQUEST_BATCHING_LIMIT = 1000;
    public static final int CHUNK_TIME_LIMIT = 1000000;

    public static final String WORLD_SAVE_LOCATION = "./world/";
    public static final String CHUNK_SAVE_LOCATION = WORLD_SAVE_LOCATION + "chunks/";
    public static final String GAME_LOCATION = "game/";
}