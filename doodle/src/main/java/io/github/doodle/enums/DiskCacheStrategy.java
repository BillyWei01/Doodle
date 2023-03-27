package io.github.doodle.enums;

public enum DiskCacheStrategy {
    /**
     * Not to saves data to disk cache.
     */
    NONE,

    /**
     * To save the http files to disk cache.
     * For local file, it's not necessary to make a copy.
     */
    SOURCE,

    /**
     * Saves bitmap result to disk cache.
     */
    RESULT,

    /**
     * Caches with both {@link #SOURCE} and {@link #RESULT}.
     */
    ALL;

    public final boolean savaSource() {
        return this == ALL || this == SOURCE;
    }

    public final boolean saveResult() {
        return this == ALL || this == RESULT;
    }
}
