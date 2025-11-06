package omnivoxel.server;

import omnivoxel.client.game.settings.ConstantGameSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class ServerInitializer {
    public static void init() throws IOException {
        createFileLocations();
    }

    private static void createFileLocations() throws IOException {
        Files.createDirectories(Path.of(ConstantGameSettings.LOG_LOCATION));
        Files.createDirectories(Path.of(ConstantGameSettings.GAME_LOCATION));
        Files.createDirectories(Path.of(ConstantServerSettings.WORLD_SAVE_LOCATION));
        Path chunkSaveLocation = Path.of(ConstantServerSettings.CHUNK_SAVE_LOCATION);
        clearDirectory(chunkSaveLocation);
        Files.createDirectories(chunkSaveLocation);
    }

    public static void clearDirectory(Path dir) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return;
        }

        try (var paths = Files.walk(dir)) {
            paths
                    .sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(dir))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete: " + path, e);
                        }
                    });
        }
    }
}