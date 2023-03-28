package io.github.doodle;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.util.ArrayMap;

import java.lang.ref.WeakReference;
import java.util.*;

import io.github.doodle.interfaces.*;
import io.github.doodle.enums.*;

/**
 * Request, wrapper of image source, decoding parameter, loading behavior and target.
 */
public final class Request {
    private CacheKey key;

    // Source
    final String path;
    Uri uri;
    private String sourceKey;

    // Decoding parameter
    int targetWidth;
    int targetHeight;
    ClipType clipType = ClipType.NOT_SET;
    boolean enableUpscale = false;
    DecodeFormat decodeFormat = DecodeFormat.ARGB_8888;
    List<Transformation> transformations;
    boolean enableDrawable = true;
    Map<String, String> options;

    // Loading behavior
    BitmapDecoder bitmapDecoder;
    boolean onlyIfCached = false;
    MemoryCacheStrategy memoryCacheStrategy = MemoryCacheStrategy.LRU;
    DiskCacheStrategy diskCacheStrategy = DiskCacheStrategy.ALL;
    boolean keepOriginal = false;
    int placeholderId = -1;
    Drawable placeholderDrawable;
    int errorId = -1;
    Drawable errorDrawable;
    int animationId;
    Animation animation;
    int crossFadeDuration;
    boolean alwaysAnimation = false;
    int hostHash;
    Bitmap.CompressFormat compressFormat = Config.defaultCompressFormat;
    CompleteListener listener;

    // Target
    Request.Waiter waiter;
    SimpleTarget simpleTarget;
    WeakReference<ImageView> targetReference;

    WeakReference<Worker> workerReference;

    /**
     * Supported types:
     * remote file, local file, assets file, media file, and resource(raw and drawable). <br>
     * <p>
     * Especially, assets path should start with "file:///android_asset/",
     * so we can tell it's an asset file rather than a local file.
     *
     * @param path url, file path, assets path, or uri path.
     * @see DataFetcher
     */
    Request(String path) {
        if (TextUtils.isEmpty(path)) {
            this.path = "";
        } else {
            this.path = path.contains("://") ? path : ("file://" + path);
        }
    }

    /**
     * We use request key to identify bitmap(both memory cache and disk cache), {@link #path} is part of key.<br/>
     * Resource name may change in different version (rename or resource confusion). <br/>
     * For this reason we disable disk cache for loading resource image by default. <br/>
     * You could set {@link DiskCacheStrategy} if you ensured resource name not change. <br/>
     *
     * @param resID id of drawable or raw
     */
    Request(int resID) {
        path = Utils.toUriPath(resID);
        if (!path.isEmpty()) {
            uri = Uri.parse(path);
        }
        diskCacheStrategy = DiskCacheStrategy.NONE;
    }

    Request(Uri uri) {
        this.uri = uri;
        if (uri != null) {
            String scheme = uri.getScheme();
            path = scheme == null || scheme.equals("file") ? ("file://" + uri.getPath()) : uri.toString();
        } else {
            path = "";
        }
    }


