package omnivoxel.server.io;

import omnivoxel.common.settings.ConstantServerSettings;
import omnivoxel.util.thread.AsyncWorkerThread;

public class CacheIO {
    private static final AsyncWorkerThread<CacheItem> cacheAsyncWorkerThread = new AsyncWorkerThread<>(CacheHandler::cache, false, ConstantServerSettings.NECESSARY_CACHE_SIZE);

    public static boolean add(CacheItem cacheItem, boolean necessary) {
        return cacheAsyncWorkerThread.add(cacheItem, necessary);
    }

    public static void stop() {
        cacheAsyncWorkerThread.stop();
    }

    public static int size() {
        return cacheAsyncWorkerThread.size();
    }
}