package io.github.doodle.enums;

public enum MemoryCacheStrategy {
    /**
     * Saves no data to memory cache
     */
    NONE,

    /**
     * Just save WeakCache
     */
    WEAK,

    /**
     * Save to LruCache first.
     * when LruCache is out of capacity, or trimMemory,
     * some of them will move to WeakCache
     */
    LRU
}
