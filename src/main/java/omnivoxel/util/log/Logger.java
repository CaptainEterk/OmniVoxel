package omnivoxel.util.log;

import omnivoxel.client.game.settings.ConstantGameSettings;
import omnivoxel.util.time.Timer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class Logger {
    private final String logName;
    private final boolean showLogs;
    private final Queue<String> logs;
    private final Queue<String> debugLogs;
    private final Map<String, Timer> timers;

    public Logger(String logName, boolean showLogs) {
        this.logName = logName;
        this.showLogs = showLogs;
        this.logs = new ArrayDeque<>();
        this.debugLogs = new ArrayDeque<>();
        timers = new HashMap<>();
    }

    public void init() {
        Path path = Path.of(ConstantGameSettings.LOG_LOCATION + logName() + ".log");
        Path debugPath = Path.of(ConstantGameSettings.LOG_LOCATION + logName() + "_debug.log");
        try {
            Files.createFile(path);
            Files.createFile(debugPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void error(String error) {
        if (showLogs) {
            System.err.println("[" + logName + "] " + error);
        }
        logs.add(error);
        debugLogs.add(error);
        try {
            writeInfo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void warn(String warn) {
        if (showLogs) {
            System.err.println("\u001B[33m" + "[" + logName + "] " + warn + "\u001B[0m");
        }
        debugLogs.add(warn);
        try {
            write();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void debug(String debug) {
        if (showLogs) {
            System.out.println("\u001B[34m" + "[" + logName + "] " + debug + "\u001B[0m");
        }
        debugLogs.add(debug);
        try {
            write();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void info(String info) {
        if (showLogs) {
            System.out.println("\u001B[32m" + "[" + logName + "] " + info + "\u001B[0m");
        }
        logs.add(info);
        debugLogs.add(info);
        try {
            writeInfo();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Timer getTimer(String name) {
        return timers.get(name);
    }

    public void addTimer(String name, Timer timer) {
        timers.put(name, timer);
    }

    public void writeDebug() throws IOException {
        Path debugPath = Path.of(ConstantGameSettings.LOG_LOCATION + logName() + "_debug.log");
        Files.write(debugPath, String.join("\n", debugLogs).getBytes());
    }

    public void writeInfo() throws IOException {
        Path infoPath = Path.of(ConstantGameSettings.LOG_LOCATION + logName() + ".log");
        Files.write(infoPath, String.join("\n", logs).getBytes());
    }

    private String logName() {
        return logName.toLowerCase().replace(' ', '_');
    }

    public void write() throws IOException {
        writeDebug();
        writeInfo();
    }
}