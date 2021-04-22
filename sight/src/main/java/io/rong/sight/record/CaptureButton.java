package io.rong.sight.record;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import io.rong.sight.R;

/**
 * 445263848@qq.com.
 */
public class CaptureButton extends View {

    public final String TAG = "Sight-CaptureButton";

    private Paint mPaint;
    private Context mContext;

    private float btn_center_Y;
    private float btn_center_X;

    private float btn_inside_radius;
    private float btn_outside_radius;
    //before radius
    private float btn_before_inside_radius;
    private float btn_before_outside_radius;
    //after radius
    private float btn_after_inside_radius;
    private float btn_after_outside_radius;

    private float btn_return_length;
    private float btn_return_X;
    private float btn_return_Y;

    private float btn_left_X, btn_right_X, btn_result_radius;

    //state
    private int STATE_SELECTED;
    private final int STATE_LESSNESS = 0;
    private final int STATE_KEY_DOWN = 1;
    private final int STATE_CAPTURED = 2;
    private final int STATE_RECORD = 3;
    private final int STATE_PICTURE_BROWSE = 4;
    private final int STATE_RECORD_BROWSE = 5;
    private static final int STATE_READYQUIT = 6;
    private final int STATE_RECORDED = 7;

    private float key_down_Y;

    private float progress = 0;
    private int captureProgressed = 0;
    private LongPressRunnable longPressRunnable = new LongPressRunnable();
    private ValueAnimator record_anim = ValueAnimator.ofFloat(0, 360);
    private CaptureListener mCaptureListener;
    private boolean supportCapture = false;
    private int maxDuration = 10;
    private RecordRunnable recordRunnable = new RecordRunnable();
    //我们UX设计提供图片icon，只是把相关view draw隐藏
    private boolean hideSomeView = true;

    public CaptureButton(Context context) {
        this(context, null);
    }

