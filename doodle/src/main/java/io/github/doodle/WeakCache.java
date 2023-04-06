package io.github.doodle;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

final class WeakCache {
    private final Map<CacheKey, ValueReference> cache = new HashMap<>();
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    synchronized Object get(CacheKey key) {
        cleanQueue();
        ValueReference reference = cache.get(key);
        return reference != null ? reference.get() : null;
    }

    synchronized void put(CacheKey key, Object value) {
        cleanQueue();
        if (value != null) {
            ValueReference ref = cache.get(key);
            if (ref == null || ref.get() != value) {
                cache.put(key, new ValueReference(key, value, queue));
            }
        }
    }

    private void cleanQueue() {
        ValueReference reference = (ValueReference) queue.poll();
        while (reference != null) {
            ValueReference ref = cache.get(reference.key);
            if (ref != null && ref.get() == null) {
                cache.remove(reference.key);
            }
            reference = (ValueReference) queue.poll();
        }
    }

    private static class ValueReference extends WeakReference<Object> {
        private final CacheKey key;

        ValueReference(CacheKey key, Object value, ReferenceQueue<Object> q) {
            super(value, q);
            this.key = key;
        }
    }
}
