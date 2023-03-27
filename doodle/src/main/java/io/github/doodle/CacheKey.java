package io.github.doodle;

/**
 * Length of cache key is fixed (128 bits).
 * Comparing with String key, it's faster by using two 'long', and takes less memory.
 */
final class CacheKey {
    private static final char[] HEX_DIGITS = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'
    };

    final long h1;
    final long h2;

    CacheKey(long h1, long h2) {
        this.h1 = h1;
        this.h2 = h2;
    }

    CacheKey(String key) {
        if (key == null) {
            h1 = 0L;
            h2 = 0L;
        } else {
            long[] a = MHash.digest128(key.getBytes());
            h1 = a[0];
            h2 = a[1];
        }
    }

    String toHex() {
        char[] buf = new char[32];
        long2Hex(h1, buf, 0);
        long2Hex(h2, buf, 16);
        return new String(buf);
    }

    static CacheKey parse(String hex) {
        byte[] buf = hex.getBytes();
        if (buf.length != 32) {
            return null;
        }
        try {
            return new CacheKey(hex2Long(buf, 0), hex2Long(buf, 16));
        } catch (Exception ignore) {
        }
        return null;
    }

    private static long hex2Long(byte[] buf, int offset) {
        long a = 0;
        for (int i = 0; i < 8; i++) {
            int index = (i << 1) + offset;
            int b = (byte2Int(buf[index]) << 4) | byte2Int(buf[index + 1]);
            a <<= 8;
            a |= b;
        }
        return a;
    }

    private static int byte2Int(byte b) {
        if (b >= '0' && b <= '9') {
            return b - '0';
        } else if (b >= 'a' && b <= 'f') {
            return b - 'a' + 10;
        } else {
            throw new NumberFormatException("invalid hex number");
        }
    }

    private void long2Hex(long a, char[] buf, int offset) {
        for (int i = 7; i >= 0; i--) {
            int index = (i << 1) + offset;
            int b = (int) (a & 0xFF);
            buf[index] = HEX_DIGITS[(b >> 4) & 0xF];
            buf[index + 1] = HEX_DIGITS[b & 0xF];
            a = a >>> 8;
        }
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        CacheKey cacheKey = (CacheKey) o;
        return h1 == cacheKey.h1 && h2 == cacheKey.h2;
    }

    @Override
    public int hashCode() {
        return (int) (h1 ^ h2);
    }
}
