package io.rong.imkit.widget.glide;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.util.Util;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

public class RoundedCornersTransform extends BitmapTransformation {
    private final String ID = getClass().getName();

    private Float[] radius = new Float[4];

    /**
     * 构造方法
     *
     * @param tl 左上
     * @param tr 右上
     * @param br 右下
     * @param bl 左下
     */
    public RoundedCornersTransform(float tl, float tr, float br, float bl) {
        this.radius[0] = tl;
        this.radius[1] = tr;
        this.radius[2] = br;
        this.radius[3] = bl;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap source, int outWidth, int outHeight) {
        Bitmap outBitmap = pool.get(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outBitmap);
        Paint paint = new Paint();
        //关联画笔绘制的原图bitmap
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        RectF rectF = new RectF(0, 0, canvas.getWidth(), canvas.getHeight());
        // 左上
        float r = radius[0];
        canvas.save();
        canvas.clipRect(0, 0, canvas.getWidth() / 2, canvas.getHeight() / 2);
        canvas.drawRoundRect(rectF, r, r, paint);
        canvas.restore();
        // 右上
        r = radius[1];
        canvas.save();
        canvas.clipRect(canvas.getWidth() / 2, 0, canvas.getWidth(), canvas.getHeight() / 2);
        canvas.drawRoundRect(rectF, r, r, paint);
        canvas.restore();
        // 右下
        r = radius[2];
        canvas.save();
        canvas.clipRect(canvas.getWidth() / 2, canvas.getHeight() / 2, canvas.getWidth(), canvas.getHeight());
        canvas.drawRoundRect(rectF, r, r, paint);
        canvas.restore();
        // 左下
        r = radius[3];
        canvas.save();
        canvas.clipRect(0, canvas.getHeight() / 2, canvas.getWidth() / 2, canvas.getHeight());
        canvas.drawRoundRect(rectF, r, r, paint);
        canvas.restore();
        return outBitmap;
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(ID.getBytes(CHARSET));
        byte[] radiusData = ByteBuffer.allocate(4).putInt(Arrays.hashCode(radius)).array();
        messageDigest.update(radiusData);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RoundedCornersTransform) {
            return Arrays.equals(this.radius, ((RoundedCornersTransform) o).radius);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Util.hashCode(ID.hashCode(), Arrays.hashCode(radius));
    }
}