    public CaptureButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptureButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        STATE_SELECTED = STATE_LESSNESS;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int height = (widthSize / 9) * 4;
        setMeasuredDimension(widthSize, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        btn_center_X = getWidth() / 2;
        btn_center_Y = getHeight() / 2;

        if (!hideSomeView) {
            btn_outside_radius = (float) (getWidth() / 9);
            btn_inside_radius = (float) (btn_outside_radius * 0.75);

            btn_before_outside_radius = (float) (getWidth() / 9);
            btn_before_inside_radius = (float) (btn_outside_radius * 0.75);
            btn_after_outside_radius = (float) (getWidth() / 6);
            btn_after_inside_radius = (float) (btn_outside_radius * 0.6);
        } else {
            initCaptureButtonRadius();

            btn_before_outside_radius = getResources().getDimension(R.dimen.sight_capture_button_circle_size_outer);
            btn_before_inside_radius = getResources().getDimension(R.dimen.sight_capture_button_circle_size_inner);
            btn_after_outside_radius = getResources().getDimension(R.dimen.sight_capture_button_record_circle_size_outer);
            btn_after_inside_radius = getResources().getDimension(R.dimen.sight_capture_button_record_circle_size_inner);
        }


        btn_return_length = (float) (btn_outside_radius * 0.35);
        btn_result_radius = (float) (getWidth() / 9);
        btn_left_X = getWidth() / 2;
        btn_right_X = getWidth() / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (STATE_SELECTED == STATE_LESSNESS || STATE_SELECTED == STATE_RECORD) {
            //draw capture button
            mPaint.setColor(getResources().getColor(R.color.color_sight_capture_button_circle_outer));
            canvas.drawCircle(btn_center_X, btn_center_Y, btn_outside_radius, mPaint);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(btn_center_X, btn_center_Y, btn_inside_radius, mPaint);

            //draw Progress bar
            Paint paintArc = new Paint();
            paintArc.setAntiAlias(true);
            paintArc.setColor(getResources().getColor(R.color.color_sight_primary));
            paintArc.setStyle(Paint.Style.STROKE);
            paintArc.setStrokeWidth(10);

            RectF rectF = new RectF(btn_center_X - (btn_after_outside_radius - 5),
                    btn_center_Y - (btn_after_outside_radius - 5),
                    btn_center_X + (btn_after_outside_radius - 5),
                    btn_center_Y + (btn_after_outside_radius - 5));
            canvas.drawArc(rectF, -90, progress, false, paintArc);

            if (!hideSomeView) {
                //draw return button
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4);
                Path path = new Path();

                btn_return_X = ((getWidth() / 2) - btn_outside_radius) / 2;
                btn_return_Y = ((float) getHeight() / 2 + 10);

                path.moveTo(btn_return_X - btn_return_length, btn_return_Y - btn_return_length);
                path.lineTo(btn_return_X, btn_return_Y);
                path.lineTo(btn_return_X + btn_return_length, btn_return_Y - btn_return_length);
                canvas.drawPath(path, paint);
            }
        } else if (STATE_SELECTED == STATE_RECORD_BROWSE || STATE_SELECTED == STATE_PICTURE_BROWSE) {

            if (hideSomeView) return;

            mPaint.setColor(getResources().getColor(R.color.color_sight_capture_button_circle_outer));
            canvas.drawCircle(btn_left_X, btn_center_Y, btn_result_radius, mPaint);
            mPaint.setColor(Color.WHITE);
            canvas.drawCircle(btn_right_X, btn_center_Y, btn_result_radius, mPaint);


            //left button
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            Path path = new Path();

            path.moveTo(btn_left_X - 2, btn_center_Y + 14);
            path.lineTo(btn_left_X + 14, btn_center_Y + 14);
            path.arcTo(new RectF(btn_left_X, btn_center_Y - 14, btn_left_X + 28, btn_center_Y + 14), 90, -180);
            path.lineTo(btn_left_X - 14, btn_center_Y - 14);
            canvas.drawPath(path, paint);


            paint.setStyle(Paint.Style.FILL);
            path.reset();
            path.moveTo(btn_left_X - 14, btn_center_Y - 22);
            path.lineTo(btn_left_X - 14, btn_center_Y - 6);
            path.lineTo(btn_left_X - 23, btn_center_Y - 14);
            path.close();
            canvas.drawPath(path, paint);


            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0xFF00CC00);
            paint.setStrokeWidth(4);
            path.reset();
            path.moveTo(btn_right_X - 28, btn_center_Y);
            path.lineTo(btn_right_X - 8, btn_center_Y + 22);
            path.lineTo(btn_right_X + 30, btn_center_Y - 20);
            path.lineTo(btn_right_X - 8, btn_center_Y + 18);
            path.close();
            canvas.drawPath(path, paint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (STATE_SELECTED == STATE_LESSNESS) {
                    if (!hideSomeView) {
                        if (event.getY() > btn_return_Y - 37 &&
                                event.getY() < btn_return_Y + 10 &&
                                event.getX() > btn_return_X - 37 &&
                                event.getX() < btn_return_X + 37) {
                            STATE_SELECTED = STATE_READYQUIT;
                        }
                    } else if (event.getY() > btn_center_Y - btn_outside_radius &&
                            event.getY() < btn_center_Y + btn_outside_radius &&
                            event.getX() > btn_center_X - btn_outside_radius &&
                            event.getX() < btn_center_X + btn_outside_radius &&
                            event.getPointerCount() == 1
                    ) {
                        key_down_Y = event.getY();
                        STATE_SELECTED = STATE_KEY_DOWN;
                        postCheckForLongTouch();
                    }
                } else if (STATE_SELECTED == STATE_RECORD_BROWSE || STATE_SELECTED == STATE_PICTURE_BROWSE) {
                    if (event.getY() > btn_center_Y - btn_result_radius &&
                            event.getY() < btn_center_Y + btn_result_radius &&
                            event.getX() > btn_left_X - btn_result_radius &&
                            event.getX() < btn_left_X + btn_result_radius &&
                            event.getPointerCount() == 1
                    ) {
                        retryRecord();
                    } else if (event.getY() > btn_center_Y - btn_result_radius &&
                            event.getY() < btn_center_Y + btn_result_radius &&
                            event.getX() > btn_right_X - btn_result_radius &&
                            event.getX() < btn_right_X + btn_result_radius &&
                            event.getPointerCount() == 1
                    ) {
                        submitRecord();
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getY() > btn_center_Y - btn_outside_radius &&
                        event.getY() < btn_center_Y + btn_outside_radius &&
                        event.getX() > btn_center_X - btn_outside_radius &&
                        event.getX() < btn_center_X + btn_outside_radius
                ) {
                }
                if (mCaptureListener != null) {
                    mCaptureListener.scale(key_down_Y - event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(longPressRunnable);
                if (STATE_SELECTED == STATE_READYQUIT) {
                    if (!hideSomeView) {
                        if (event.getY() > btn_return_Y - 37 &&
                                event.getY() < btn_return_Y + 10 &&
                                event.getX() > btn_return_X - 37 &&
                                event.getX() < btn_return_X + 37) {
                            STATE_SELECTED = STATE_LESSNESS;
                            if (mCaptureListener != null) {
                                mCaptureListener.quit();
                            }
                        }
                    }
                } else if (STATE_SELECTED == STATE_KEY_DOWN) {
                    if (supportCapture) {
                        if (event.getY() > btn_center_Y - btn_outside_radius &&
                                event.getY() < btn_center_Y + btn_outside_radius &&
                                event.getX() > btn_center_X - btn_outside_radius &&
                                event.getX() < btn_center_X + btn_outside_radius) {
                            capture();
                        }
                    } else {
                        STATE_SELECTED = STATE_LESSNESS;
                        initCaptureButtonRadius();
                        invalidate();
                    }
                } else if (STATE_SELECTED == STATE_RECORD) {
                    recordEnd(true);
                }
                break;
        }
        return true;
    }

    public void captureSuccess() {
        captureAnimation(getWidth() / 5, (getWidth() / 5) * 4);
    }

    private void postCheckForLongTouch() {
        postDelayed(longPressRunnable, 500);
    }


    private class LongPressRunnable implements Runnable {
        @Override
        public void run() {
            startAnimation(btn_before_outside_radius, btn_after_outside_radius, btn_before_inside_radius, btn_after_inside_radius);
            STATE_SELECTED = STATE_RECORD;
        }
    }

    private class RecordRunnable implements Runnable {

        public RecordRunnable() {
            record_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (STATE_SELECTED == STATE_RECORD) {
                        progress = (float) animation.getAnimatedValue();
                        int newProgress = (int) (animation.getCurrentPlayTime() / 1000);
                        if (newProgress != captureProgressed) {
                            captureProgressed = newProgress;
                            if (mCaptureListener != null && captureProgressed >= 1) {
                                mCaptureListener.recordProgress(captureProgressed);
                            }
                        }
                        invalidate();
                    }
                }
            });
            record_anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (STATE_SELECTED == STATE_RECORD) {
                        STATE_SELECTED = STATE_RECORD_BROWSE;
                        progress = 0;
                        captureProgressed = 0;
                        invalidate();
                        captureAnimation(getWidth() / 5, (getWidth() / 5) * 4);
                        startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius);
                        if (mCaptureListener != null) {
                            mCaptureListener.recordEnd(maxDuration * 1000L);
                        }
                    }
                }
            });
            record_anim.setInterpolator(new LinearInterpolator());
            record_anim.setDuration(maxDuration * 1000);
            record_anim.setStartDelay(1000);
        }

        @Override
        public void run() {
            if (mCaptureListener != null) {
                mCaptureListener.record();
            }
            record_anim.start();
        }
    }

    private void startAnimation(float outside_start, float outside_end, float inside_start, float inside_end) {

        ValueAnimator outside_anim = ValueAnimator.ofFloat(outside_start, outside_end);
        ValueAnimator inside_anim = ValueAnimator.ofFloat(inside_start, inside_end);
        outside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_outside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }

        });
        outside_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (STATE_SELECTED == STATE_RECORD) {
                    post(recordRunnable);
                }
            }
        });
        inside_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_inside_radius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        outside_anim.setDuration(100);
        inside_anim.setDuration(100);
        outside_anim.start();
        inside_anim.start();
    }

    private void captureAnimation(float left, float right) {
        ValueAnimator left_anim = ValueAnimator.ofFloat(btn_left_X, left);
        ValueAnimator right_anim = ValueAnimator.ofFloat(btn_right_X, right);
        left_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_left_X = (float) animation.getAnimatedValue();
                invalidate();
            }

        });
        right_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                btn_right_X = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        left_anim.setDuration(200);
        right_anim.setDuration(200);
        left_anim.start();
        right_anim.start();
    }

    public void setCaptureListener(CaptureListener mCaptureListener) {
        this.mCaptureListener = mCaptureListener;
    }

    public interface CaptureListener {
        /**
         * 拍照
         */
        void capture();

        /**
         * 拍照预览时选择重新拍摄
         */
        void cancel();

        /**
         * 拍照预览时选择确认
         */
        void determine();

        /**
         * 退出小视频拍摄
         */
        void quit();

        /**
         * 开始录制视频
         */
        void record();

        /**
         * 录制结束
         */
        void recordEnd(long duration);

        /**
         * 录制预览时选择确认时执行
         */
        void getRecordResult();

        /**
         * 录制预览时选择重新拍摄删除当前拍摄视频时执行
         */
        void deleteRecordResult();

        /**
         * 录制时放大
         */
        void scale(float scaleValue);

        /**
         * 录制时更新时间进度
         */
        void recordProgress(int progress);

        /**
         * 录制时间短时提示用户后直接重新拍摄
         */
        void retryRecord();
    }

    public void setSupportCapture(boolean support) {
        supportCapture = support;
    }

    public void setMaxRecordDuration(int duration) {
        maxDuration = duration;
        if (record_anim != null) {
            record_anim.setDuration(maxDuration * 1000);
        }
    }

    /**
     * 拍摄预览界面选择确认发送小视频
     */
    public void submitRecord() {
        if (mCaptureListener != null) {
            if (STATE_SELECTED == STATE_RECORD_BROWSE) {
                mCaptureListener.getRecordResult();
            } else if (STATE_SELECTED == STATE_PICTURE_BROWSE) {
                mCaptureListener.determine();
            }
        }
        btn_left_X = btn_center_X;
        btn_right_X = btn_center_X;
        invalidate();
    }

    /**
     * 拍摄预览界面选择重新拍摄
     */
    public void retryRecord() {
        if (mCaptureListener != null) {
            if (STATE_SELECTED == STATE_RECORD_BROWSE) {
                mCaptureListener.deleteRecordResult();
            } else if (STATE_SELECTED == STATE_PICTURE_BROWSE) {
                mCaptureListener.cancel();
            }
        }
        STATE_SELECTED = STATE_LESSNESS;
        btn_left_X = btn_center_X;
        btn_right_X = btn_center_X;
        invalidate();
    }

    private void capture() {
        if (supportCapture) {
            if (mCaptureListener != null) {
                mCaptureListener.capture();
            }
            STATE_SELECTED = STATE_PICTURE_BROWSE;
        } else {
            STATE_SELECTED = STATE_LESSNESS;
            initCaptureButtonRadius();
            invalidate();
        }
    }

    private void recordEnd(boolean needAnimation) {
        long playTime = record_anim.getCurrentPlayTime();
        Log.d(TAG, "recordEnd " + playTime);
        progress = 0;
        captureProgressed = 0;
        if (!record_anim.isRunning() || playTime < 1000) {
            Log.d(TAG, "recordEnd-retryRecord()");
            if (needAnimation) {
                Toast.makeText(mContext, R.string.rc_sight_record_too_short_time, Toast.LENGTH_SHORT).show();
            }
            if (mCaptureListener != null) {
                mCaptureListener.retryRecord();
            }
            STATE_SELECTED = STATE_LESSNESS;
            record_anim.cancel();
            invalidate();
        } else {
            STATE_SELECTED = STATE_RECORD_BROWSE;
            removeCallbacks(recordRunnable);
            if (needAnimation) {
                captureAnimation(getWidth() / 5, (getWidth() / 5) * 4);
            }
            record_anim.cancel();
            invalidate();
            if (mCaptureListener != null) {
                mCaptureListener.recordEnd(playTime);
            }
        }
        if (needAnimation) {
            startAnimation(btn_after_outside_radius, btn_before_outside_radius, btn_after_inside_radius, btn_before_inside_radius);
        } else {
            initCaptureButtonRadius();
        }
    }

    public void onPause() {
        Log.d(TAG, "onPause");
        removeCallbacks(longPressRunnable);
        if (STATE_SELECTED == STATE_KEY_DOWN) {
            capture();
        } else if (STATE_SELECTED == STATE_RECORD) {
            recordEnd(false);
        }
    }

    private void initCaptureButtonRadius() {
        btn_outside_radius = getResources().getDimension(R.dimen.sight_capture_button_circle_size_outer);
        btn_inside_radius = getResources().getDimension(R.dimen.sight_capture_button_circle_size_inner);
    }
}
