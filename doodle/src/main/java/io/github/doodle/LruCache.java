
package io.github.doodle;

import android.content.ComponentCallbacks2;
import android.graphics.Bitmap;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

final class LruCache {
    private static final long MIN_TRIM_SIZE = Runtime.getRuntime().maxMemory() / 64;
    private static long sum = 0;
    private static final Map<CacheKey, BitmapWrapper> cache =
            new LinkedHashMap<>(16, 0.75f, true);

    static synchronized Bitmap get(CacheKey key) {
        BitmapWrapper wrapper = cache.get(key);
        return wrapper != null ? wrapper.bitmap : null;
    }

    static synchronized void put(CacheKey key, Bitmap bitmap) {
        long capacity = Config.memoryCacheCapacity;
        if (bitmap == null || capacity <= 0 || cache.containsKey(key)) {
            return;
        }
        BitmapWrapper wrapper = new BitmapWrapper(bitmap);
        cache.put(key, wrapper);
        sum += wrapper.bytesCount;
        if (sum > capacity) {
            trimToSize(capacity * 9 / 10);
        }
    }

    static synchronized void clearMemory() {
        trimToSize(0);
    }

    static synchronized void trimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            trimToSize(0);
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
                || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            trimToSize(Math.max(sum >> 1, MIN_TRIM_SIZE));
        }
    }

    private static void trimToSize(long size) {
        Iterator<Map.Entry<CacheKey, BitmapWrapper>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext() && sum > size) {
            Map.Entry<CacheKey, BitmapWrapper> entry = iterator.next();
            BitmapWrapper wrapper = entry.getValue();
            WeakCache.put(entry.getKey(), wrapper.bitmap);
            iterator.remove();
            sum -= wrapper.bytesCount;
        }
    }

    private static class BitmapWrapper {
        final Bitmap bitmap;
        final int bytesCount;

        BitmapWrapper(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.bytesCount = Utils.getBytesCount(bitmap);
        }
    }
}
