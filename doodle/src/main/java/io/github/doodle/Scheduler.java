package io.github.doodle;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.*;

/**
 * Loading tasks scheduler.
 */
final class Scheduler {
    private static final int CUP_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int WINDOW_SIZE = Math.min(Math.max(2, CUP_COUNT), 4);

    // for computation tasks
    static final PipeExecutor cpExecutor = new PipeExecutor(WINDOW_SIZE);

    // for tasks with downloading
    static final PipeExecutor ioExecutor = new PipeExecutor(8);

    static final TagExecutor tagExecutor = new TagExecutor();

    // Windows size is one, tasks execute in serial.
    // Use to storage result bitmap.
    static final PipeExecutor storageExecutor = new PipeExecutor(1);

    private static Executor realExecutor = null;

    static void setExecutor(Executor executor) {
        if (executor != null) {
            realExecutor = executor;
        }
    }

    private static Executor getExecutor() {
        if (realExecutor == null) {
            realExecutor = Executors.newCachedThreadPool();
        }
        return realExecutor;
    }

    /**
     * Tasks with same tag executes serially.
     * Tasks in different tag executes concurrently.
     * <p>
     * For we have remote file cache (see {@link Downloader#sourceCache},
     * we could use URL as tag to avoid more than one task downloading same remote files
     * at almost same time (May have problem).
     * Because of tasks with same URL executing serially , after the first task download finish,
     * the later tasks with same URL can reuse the cache.
     * <p>
     * Similarly, we have result image cache (see {@link LruCache},
     * we could use the cache key as tag to avoid decoding duplicate bitmaps.
     */
    static class TagExecutor {
        private static final Set<CacheKey> scheduledTags = new HashSet<>();
        private static final Map<CacheKey, LinkedList<Task>> waitingQueues = new HashMap<>();

        public synchronized void execute(CacheKey tag, Runnable r, boolean needDownloading) {
            if (r == null) {
                return;
            }
            Task task = wrapTask(r, tag, needDownloading);
            if (!scheduledTags.contains(tag)) {
                start(tag, task);
            } else {
                LinkedList<Task> queue = waitingQueues.get(tag);
                if (queue == null) {
                    queue = new LinkedList<>();
                    waitingQueues.put(tag, queue);
                }
                queue.offer(task);
            }
        }

        private void start(CacheKey tag, Task task) {
            scheduledTags.add(tag);
            if (task.needDownloading) {
                ioExecutor.execute(task);
            } else {
                cpExecutor.execute(task);
            }
        }

        private Task wrapTask(Runnable r, CacheKey tag, boolean needDownloading){
            return new Task(r, needDownloading) {
                @Override
                public void run() {
                    try {
                        r.run();
                    } finally {
                        scheduleNext(tag);
                    }
                }
            };
        }

        private synchronized void scheduleNext(CacheKey tag) {
            scheduledTags.remove(tag);
            LinkedList<Task> queue = waitingQueues.get(tag);
            if (queue != null) {
                Task r = queue.poll();
                if (r == null) {
                    waitingQueues.remove(tag);
                } else {
                    start(tag, r);
                }
            }
        }
    }

    private static abstract class Task implements Runnable {
        final Runnable r;
        final boolean needDownloading;

        Task(Runnable runnable, boolean needDownloading) {
            this.r = runnable;
            this.needDownloading = needDownloading;
        }
    }


    /**
     * Support control the currency. <br>
     * Support change priority by state(pause/resume) of UI component.
     */
    static class PipeExecutor implements Executor {
        private final LinkedList<Runnable> frontList = new LinkedList<>();
        private final LinkedList<Runnable> backList = new LinkedList<>();
        private final int windowSize;
        private int count = 0;

        private final Executor executor = getExecutor();

        PipeExecutor(int windowSize) {
            this.windowSize = windowSize > 0 ? windowSize : 1;
        }

        public synchronized void execute(Runnable r) {
            if (r == null) {
                return;
            }
            if (count < windowSize) {
                start(r);
            } else {
                frontList.offer(r);
            }
        }

        private void start(Runnable r) {
            count++;
            executor.execute(() -> {
                try {
                    r.run();
                } catch (Throwable e) {
                    LogProxy.e("Doodle", e);
                } finally {
                    scheduleNext();
                }
            });
        }

        private synchronized void scheduleNext(){
            count--;
            if (count < windowSize) {
                Runnable next = poll();
                if (next != null) {
                    start(next);
                }
            }
        }

        private Runnable poll() {
            Runnable r = frontList.poll();
            return (r != null) ? r : backList.poll();
        }

        synchronized void pushBack(Runnable r) {
            flip(frontList, backList, r);
        }

        synchronized void popFront(Runnable r) {
            flip(backList, frontList, r);
        }

        private void flip(LinkedList<Runnable> aList, LinkedList<Runnable> bList, Runnable r) {
            if (aList.isEmpty()) return;
            Iterator<Runnable> it = aList.iterator();
            while (it.hasNext()) {
                Runnable e = it.next();
                if (e == r || ((e instanceof Task) && ((Task) e).r == r)) {
                    it.remove();
                    bList.offer(e);
                    break;
                }
            }
        }
    }
}
