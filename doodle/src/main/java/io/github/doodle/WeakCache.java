package io.github.doodle;

import android.graphics.Bitmap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

final class WeakCache {
    private static final Map<CacheKey, BitmapReference> cache = new HashMap<>();
    private static final ReferenceQueue<Bitmap> queue = new ReferenceQueue<>();

    static synchronized Bitmap get(CacheKey key) {
        cleanQueue();
        BitmapReference ref = cache.get(key);
        return ref != null ? ref.get() : null;
    }

    static synchronized void put(CacheKey key, Bitmap bitmap) {
        cleanQueue();
        if (bitmap != null) {
            BitmapReference ref = cache.get(key);
            if (ref == null || ref.get() != bitmap) {
                cache.put(key, new BitmapReference(key, bitmap, queue));
            }
        }
    }

    private static void cleanQueue() {
        BitmapReference reference = (BitmapReference) queue.poll();
        while (reference != null) {
            BitmapReference ref = cache.get(reference.key);
            if (ref != null && ref.get() == null) {
                cache.remove(reference.key);
            }
            reference = (BitmapReference) queue.poll();
        }
    }

    private static class BitmapReference extends WeakReference<Bitmap> {
        private final CacheKey key;

        BitmapReference(CacheKey key, Bitmap bitmap, ReferenceQueue<Bitmap> q) {
            super(bitmap, q);
            this.key = key;
        }
    }
}
