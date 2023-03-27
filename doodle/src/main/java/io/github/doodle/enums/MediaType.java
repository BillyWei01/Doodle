package io.github.doodle.enums;

public enum MediaType {
    UNKNOWN,

    MP4,
    MOV,
    _3GP,
    MKV,

    GIF,
    JPG,
    PNG,
    PNG_NO_ALPHA,
    WEBP_LOSSY,
    WEBP_LOSSY_NO_ALPHA,
    WEBP_LOSSLESS,
    WEBP_LOSSLESS_NO_ALPHA,
    WEBP_ANIMATED,
    BMP,
    HEIF;

    public boolean isVideo() {
        return this == MP4 || this == MOV || this == _3GP || this == MKV;
    }

    public boolean isLossy() {
        return this == JPG || this == WEBP_LOSSY || this == WEBP_LOSSY_NO_ALPHA;
    }

    public boolean noAlpha() {
        return this == JPG ||
                this == PNG_NO_ALPHA ||
                this == WEBP_LOSSY_NO_ALPHA ||
                this == WEBP_LOSSLESS_NO_ALPHA;
    }
}
