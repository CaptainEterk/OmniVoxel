package omnivoxel.server.world;

import omnivoxel.common.settings.ConstantServerSettings;
import omnivoxel.server.client.chunk.ChunkIO;
import omnivoxel.server.client.chunk.result.ChunkCacheItem;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ChunkCacheHandler {
    public static void cache(ChunkCacheItem item) {
        try {
            Path finalPath = Path.of(ConstantServerSettings.CHUNK_SAVE_LOCATION + item.chunkPosition().getPath());
            Files.createDirectories(finalPath.getParent());

            Path tempPath = finalPath.resolveSibling(
                    finalPath.getFileName() + ".tmp"
            );

            Files.write(tempPath, ChunkIO.encode(item.chunk()));

            try {
                Files.move(tempPath, finalPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, finalPath,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}