package omnivoxel.client.launcher;

import omnivoxel.common.settings.ConstantCommonSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ClientInitializer {
    public static boolean SHOW_LOGS;

    public static void init() throws IOException {
        createFileLocations();
    }

    private static void createFileLocations() throws IOException {
        Files.createDirectories(Path.of(ConstantCommonSettings.LOG_LOCATION));
    }
}