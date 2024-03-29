package com.felipecsl.gifimageview.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;
import io.rong.common.rlog.RLog;

public class GifImageView extends ImageView implements Runnable {
    private static final String TAG = "GifDecoderView";
    private GifDecoder gifDecoder;
    private Bitmap tmpBitmap;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean animating;
    private boolean renderFrame;
    private boolean shouldClear;
    private Thread animationThread;
    private OnFrameAvailable frameCallback = null;
    private long framesDisplayDuration = -1L;
    private OnAnimationStop animationStopCallback = null;
    private OnAnimationStart animationStartCallback = null;

    private final Runnable updateResults =
            new Runnable() {
                @Override
                public void run() {
                    if (tmpBitmap != null && !tmpBitmap.isRecycled()) {
                        setImageBitmap(tmpBitmap);
                    }
                }
            };

    private final Runnable cleanupRunnable =
            new Runnable() {
                @Override
                public void run() {
                    synchronized (GifImageView.this) {
                        animating = false;
                        renderFrame = false;
                        tmpBitmap = null;
                        gifDecoder = null;
                        animationThread = null;
                        shouldClear = false;
                    }
                }
            };

    public GifImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public GifImageView(final Context context) {
        super(context);
    }

    public void setBytes(final byte[] bytes) {
        gifDecoder = new GifDecoder();
        try {
            gifDecoder.read(bytes);
        } catch (final Exception e) {
            gifDecoder = null;
            RLog.e(TAG, e.getMessage(), e);
            return;
        }

        if (animating) {
            startAnimationThread();
        } else {
            gotoFrame(0);
        }
    }

    public long getFramesDisplayDuration() {
        return framesDisplayDuration;
    }

    /**
     * Sets custom display duration in milliseconds for the all frames. Should be called before
     * {@link #startAnimation()}
     *
     * @param framesDisplayDuration Duration in milliseconds. Default value = -1, this property will
     *     be ignored and default delay from gif file will be used.
     */
    public void setFramesDisplayDuration(long framesDisplayDuration) {
        this.framesDisplayDuration = framesDisplayDuration;
    }

    public void startAnimation() {
        animating = true;
        renderFrame = true;
        startAnimationThread();
    }

    public boolean isAnimating() {
        return animating;
    }

    public void stopAnimation() {
        animating = false;

        if (animationThread != null) {
            animationThread.interrupt();
            animationThread = null;
        }
    }

    public void gotoFrame(int frame) {
        if (gifDecoder.getCurrentFrameIndex() == frame) return;
        if (gifDecoder.setFrameIndex(frame - 1) && !animating) {
            renderFrame = true;
            startAnimationThread();
        }
    }

    public void resetAnimation() {
        gifDecoder.resetLoopIndex();
        gotoFrame(0);
    }

    public void clearSync() {
        synchronized (this) {
            animating = false;
            renderFrame = false;
            shouldClear = true;
            stopAnimation();
            animating = false;
            renderFrame = false;
            tmpBitmap = null;
            gifDecoder = null;
            animationThread = null;
            shouldClear = false;
        }
    }

    public void clear() {
        animating = false;
        renderFrame = false;
        shouldClear = true;
        stopAnimation();
        handler.post(cleanupRunnable);
    }

    private boolean canStart() {
        return (animating || renderFrame) && gifDecoder != null && animationThread == null;
    }

    public int getGifWidth() {
        return gifDecoder.getWidth();
    }

    /**
     * Gets the number of frames read from file.
     *
     * @return frame count.
     */
    public int getFrameCount() {
        return gifDecoder.getFrameCount();
    }

    public int getGifHeight() {
        return gifDecoder.getHeight();
    }

    @Override
    public void run() {
        if (animationStartCallback != null) {
            animationStartCallback.onAnimationStart();
        }

        do {
            boolean advance;
            synchronized (this) {
                if (!animating && !renderFrame) {
                    break;
                }
                if (gifDecoder == null) {
                    break;
                }
                advance = gifDecoder.advance();
            }

            // milliseconds spent on frame decode
            long frameDecodeTime = 0;
            try {
                long before = System.nanoTime();
                synchronized (this) {
                    if (!animating && !renderFrame) {
                        break;
                    }
                    if (gifDecoder == null) {
                        break;
                    }
                    tmpBitmap = gifDecoder.getNextFrame();
                }
                if (frameCallback != null) {
                    tmpBitmap = frameCallback.onFrameAvailable(tmpBitmap);
                }
                frameDecodeTime = (System.nanoTime() - before) / 1000000;
                handler.post(updateResults);
            } catch (final ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                RLog.w(TAG, "e:" + e);
            }

            renderFrame = false;
            if (!animating || !advance) {
                animating = false;
                break;
            }
            try {
                int delay;
                synchronized (this) {
                    if (!animating && !renderFrame) {
                        break;
                    }
                    if (gifDecoder == null) {
                        break;
                    }
                    delay = gifDecoder.getNextDelay();
                }
                // Sleep for frame duration minus time already spent on frame decode
                // Actually we need next frame decode duration here,
                // but I use previous frame time to make code more readable
                delay -= frameDecodeTime;
                if (delay > 0) {
                    Thread.sleep(framesDisplayDuration > 0 ? framesDisplayDuration : delay);
                }
            } catch (final InterruptedException e) {
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            }
        } while (animating);

        if (shouldClear) {
            handler.post(cleanupRunnable);
        }
        animationThread = null;

        if (animationStopCallback != null) {
            animationStopCallback.onAnimationStop();
        }
    }

    public OnFrameAvailable getOnFrameAvailable() {
        return frameCallback;
    }

    public void setOnFrameAvailable(OnFrameAvailable frameProcessor) {
        this.frameCallback = frameProcessor;
    }

    public interface OnFrameAvailable {
        Bitmap onFrameAvailable(Bitmap bitmap);
    }

    public OnAnimationStop getOnAnimationStop() {
        return animationStopCallback;
    }

    public void setOnAnimationStop(OnAnimationStop animationStop) {
        this.animationStopCallback = animationStop;
    }

    public void setOnAnimationStart(OnAnimationStart animationStart) {
        this.animationStartCallback = animationStart;
    }

    public interface OnAnimationStop {
        void onAnimationStop();
    }

    public interface OnAnimationStart {
        void onAnimationStart();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearSync();
    }

    private void startAnimationThread() {
        if (canStart()) {
            animationThread = new Thread(this);
            animationThread.start();
        }
    }
}
