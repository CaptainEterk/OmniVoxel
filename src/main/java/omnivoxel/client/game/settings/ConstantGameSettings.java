package omnivoxel.client.game.settings;

public class ConstantGameSettings {
    public static final int CHUNK_SIZE = 32;
    public static final int CHUNK_WIDTH = CHUNK_SIZE;
    public static final int PADDED_WIDTH = ConstantGameSettings.CHUNK_WIDTH + 2;
    public static final int CHUNK_HEIGHT = CHUNK_SIZE;
    public static final int PADDED_HEIGHT = ConstantGameSettings.CHUNK_HEIGHT + 2;
    public static final int CHUNK_LENGTH = CHUNK_SIZE;
    public static final int PADDED_LENGTH = ConstantGameSettings.CHUNK_LENGTH + 2;
    public static final int BLOCKS_IN_CHUNK_PADDED = PADDED_WIDTH * PADDED_HEIGHT * PADDED_LENGTH;
    public static final int BLOCKS_IN_CHUNK = ((ConstantGameSettings.CHUNK_WIDTH) * (ConstantGameSettings.CHUNK_LENGTH) * (ConstantGameSettings.CHUNK_HEIGHT));
    public static final int BLOCKS_IN_CHUNK_2D = ConstantGameSettings.CHUNK_WIDTH*ConstantGameSettings.CHUNK_LENGTH;

    public static final String DEFAULT_WINDOW_TITLE = "OmniVoxel v0.8.1-alpha";

    // TODO: Move this to settings
    public static final int MAX_MESH_GENERATOR_THREADS = Runtime.getRuntime().availableProcessors();
    public static final int MAX_LIGHTING_GENERATOR_THREADS = Runtime.getRuntime().availableProcessors();
    public static final int BUFFERIZE_CHUNKS_PER_FRAME = 10;

    public static final String FILE_LOCATION = getRootFolder();

    public static final String CONFIG_LOCATION = FILE_LOCATION + ".config/";
    public static final String DEFAULT_SETTING_CONTENTS = """
            width=750
            height=750
            render_distance=128
            render_scale=1.0
            render_filter=nearest
            sensitivity=2f
            frustum_bias=10""";

    public static final String DATA_LOCATION = "";
    public static final String LOG_LOCATION = FILE_LOCATION + ".logs/";

    // TODO: Move this to settings
    public static final float COLLISION_STEPS = 5;
    public static final int COLLISION_COUNT = 2;

    // TODO: Move this to settings
    public static final int TARGET_TPS = 60;
    public static final long TICK_LENGTH_NS = 1_000_000_000L / TARGET_TPS;
    public static final String GAME_LOCATION = FILE_LOCATION + "games/";
    public static final int CHUNK_TICK_TIMEOUT = 10;
    public static long BUFFERIZE_END_TIME_LIMIT_MS = 1;

    private static String getRootFolder() {
        return System.getProperty("user.dir") + "/.omnivoxel/";
    }
}
