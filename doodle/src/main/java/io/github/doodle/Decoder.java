
package io.github.doodle;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import io.github.doodle.enums.DecodeFormat;
import io.github.doodle.enums.ClipType;

/**
 * Bitmap Decoder.
 * Decode image with BitmapFactory.
 * Decode video with MediaMetadataRetriever.
 */
final class Decoder {
    private static final String TAG = "Decoder";

    static Bitmap decodeFile(String path, Bitmap.Config config) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = config;
        if (options.inPreferredConfig == Bitmap.Config.RGB_565) {
            options.inDither = true;
        }
        return BitmapFactory.decodeFile(path, options);
    }

    static Bitmap decode(DataFetcher dataFetcher, Request request) throws IOException {
        return dataFetcher.isVideo() ? decodeVideo(dataFetcher, request) : decodeImage(dataFetcher, request);
    }

    private static Bitmap decodeImage(DataFetcher dataFetcher, Request request) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inMutable = true;
        DecodeFormat format = request.decodeFormat;
        if (format != null) {
            boolean isRGB565 = (format == DecodeFormat.RGB_565) ||
                    (format == DecodeFormat.AUTO && dataFetcher.getMediaType().noAlpha());
            options.inPreferredConfig = isRGB565 ? Bitmap.Config.RGB_565 : Bitmap.Config.ARGB_8888;
            if (isRGB565) {
                request.decodeFormat = DecodeFormat.RGB_565;
                options.inDither = true;
            }
        }
        int orientation = ExifHelper.ORIENTATION_UNDEFINED;
        boolean rotated = false;
        if (dataFetcher.possiblyExif()) {
            orientation = dataFetcher.getOrientation();
            // orientation in [5,8] means rotate 90 or 270 degrees
            rotated = orientation >= ExifHelper.ORIENTATION_TRANSPOSE;
        }

        ClipType clipType = request.clipType == ClipType.NOT_SET ? ClipType.NO_CLIP : request.clipType;
        if (clipType != ClipType.NO_CLIP) {
            options.inJustDecodeBounds = true;
            dataFetcher.decode(options);
            if (options.outWidth <= 0 || options.outHeight <= 0) {
                clipType = ClipType.NO_CLIP;
            }
            options.inJustDecodeBounds = false;
        }

        int sourceWidth = options.outWidth;
        int sourceHeight = options.outHeight;

        int targetWidth = rotated ? request.targetHeight : request.targetWidth;
        int targetHeight = rotated ? request.targetWidth : request.targetHeight;

        Bitmap bitmap;
        switch (clipType) {
            case NO_CLIP:
                bitmap = dataFetcher.decode(options);
                break;
            case MATRIX:
                if (sourceWidth > targetWidth || sourceHeight > targetHeight) {
                    int right = Math.min(sourceWidth, targetWidth);
                    int bottom = Math.min(sourceHeight, targetHeight);
                    Rect rect = new Rect(0, 0, right, bottom);
                    bitmap = dataFetcher.decodeRegion(rect, options);
                } else {
                    bitmap = dataFetcher.decode(options);
                }
                break;
            case CENTER:
                if (sourceWidth > targetWidth || sourceHeight > targetHeight) {
                    int left, top, right, bottom;
                    if (sourceWidth > targetWidth) {
                        left = Math.round((sourceWidth - targetWidth) * 0.5f);
                        right = Math.min(left + targetWidth, sourceWidth);
                    } else {
                        left = 0;
                        right = sourceWidth;
                    }
                    if (sourceHeight > targetHeight) {
                        top = Math.round((sourceHeight - targetHeight) * 0.5f);
                        bottom = Math.min(top + targetHeight, sourceHeight);
                    } else {
                        top = 0;
                        bottom = sourceHeight;
                    }
                    Rect rect = new Rect(left, top, right, bottom);
                    bitmap = dataFetcher.decodeRegion(rect, options);
                } else {
                    bitmap = dataFetcher.decode(options);
                }
                break;
            case CENTER_CROP:
                if (sourceWidth * targetHeight > targetWidth * sourceHeight) {
                    if (request.enableUpscale ? sourceHeight != targetHeight : sourceHeight > targetHeight) {
                        options.inScaled = true;
                        options.inTargetDensity = targetHeight;
                        options.inDensity = sourceHeight;
                    }
                } else {
                    if (request.enableUpscale ? sourceWidth != targetWidth : sourceWidth > targetWidth) {
                        options.inScaled = true;
                        options.inTargetDensity = targetWidth;
                        options.inDensity = sourceWidth;
                    }
                }
                bitmap = dataFetcher.decode(options);
                if (bitmap != null) {
                    bitmap = centerCrop(bitmap, targetWidth, targetHeight);
                }
                break;
            default:
                // TYPE_FIT_CENTER or TYPE_CENTER_INSIDE
                boolean centerInside = clipType == ClipType.CENTER_INSIDE;
                boolean enableUpscale = request.enableUpscale;
                if (sourceWidth * targetHeight > targetWidth * sourceHeight) {
                    if ((centerInside || !enableUpscale) ? sourceWidth > targetWidth : sourceWidth != targetWidth) {
                        options.inScaled = true;
                        options.inTargetDensity = targetWidth;
                        options.inDensity = sourceWidth;
                    }
                } else {
                    if ((centerInside || !enableUpscale) ? sourceHeight > targetHeight : sourceHeight != targetHeight) {
                        options.inScaled = true;
                        options.inTargetDensity = targetHeight;
                        options.inDensity = sourceHeight;
                    }
                }
                bitmap = dataFetcher.decode(options);
                break;
        }

        if (bitmap == null) {
            throw new IllegalArgumentException("Not support to decode the file: " + dataFetcher.getMediaType());
        }

        if (orientation > ExifHelper.ORIENTATION_NORMAL) {
            bitmap = ExifHelper.rotateImage(bitmap, orientation);
        }

        if (LogProxy.isDebug()) {
            logResult(bitmap, request, sourceWidth, sourceHeight);
        }

        return bitmap;
    }

    private static Bitmap decodeVideo(DataFetcher dataFetcher, Request request) throws IOException {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            // If the source not support by MediaMetadataRetriever,
            // it will throws an exception here (we will catch it outside)
            dataFetcher.setDataSource(retriever);

            final int targetWidth = request.targetWidth;
            final int targetHeight = request.targetHeight;
            int sourceWidth = 0;
            int sourceHeight = 0;

            ClipType clipType = request.clipType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                    && targetWidth > 0
                    && targetHeight > 0
                    && clipType != ClipType.NO_CLIP
                    && clipType != ClipType.MATRIX
                    && clipType != ClipType.CENTER
            ) {
                int orientation = 0;
                try {
                    String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    String orientationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                    if (!TextUtils.isEmpty(widthStr) &&
                            !TextUtils.isEmpty(heightStr) &&
                            !TextUtils.isEmpty(orientationStr)) {
                        sourceWidth = Integer.parseInt(widthStr);
                        sourceHeight = Integer.parseInt(heightStr);
                        orientation = Integer.parseInt(orientationStr);
                    }
                } catch (Throwable e) {
                    LogProxy.e(TAG, e);
                }
                if (sourceWidth > 0 && sourceHeight > 0) {
                    if (orientation == 90 || orientation == 270) {
                        int temp = sourceWidth;
                        //noinspection SuspiciousNameCombination
                        sourceWidth = sourceHeight;
                        sourceHeight = temp;
                    }
                    float scale = getScale(sourceWidth, sourceHeight, targetWidth, targetHeight,
                            clipType, request.enableUpscale);
                    scale = Math.min(scale, 1f);
                    int w = Math.round(scale * sourceWidth);
                    int h = Math.round(scale * sourceHeight);
                    try {
                        bitmap = retriever.getScaledFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, w, h);
                    } catch (Throwable e) {
                        LogProxy.e(TAG, e);
                    }
                }
            }
            if (bitmap == null) {
                bitmap = retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            }
            bitmap = handleScaleAndCrop(bitmap, request);
            if (LogProxy.isDebug()) {
                logResult(bitmap, request, sourceWidth, sourceHeight);
            }
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                retriever.close();
            } else {
                retriever.release();
            }
        }
        if (bitmap == null) {
            throw new IllegalArgumentException("Not support to decode the file");
        }
        return bitmap;
    }

    static Bitmap handleScaleAndCrop(Bitmap bitmap, Request request) {
        if (bitmap == null) {
            return null;
        }
        final int targetWidth = request.targetWidth;
        final int targetHeight = request.targetHeight;
        final ClipType clipType = request.clipType;
        if ((targetWidth > 0 && targetHeight > 0)
                && clipType != ClipType.NO_CLIP
                && clipType != ClipType.MATRIX
                && clipType != ClipType.CENTER) {
            final int sourceWidth = bitmap.getWidth();
            final int sourceHeight = bitmap.getHeight();
            float scale = getScale(sourceWidth, sourceHeight, targetWidth, targetHeight, clipType, request.enableUpscale);
            if (scale != 1f) {
                Matrix matrix = new Matrix();
                matrix.postScale(scale, scale);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, sourceWidth, sourceHeight, matrix, true);
            }
            if (bitmap != null && clipType == ClipType.CENTER_CROP) {
                bitmap = centerCrop(bitmap, targetWidth, targetHeight);
            }
        }
        return bitmap;
    }

    private static Bitmap centerCrop(Bitmap bitmap, int targetWidth, int targetHeight) {
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        int d = bw * targetHeight - targetWidth * bh;
        if (d > 0) {
            float dx = (bw - targetWidth * bh / (float) targetHeight) * 0.5f;
            if (dx > 0 && (2 * dx) < bw) {
                bitmap = Bitmap.createBitmap(bitmap, Math.round(dx), 0, Math.round(bw - 2 * dx), bh);
            }
        } else if (d < 0) {
            float dy = (bh - targetHeight * bw / (float) targetWidth) * 0.5f;
            if (dy > 0 && (2 * dy) < bh) {
                bitmap = Bitmap.createBitmap(bitmap, 0, Math.round(dy), bw, Math.round(bh - 2 * dy));
            }
        }
        return bitmap;
    }

    private static float getScale(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight,
                                  ClipType clipType, boolean enableUpscale) {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return 1f;
        }
        if (clipType == ClipType.CENTER_CROP) {
            if (sourceWidth * targetHeight > targetWidth * sourceHeight) {
                if (enableUpscale ? sourceHeight != targetHeight : sourceHeight > targetHeight) {
                    return (float) targetHeight / (float) sourceHeight;
                }
            } else {
                if (enableUpscale ? sourceWidth != targetWidth : sourceWidth > targetWidth) {
                    return (float) targetWidth / (float) sourceWidth;
                }
            }
        } else {
            boolean centerInside = clipType == ClipType.CENTER_INSIDE;
            if (sourceWidth * targetHeight > targetWidth * sourceHeight) {
                if ((centerInside || !enableUpscale) ? sourceWidth > targetWidth : sourceWidth != targetWidth) {
                    return (float) targetWidth / (float) sourceWidth;
                }
            } else {
                if ((centerInside || !enableUpscale) ? sourceHeight > targetHeight : sourceHeight != targetHeight) {
                    return (float) targetHeight / (float) sourceHeight;
                }
            }
        }
        return 1f;
    }

    private static void logResult(Bitmap bitmap, Request request, int sourceWidth, int sourceHeight) {
        if (bitmap != null) {
            Log.i(TAG, "source:" + sourceWidth + "x" + sourceHeight
                    + " target:" + request.targetWidth + "x" + request.targetHeight
                    + " result:" + bitmap.getWidth() + "x" + bitmap.getHeight()
            );
        }
    }
}
