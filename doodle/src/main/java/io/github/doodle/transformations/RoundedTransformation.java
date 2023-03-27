package io.github.doodle.transformations;

import android.graphics.*;
import io.github.doodle.interfaces.Transformation;

/**
 * Rounded corner transformation
 */
public class RoundedTransformation implements Transformation {
    private final int mRadius;

    /**
     * @param radius corner radius，in pixel
     */
    public RoundedTransformation(int radius) {
        mRadius = radius;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        if (mRadius <= 0) {
            return source;
        }

        int w = source.getWidth();
        int h = source.getHeight();
        Rect rect = new Rect(0, 0, w, h);

        Bitmap.Config config = source.getConfig();
        Bitmap output = Bitmap.createBitmap(w, h, config != null ? config : Bitmap.Config.ARGB_8888);
        output.setDensity(source.getDensity());

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        canvas.drawRoundRect(new RectF(rect), mRadius, mRadius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, rect, rect, paint);

        return output;
    }

    @Override
    public String key() {
        return "Rounded:" + mRadius;
    }
}
