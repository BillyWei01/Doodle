package io.github.doodle;

/**
 * MurmurHash works well and it's much faster than MD5.
 * <p>
 * Base on https://en.wikipedia.org/wiki/Birthday_attack,
 * it's good enough with 128 bits to identify the caches.
 * <p>
 * https://sites.google.com/site/murmurhash/
 * https://github.com/tamtam180/MurmurHash-For-Java/
 */
final class MHash {
    static long[] digest128(byte[] data) {
        final long seed = 0xe17a1465L;
        final long c1 = 0x87c37b91114253d5L;
        final long c2 = 0x4cf5ad432745937fL;

        final int len = data.length;
        final int block_remain = len & 0xF;
        final int block_size = len - block_remain;

        long h1 = seed;
        long h2 = seed;

        int i;
        for (i = 0; i < block_size; i += 16) {
            long k1 = toLongLE(data, i);
            long k2 = toLongLE(data, i + 8);

            k1 = updateK(k1, c1, 31, c2);
            h1 = updateH(h1, k1, 27, h2, 0x52dce729L);

            k2 = updateK(k2, c2, 33, c1);
            h2 = updateH(h2, k2, 31, h1, 0x38495ab5L);
        }

        long k1 = 0;
        long k2 = 0;

        switch (block_remain) {
            case 15:
                k2 ^= (long) data[i + 14] << 48;
            case 14:
                k2 ^= (long) data[i + 13] << 40;
            case 13:
                k2 ^= (long) data[i + 12] << 32;
            case 12:
                k2 ^= (long) data[i + 11] << 24;
            case 11:
                k2 ^= data[i + 10] << 16;
            case 10:
                k2 ^= data[i + 9] << 8;
            case 9:
                k2 ^= data[i + 8];
                k2 = updateK(k2, c2, 33, c1);
                h2 ^= k2;
            case 8:
                k1 ^= (long) data[i + 7] << 56;
            case 7:
                k1 ^= (long) data[i + 6] << 48;
            case 6:
                k1 ^= (long) data[i + 5] << 40;
            case 5:
                k1 ^= (long) data[i + 4] << 32;
            case 4:
                k1 ^= (long) data[i + 3] << 24;
            case 3:
                k1 ^= data[i + 2] << 16;
            case 2:
                k1 ^= data[i + 1] << 8;
            case 1:
                k1 ^= data[i];
                k1 = updateK(k1, c1, 31, c2);
                h1 ^= k1;
        }

        h1 ^= len;
        h2 ^= len;

        h1 += h2;
        h2 += h1;

        h1 = mix(h1);
        h2 = mix(h2);

        h1 += h2;
        h2 += h1;

        return new long[]{h1, h2};
    }

    private static long toLongLE(byte[] b, int i) {
        return (((long) b[i + 7] << 56) +
                ((long) (b[i + 6] & 255) << 48) +
                ((long) (b[i + 5] & 255) << 40) +
                ((long) (b[i + 4] & 255) << 32) +
                ((long) (b[i + 3] & 255) << 24) +
                ((b[i + 2] & 255) << 16) +
                ((b[i + 1] & 255) << 8) +
                ((b[i] & 255)));
    }

    private static long mix(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }

    private static long updateK(long k, long cx1, int rnum, long cx2) {
        k *= cx1;
        k = Long.rotateLeft(k, rnum);
        k *= cx2;
        return k;
    }

    private static long updateH(long h, long kx, int rnum, long hx, long cc) {
        h ^= kx;
        h = Long.rotateLeft(h, rnum);
        h += hx;
        h = h * 5 + cc;
        return h;
    }
}
