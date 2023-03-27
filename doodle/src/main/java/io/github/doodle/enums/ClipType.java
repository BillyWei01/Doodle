package io.github.doodle.enums;

import android.widget.ImageView;

public enum ClipType {
    NOT_SET('0'),
    NO_CLIP('1'),
    MATRIX('2'),
    CENTER('3'),

    /**
     * Scales, maintaining the original aspect ratio, so that one of the image's dimensions is exactly
     * equal to the requested size and the other dimension is less than or equal to the requested
     * size.<p>
     * <p>
     * When Request#enableUpscaleis true,
     * This method will upscale if the requested width and height are greater than the source width and height.
     */
    FIT_CENTER('4'),

    /**
     * Scales, maintaining the original aspect ratio, so that one of the image's dimensions is exactly
     * equal to the requested size and the other dimension is greater than or equal to the requested
     * size. <p>
     * <p>
     * When Request#enableUpscale is true,
     * this method will upscale if the requested width and height are greater than the source width and height.
     */
    CENTER_CROP('5'),

    /**
     * Identical to {@link #FIT_CENTER}, but never upscales.
     */
    CENTER_INSIDE('6');

    public final char nativeChar;

    ClipType(char ni) {
        this.nativeChar = ni;
    }

    public static ClipType mapScaleType(ImageView.ScaleType scaleType) {
        switch (scaleType) {
            case MATRIX:
                return MATRIX;
            case CENTER:
                return CENTER;
            case FIT_START:
            case FIT_END:
            case FIT_CENTER:
                return FIT_CENTER;
            case CENTER_CROP:
                return CENTER_CROP;
            default:
                return CENTER_INSIDE;
        }
    }
}
