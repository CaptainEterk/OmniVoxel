package omnivoxel.server.world;

import omnivoxel.common.settings.ConstantServerSettings;
import omnivoxel.server.client.chunk.result.ChunkCacheItem;
import omnivoxel.util.log.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class ChunkCacheHandler {
    private static final BlockingQueue<ChunkCacheItem> chunkCacheQueue = new LinkedBlockingDeque<>();

    public static void queueCache(ChunkCacheItem chunkCacheItem) {
        chunkCacheQueue.add(chunkCacheItem);
    }

    public static void cacheAll() throws InterruptedException {
        while (!chunkCacheQueue.isEmpty()) {
            ChunkCacheItem item = chunkCacheQueue.poll(100, TimeUnit.MILLISECONDS);
            if (item != null) {
                try {
                    Path finalPath = Path.of(ConstantServerSettings.CHUNK_SAVE_LOCATION + item.chunkPosition().getPath());
                    Files.createDirectories(finalPath.getParent());

                    Path tempPath = finalPath.resolveSibling(
                            finalPath.getFileName() + ".tmp"
                    );

                    Files.write(tempPath, item.bytes());

                    try {
                        Files.move(tempPath, finalPath,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(tempPath, finalPath,
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Logger.warn("Failed to cache chunk");
            }
        }
    }
}