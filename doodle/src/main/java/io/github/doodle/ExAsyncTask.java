package io.github.doodle;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import io.github.doodle.LifecycleManager.Event;

/**
 * Extended AsyncTask.
 * <p>
 * ExAsyncTask is similar to {@link android.os.AsyncTask}, with many modifications.
 *
 * <p>
 *  Features:<br>
 * - Tasks destroy when Activity onDestroy.<br>
 * - Tasks bring to front/back of queue when Activity or Fragment onResume/onPause.<br>
 * (By {@link LifecycleManager}).<br>
 * <p>
 * - Tasks execute in the FIFO queue with dynamic window size.
 * When the task need to do downloading work(with slowly IO), the window will be extend,
 * and will be reduce when download finished.<br>
 * (By {@link Scheduler.PipeExecutor}).
 * <p>
 * - Avoid downloading same remote files or decoding same bitmap for more than once.<br>
 * (By {@link Scheduler.TagExecutor}).
 */
abstract class ExAsyncTask {
    private final static Handler sHandler = new Handler(Looper.getMainLooper());

    private final Callable<Object> mWorker;
    private final FutureTask<Object> mFuture;

    volatile boolean mDone = false;
    volatile Status mStatus = Status.PENDING;

    private final AtomicBoolean mCancelled = new AtomicBoolean();
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();

    private int mHostHash;

    protected boolean needDownloading;

    enum Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    public ExAsyncTask() {
        mWorker = () -> {
            mTaskInvoked.set(true);
            Object result = null;
            try {
                if (!mCancelled.get()) {
                    result = doInBackground();
                }
            } catch (Throwable e) {
                mCancelled.set(true);
            }
            postResult(result);
            return result;
        };

        mFuture = new FutureTask<Object>(mWorker) {
            @Override
            protected void done() {
                mDone = true;
                try {
                    postResultIfNotInvoked(get());
                } catch (CancellationException e) {
                    // normal case, no need to log.
                    postResultIfNotInvoked(null);
                } catch (Throwable e) {
                    Log.w("Doodle", e);
                    postResultIfNotInvoked(null);
                }
            }
        };
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public final boolean isDone() {
        return mDone;
    }

    public final boolean isCancelled() {
        return mCancelled.get();
    }

    private void postResultIfNotInvoked(Object result) {
        if (mFuture.isCancelled()) {
            mCancelled.set(true);
        }
        if (!mTaskInvoked.get()) {
            postResult(result);
        }
    }

    private void postResult(Object result) {
        sHandler.post(() -> finish(result));
    }

    public final void cancel(boolean mayInterruptIfRunning) {
        mCancelled.set(true);
        try {
            mFuture.cancel(mayInterruptIfRunning);
        }catch (Throwable ignore){
        }
    }

    public final Object get() throws InterruptedException, ExecutionException {
        return mFuture.get();
    }

    public Object get(Long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return mFuture.get(timeout, unit);
    }

    protected abstract CacheKey generateTag();

    protected abstract Object doInBackground();

    protected void onPostExecute(Object result) {
    }

    protected void onCancelled() {
    }

    public final void execute(int hostHash) {
        if (mStatus != Status.PENDING) {
            return;
        }
        if (hostHash != 0) {
            LifecycleManager.register(hostHash, this);
        }
        mHostHash = hostHash;
        mStatus = Status.RUNNING;
        Scheduler.tagExecutor.execute(generateTag(), mFuture, needDownloading);
    }

    private void finish(Object result) {
        detachHost();
        if (isCancelled()) {
            onCancelled();
        } else {
            onPostExecute(result);
        }
        mStatus = Status.FINISHED;
    }

    private void detachHost() {
        if (mHostHash != 0) {
            LifecycleManager.unregister(mHostHash, this);
        }
    }

    void handleEvent(int event) {
        if (!isCancelled() && mStatus != Status.FINISHED) {
            if (event == Event.PAUSE) {
                getExecutor().pushBack(mFuture);
            } else if (event == Event.RESUME) {
                getExecutor().popFront(mFuture);
            } else if (event == Event.DESTROY) {
                // When getting the DESTROY event, it means the LifecycleManager had already removed this task.
                // Mark mHostHash to be zero, to indicate that there's no need to call 'unregister'.
                mHostHash = 0;
                cancel(true);
            }
        }
    }

    private Scheduler.PipeExecutor getExecutor() {
        return needDownloading ? Scheduler.ioExecutor : Scheduler.cpExecutor;
    }
}
