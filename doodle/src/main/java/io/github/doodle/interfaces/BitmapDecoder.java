package io.github.doodle.interfaces;

import android.graphics.Bitmap;

import io.github.doodle.DecodingInfo;

public interface BitmapDecoder {
    /**
     * @param info Source info (path, type, header) and decoding params.
     *             You could decode the file with decoding params,
     *             if the source match your pattern (check by source info).
     * @return bitmap or null.
     */
    Bitmap decode(DecodingInfo info);
}
