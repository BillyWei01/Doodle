package io.github.doodle;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Size;

import io.github.doodle.interfaces.BitmapDecoder;

class MediaThumbnailDecoder implements BitmapDecoder {
    static final String KEY = "ThumbnailDecoder";
    static final MediaThumbnailDecoder INSTANCE = new MediaThumbnailDecoder();

    @Override
    public Bitmap decode(DecodingInfo info) {
        String path = info.path;
        if (!(path.startsWith("content://media/") && info.options.containsKey(KEY))) {
            return null;
        }
        Bitmap bitmap = null;
        try {
            Uri uri = Uri.parse(path);
            ContentResolver contentResolver = Utils.appContext.getContentResolver();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    bitmap = contentResolver.loadThumbnail(uri, new Size(info.targetWidth, info.targetHeight), null);
                } catch (Exception ignore) {
                }
            }
            if (bitmap == null) {
                int index = path.lastIndexOf('/');
                if (index > 0) {
                    long mediaId = Long.parseLong(path.substring(index + 1));
                    bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                            contentResolver,
                            mediaId,
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                    );
                }
            }
        } catch (Throwable e) {
            LogProxy.e("Doodle", e);
        }
        return bitmap;
    }
}
