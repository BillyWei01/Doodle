
package io.github.doodle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.concurrent.FutureTask;
import io.github.doodle.interfaces.HttpSourceFetcher;

/**
 * Provider api to download files and manage the file caches.
 */
final class Downloader {
    static final DiskCache sourceCache = new DiskCache("/doodle/source/",
            Config.sourceMaxCount, Config.sourceCapacity
    );

    private static InputStream getStream(String url, int count) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        int statusCode = connection.getResponseCode();
        if (statusCode >= 200 && statusCode < 300) {
            return connection.getInputStream();
        } else if (statusCode >= 300 && statusCode < 400) {
            if (count > 5) {
                connection.disconnect();
                throw new IOException("Redirect too much");
            }
            String redirectUrl = connection.getHeaderField("Location");
            connection.disconnect();
            if (redirectUrl == null || redirectUrl.isEmpty()) {
                throw new IOException("Empty redirect url");
            }
            if (url.equals(redirectUrl)) {
                throw new IOException("Redirect loop");
            }
            return getStream(redirectUrl, count + 1);
        }
        throw new IOException("Request failed, status code:" + statusCode);
    }

    static InputStream getInputStream(String url) throws IOException {
        HttpSourceFetcher fetcher = Config.httpSourceFetcher;
        if (fetcher != null) {
            InputStream inputStream = fetcher.getInputStream(url);
            if (inputStream != null) {
                return inputStream;
            }
        }
        return getStream(url, 1);
    }

    static String getCachePath(CacheKey key) {
        String path = sourceCache.getPath(key);
        if (path != null && new File(path).exists()) {
            return path;
        } else {
            sourceCache.delete(key);
            return null;
        }
    }

    static File download(String url, CacheKey key) throws IOException {
        return streamToFile(getInputStream(url), key,true);
    }

    static File downloadTemporary(InputStream inputStream, String url) throws IOException {
        return streamToFile(inputStream, new CacheKey(url),  false);
    }

    private static File streamToFile(InputStream inputStream, CacheKey key, boolean needCache) throws IOException {
        File desFile = new File(sourceCache.keyToPath(key));
        if (desFile.exists()) {
            return desFile;
        }
        // Downloading job uses time on waiting IO much more than CPU computing,
        // so extend window size to increase the concurrency.
        Scheduler.pipeExecutor.extendWindow();
        File tmpFile = null;
        try {
            tmpFile = new File(desFile.getParent(), desFile.getName() + ".tmp");
            if (Utils.streamToFile(inputStream, tmpFile)) {
                if (!needCache) {
                    return tmpFile;
                }
                if (tmpFile.renameTo(desFile) || desFile.exists()) {
                    sourceCache.record(key, desFile, false);
                    return desFile;
                }
            }
            throw new IOException("Download failed");
        } finally {
            Scheduler.pipeExecutor.reduceWindow();
            if (needCache) {
                Utils.deleteQuietly(tmpFile);
            }
        }
    }

    static File downloadOnly(String url) {
        CacheKey key = new CacheKey(url);
        FutureTask<File> future = new FutureTask<>(() -> download(url, key));
        Scheduler.tagExecutor.execute(key, future);
        try {
            return future.get();
        } catch (Throwable e) {
            LogProxy.e("Doodle", e);
        }
        return null;
    }
}
