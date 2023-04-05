package io.github.doodle.interfaces;

import android.graphics.drawable.Drawable;

/**
 * This interface defines for custom views which is not instance of {@link android.widget.ImageView}. <br>
 * Note: The implementation class of CustomView should be instance of  {@link android.view.View}
 */
public interface CustomView {
    void setDrawable(Drawable drawable);

    Drawable getDrawable();

    void handleResult(Object result);
}
