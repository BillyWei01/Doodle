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

import io.github.doodle.interfaces.CustomView;

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
    private static final LinkedHashMap<View, Request> requests = new LinkedHashMap<>();

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

    private static synchronized void cacheRequest(Request request, View target) {
        // If the target had paused request before, cover it with new request.
        // That's useful for RecycleView scrolling.
        requests.put(target, request);
    }

    /**
     * @return true if the ImageView has already bound a same request.
     */
    private static boolean checkTag(Request request, View view) {
        Object tag = view.getTag(R.id.doodle_view_tag);
        if (tag instanceof Request) {
            Request preRequest = (Request) tag;
            if (request.getKey().equals(preRequest.getKey())) {
                return true;
            } else {
                WeakReference<View> viewRef = preRequest.viewReference;
                if (viewRef != null) {
                    stopAnimDrawable(viewRef.get());
                    preRequest.viewReference = null;
                }
                // If tag not equal, cancel previous request which binds to view.
                cancelTask(preRequest);
            }
        } else if (tag != null) {
            // Should not be here
            throw new IllegalArgumentException("Invalid tag type");
        }
        return false;
    }

    private static void cancelTask(Request request) {
        WeakReference<Worker> workerRef = request.workerReference;
        if (workerRef != null) {
            request.workerReference = null;
            Worker preTask = workerRef.get();
            if (preTask != null && !preTask.isCancelled()) {
                preTask.cancel(true);
            }
        }
    }

    private static View prepareView(Request request) {
        View view = request.viewReference.get();
        if (view != null) {
            Activity activity = Utils.pickActivity(view);
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                LogProxy.e(TAG, new Exception("Activity not available"));
                return null;
            }
            if (request.hostHash == 0) {
                request.hostHash = System.identityHashCode(activity);
            }
            if (checkTag(request, view)) {
                return null;
            }
            if (!request.keepOriginal) {
                if (view instanceof ImageView) {
                    ((ImageView) view).setImageDrawable(null);
                } else if (view instanceof CustomView) {
                    ((CustomView) view).setDrawable(null);
                }
            }
            view.setTag(R.id.doodle_view_tag, null);
        }
        return view;
    }

    static void clear(View view) {
        Object tag = view.getTag(R.id.doodle_view_tag);
        if (tag instanceof Request) {
            cancelTask((Request) tag);
            view.setTag(R.id.doodle_view_tag, null);
        }
    }

    static void start(Request request) {
        View view = null;
        if (request.viewReference != null) {
            view = prepareView(request);
            if (view == null) {
                return;
            }
        }

        // If source is invalid, just callback and return
        if (TextUtils.isEmpty(request.path)) {
            abort(request, view);
            return;
        }

        Bitmap bitmap = MemoryCache.getBitmap(request.getKey());

        final Request.Waiter waiter = request.waiter;
        if (waiter != null && (bitmap != null || waiter.timeout == 0)) {
            waiter.result = bitmap;
            return;
        }

        if (bitmap == null) {
            Object result = MemoryCache.resultWeakCache.get(request.getKey());
            if (result != null) {
                setResult(request, view, result, true);
                return;
            }
        } else {
            setResult(request, view, bitmap, true);
            return;
        }

        if (view != null) {
            setPlaceholder(request, view);
            Drawable placeholder = getDrawable(view);
            if (placeholder instanceof Animatable && !pauseFlag) {
                startAnimate(view, (Animatable) placeholder);
            }
        }

        if (view != null && pauseFlag) {
            cacheRequest(request, view);
            return;
        }

        Worker worker = new Worker(request, view);
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

    private static void abort(Request request, View view) {
        if (request.simpleTarget != null) {
            request.simpleTarget.onComplete(null);
        } else if (view != null) {
            if (request.errorId >= 0 || request.errorDrawable != null) {
                setError(request, view);
            } else {
                setPlaceholder(request, view);
            }
            if (request.listener != null) {
                request.listener.onComplete(false);
            }
        }
    }

    static void setResult(Request request, View view, Object result, boolean fromMemory) {
        if (request.waiter != null) {
            return;
        }

        if (request.simpleTarget != null) {
            request.simpleTarget.onComplete(result);
            return;
        }

        if (view == null) {
            return;
        }

        Activity activity = Utils.pickActivity(view);
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (result != null) {
            if (result instanceof Bitmap) {
                setBitmap(request, view, (Bitmap) result, fromMemory);
            } else if (result instanceof Drawable) {
                setDrawable(view, (Drawable) result);
                if (result instanceof Animatable) {
                    startAnimate(view, (Animatable) result);
                }
            } else if (view instanceof CustomView) {
                ((CustomView) view).handleResult(result);
                if (view instanceof Animatable) {
                    startAnimate(view, (Animatable) view);
                }
            }
        } else {
            setError(request, view);
        }

        if (request.listener != null) {
            request.listener.onComplete(result != null);
        }
    }

    static void setBitmap(Request request, View view, Bitmap bitmap, Boolean fromMemory) {
        if (request.alwaysAnimation || !fromMemory) {
            if (request.crossFadeDuration > 0) {
                crossFade(view, bitmap, request);
            } else {
                setBitmap(view, bitmap);
                showAnimation(request, view);
            }
        } else {
            setBitmap(view, bitmap);
        }
    }

    private static void startAnimate(View view, Animatable animatable) {
        animatable.start();
        view.addOnAttachStateChangeListener(attachListener);
    }

    private static class AttachListener implements View.OnAttachStateChangeListener {
        @Override
        public void onViewAttachedToWindow(View v) {
            startAnimate(v);
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            stopAnimDrawable(v);
        }
    }

    private static Animatable getAnimDrawable(View view) {
        if (view instanceof ImageView) {
            Drawable drawable = ((ImageView) view).getDrawable();
            if (drawable instanceof Animatable) {
                return (Animatable) drawable;
            }
        } else if (view instanceof Animatable) {
            return (Animatable) view;
        }
        return null;
    }

    private static void startAnimate(View view) {
        try {
            Animatable animatable = getAnimDrawable(view);
            if (animatable != null && !animatable.isRunning()) {
                animatable.start();
            }
        } catch (Throwable e) {
            LogProxy.e(TAG, e);
        }
    }

    static void stopAnimDrawable(View view) {
        try {
            Animatable animatable = getAnimDrawable(view);
            if (animatable != null && animatable.isRunning()) {
                animatable.stop();
            }
        } catch (Throwable e) {
            LogProxy.e(TAG, e);
        }
    }

    private static void crossFade(View view, Bitmap bitmap, Request request) {
        Drawable previous = getDrawable(view);
        if (previous == null) {
            previous = new ColorDrawable(Color.TRANSPARENT);
        }
        Drawable current = new BitmapDrawable(Utils.appContext.getResources(), bitmap);
        TransitionDrawable drawable = new TransitionDrawable(new Drawable[]{previous, current});
        drawable.setCrossFadeEnabled(true);
        setDrawable(view, drawable);
        drawable.startTransition(request.crossFadeDuration);
    }

    private static void showAnimation(Request request, View view) {
        if (request.animation != null) {
            view.clearAnimation();
            view.startAnimation(request.animation);
        } else if (request.animationId != 0) {
            try {
                Animation animation = AnimationUtils.loadAnimation(Utils.appContext, request.animationId);
                view.clearAnimation();
                view.startAnimation(animation);
            } catch (Exception e) {
                LogProxy.e(TAG, e);
            }
        }
    }

    private static void setPlaceholder(Request request, View view) {
        setDrawable(view, getDrawable(request.placeholderDrawable, request.placeholderId));
    }

    private static void setError(Request request, View view) {
        setDrawable(view, getDrawable(request.errorDrawable, request.errorId));
    }

    private static void setBitmap(View view, Bitmap bitmap) {
        if (bitmap != null) {
            if (view instanceof ImageView) {
                ((ImageView) view).setImageBitmap(bitmap);
            } else if (view instanceof CustomView) {
                BitmapDrawable drawable = new BitmapDrawable(Utils.appContext.getResources(), bitmap);
                ((CustomView) view).setDrawable(drawable);
            }
        }
    }

    private static Drawable getDrawable(View view) {
        if (view instanceof ImageView) {
            return ((ImageView) view).getDrawable();
        } else if (view instanceof CustomView) {
            return ((CustomView) view).getDrawable();
        }
        return null;
    }

    private static void setDrawable(View view, Drawable drawable) {
        if (drawable != null) {
            if (view instanceof ImageView) {
                ((ImageView) view).setImageDrawable(drawable);
            } else if (view instanceof CustomView) {
                ((CustomView) view).setDrawable(drawable);
            }
        }
    }

    private static Drawable getDrawable(Drawable drawable, int resId) {
        if (drawable != null) {
            return drawable;
        } else if (resId > 0) {
            try {
                return Utils.appContext.getDrawable(resId);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }
}
