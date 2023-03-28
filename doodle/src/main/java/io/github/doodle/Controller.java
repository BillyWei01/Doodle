package io.github.doodle;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.LinkedHashMap;

/**
 * Task Controller.
 * Handle start, pause, resume, and result feedback of tasks.
 * The bridge of requests, targets and background workers.
 */
final class Controller {
    private static final String TAG = "Controller";

    private static volatile boolean pauseFlag = false;
    private static final LinkedHashMap<ImageView, Request> requests = new LinkedHashMap<>();

    private static final AttachListener attachListener = new AttachListener();

    static void pause() {
        pauseFlag = true;
    }

    static void resume() {
        if (pauseFlag) {
            pauseFlag = false;
            awakeRequests();
        }
    }

    private static synchronized void awakeRequests() {
        for (Request request : requests.values()) {
            start(request);
        }
        requests.clear();
    }

    private static synchronized void cacheRequest(Request request, ImageView target) {
        // If the target had paused request before, cover it with new request.
        // That's useful for RecycleView scrolling.
        requests.put(target, request);
    }

    /**
     * @return true if the ImageView has already bound a same request.
     */
    private static boolean checkTag(Request request, ImageView imageView) {
        Object tag = imageView.getTag(R.id.doodle_view_tag);
        if (tag instanceof Request) {
            Request preRequest = (Request) tag;
            if (request.getKey().equals(preRequest.getKey())) {
                return true;
            } else {
                WeakReference<ImageView> targetRef = preRequest.targetReference;
                if (targetRef != null) {
                    stopAnimDrawable(targetRef.get());
                    preRequest.targetReference = null;
                }
                // If tag not equal, cancel previous request which binds to imageView.
                WeakReference<Worker> workerRef = preRequest.workerReference;
                if (workerRef != null) {
                    preRequest.workerReference = null;
                    Worker preTask = workerRef.get();
                    if (preTask != null && !preTask.isCancelled()) {
                        preTask.cancel(true);
                    }
                }
            }
        } else if (tag != null) {
            // Should not be here
            throw new IllegalArgumentException("Invalid tag type");
        }
        return false;
    }

