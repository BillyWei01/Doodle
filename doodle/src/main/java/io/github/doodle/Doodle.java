
package io.github.doodle;

import android.graphics.Bitmap;
import android.net.Uri;

import io.github.doodle.LifecycleManager.Event;

import java.io.File;

public final class Doodle {
    /**
     * Global config
     */
    public static Config config() {
        return Config.INSTANCE;
    }

    /**
     * Load bitmap by file path, url, or asserts path.
     *
     * @param path image path
     */
    public static Request load(String path) {
        return new Request(path);
    }

    /**
     * Load bitmap by file.
     *
     * @param file File
     */
    public static Request load(File file) {
        return new Request(file != null ? file.getPath() : "");
    }

    /**
     * Load bitmap from drawable or raw resource.
     *
     * @param resID drawable id or raw id
     */
    public static Request load(int resID) {
        return new Request(resID);
    }

    public static Request load(Uri uri) {
        return new Request(uri);
    }

    /**
     * Cache bitmap to {@link WeakCache},
     * (with a map of WeakReference)
     *
     * @param tag    identify the bitmap
     * @param bitmap bitmap
     */
    public static void cacheBitmap(String tag, Bitmap bitmap) {
        cacheBitmap(tag, bitmap, true);
    }

    /**
     * @param tag         identify the bitmap
     * @param bitmap      bitmap
     * @param toWeakCache cache to {@link WeakCache} if true,
     *                    otherwise cache to {@link LruCache}
     */
    public static void cacheBitmap(String tag, Bitmap bitmap, boolean toWeakCache) {
        CacheKey key = new CacheKey(tag);
        MemoryCache.putBitmap(key, bitmap, toWeakCache);
    }

    public static Bitmap getCacheBitmap(String tag) {
        return MemoryCache.getBitmap(new CacheKey(tag));
    }

    /**
     * This method should call on worker thread.
     * Note: The file will be cache if download success.
     *
     * @param url file url
     * @return Return file if exist or download success
     */
    public static File downloadOnly(String url) {
        return Downloader.downloadOnly(url);
    }

    /**
     * To get the cache file (if exist).
     * This method could call on main thread.
     *
     * @param url file url
     * @return Return cache file if exist, otherwise return null.
     */
    public static File getCacheFile(String url) {
        String path = Downloader.getCachePath(new CacheKey(url));
        return path != null ? new File(path) : null;
    }

    /**
     * Stop to put requests to {@link Worker}
     */
    public static void pauseRequests() {
        Controller.pause();
    }

    /**
     * Resume requests
     */
    public static void resumeRequests() {
        Controller.resume();
    }

    /**
     * It's suggest to call this in the callback of {@link android.app.Application#onTrimMemory(int)}
     */
    public static void trimMemory(int level) {
        LruCache.trimMemory(level);
    }

    public static void clearMemory() {
        LruCache.clearMemory();
    }

    public static void notifyPause(Object host) {
        LifecycleManager.notify(host, Event.PAUSE);
    }

    public static void notifyResume(Object host) {
        LifecycleManager.notify(host, Event.RESUME);
    }

    public static void notifyDestroy(Object host) {
        LifecycleManager.notify(host, Event.DESTROY);
    }
}
