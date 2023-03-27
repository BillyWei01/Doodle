package io.github.doodle.interfaces;

import android.graphics.Bitmap;

public interface Transformation {
    Bitmap transform(Bitmap source);

    /**
     * @return identify of this transformation, part of request key, <br/>
     * suggest to be "constance string" or "constance string + final parameter".
     */
    String key();
}
