package omnivoxel.util.cache;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IDCache<K, V> {
    private final Map<K, V> cache;
    private final Map<Class<?>, Constructor<?>> constructorCache = new ConcurrentHashMap<>();

    public IDCache(Map<K, V> cache) {
        this.cache = cache;
    }

    public IDCache() {
        this(new ConcurrentHashMap<>());
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    // alias for put (optional)
    public void add(K key, V value) {
        put(key, value);
    }

    public V get(K key, Class<? extends V> clazz) {
        V existing = cache.get(key);
        if (existing != null) return existing;

        if (clazz == null) return null; // no caching nulls

        Constructor<?> ctor = constructorCache.computeIfAbsent(clazz, c -> {
            try {
                Constructor<?> constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("No default constructor for " + c.getName(), e);
            }
        });

        try {
            @SuppressWarnings("unchecked")
            V instance = (V) ctor.newInstance();
            cache.put(key, instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getName(), e);
        }
    }

    public V get(K key, Class<? extends V> clazz, Class<?>[] parameterTypes, Object[] args) {
        V existing = cache.get(key);
        if (existing != null) return existing;

        Constructor<?> ctorKey = constructorCache.computeIfAbsent(
                clazz,
                ck -> {
                    try {
                        Constructor<?> c = clazz.getDeclaredConstructor(parameterTypes);
                        c.setAccessible(true);
                        return c;
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException("No matching constructor for " + clazz.getName(), e);
                    }
                });

        try {
            @SuppressWarnings("unchecked")
            V instance = (V) ctorKey.newInstance(args);
            cache.put(key, instance);
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getName() + " with args", e);
        }
    }

    // helper to differentiate parameterized constructor cache entries
    private record ConstructorKey(Class<?> clazz, Class<?>[] params) {
        private ConstructorKey(Class<?> clazz, Class<?>[] params) {
            this.clazz = clazz;
            this.params = params.clone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConstructorKey(Class<?> clazz1, Class<?>[] params1))) return false;
            if (!clazz.equals(clazz1)) return false;
            if (params.length != params1.length) return false;
            for (int i = 0; i < params.length; i++) {
                if (!params[i].equals(params1[i])) return false;
            }
            return true;
        }

    }
}