    private static ImageView prepareImageView(Request request) {
        ImageView imageView = request.targetReference.get();
        if (imageView != null) {
            Activity activity = Utils.pickActivity(imageView);
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                LogProxy.e(TAG, new Exception("Activity not available"));
                return null;
            }
            if (request.hostHash == 0) {
                request.hostHash = System.identityHashCode(activity);
            }
            if (checkTag(request, imageView)) {
                return null;
            }
            if (!request.keepOriginal) {
                imageView.setImageDrawable(null);
            }
            imageView.setTag(R.id.doodle_view_tag, null);
        }
        return imageView;
    }

    static void start(Request request) {
        ImageView imageView = null;
        if (request.targetReference != null) {
            imageView = prepareImageView(request);
            if (imageView == null) {
                return;
            }
        }

        // If source is invalid, just callback and return
        if (TextUtils.isEmpty(request.path)) {
            abort(request, imageView);
            return;
        }

        Bitmap bitmap = MemoryCache.getBitmap(request.getKey());

        final Request.Waiter waiter = request.waiter;
        if (waiter != null && (bitmap != null || waiter.timeout == 0)) {
            waiter.result = bitmap;
            return;
        }

        if (imageView != null) {
            if (bitmap != null) {
                setBitmap(request, imageView, bitmap, true);
            } else {
                setPlaceholder(request, imageView);
                Drawable placeholder = imageView.getDrawable();
                if (placeholder instanceof Animatable && !pauseFlag) {
                    startAnimDrawable(imageView, (Animatable) placeholder);
                }
            }
        }

        if (bitmap == null && imageView != null && pauseFlag) {
            cacheRequest(request, imageView);
            return;
        }

        if (bitmap == null) {
            Worker worker = new Worker(request, imageView);
            worker.execute(request.hostHash);
            if (waiter != null && !worker.isDone() && !worker.isCancelled()) {
                try {
                    waiter.result = (Bitmap) worker.get(waiter.timeout, TimeUnit.MILLISECONDS);
                } catch (Throwable e) {
                    LogProxy.e(TAG, e);
                }
                if (!worker.isDone()) {
                    worker.cancel(true);
                }
            }
        }
    }

    private static void abort(Request request, ImageView imageView) {
        if (request.simpleTarget != null) {
            request.simpleTarget.onComplete(null);
        } else if (imageView != null) {
            if (request.errorId >= 0 || request.errorDrawable != null) {
                setError(request, imageView);
            } else {
                setPlaceholder(request, imageView);
            }
            if (request.listener != null) {
                request.listener.onComplete(false);
            }
        }
    }

    static void setResult(Request request, ImageView imageView, Object result, boolean fromMemory) {
        if (request.waiter != null) {
            return;
        }

        if (request.simpleTarget != null) {
            request.simpleTarget.onComplete(result);
            return;
        }

        if (imageView == null) {
            return;
        }

        Activity activity = Utils.pickActivity(imageView);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (result != null) {
            if (result instanceof Bitmap) {
                setBitmap(request, imageView, (Bitmap) result, fromMemory);
            } else if (result instanceof Drawable) {
                imageView.setImageDrawable((Drawable) result);
                if (result instanceof Animatable) {
                    startAnimDrawable(imageView, (Animatable) result);
                }
            }
        } else {
            setError(request, imageView);
        }

        if (request.listener != null) {
            request.listener.onComplete(result != null);
        }
    }

    static void setBitmap(Request request, ImageView imageView, Bitmap bitmap, Boolean fromMemory){
        if (request.alwaysAnimation || !fromMemory) {
            if (request.crossFadeDuration > 0) {
                crossFade(imageView, bitmap, request);
            } else {
                imageView.setImageBitmap(bitmap);
                startAnimation(request, imageView);
            }
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }


    private static void startAnimDrawable(ImageView imageView, Animatable animatable) {
        animatable.start();
        imageView.addOnAttachStateChangeListener(attachListener);
    }

    private static class AttachListener implements View.OnAttachStateChangeListener {
        @Override
        public void onViewAttachedToWindow(View v) {
            resumeAnimDrawable(v);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            stopAnimDrawable(v);
        }
    }

    private static Animatable getAnimDrawable(View target) {
        if (target instanceof ImageView) {
            Drawable drawable = ((ImageView) target).getDrawable();
            if (drawable instanceof Animatable) {
                return (Animatable) drawable;
            }
        }
        return null;
    }

    private static void resumeAnimDrawable(View target) {
        try {
            Animatable animatable = getAnimDrawable(target);
            if (animatable != null && !animatable.isRunning()) {
                animatable.start();
            }
        } catch (Throwable e) {
            LogProxy.e(TAG, e);
        }
    }

    static void stopAnimDrawable(View target) {
        try {
            Animatable animatable = getAnimDrawable(target);
            if (animatable != null && animatable.isRunning()) {
                animatable.stop();
            }
        } catch (Throwable e) {
            LogProxy.e(TAG, e);
        }
    }

    private static void crossFade(ImageView imageView, Bitmap bitmap, Request request) {
        Drawable previous = imageView.getDrawable();
        if (previous == null) {
            previous = new ColorDrawable(Color.TRANSPARENT);
        }
        Drawable current = new BitmapDrawable(Utils.appContext.getResources(), bitmap);
        TransitionDrawable drawable = new TransitionDrawable(new Drawable[]{previous, current});
        drawable.setCrossFadeEnabled(true);
        imageView.setImageDrawable(drawable);
        drawable.startTransition(request.crossFadeDuration);
    }

    private static void startAnimation(Request request, ImageView imageView) {
        if (request.animation != null) {
            imageView.clearAnimation();
            imageView.startAnimation(request.animation);
        } else if (request.animationId != 0) {
            try {
                Animation animation = AnimationUtils.loadAnimation(Utils.appContext, request.animationId);
                imageView.clearAnimation();
                imageView.startAnimation(animation);
            } catch (Exception e) {
                LogProxy.e(TAG, e);
            }
        }
    }

    private static void setPlaceholder(Request request, ImageView imageView) {
        if (request.placeholderDrawable != null) {
            imageView.setImageDrawable(request.placeholderDrawable);
        } else if (request.placeholderId >= 0) {
            imageView.setImageResource(request.placeholderId);
        }
    }

    private static void setError(Request request, ImageView imageView) {
        if (request.errorDrawable != null) {
            imageView.setImageDrawable(request.errorDrawable);
        } else if (request.errorId >= 0) {
            imageView.setImageResource(request.errorId);
        }
    }
}
