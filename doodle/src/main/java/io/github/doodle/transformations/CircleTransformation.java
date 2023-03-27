package io.github.doodle.transformations;

import android.graphics.*;
import io.github.doodle.interfaces.Transformation;

public class CircleTransformation implements Transformation {
    @Override
    public Bitmap transform(Bitmap source) {
        int w = source.getWidth();
        int h = source.getHeight();
        int d = Math.min(w, h);
        int r = d >> 1;
        int x = w >> 1;
        int y = h >> 1;

        Rect src = new Rect(x - r, y - r, x + r, y + r);
        RectF dst = new RectF(0F, 0F, d, d);

        Bitmap.Config config = source.getConfig();
        Bitmap output = Bitmap.createBitmap(d, d, config != null ? config : Bitmap.Config.ARGB_8888);
        output.setDensity(source.getDensity());

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawOval(dst, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, src, dst, paint);

        return output;
    }

    @Override
    public String key() {
        return "Circle";
    }
}

