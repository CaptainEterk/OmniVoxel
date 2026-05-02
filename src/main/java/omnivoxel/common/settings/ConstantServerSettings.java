package omnivoxel.common.settings;

// TODO: Move to Settings
public final class ConstantServerSettings {
    public static final int CHUNK_GENERATOR_THREAD_LIMIT = Runtime.getRuntime().availableProcessors();

    public static final String WORLD_SAVE_LOCATION = ConstantCommonSettings.FILE_LOCATION + "./world/";
    public static final String CHUNK_SAVE_LOCATION = WORLD_SAVE_LOCATION + "chunks/";
    public static final String GAME_LOCATION = ConstantCommonSettings.FILE_LOCATION + "game/";
}