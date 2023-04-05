package io.github.doodle.interfaces;


import io.github.doodle.DecodingInfo;

/**
 * Decode the image file if the resource match your pattern.
 * Especially, you could use this to handle animated gif image.
 */
public interface AnimatedDecoder {
    /**
     * If the image file is just one frame, it's recommend to return a bitmap,
     * then Doodle will cache the bitmap to speed up next loading.
     *
     * @param info Source info (path, type, header) and decoding params.
     *             You could decode the file with decoding params
     *             if the resource match your pattern (check by source info).
     * @return drawable, bitmap, or result with other type. Return null if the source not match your pattern.
     */
    Object decode(DecodingInfo info);
}
