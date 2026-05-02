package omnivoxel.common.settings;

public class ConstantCommonSettings {
    public static final int CHUNK_SIZE = 32;
    public static final int CHUNK_LENGTH = CHUNK_SIZE;
    public static final int PADDED_LENGTH = CHUNK_LENGTH + 2;
    public static final int CHUNK_HEIGHT = CHUNK_SIZE;
    public static final int PADDED_HEIGHT = CHUNK_HEIGHT + 2;
    public static final int CHUNK_WIDTH = CHUNK_SIZE;
    public static final int BLOCKS_IN_CHUNK_2D = CHUNK_WIDTH * CHUNK_LENGTH;
    public static final int BLOCKS_IN_CHUNK = ((CHUNK_WIDTH) * (CHUNK_LENGTH) * (CHUNK_HEIGHT));
    public static final int PADDED_WIDTH = CHUNK_WIDTH + 2;
    public static final int BLOCKS_IN_CHUNK_PADDED = PADDED_WIDTH * PADDED_HEIGHT * PADDED_LENGTH;
    public static final String FILE_LOCATION = getRootFolder();
    public static final String CONFIG_LOCATION = FILE_LOCATION + ".config/";
    public static final String LOG_LOCATION = FILE_LOCATION + ".logs/";
    public static final int CHUNK_TICK_TIMEOUT = 10;
    public static final int MODIFICATION_GENERALIZATION_LIMIT = 10;

    private static String getRootFolder() {
        return System.getProperty("user.dir") + "/.omnivoxel/";
    }
}