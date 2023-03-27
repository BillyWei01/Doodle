package io.github.doodle.interfaces;


import android.graphics.drawable.Drawable;

import io.github.doodle.DecodingInfo;

/**
 * Decode image custom if the resource match your pattern.
 * Especially, you could use this to handle animated gif image.
 */
public interface DrawableDecoder {
    /**
     * If the image file is just one frame, it's recommend to return a BitmapDrawable,
     * then Doodle will cache the bitmap to speed up next loading.
     *
     * @param info Source info (path, type, header) and decoding params.
     *             You could decode the file with decoding params
     *             if the resource match your pattern (check by source info).
     * @return drawable or null
     */
    Drawable decode(DecodingInfo info);
}
