package io.github.doodle;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.*;

/**
 * Loading tasks scheduler.
 * For handling concurrency and priority.
 */
final class Scheduler {
    private static final int CUP_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int WINDOW_SIZE = Math.min(Math.max(2, CUP_COUNT), 4);
    static final PipeExecutor pipeExecutor = new PipeExecutor(WINDOW_SIZE, WINDOW_SIZE * 2);
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
        private static final Map<CacheKey, LinkedList<Runnable>> waitingQueues = new HashMap<>();

        public synchronized void execute(CacheKey tag, Runnable r) {
            if (r == null) {
                return;
            }
            if (!scheduledTags.contains(tag)) {
                start(tag, r);
            } else {
                LinkedList<Runnable> queue = waitingQueues.get(tag);
                if (queue == null) {
                    queue = new LinkedList<>();
                    waitingQueues.put(tag, queue);
                }
                queue.offer(r);
            }
        }

        private void start(CacheKey tag, Runnable r) {
            scheduledTags.add(tag);
            pipeExecutor.execute(new Wrapper(r) {
                @Override
                public void run() {
                    try {
                        task.run();
                    } finally {
                        scheduleNext(tag);
                    }
                }
            });
        }

        private synchronized void scheduleNext(CacheKey tag) {
            scheduledTags.remove(tag);
            LinkedList<Runnable> queue = waitingQueues.get(tag);
            if (queue != null) {
                Runnable r = queue.poll();
                if (r == null) {
                    waitingQueues.remove(tag);
                } else {
                    start(tag, r);
                }
            }
        }
    }

    private static abstract class Wrapper implements Runnable {
        final Runnable task;

        Wrapper(Runnable r) {
            this.task = r;
        }
    }

    /**
     * Support control the currency. <br>
     * Support change the execute window in range [minSize, maxSize]. <br>
     * Support change priority by state(pause/resume) of UI component.
     */
    static class PipeExecutor implements Executor {
        private final LinkedList<Runnable> frontList = new LinkedList<>();
        private final LinkedList<Runnable> backList = new LinkedList<>();
        private final int minSize;
        private final int maxSize;
        private int windowSize;
        private int count = 0;

        private final Executor executor = getExecutor();

        PipeExecutor(int windowSize) {
            this.windowSize = windowSize > 0 ? windowSize : 1;
            this.minSize = this.windowSize;
            this.maxSize = this.windowSize;
        }

        PipeExecutor(int minSize, int maxSize) {
            this.minSize = minSize > 0 ? minSize : 1;
            this.windowSize = this.minSize;
            this.maxSize = Math.max(this.minSize, maxSize);
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
                if (e == r || ((e instanceof Wrapper) && ((Wrapper) e).task == r)) {
                    it.remove();
                    bList.offer(e);
                    break;
                }
            }
        }

        synchronized void extendWindow() {
            if (windowSize < maxSize) {
                windowSize++;
                if (count < windowSize) {
                    Runnable r = poll();
                    if (r != null) {
                        start(r);
                    }
                }
            }
        }

        synchronized void reduceWindow() {
            if (windowSize > minSize) {
                windowSize--;
            }
        }
    }
}
