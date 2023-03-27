package io.github.doodle.enums;

public enum DecodeFormat {
    /**
     * Use {@link android.graphics.Bitmap.Config#ARGB_8888}
     */
    ARGB_8888('0'),

    /**
     * Use {@link android.graphics.Bitmap.Config#RGB_565}
     */
    RGB_565('1'),

    /**
     * If the source does not have alpha channel, use RGB_565, otherwise use ARGB_8888.
     * RGB_565 is not sharp enough, be care of using this mode.
     */
    AUTO('2');

    public final char nativeChar;

    DecodeFormat(char ni) {
        this.nativeChar = ni;
    }
}
