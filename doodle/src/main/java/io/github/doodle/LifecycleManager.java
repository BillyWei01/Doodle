package io.github.doodle;

import android.content.Context;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Manager of lifecycle to image loading tasks. <br>
 * Doodle has observed Activity's lifecycle in {@link Utils#registerActivityLifecycle(Context)}.
 * Fragment has not did this yet,
 * You could call
 * {@link Doodle#notifyPause(Object)},
 * {@link Doodle#notifyResume(Object)},
 * {@link Doodle#notifyDestroy(Object)}
 *  at your BaseFragment (if have) or LifecycleObserver(androidx).
 *  
 * @see Request#observeHost(Object)
 */
final class LifecycleManager {
    // Host(identityHashCode) to ExAsyncTasks
    private static final SparseArray<TaskHolder> hostToTask = new SparseArray<>();

    static class Event {
        static final int PAUSE = 1;
        static final int RESUME = 2;
        static final int DESTROY = 3;
    }

    static synchronized void register(int hostHash, ExAsyncTask task) {
        TaskHolder holder = hostToTask.get(hostHash);
        if (holder == null) {
            holder = new TaskHolder();
            hostToTask.put(hostHash, holder);
        }
        holder.add(task);
    }

    static synchronized void unregister(int hostHash, ExAsyncTask task) {
        TaskHolder holder = hostToTask.get(hostHash);
        if (holder != null) {
            holder.remove(task);
        }
    }

    static synchronized void notify(Object host, int event) {
        if (host == null) return;
        int hostHash = System.identityHashCode(host);
        int index = hostToTask.indexOfKey(hostHash);
        if (index >= 0) {
            TaskHolder holder = hostToTask.valueAt(index);
            if (event == Event.DESTROY) {
                hostToTask.removeAt(index);
            }
            holder.dispatch(event);
        }
    }

    private static class TaskHolder {
        private final LinkedList<WeakReference<ExAsyncTask>> taskList = new LinkedList<>();

        void add(ExAsyncTask task) {
            boolean contain = false;
            for (WeakReference<ExAsyncTask> reference : taskList) {
                if (reference.get() == task) {
                    contain = true;
                    break;
                }
            }
            if (!contain) {
                taskList.add(new WeakReference<>(task));
            }
        }

        void remove(ExAsyncTask task) {
            Iterator<WeakReference<ExAsyncTask>> iterator = taskList.descendingIterator();
            while (iterator.hasNext()) {
                WeakReference<ExAsyncTask> reference = iterator.next();
                ExAsyncTask target = reference.get();
                if (target == null || target == task) {
                    iterator.remove();
                }
            }
        }

        void dispatch(int event) {
            for (WeakReference<ExAsyncTask> reference : taskList) {
                ExAsyncTask target = reference.get();
                if (target != null) {
                    target.handleEvent(event);
                }
            }
        }
    }
}
