package omnivoxel.util.config;

import java.io.IOException;
import java.util.Map;

public final class Config {
    private final ConfigFile configFile;
    private final Map<String, Object> properties;

    public Config(String file) {
        this.configFile = new ConfigFile(file);
        properties = configFile.load();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }

    public String get(String key) {
        return properties.get(key).toString();
    }

    public boolean has(String key) {
        return properties.containsKey(key);
    }

    public void set(String key, Object value) {
        properties.put(key, value);
    }

    public void save() {
        try {
            configFile.write(properties);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public void remove(String key) {
        properties.remove(key);
    }

    public void clear() {
        properties.clear();
    }
}