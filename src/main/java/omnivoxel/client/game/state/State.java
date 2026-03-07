package omnivoxel.client.game.state;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class State {
    private final Map<String, Object> state;

    public State() {
        state = new ConcurrentHashMap<>();
    }

    public State(boolean ignored) {
        state = new HashMap<>();
    }

    public <T> void setItem(String key, T value) {
        state.put(key, value);
    }

    public <T> T getItem(String key, Class<T> valueType) {
        Object value = state.get(key);
        if (valueType.isInstance(value)) {
            return valueType.cast(value);
        }
        if (value == null) {
            return null;
        }
        throw new IllegalArgumentException("Invalid type for key: " + key);
    }
}