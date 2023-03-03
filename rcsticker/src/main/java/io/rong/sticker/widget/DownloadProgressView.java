package io.rong.sticker.widget;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.IntDef;
import io.rong.sticker.R;
import java.lang.annotation.Retention;

/** Created by luoyanlong on 2018/08/21. 下载进度控件 */
public class DownloadProgressView extends View {

    private Paint bgPaint;
    private Paint tvPaint;
    private RectF rectF;
    private int strokeWidth;
    private int radius;
    private int progress = 30;
    private int status = NOT_DOWNLOAD;

    public DownloadProgressView(Context context) {
        super(context);
        init();
    }

    public DownloadProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokeWidth =
                getResources().getDimensionPixelOffset(R.dimen.download_progress_stroke_width);
        bgPaint.setStrokeWidth(strokeWidth);

        tvPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tvPaint.setColor(0xff999999);
        tvPaint.setTextSize(
                getResources().getDimensionPixelSize(R.dimen.download_progress_text_size));
        tvPaint.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();
        rectF.left = strokeWidth / 2;
        rectF.top = strokeWidth / 2;

        radius = getResources().getDimensionPixelSize(R.dimen.download_progress_radius);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (h != oldh) {
            rectF.bottom = h - strokeWidth / 2;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (status == NOT_DOWNLOAD) {
            drawNotDownload(canvas);
            tvPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.download_text_size));
            tvPaint.setColor(Color.WHITE);
            drawText(canvas, getResources().getString(R.string.download_sticker));
        } else {
            drawDownloading(canvas);
            tvPaint.setTextSize(
                    getResources().getDimensionPixelSize(R.dimen.download_progress_text_size));
            tvPaint.setColor(0x50333333);
            String text = progress + "%";
            drawText(canvas, text);
        }
    }

    public void setStatus(@Status int status) {
        this.status = status;
        invalidate();
    }

    public void setProgress(int progress) {
        if (status == NOT_DOWNLOAD) {
            status = DOWNLOADING;
        }
        this.progress = progress;
        invalidate();
    }

    private void drawNotDownload(Canvas canvas) {
        bgPaint.setColor(0xff0099ff);
        bgPaint.setStyle(Paint.Style.FILL);
        rectF.right = getWidth() - strokeWidth;
        canvas.drawRoundRect(rectF, radius, radius, bgPaint);
    }

    private void drawDownloading(Canvas canvas) {
        bgPaint.setColor(0xff6dc4ff);
        bgPaint.setStyle(Paint.Style.STROKE);
        rectF.right = getWidth() - strokeWidth;
        canvas.drawRoundRect(rectF, radius, radius, bgPaint);
        rectF.right = rectF.left + (float) progress / 100 * (getWidth() - strokeWidth);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rectF, radius, radius, bgPaint);
    }

    private void drawText(Canvas canvas, String text) {
        int x = getWidth() / 2;
        int y = (int) ((getHeight() / 2) - ((tvPaint.descent() + tvPaint.ascent()) / 2));
        canvas.drawText(text, x, y, tvPaint);
    }

    @Retention(SOURCE)
    @IntDef({NOT_DOWNLOAD, DOWNLOADING})
    public @interface Status {
        // default implementation ignored
    }

    public static final int NOT_DOWNLOAD = 0;
    public static final int DOWNLOADING = 1;
}
