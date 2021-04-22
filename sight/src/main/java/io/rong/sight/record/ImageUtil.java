package io.rong.sight.record;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * 445263848@qq.com.
 */
public class ImageUtil {
    public static Bitmap getRotateBitmap(Bitmap bitmap, float rotateDegree) {
        Matrix matrix = new Matrix();
        matrix.setRotate(rotateDegree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }
}