    /**
     * Sometimes url contains some dynamic parameter, make url change frequently. <br>
     * To make request key stable，you can set sourceKey (remove dynamic parameter).
     * When setting sourceKey, Doodle will use sourceKey to build request key instead of using path.
     *
     * @param sourceKey the key to identify the image source
     */
    public Request sourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
        return this;
    }

    public Request override(int width, int height) {
        this.targetWidth = width;
        this.targetHeight = height;
        return this;
    }

    public Request scaleType(ImageView.ScaleType scaleType) {
        this.clipType = ClipType.mapScaleType(scaleType);
        return this;
    }

    public Request clipType(ClipType clipType) {
        this.clipType = clipType;
        return this;
    }

    /**
     * By default, enableUpscale = false, <br>
     * Doodle decodes files with down sampling and not to scale.
     * <p/>
     * For example,<br>
     * When source file's resolution is 480x400,<br>
     * target size is 600x600, scaleType='centerCrop',<br>
     * it will get a 400x400 bitmap, to attach to the target.<br>
     * But if you want to get a 600x600 bitmap, open this option.<br>
     */
    public Request enableUpscale() {
        this.enableUpscale = true;
        return this;
    }

    /**
     * @see MemoryCacheStrategy
     */
    public Request memoryCacheStrategy(MemoryCacheStrategy strategy) {
        this.memoryCacheStrategy = strategy;
        return this;
    }

    /**
     * @see DiskCacheStrategy
     */
    public Request diskCacheStrategy(DiskCacheStrategy strategy) {
        this.diskCacheStrategy = strategy;
        return this;
    }

    /**
     * Not to save bitmap to memory or disk.
     *
     * @see #memoryCacheStrategy(MemoryCacheStrategy)
     * @see #diskCacheStrategy(DiskCacheStrategy)
     */
    public Request noCache() {
        this.memoryCacheStrategy = MemoryCacheStrategy.NONE;
        this.diskCacheStrategy = DiskCacheStrategy.NONE;
        return this;
    }

    /**
     * Network control. <br>
     * If set it true, Doodle will just check if cache has the photo,
     * and will not access network to download the photo.
     *
     * @param onlyIfCached Only try to get data from source cache if set it true.
     */
    public Request onlyIfCached(boolean onlyIfCached) {
        this.onlyIfCached = onlyIfCached;
        return this;
    }

    /**
     * Set decoding format, to decide the request to use which {@link Bitmap.Config}.
     *
     * @param format decoding format.
     * @return Request
     */
    public Request decodeFormat(DecodeFormat format) {
        if (format != null) {
            this.decodeFormat = format;
        }
        return this;
    }

    /**
     * Set Bitmap.CompressFormat for caching result bitmap.
     * The default value of compressFormat is assigned by {@link Config#defaultCompressFormat}.
     * If both compressFormat of global config and request is null,
     * Doodle will make compression strategy by source type and bitmap config.
     *
     * @param format Bitmap.CompressFormat
     * @return Request
     */
    public Request encodeFormat(Bitmap.CompressFormat format) {
        this.compressFormat = format;
        return this;
    }

    public Request transform(Transformation transformation) {
        if (transformation == null) {
            throw new IllegalArgumentException("Transformation can not be null.");
        }
        if (transformation.key() == null) {
            throw new IllegalArgumentException("Transformation key can not be null.");
        }
        if (transformations == null) {
            transformations = new ArrayList<>(2);
        }
        transformations.add(transformation);
        return this;
    }

    /**
     * Keep original drawable in ImageView(target) util we get the result of request.
     */
    public Request keepOriginalDrawable(boolean keep) {
        keepOriginal = keep;
        return this;
    }

    public Request placeholder(int placeholderId) {
        this.placeholderId = placeholderId;
        return this;
    }

    public Request placeholder(Drawable drawable) {
        this.placeholderDrawable = drawable;
        return this;
    }

    public Request error(int errorId) {
        this.errorId = errorId;
        return this;
    }

    public Request error(Drawable drawable) {
        this.errorDrawable = drawable;
        return this;
    }

    public Request animation(int animationId) {
        this.animationId = animationId;
        return this;
    }

    public Request animation(Animation animation) {
        this.animation = animation;
        return this;
    }

    public Request fadeIn() {
        return fadeIn(300);
    }

    public Request fadeIn(int duration) {
        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
        animation.setDuration(duration);
        this.animation = animation;
        return this;
    }

    public Request crossFade() {
        return crossFade(300);
    }

    public Request crossFade(int duration) {
        this.crossFadeDuration = duration;
        return this;
    }

    /**
     * By default, when the request set animation,
     * only the bitmap is just decoding from disk or network will do the animation. <br>
     * If set 'alwaysAnimation' be true,
     * do animation no matter bitmap is from cache or brand-new from decoding.
     */
    public Request alwaysAnimation(boolean alwaysAnimation) {
        this.alwaysAnimation = alwaysAnimation;
        return this;
    }

    /**
     * If there are {@link io.github.doodle.interfaces.DrawableDecoder} in {@link Config#drawableDecoders},
     * Doodle will call the DrawableDecoder to at first to see if the source file could be decode into Drawable.
     * DrawableDecoder may return some AnimatedDrawable if the format of file is gif or animated webp.
     * <p>
     * If you want bitmap only, you could call this method,
     * then not matter source file is gif or other format, Doodle will decode the file to a bitmap.
     */
    public Request asBitmap() {
        this.enableDrawable = false;
        return this;
    }

    public Request asBitmap(boolean asBitmap) {
        this.enableDrawable = !asBitmap;
        return this;
    }

    /**
     * If host is Activity and target is ImageView,
     * it's not necessary to call this,
     * because Doodle will pick the activity automatically by {@link Utils#pickActivity}
     * <p>
     * This method has a little like Glide's "with",
     * what different is that this method is optional.
     * <p>
     * For Fragment, you could use this to observe it's lifecycle.<br>
     * If the fragment will destroy only when the relative activity destroy, it's unnecessary to call this.
     * <p>
     * You can also call this on Dialog or View,
     * And call {@link Doodle#notifyDestroy(Object)} when the dialog dismiss or the view detech.
     *
     * @param host component with lifecycle, like Activity/Fragment/Dialog/View.
     * @see LifecycleManager
     * @see Doodle#notifyDestroy(Object)
     */
    public Request observeHost(Object host) {
        // Only mark host's identityHashCode, not the reference,
        // no need to worry about memory leak.
        this.hostHash = System.identityHashCode(host);
        return this;
    }

    /**
     * Set options, for BitmapDecoder/DrawableDecoder.
     * BitmapDecoder/DrawableDecoder intercept every request.
     * You could use the options to filter the requests, or to pass parameters.
     */
    public Request addOption(String key, String value) {
        if (options == null) {
            options = new ArrayMap<>();
        }
        options.put(key, value);
        return this;
    }

    /**
     * Request handle DrawableDecoder at first if {@link #enableDrawable} is true.
     * You could call this method if you need to handle the image file by a specify BitmapDecoder.
     */
    public Request setBitmapDecoder(BitmapDecoder decoder){
        bitmapDecoder = decoder;
        return this;
    }

    /**
     * Only effect on requests which path starts with "content://media/"
     */
    public Request enableThumbnailDecoder() {
        addOption(MediaThumbnailDecoder.KEY, "");
        return setBitmapDecoder(MediaThumbnailDecoder.INSTANCE);
    }

    /**
     * preload the bitmap. <br/>
     * assign sizes with {@link #override}, otherwise it will load with original size.
     */
    public void preload() {
        fillSizeAndLoad(targetWidth, targetHeight);
    }

    /**
     * Get the bitmap on current thread within 3000 millis.
     * <p>
     * {@link #get(long)}
     */
    public Bitmap get() {
        return get(3000L);
    }

    /**
     * get the bitmap on current thread. <br>
     * assign sizes with {@link #override}, otherwise it will load with original size.
     * <p>
     * It's recommended to call this method in background thread. <br/>
     * But it's also ok to call this in main thread if you have to do so,
     * in that way, set a short timeout in case of blocking UI or ANR.
     *
     * @param millis timeout for getting bitmap.
     *               <li>timeout = 0, just try to get the bitmap from memory</li>
     *               <li>timeout > 0, wait until get the bitmap or out of time</li>
     *               <li>timeout < 0, throw IllegalArgumentException</li>
     * @return Return bitmap if the request hit cache or decode correctly, return null if error occur or out of time.
     * @throws IllegalArgumentException if timeout is negative
     */
    public Bitmap get(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Timeout can't be negative");
        }
        enableDrawable = false;
        this.waiter = new Request.Waiter(millis);
        fillSizeAndLoad(targetWidth, targetHeight);
        return waiter.result;
    }

    /**
     * get bitmap/drawable by SimpleTarget
     *
     * @see SimpleTarget
     */
    public void into(SimpleTarget target) {
        this.simpleTarget = target;
        fillSizeAndLoad(targetWidth, targetHeight);
    }

    /**
     * Listen if success to get result.
     * <p>
     * Only callback when the target is ImageView,
     * Invoke after updating target(ImageView).
     *
     * @param listener RequestListener
     */
    public Request listen(CompleteListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * Load bitmap into ImageView.
     *
     * @param target ImageView
     */
    public void into(ImageView target) {
        if (target == null) {
            return;
        }
        targetReference = new WeakReference<>(target);

        if (clipType == ClipType.NOT_SET) {
            clipType = ClipType.mapScaleType(target.getScaleType());
        }

        if (clipType == ClipType.NO_CLIP) {
            fillSizeAndLoad(0, 0);
        } else if (targetWidth > 0 && targetHeight > 0) {
            fillSizeAndLoad(targetWidth, targetHeight);
        } else if (target.getWidth() > 0 && target.getHeight() > 0) {
            fillSizeAndLoad(target.getWidth(), target.getHeight());
        } else if (isParamsValid(target.getLayoutParams())) {
            ViewGroup.LayoutParams params = target.getLayoutParams();
            int pw = params.width;
            int ph = params.height;
            // If both width and height is wrap_content, load with original size
            if (pw < 0 && ph < 0) {
                fillSizeAndLoad(0, 0);
            } else {
                int w = pw > 0 ? pw : Utils.getDisplayDimens().x;
                int h = ph > 0 ? ph : Utils.getDisplayDimens().y;
                fillSizeAndLoad(w, h);
            }
        } else if (target.getWindowToken() != null) {
            fillSizeAndLoad(0, 0);
        } else {
            target.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            ImageView view = targetReference.get();
                            if (view == null) {
                                return true;
                            }
                            ViewTreeObserver vto = view.getViewTreeObserver();
                            if (vto.isAlive()) {
                                vto.removeOnPreDrawListener(this);
                            }
                            fillSizeAndLoad(view.getWidth(), view.getHeight());
                            return true;
                        }
                    });
        }
    }

    private static boolean isParamsValid(ViewGroup.LayoutParams params) {
        return params != null
                && (params.width > 0 || params.width == ViewGroup.LayoutParams.WRAP_CONTENT)
                && (params.height > 0 || params.height == ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void fillSizeAndLoad(int width, int height) {
        targetWidth = width;
        targetHeight = height;

        // align params
        if (clipType == ClipType.NOT_SET && width > 0 && height > 0) {
            clipType = ClipType.CENTER_INSIDE;
        }
        if (clipType == ClipType.NOT_SET || clipType == ClipType.NO_CLIP || width <= 0 || height <= 0) {
            clipType = ClipType.NO_CLIP;
            targetWidth = 0;
            targetHeight = 0;
        }

        if (targetReference != null) {
            ImageView target = targetReference.get();
            if (target != null && clipType != ClipType.NO_CLIP) {
                int horizonPadding = target.getPaddingLeft() + target.getPaddingRight();
                int verticalPadding = target.getPaddingTop() + target.getPaddingBottom();
                if (targetWidth > horizonPadding) {
                    targetWidth -= horizonPadding;
                }
                if (targetHeight > verticalPadding) {
                    targetHeight -= verticalPadding;
                }
            }
        }

        Controller.start(this);
    }

    CacheKey getKey() {
        if (key == null) {
            key = buildKey();
        }
        return key;
    }

    private CacheKey buildKey() {
        StringBuilder builder = new StringBuilder(128);
        builder.append(TextUtils.isEmpty(sourceKey) ? path : sourceKey)
                .append(':').append('s').append(targetWidth).append('x').append(targetHeight)
                .append(':').append('c').append(clipType.nativeChar)
                .append(':').append('f').append(decodeFormat.nativeChar)
                .append(':').append('u').append(enableUpscale ? '1' : '0')
                .append(':').append('d').append(enableDrawable ? '1' : '0');
        if (transformations != null && !transformations.isEmpty()) {
            builder.append(':').append('t');
            for (Transformation transformation : transformations) {
                builder.append(',').append(transformation.key());
            }
        }
        if (options != null) {
            builder.append(':').append('o');
            for (Map.Entry<String, String> entry : options.entrySet()) {
                builder.append(',').append(entry.getKey()).append('|').append(entry.getValue());
            }
        }
        return new CacheKey(builder.toString());
    }

    /**
     * Wrapper for getting bitmap synchronously
     */
    static class Waiter {
        Bitmap result;
        long timeout;

        Waiter(long timeout) {
            this.timeout = timeout;
        }
    }
}
