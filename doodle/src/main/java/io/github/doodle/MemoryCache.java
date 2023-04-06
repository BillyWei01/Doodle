package io.github.doodle;

import android.content.ComponentCallbacks2;
import android.graphics.Bitmap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * manager of {@link LruCache} and {@link WeakCache},
 * gather some common operation.
 */
final class MemoryCache {
    private static final long MAX_MEMORY = Runtime.getRuntime().maxMemory();
    private static final long LOW_MEMORY = 10 << 20;
    private static final long CRITICAL_MEMORY = 4 << 20;

    private static final AtomicBoolean FLAG = new AtomicBoolean(false);

    static final WeakCache bitmapWeakCache = new WeakCache();
    static final WeakCache resultWeakCache = new WeakCache();

    static Bitmap getBitmap(CacheKey key) {
        Bitmap bitmap = LruCache.get(key);
        if (bitmap == null) {
            bitmap = (Bitmap) bitmapWeakCache.get(key);
        }
        return bitmap;
    }

    static void putBitmap(CacheKey key, Bitmap bitmap, boolean toWeakCache) {
        if (toWeakCache) {
            bitmapWeakCache.put(key, bitmap);
        } else {
            LruCache.put(key, bitmap);
        }
    }

    static void checkMemory() {
        if (FLAG.compareAndSet(false, true)) {
            Runtime runtime = Runtime.getRuntime();
            long remaining = MAX_MEMORY - runtime.totalMemory() + runtime.freeMemory();
            if (remaining < CRITICAL_MEMORY) {
                LruCache.clearMemory();
            } else if (remaining < LOW_MEMORY) {
                LruCache.trimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND);
            }
            FLAG.set(false);
        }
    }
}
