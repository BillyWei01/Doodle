package io.github.doodle;

import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

final class ByteArrayPool {
    private static final int BASIC_ARRAY_SIZE = 8192;
    private static final int MIN_BUFFER_SIZE = BASIC_ARRAY_SIZE * 2;
    private static final int MAX_BUFFER_SIZE = 1 << 22; // 4M

    private static final int BASIC_COUNT_LIMIT = 10;
    private static int basicCount = 0;
    private static final byte[][] basicArrays = new byte[BASIC_COUNT_LIMIT][];
    private static final SparseArray<ArrayList<WeakReference<byte[]>>> bufferArrays = new SparseArray<>();

    static byte[] getBasicArray() {
        synchronized (basicArrays) {
            if (basicCount > 0) {
                byte[] bytes = basicArrays[--basicCount];
                basicArrays[basicCount] = null;
                return bytes;
            }
        }
        return new byte[BASIC_ARRAY_SIZE];
    }

    static void recycleBasicArray(byte[] bytes) {
        if (bytes == null) {
            return;
        }
        synchronized (basicArrays) {
            if (basicCount < BASIC_COUNT_LIMIT) {
                basicArrays[basicCount++] = bytes;
            }
        }
    }

    /**
     * Round up to power of two
     * <p>
     * copy from {@link java.util.HashMap}
     */
    private static int roundPowerTwo(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : n + 1;
    }

    static byte[] getArray(int len) {
        if (len <= BASIC_ARRAY_SIZE) {
            return getBasicArray();
        }
        if (len <= MIN_BUFFER_SIZE) {
            len = MIN_BUFFER_SIZE;
        } else if (len > MAX_BUFFER_SIZE) {
            return new byte[len];
        } else {
            len = roundPowerTwo(len);
        }
        synchronized (bufferArrays) {
            int size = bufferArrays.size();
            int index = bufferArrays.indexOfKey(len);
            if (index < 0) {
                // SparseArray's indexOfKey return 'reverse' of low position if key not found
                index = ~index;
            }
            for (int i = index; i < size; i++) {
                ArrayList<WeakReference<byte[]>> bytesList = bufferArrays.valueAt(i);
                if (!bytesList.isEmpty()) {
                    byte[] bytes = getBytes(bytesList);
                    // If bytes is not null, bytes.length should be large than len,
                    // just make a check to ensure that.
                    if (bytes != null && bytes.length >= len) {
                        return bytes;
                    }
                }
            }
        }
        return new byte[len];
    }

    static void recycleArray(byte[] bytes) {
        if (bytes == null) {
            return;
        }
        int len = bytes.length;
        if (len == BASIC_ARRAY_SIZE) {
            recycleBasicArray(bytes);
            return;
        }
        if (len < MIN_BUFFER_SIZE || len > MAX_BUFFER_SIZE || (len & 0x3FFF) != 0) {
            return;
        }
        synchronized (bufferArrays) {
            ArrayList<WeakReference<byte[]>> bytesList = bufferArrays.get(len);
            if (bytesList == null) {
                bytesList = new ArrayList<>();
                bufferArrays.put(len, bytesList);
            }
            bytesList.add(new WeakReference<>(bytes));
        }
    }

    private static byte[] getBytes(ArrayList<WeakReference<byte[]>> bytesList) {
        int n = bytesList.size();
        for (int i = n - 1; i >= 0; i--) {
            WeakReference<byte[]> ref = bytesList.get(i);
            bytesList.remove(i);
            byte[] bytes = ref.get();
            if (bytes != null) {
                return bytes;
            }
        }
        return null;
    }
}
