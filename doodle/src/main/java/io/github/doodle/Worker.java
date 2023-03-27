package io.github.doodle;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;
import android.graphics.drawable.BitmapDrawable;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.doodle.interfaces.*;
import io.github.doodle.enums.*;

final class Worker extends ExAsyncTask {
    private static final String TAG = "Worker";

    private static final DiskCache resultCache = new DiskCache("/doodle/result/",
            Config.resultMaxCount, Config.resultCapacity
    );

    private final Request request;

    private boolean fromMemory = false;
    private boolean fromResultCache = false;

    private long time;
    private static final AtomicInteger count = new AtomicInteger();

    Worker(Request request, ImageView imageView) {
        this.request = request;
        if (LogProxy.isDebug()) {
            Log.d(TAG, "Loading start, count:" + count.incrementAndGet());
        }
        if (imageView != null) {
            request.workerReference = new WeakReference<>(this);
            imageView.setTag(R.id.doodle_view_tag, request);
        }
    }

    @Override
    protected CacheKey generateTag() {
        if (request.path.startsWith("http")) {
            return new CacheKey(request.path);
        } else {
            return request.getKey();
        }
    }

    @Override
    protected Object doInBackground() {
        long startTime = System.nanoTime();
        Bitmap bitmap;
        DecodingInfo decodingInfo = null;
        CacheKey key = request.getKey();
        try {
            if (request.targetReference != null && getTarget() == null) {
                // Target missed or changed request
                return null;
            }

            // Try to get bitmap from cache
            bitmap = MemoryCache.getBitmap(key);
            if (bitmap != null) {
                fromMemory = true;
                return bitmap;
            }
            MemoryCache.checkMemory();
            DiskCache.CacheInfo cacheInfo = resultCache.getCacheInfo(key);
            fromResultCache = cacheInfo != null;
            if (fromResultCache) {
                bitmap = decodeResultCache(cacheInfo);
            }

            // Decode
            if (bitmap == null) {
                decodingInfo = new DecodingInfo(request);
                if (request.bitmapDecoder != null) {
                    bitmap = request.bitmapDecoder.decode(decodingInfo);
                    bitmap = Decoder.handleScaleAndCrop(bitmap, request);
                }
                if (bitmap == null && request.enableDrawable && Config.drawableDecoders != null) {
                    Object result = tryDrawableDecoders(decodingInfo);
                    if (result != null) {
                        if (result instanceof Bitmap) {
                            bitmap = (Bitmap) result;
                        } else {
                            return result;
                        }
                    }
                }
                if (bitmap == null && Config.bitmapDecoders != null) {
                    bitmap = tryBitmapDecoders(decodingInfo);
                }
                if (bitmap == null) {
                    bitmap = Decoder.decode(decodingInfo.getDataFetcher(), request);
                }
            }
            if (bitmap != null && !fromResultCache) {
                bitmap = transform(request, bitmap);
            }

            // Save bitmap to cache
            if (bitmap != null) {
                if (request.memoryCacheStrategy != MemoryCacheStrategy.NONE) {
                    boolean toWeakCache = request.memoryCacheStrategy == MemoryCacheStrategy.WEAK;
                    MemoryCache.putBitmap(key, bitmap, toWeakCache);
                }
                if (!fromResultCache && request.diskCacheStrategy.saveResult()) {
                    storeResult(request, bitmap, decodingInfo != null ? decodingInfo.dataFetcher : null);
                }
            }

            return bitmap;
        } catch (InterruptedIOException e) {
            // Interrupted by cancel, or timeout.
            if (LogProxy.isDebug()) {
                Log.d(TAG, "Interrupted reason: " + e.getClass().getSimpleName());
            }
        } catch (Throwable e) {
            DataFetcher dataFetcher = decodingInfo != null ? decodingInfo.dataFetcher : null;
            if (dataFetcher != null && dataFetcher.fromSourceCache) {
                Downloader.sourceCache.delete(key);
            }
            LogProxy.e(TAG, new Exception("Load bitmap failed, path: " + request.path, e));
        } finally {
            if (decodingInfo != null) {
                Utils.closeQuietly(decodingInfo.dataFetcher);
            }
            time = (System.nanoTime() - startTime) / 1000000;
        }
        return null;
    }

    private Bitmap decodeResultCache(DiskCache.CacheInfo cacheInfo) {
        Bitmap bitmap = null;
        try {
            bitmap = Decoder.decodeFile(cacheInfo.path, cacheInfo.isRGB565
                    ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888);
        } catch (Throwable e) {
            LogProxy.e(TAG, e);
        }
        if (bitmap == null) {
            fromResultCache = false;
            resultCache.delete(request.getKey());
        }
        return bitmap;
    }

