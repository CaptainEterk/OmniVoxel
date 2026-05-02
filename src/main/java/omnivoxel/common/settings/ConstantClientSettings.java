package omnivoxel.common.settings;

public class ConstantClientSettings {
    public static final String DEFAULT_WINDOW_TITLE = "OmniVoxel v0.8.2-alpha";

    // TODO: Move this to settings
    public static final int MAX_MESH_GENERATOR_THREADS = Runtime.getRuntime().availableProcessors();
    public static final int MAX_LIGHTING_GENERATOR_THREADS = Runtime.getRuntime().availableProcessors();
    public static final int BUFFERIZE_CHUNKS_PER_FRAME = 10;

    public static final String DEFAULT_SETTING_CONTENTS = """
            width=750
            height=750
            render_distance=128
            render_scale=1.0
            render_filter=nearest
            sensitivity=2f
            frustum_bias=10
            shader=default""";

    public static final String DATA_LOCATION = "";

    // TODO: Move this to settings
    public static final int TARGET_TPS = 60;
    public static final long TICK_LENGTH_NS = 1_000_000_000L / TARGET_TPS;
    public static final boolean OPENGL_DEBUG = true;
    public static long BUFFERIZE_END_TIME_LIMIT_MS = 1;
}
