package io.github.doodle;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A class get the exif orientation and fix the orientation.
 * <p>
 * Copy from Glide, with modification.
 */
final class ExifHelper {
    private static final int JPG_HEADER = 0xFFD8;
    // "MM".
    private static final int MOTOROLA_TIFF_MAGIC_NUMBER = 0x4D4D;
    // "II".
    private static final int INTEL_TIFF_MAGIC_NUMBER = 0x4949;
    //"Exif\0\0";
    private static final byte[] JPEG_EXIF_SEGMENT_PREAMBLE = new byte[]{69, 120, 105, 102, 0, 0};

    private static final int SEGMENT_SOS = 0xDA;
    private static final int MARKER_EOI = 0xD9;
    private static final int SEGMENT_START_ID = 0xFF;
    private static final int EXIF_SEGMENT_TYPE = 0xE1;
    private static final int ORIENTATION_TAG_TYPE = 0x0112;
    private static final int[] BYTES_PER_FORMAT = {0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};

    // Constants used for the Orientation Exif tag, Copy from ExifInterface
    public static final int ORIENTATION_UNDEFINED = 0;
    public static final int ORIENTATION_NORMAL = 1;
    public static final int ORIENTATION_FLIP_HORIZONTAL = 2;  // left right reversed mirror
    public static final int ORIENTATION_ROTATE_180 = 3;
    public static final int ORIENTATION_FLIP_VERTICAL = 4;  // upside down mirror
    // flipped about top-left <--> bottom-right axis
    public static final int ORIENTATION_TRANSPOSE = 5;
    public static final int ORIENTATION_ROTATE_90 = 6;  // rotate 90 cw to right it
    // flipped about top-right <--> bottom-left axis
    public static final int ORIENTATION_TRANSVERSE = 7;
    public static final int ORIENTATION_ROTATE_270 = 8;  // rotate 270 to right it

    static boolean possiblyExif(byte[] header) {
        int magicNumber = ((header[0] & 0xFF) << 8) | (header[1] & 0xFF);
        return (magicNumber & JPG_HEADER) == JPG_HEADER
                || magicNumber == MOTOROLA_TIFF_MAGIC_NUMBER
                || magicNumber == INTEL_TIFF_MAGIC_NUMBER;
    }

    static int getOrientation(InputStream in) throws IOException {
        byte[] exifData = null;
        try {
            if (in.skip(2) != 2) {
                return ORIENTATION_UNDEFINED;
            }
            int exifSegmentLength = moveToExifSegmentAndGetLength(in);
            if (exifSegmentLength == -1) {
                return ORIENTATION_UNDEFINED;
            }
            exifData = ByteArrayPool.getArray(exifSegmentLength);
            int toRead = exifSegmentLength;
            int read;
            while (toRead > 0 && ((read = in.read(exifData, exifSegmentLength - toRead, toRead)) != -1)) {
                toRead -= read;
            }
            if (toRead != 0) {
                return ORIENTATION_UNDEFINED;
            }

            if (hasJpegExifPreamble(exifData, exifSegmentLength)) {
                return parseExifSegment(new RandomAccessReader(exifData, exifSegmentLength));
            }
            return ORIENTATION_UNDEFINED;
        } finally {
            ByteArrayPool.recycleArray(exifData);
        }
    }

