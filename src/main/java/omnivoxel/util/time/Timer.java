package omnivoxel.util.time;

public class Timer {
    private final long[] times;
    private int timeIndex = 0;
    private int recordedCount = 0;

    private long startTime = 0;
    private boolean running = false;

    public Timer(int timerSize) {
        this.times = new long[timerSize];
    }

    public void start() {
        if (running) {
            throw new IllegalStateException("Timer is already running.");
        }
        startTime = System.nanoTime();
        running = true;
    }

    public long stop() {
        if (!running) {
            throw new IllegalStateException("Timer is not running.");
        }
        long duration = System.nanoTime() - startTime;
        times[timeIndex % times.length] = duration;
        timeIndex++;
        recordedCount = Math.min(recordedCount + 1, times.length);
        running = false;
        return duration;
    }

    public double averageTimes() {
        if (recordedCount == 0) return 0;
        long total = 0;
        for (int i = 0; i < recordedCount; i++) {
            total += times[i];
        }
        return (double) total / recordedCount;
    }

    public long time(Runnable runnable) {
        start();
        runnable.run();
        return stop();
    }

    public long getLastDuration() {
        if (recordedCount == 0) return 0;
        return times[(timeIndex - 1 + times.length) % times.length];
    }

    public boolean isRunning() {
        return running;
    }
}