    private Object tryDrawableDecoders(DecodingInfo decodingInfo) {
        for (DrawableDecoder decoder : Config.drawableDecoders) {
            Drawable drawable = decoder.decode(decodingInfo);
            if (drawable != null) {
                if (drawable instanceof BitmapDrawable) {
                    // Extract the bitmap for caching, to speed up next loading
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                    return Decoder.handleScaleAndCrop(bitmap, request);
                } else {
                    return drawable;
                }
            }
        }
        return null;
    }

    private Bitmap tryBitmapDecoders(DecodingInfo decodingInfo) {
        for (BitmapDecoder decoder : Config.bitmapDecoders) {
            Bitmap bitmap = decoder.decode(decodingInfo);
            if (bitmap != null) {
                return Decoder.handleScaleAndCrop(bitmap, request);
            }
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        logStatus("cancel");
        ImageView imageView = getTarget();
        clearImageView(imageView);
        request.simpleTarget = null;
        request.listener = null;
        request.targetReference = null;
        request.workerReference = null;
    }

    @Override
    protected void onPostExecute(Object result) {
        logStatus("finish");
        ImageView imageView = getTarget();
        clearImageView(imageView);
        Controller.setResult(request, imageView, result, fromMemory);
    }

    private void clearImageView(ImageView imageView) {
        if (imageView != null) {
            imageView.setTag(R.id.doodle_view_tag, null);
            Controller.stopAnimDrawable(imageView);
        }
    }

    private void logStatus(String state) {
        if (LogProxy.isDebug()) {
            Log.d(TAG, "Loading " + state + ", path:" + request.path
                    + ", time:" + time + "ms, remain:" + count.decrementAndGet());
        }
    }

    private ImageView getTarget() {
        WeakReference<ImageView> targetRef = request.targetReference;
        if (targetRef != null) {
            ImageView imageView = targetRef.get();
            if (imageView != null) {
                if (imageView.getTag(R.id.doodle_view_tag) == request) {
                    return imageView;
                }
            }
        }
        return null;
    }

    private Bitmap transform(Request request, Bitmap source) {
        Bitmap output = source;
        if (output != null && !fromResultCache && (request.transformations != null)) {
            for (Transformation transformation : request.transformations) {
                output = transformation.transform(output);
                if (output == null) {
                    throw new IllegalArgumentException("Failed to transform with:" + transformation.getClass());
                }
            }
        }
        return output;
    }

    private static void storeResult(Request request, Bitmap bitmap, DataFetcher dataFetcher) {
        CacheKey key = request.getKey();
        boolean isRGB565 = request.decodeFormat == DecodeFormat.RGB_565;
        Bitmap.CompressFormat compressFormat = request.compressFormat;
        if (compressFormat == null) {
            MediaType type;
            try {
                // If the bitmap is RGB565, the bitmap is lossy, it's unnecessary to get the type.
                type = isRGB565 || dataFetcher == null ? MediaType.UNKNOWN : dataFetcher.getMediaType();
            } catch (Throwable ignore) {
                type = MediaType.UNKNOWN;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                compressFormat = (isRGB565 || type.isLossy() || type.isVideo()) ?
                        Bitmap.CompressFormat.WEBP_LOSSY : Bitmap.CompressFormat.WEBP_LOSSLESS;
            } else {
                compressFormat = (isRGB565 || (type.noAlpha() || type.isVideo()))
                        ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.PNG;
            }
        }
        final Bitmap.CompressFormat format = compressFormat;
        Scheduler.storageExecutor.execute(() -> saveResult(key, bitmap, format, isRGB565));
    }

    private static void saveResult(CacheKey key, Bitmap bitmap, Bitmap.CompressFormat format, boolean isRGB565) {
        try {
            if (resultCache.needToSave(key)) {
                String path = resultCache.keyToPath(key);
                File tmpFile = new File(path + ".tmp");
                if (Utils.makeFileIfNotExist(tmpFile)) {
                    saveBitmap(tmpFile, bitmap, format);
                    File file = new File(path);
                    if (file.exists()) {
                        // should not happen
                        Utils.deleteQuietly(file);
                    }
                    if (tmpFile.renameTo(file)) {
                        resultCache.record(key, file, isRGB565);
                    }
                }
            }
        } catch (Throwable e) {
            LogProxy.e(TAG, e);
        }
    }

    private static void saveBitmap(File file, Bitmap bitmap, Bitmap.CompressFormat format) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        try {
            int quality = format == Bitmap.CompressFormat.WEBP ? 100 : 95;
            bitmap.compress(format, quality, out);
            out.flush();
        } finally {
            Utils.closeQuietly(out);
        }
    }
}
