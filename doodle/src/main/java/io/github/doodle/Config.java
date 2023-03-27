package io.github.doodle;

import android.graphics.Bitmap;

import io.github.doodle.interfaces.*;

import java.util.concurrent.*;
import java.util.*;

/**
 * Global config
 */
public final class Config {
    static String cachePath;
    static int resultMaxCount = 8192;
    static long resultCapacity = 128L << 20;
    static int sourceMaxCount = 4096;
    static long sourceCapacity = 256L << 20;
    static long memoryCacheCapacity = Runtime.getRuntime().maxMemory() / 6;
    static Bitmap.CompressFormat defaultCompressFormat;
    static HttpSourceFetcher httpSourceFetcher;
    static List<DataParser> dataParsers;
    static List<DrawableDecoder> drawableDecoders;
    static List<BitmapDecoder> bitmapDecoders;

    static final Config INSTANCE = new Config();

    private Config() {
    }

    /**
     * Set an executor for Doodle to run image loading task,
     * otherwise Doodle will create make a inner thread pool.
     * <p>
     * It's suggest to provide a large concurrency window or no-limit Executor,
     * {@link Scheduler} will do the concurrency controlling job.
     */
    public Config setExecutor(Executor executor) {
        Scheduler.setExecutor(executor);
        return this;
    }

    public Config setLogger(DLogger logger) {
        LogProxy.register(logger);
        return this;
    }

    public Config setHttpSourceFetcher(HttpSourceFetcher fetcher) {
        httpSourceFetcher = fetcher;
        return this;
    }

    public Config addDataParser(DataParser parser) {
        if (parser != null) {
            if (dataParsers == null) {
                dataParsers = new ArrayList<>(1);
            }
            dataParsers.add(parser);
        }
        return this;
    }

    public Config addDrawableDecoders(DrawableDecoder decoder) {
        if (decoder != null) {
            if (drawableDecoders == null) {
                drawableDecoders = new ArrayList<>(1);
            }
            drawableDecoders.add(decoder);
        }
        return this;
    }

    public Config addBitmapDecoders(BitmapDecoder decoder) {
        if (decoder != null) {
            if (bitmapDecoders == null) {
                bitmapDecoders = new ArrayList<>(1);
            }
            bitmapDecoders.add(decoder);
        }
        return this;
    }

    public Config setCachePath(String path) {
        cachePath = path;
        return this;
    }

    /**
     * @param maxCount Max files count to keep result cache.
     *                 If maxCount is negative or zero, it will never cache the result to disk, so does capacity.
     * @return Config
     */
    public Config setResultMaxCount(int maxCount) {
        resultMaxCount = maxCount;
        return this;
    }

    public Config setResultCapacity(long capacity) {
        if (capacity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Capacity too large");
        }
        resultCapacity = capacity;
        return this;
    }

    public Config setSourceMaxCount(int maxCount) {
        sourceMaxCount = maxCount;
        return this;
    }

    public Config setSourceCapacity(long capacity) {
        if (capacity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Capacity too large");
        }
        sourceCapacity = capacity;
        return this;
    }

    public Config setMemoryCacheCapacity(long capacity) {
        if (capacity <= (Runtime.getRuntime().maxMemory() / 2)) {
            memoryCacheCapacity = capacity;
        }
        return this;
    }

    /**
     * Set default compress format.
     * This config will be default value of {@link Request#compressFormat}.
     * You could assign the compress format by {@link Request#encodeFormat(Bitmap.CompressFormat)} for signal request.
     */
    public Config setCompressFormat(Bitmap.CompressFormat format) {
        if (format != null) {
            defaultCompressFormat = format;
        }
        return this;
    }
}
