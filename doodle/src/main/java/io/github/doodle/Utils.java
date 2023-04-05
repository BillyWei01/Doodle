package io.github.doodle;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.View;

import java.util.Arrays;

import java.io.*;


final class Utils {
    // Assign value by DoodleContentProvider
    static Context appContext;

    private static ContentResolver resolver;
    private static volatile String cachePath;
    private static volatile Point sDisplayDimens;

    static ContentResolver getContentResolver() {
        if (resolver == null) {
            resolver = appContext.getContentResolver();
        }
        return resolver;
    }

    @android.annotation.SuppressLint("SdCardPath")
    static String getCachePath() {
        if (cachePath == null) {
            synchronized (Utils.class) {
                if (cachePath == null) {
                    String path = Config.cachePath;
                    if (path == null || path.isEmpty()) {
                        File file = appContext.getCacheDir();
                        cachePath = file != null ? file.getPath()
                                : "/data/data/" + appContext.getPackageName() + "/cache";
                    } else {
                        int len = path.length();
                        cachePath = path.charAt(len - 1) == '/' ? path.substring(0, len - 1) : path;
                    }
                }
            }
        }
        return cachePath;
    }

    static void registerActivityLifecycle(final Context context) {
        if (!(context instanceof Application)) {
            return;
        }
        ((Application) context).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
                Doodle.notifyResume(this);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                Doodle.notifyPause(this);
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Doodle.notifyDestroy(activity);
            }
        });
    }

    static Point getDisplayDimens() {
        if (sDisplayDimens == null) {
            synchronized (Utils.class) {
                if (sDisplayDimens == null) {
                    fetchDimens();
                }
            }
        }
        return sDisplayDimens;
    }

    private static void fetchDimens() {
        try {
            WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                Point point = new Point();
                windowManager.getDefaultDisplay().getSize(point);
                if (point.x > 0 && point.y > 0) {
                    sDisplayDimens = point;
                }
            }
        } catch (Exception ignore) {
        }
        if (sDisplayDimens == null) {
            // should not be here, just in case
            sDisplayDimens = new Point(1080, 1920);
        }
    }

    static int getBytesCount(Bitmap bitmap) {
        return bitmap != null ? bitmap.getAllocationByteCount() : 0;
    }

    static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }

    static boolean deleteQuietly(File file) {
        try {
            if (file != null) {
                return file.delete();
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    static Activity pickActivity(View view) {
        Context context = view.getContext();
        if (context instanceof Activity) {
            return (Activity) context;
        }
        if ((context instanceof ContextWrapper)
                && ((ContextWrapper) context).getBaseContext() instanceof Activity) {
            return (Activity) ((ContextWrapper) context).getBaseContext();
        }
        context = view.getRootView().getContext();
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return null;
    }

    static String toUriPath(int resID) {
        if (resID != 0) {
            try {
                Resources resources = Utils.appContext.getResources();
                return ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                        + resources.getResourcePackageName(resID) + "/"
                        + resources.getResourceTypeName(resID) + "/"
                        + resources.getResourceEntryName(resID);
            } catch (Exception e) {
                LogProxy.e("Doodle", e);
            }
        }
        return "";
    }

    static void readFully(InputStream in, byte[] b) throws IOException {
        int n = 0;
        int len = b.length;
        do {
            int count = in.read(b, n, len - n);
            if (count <= 0) break;
            n += count;
        } while (n < len);
    }

    static boolean makeFileIfNotExist(File file) throws IOException {
        if (file.isFile()) {
            return true;
        } else {
            File parent = file.getParentFile();
            return parent != null && (parent.isDirectory() || parent.mkdirs()) && file.createNewFile();
        }
    }

    static boolean streamToFile(InputStream inputStream, File des) throws IOException {
        if (inputStream == null || des == null) {
            return false;
        }
        if (!Utils.makeFileIfNotExist(des)) {
            closeQuietly(inputStream);
            return false;
        }
        byte[] buffer = ByteArrayPool.getBasicArray();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(des));
        try {
            while (true) {
                int count = inputStream.read(buffer, 0, buffer.length);
                if (count <= 0) break;
                out.write(buffer, 0, count);
            }
        } finally {
            closeQuietly(out);
            closeQuietly(inputStream);
            ByteArrayPool.recycleBasicArray(buffer);
        }
        return true;
    }

    static byte[] streamToBytes(InputStream inputStream) throws IOException {
        final int limit = 1 << 27; // 128M
        int available = inputStream.available();
        if (available > limit) {
            throw new IOException("File too large:" + available);
        }
        byte[] buffer = ByteArrayPool.getArray(available);
        int bufLen = buffer.length;
        int off = 0;
        int count;
        try {
            while (true) {
                count = inputStream.read(buffer, off, bufLen - off);
                if(count <= 0) break;
                off += count;
                if (off == bufLen) {
                    byte[] oldBuffer = buffer;
                    int newSize = bufLen << 1;
                    if (newSize > limit) {
                        throw new IOException("Required buffer too large:" + newSize);
                    }
                    buffer = ByteArrayPool.getArray(newSize);
                    System.arraycopy(oldBuffer, 0, buffer, 0, bufLen);
                    bufLen = buffer.length;
                    ByteArrayPool.recycleArray(oldBuffer);
                }
            }
            return Arrays.copyOf(buffer, off);
        } finally {
            ByteArrayPool.recycleArray(buffer);
        }
    }
}
