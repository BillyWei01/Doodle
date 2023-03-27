package io.github.doodle.interfaces;

import java.io.InputStream;

/**
 * DataFetcher and handle kind of data,like:
 * http, file, android.resource, and media file, which can open with 'ContentResolver().openInputStream()'.
 *
 * To make Doodle be more generic, we define this interface for custom data fetching.
 */
public interface DataParser {
    /**
     * @param path data path.
     * @return InputStream, null if path not match the pattern.
     */
    InputStream parse(String path);
}
