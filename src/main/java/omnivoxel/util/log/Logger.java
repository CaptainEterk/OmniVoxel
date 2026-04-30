package omnivoxel.util.log;

import omnivoxel.client.game.settings.ConstantGameSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public final class Logger {
    private static final Object LOCK = new Object();
    private static final Map<String, Queue<String>> INFO_LOGS = new HashMap<>();
    private static final Map<String, Queue<String>> DEBUG_LOGS = new HashMap<>();
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static boolean showLogs = true;
    private static volatile Priority minPriority = Priority.LOW;

    private Logger() {
    }

    public static void setMinPriority(Priority priority) {
        minPriority = priority;
    }

    private static boolean allowed(Priority priority) {
        return priority.ordinal() >= minPriority.ordinal();
    }

    public static void setShowLogs(boolean showLogs) {
        Logger.showLogs = showLogs;
    }

    public static void error(String message) {
        logError(source(), Priority.NORMAL, message);
    }

    public static void warn(String message) {
        logWarn(source(), Priority.NORMAL, message);
    }

    public static void debug(String message) {
        logDebug(source(), Priority.LOW, message);
    }

    public static void info(String message) {
        logInfo(source(), Priority.NORMAL, message);
    }

    public static void error(Priority priority, String message) {
        logError(source(), priority, message);
    }

    public static void warn(Priority priority, String message) {
        logWarn(source(), priority, message);
    }

    public static void debug(Priority priority, String message) {
        logDebug(source(), priority, message);
    }

    public static void info(Priority priority, String message) {
        logInfo(source(), priority, message);
    }

    public static void error(String source, String message) {
        logError(source, Priority.NORMAL, message);
    }

    public static void warn(String source, String message) {
        logWarn(source, Priority.NORMAL, message);
    }

    public static void debug(String source, String message) {
        logDebug(source, Priority.LOW, message);
    }

    public static void info(String source, String message) {
        logInfo(source, Priority.NORMAL, message);
    }

    public static void error(String source, Priority priority, String message) {
        logError(source, priority, message);
    }

    public static void warn(String source, Priority priority, String message) {
        logWarn(source, priority, message);
    }

    public static void debug(String source, Priority priority, String message) {
        logDebug(source, priority, message);
    }

    public static void info(String source, Priority priority, String message) {
        logInfo(source, priority, message);
    }

    private static void logError(String source, Priority priority, String message) {
        if (!allowed(priority)) return;

        String formatted = format(priority, message);
        if (showLogs) {
            System.err.println("[" + source + "] " + formatted);
        }
        synchronized (LOCK) {
            getQueue(INFO_LOGS, source).add(formatted);
            getQueue(DEBUG_LOGS, source).add(formatted);
            writeInfo(source);
        }
    }

    private static void logWarn(String source, Priority priority, String message) {
        if (!allowed(priority)) return;

        String formatted = format(priority, message);
        if (showLogs) {
            System.err.println("\u001B[33m[" + source + "] " + formatted + "\u001B[0m");
        }
        synchronized (LOCK) {
            getQueue(DEBUG_LOGS, source).add(formatted);
            write(source);
        }
    }

    private static void logDebug(String source, Priority priority, String message) {
        if (!allowed(priority)) return;

        String formatted = format(priority, message);
        if (showLogs) {
            System.out.println("\u001B[34m[" + source + "] " + formatted + "\u001B[0m");
        }
        synchronized (LOCK) {
            getQueue(DEBUG_LOGS, source).add(formatted);
            write(source);
        }
    }

    private static void logInfo(String source, Priority priority, String message) {
        if (!allowed(priority)) return;

        String formatted = format(priority, message);
        if (showLogs) {
            System.out.println("\u001B[32m[" + source + "] " + formatted + "\u001B[0m");
        }
        synchronized (LOCK) {
            getQueue(INFO_LOGS, source).add(formatted);
            getQueue(DEBUG_LOGS, source).add(formatted);
            writeInfo(source);
        }
    }

    private static Queue<String> getQueue(Map<String, Queue<String>> logs, String source) {
        return logs.computeIfAbsent(source, ignored -> new ArrayDeque<>());
    }

    private static void writeDebug(String source) {
        writeFile(Path.of(ConstantGameSettings.LOG_LOCATION + sanitize(source) + "_debug.log"), getQueue(DEBUG_LOGS, source));
    }

    private static void writeInfo(String source) {
        writeFile(Path.of(ConstantGameSettings.LOG_LOCATION + sanitize(source) + ".log"), getQueue(INFO_LOGS, source));
    }

    private static void write(String source) {
        writeDebug(source);
        writeInfo(source);
    }

    private static void writeFile(Path path, Queue<String> logs) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    path,
                    String.join("\n", logs),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String sanitize(String source) {
        return source.toLowerCase().replace(' ', '_');
    }

    private static String format(Priority priority, String message) {
        return "[" + priority + "] " + message;
    }

    private static String source() {
        return STACK_WALKER.walk(stream -> stream
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(clazz -> clazz != Logger.class)
                .findFirst()
                .map(Class::getSimpleName)
                .orElse(Logger.class.getSimpleName()));
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH
    }
}
