package io.github.doodle;

import io.github.doodle.enums.MediaType;

final class MediaTypeParser {
    private static final int JPG_HEADER = 0xFFD8FF;
    private static final int GIF_HEADER = 0x474946;
    private static final int PNG_HEADER = 0x89504E47;
    private static final int BMP_HEADER = 0x424D;

    // webp
    private static final int RIFF = 0x52494646;
    private static final int WEBP = 0x57454250;
    private static final int VP8X = 0x56503858;
    private static final int VP8L = 0x5650384C;

    // https://www.ftyps.com/
    private static final int FILE_TYPE = 0x66747970; // ftyp
    // video/mp4
    private static final int ISO = 0x69736F;   // isox, to isom, iso2,
    private static final int MP4 = 0x6D7034;    // mp4x, to mp41, mp42
    private static final int ND = 0x4E44;      // NDxx
    // video/3gpp2, video/3gpp
    private static final int _3G = 0x3367;
    // video/quicktime (Apple QuickTime, .MOV)
    private static final int QT = 0x7174;
    // image/heic, image/heif
    private static final int HE = 0x6865; // heic, heis, heix, hevc, hevx
    private static final int HXF1 = 0x6D006631;  // mif1, msf1

    // header of mkv
    private static final int EBML = 0x1A45DFA3;

    static MediaType parse(byte[] header) {
        int h0 = readInt(header, 0);
        if ((h0 >>> 8) == JPG_HEADER) {
            return MediaType.JPG;
        }
        if (h0 == PNG_HEADER) {
            // https://stackoverflow.com/questions/2057923/how-to-check-a-png-for-grayscale-alpha-color-type
            int alpha = header[25] & 0xFF;
            return alpha >= 3 ? MediaType.PNG : MediaType.PNG_NO_ALPHA;
        }
        if ((h0 >>> 8) == GIF_HEADER) {
            return MediaType.GIF;
        }

        int h2 = readInt(header, 2);
        if (h0 == RIFF && h2 == WEBP) {
            int h3 = readInt(header, 3);
            byte flag = header[20];
            if (h3 == VP8X) {
                if ((flag & 0x2) != 0) {
                    return MediaType.WEBP_ANIMATED;
                }
                return ((flag & 0x10) != 0) ? MediaType.WEBP_LOSSY : MediaType.WEBP_LOSSY_NO_ALPHA;
            } else if (h3 == VP8L) {
                return ((flag & 0x8) != 0) ? MediaType.WEBP_LOSSLESS : MediaType.WEBP_LOSSLESS_NO_ALPHA;
            }
            return MediaType.WEBP_LOSSLESS;
        }

        if ((h0 >>> 16) == BMP_HEADER) {
            return MediaType.BMP;
        }
        if (h0 == EBML) {
            return MediaType.MKV;
        }

        int h1 = readInt(header, 1);
        if (h1 == FILE_TYPE) {
            int a = h2 >>> 8;
            int b = h2 >>> 16;
            if ((a == ISO || a == MP4 || b == ND)) {
                return MediaType.MP4;
            }
            if (b == QT) {
                return MediaType.MOV;
            }
            if (b == _3G) {
                return MediaType._3GP;
            }
            if ((h2 & 0xFF00FFFF) == HXF1 || b == HE) {
                return MediaType.HEIF;
            }
        }

        return MediaType.UNKNOWN;
    }

    private static int readInt(byte[] header, int i) {
        int j = i << 2;
        return (header[j] << 24) |
                ((header[j + 1] & 0xFF) << 16) |
                ((header[j + 2] & 0xFF) << 8) |
                (header[j + 3] & 0xFF);
    }
}