    private static boolean hasJpegExifPreamble(byte[] exifData, int exifSegmentLength) {
        int exifPreambleLen = JPEG_EXIF_SEGMENT_PREAMBLE.length;
        if (exifSegmentLength <= exifPreambleLen) {
            return false;
        }
        for (int i = 0; i < exifPreambleLen; i++) {
            if (exifData[i] != JPEG_EXIF_SEGMENT_PREAMBLE[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Moves reader to the start of the exif segment and returns the length of the exif segment or
     * {@code -1} if no exif segment is found.
     */
    private static int moveToExifSegmentAndGetLength(InputStream in) throws IOException {
        while (true) {
            int segmentId = in.read() & 0xFF;
            if (segmentId != SEGMENT_START_ID) {
                return -1;
            }

            int segmentType = in.read() & 0xFF;
            if (segmentType == SEGMENT_SOS || segmentType == MARKER_EOI) {
                return -1;
            }

            // Segment length includes bytes for segment length.
            int segmentLength = ((in.read() << 8 & 0xFF00) | (in.read() & 0xFF)) - 2;
            if (segmentType != EXIF_SEGMENT_TYPE) {
                if (skip(in, segmentLength) != segmentLength) {
                    return -1;
                }
            } else {
                return segmentLength;
            }
        }
    }

    private static long skip(InputStream in, long total) throws IOException {
        if (total < 0) {
            return 0;
        }

        long toSkip = total;
        while (toSkip > 0) {
            long skipped = in.skip(toSkip);
            if (skipped > 0) {
                toSkip -= skipped;
            } else {
                // Skip has no specific contract as to what happens when you reach the end of
                // the stream. To differentiate between temporarily not having more data and
                // having finished the stream, we read a single byte when we fail to skip any
                // amount of data.
                int testEofByte = in.read();
                if (testEofByte == -1) {
                    break;
                } else {
                    toSkip--;
                }
            }
        }
        return total - toSkip;
    }

    private static int parseExifSegment(RandomAccessReader segmentData) {
        final int headerOffsetSize = JPEG_EXIF_SEGMENT_PREAMBLE.length;

        short byteOrderIdentifier = segmentData.getInt16(headerOffsetSize);
        ByteOrder byteOrder = byteOrderIdentifier == INTEL_TIFF_MAGIC_NUMBER
                ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        segmentData.order(byteOrder);

        int firstIfdOffset = segmentData.getInt32(headerOffsetSize + 4) + headerOffsetSize;
        int tagCount = segmentData.getInt16(firstIfdOffset);
        for (int i = 0; i < tagCount; i++) {
            final int tagOffset = calcTagOffset(firstIfdOffset, i);

            final int tagType = segmentData.getInt16(tagOffset);
            // We only want orientation.
            if (tagType != ORIENTATION_TAG_TYPE) {
                continue;
            }

            final int formatCode = segmentData.getInt16(tagOffset + 2);
            // 12 is max format code.
            if (formatCode < 1 || formatCode > 12) {
                continue;
            }

            final int componentCount = segmentData.getInt32(tagOffset + 4);
            if (componentCount < 0) {
                continue;
            }

            final int byteCount = componentCount + BYTES_PER_FORMAT[formatCode];
            if (byteCount > 4) {
                continue;
            }

            final int tagValueOffset = tagOffset + 8;
            if (tagValueOffset < 0 || tagValueOffset > segmentData.length()) {
                continue;
            }

            if (byteCount < 0 || tagValueOffset + byteCount > segmentData.length()) {
                continue;
            }

            // assume componentCount == 1 && fmtCode == 3
            return segmentData.getInt16(tagValueOffset);
        }

        return -1;
    }

    private static int calcTagOffset(int ifdOffset, int tagIndex) {
        return ifdOffset + 2 + 12 * tagIndex;
    }

    private static final class RandomAccessReader {
        private final ByteBuffer data;

        RandomAccessReader(byte[] data, int length) {
            this.data = (ByteBuffer) ByteBuffer.wrap(data)
                    .order(ByteOrder.BIG_ENDIAN)
                    .limit(length);
        }

        void order(ByteOrder byteOrder) {
            this.data.order(byteOrder);
        }

        int length() {
            return data.remaining();
        }

        int getInt32(int offset) {
            return isAvailable(offset, 4) ? data.getInt(offset) : -1;
        }

        short getInt16(int offset) {
            return isAvailable(offset, 2) ? data.getShort(offset) : -1;
        }

        private boolean isAvailable(int offset, int byteSize) {
            return data.remaining() - offset >= byteSize;
        }
    }

    static Bitmap rotateImage(Bitmap bitmap, int orientation) {
        final Matrix matrix = new Matrix();
        switch (orientation) {
            case ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }
}
