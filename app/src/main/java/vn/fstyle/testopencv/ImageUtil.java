package vn.fstyle.testopencv;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.webkit.MimeTypeMap;

import java.util.Locale;

/**
 * ImageUtil.
 *
 * @author DaoLQ
 */
public class ImageUtil {
    private ImageUtil() {
    }

    public static Bitmap resizeImage(@NonNull Bitmap bitmap, int size) {

        int imageW = bitmap.getWidth();
        int imageH = bitmap.getHeight();

        if (imageW > size || imageH > size) {
            float scale = (imageW < imageH ? (float) size / (float) imageH : (float) size / (float) imageW);
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) (imageW * scale), (int) (imageH * scale), true);
            return bitmap;
        } else {
            return bitmap;
        }
    }

    public static Bitmap cropByPercentage(@NonNull Bitmap bitmap, float percentage) {

        if (percentage < 0.0F || percentage > 1.0F) {
            return bitmap;
        }

        int imageW = bitmap.getWidth();
        int imageH = bitmap.getHeight();

        int size = (imageW < imageH ? Math.round(imageW * percentage) : Math.round(imageH * percentage));
        int x = (imageW - size) / 2;
        int y = (imageH - size) / 2;

        bitmap = Bitmap.createBitmap(bitmap, x, y, size, size);
        return bitmap;
    }

    public static Bitmap getThumbnailFromVideo(@NonNull String videoUrl) {
        return ThumbnailUtils.createVideoThumbnail(videoUrl, MediaStore.Video.Thumbnails.MINI_KIND);
    }

    public static boolean isImageUrl(@NonNull final String url) {
        final String jpegMime = "image/jpeg";
        final String pngMime = "image/png";
        String ext = url.substring(url.lastIndexOf('.') + 1);
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.getDefault()));
        return jpegMime.equals(mime) || pngMime.equals(mime);
    }

}
