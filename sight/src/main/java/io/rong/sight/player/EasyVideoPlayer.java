package io.rong.sight.player;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;

import java.io.File;
import java.io.IOException;

import io.rong.common.RLog;
import io.rong.imkit.utils.KitStorageUtils;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.widget.dialog.OptionsPopupDialog;
import io.rong.sight.R;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class EasyVideoPlayer extends FrameLayout implements IUserMethods, TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnErrorListener, View.OnClickListener, View.OnLongClickListener, SeekBar.OnSeekBarChangeListener, MediaPlayer.OnInfoListener {

    public static final String TAG = "Sight-EasyVideoPlayer";
    private static final int UPDATE_INTERVAL = 100;
    public static final int PLAYER_STATUS_STARTED = 1;
    public static final int PLAYER_STATUS_PAUSED = 2;
    public static final int PLAYER_STATUS_PREPARING = 3;
    public static final int PLAYER_STATUS_PREPARED = 4;
    public static final int PLAYER_STATUS_COMPLETION = 5;
    public static final int PLAYER_STATUS_CLOSE = 6;
    private int currentPlayerStatus;
    private int beforePausePlayerStatus;


    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private TextureView mTextureView;
    private Surface mSurface;

    private View mControlsFrame;
    private View mClickFrame;
    private ImageView mImageViewClose;
    private ImageView mImageViewSightList;
    private ImageView mImageViewPlay;

    private SeekBar mSeeker;
    private TextView mCurrent;
    private TextView mTime;
    private ImageView mBtnPlayPause;

    private MediaPlayer mPlayer;
    private boolean mSurfaceAvailable;
    private boolean mIsPrepared;
    private boolean mWasPlaying;

    private Handler mHandler;

    private Uri mSource;
    private EasyVideoCallback mCallback;
    private EasyVideoProgressCallback mProgressCallback;

    private boolean mHideControlsOnPlay = true;
    private boolean mAutoPlay;
    private int mInitialPosition = -1;
    private boolean mControlsDisabled;
    private int mThemeColor = 0;
    private boolean mAutoFullscreen = false;
    private boolean mLoop = false;
    private OnInfoCallBack onInfoCallBack;
    private int currentPos;
    private boolean isLongClickable;
    // Runnable used to run code on an interval to update counters and seeker
    private final Runnable mUpdateCounters = new Runnable() {
        @Override
        public void run() {
            if (mHandler == null || !mIsPrepared || mSeeker == null || mPlayer == null)
                return;
            int pos = mPlayer.getCurrentPosition();
            final int dur = mPlayer.getDuration();
            if ("oppo".equals(Build.BRAND.toLowerCase()) && "OPPO R9sk".equals(Build.MODEL)) {
                if (pos <= currentPos) {
                    pos = currentPos;
                }
            }
            if (pos > dur) pos = dur;
            mCurrent.setText(Util.getDurationString(pos));
            //mTime.setText(Util.getDurationString(Math.round(dur)));
            mSeeker.setProgress(pos);
            mSeeker.setMax(dur);

            if (mProgressCallback != null)
                mProgressCallback.onVideoProgressUpdate(pos, dur);
            if (mHandler != null)
                mHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    public EasyVideoPlayer(Context context) {
        super(context);
        init(context, null);
    }

    public EasyVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public EasyVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setBackgroundColor(Color.BLACK);
        mHideControlsOnPlay = true;
        mAutoPlay = true;
        mControlsDisabled = false;
        mThemeColor = 0x3F51B5;
        mAutoFullscreen = false;
        mLoop = false;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                // do nothing
            }
        };
    }

    // View events
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        setKeepScreenOn(true);

        mHandler = new Handler();
        mPlayer = new MediaPlayer();
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnVideoSizeChangedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setLooping(mLoop);
        mPlayer.setOnInfoListener(this);

        // Instantiate and add TextureView for rendering
        final FrameLayout.LayoutParams textureLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        textureLp.gravity = Gravity.CENTER;
        mTextureView = new TextureView(getContext());
        addView(mTextureView, textureLp);
        mTextureView.setSurfaceTextureListener(this);

        final LayoutInflater li = LayoutInflater.from(getContext());

        // Instantiate and add click frame (used to toggle controls)
        mClickFrame = new FrameLayout(getContext());
        //noinspection RedundantCast
        // ((FrameLayout) mClickFrame).setForeground(Util.resolveDrawable(getContext(), R.attr.selectableItemBackground));
        addView(mClickFrame, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        initPlayView();
        initCloseView();
        initSightListView();
        // Inflate controls
        mControlsFrame = li.inflate(R.layout.rc_sight_play_control, this, false);
        if (mControlsFrame == null) {
            return;
        }
        final FrameLayout.LayoutParams controlsLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsLp.gravity = Gravity.BOTTOM;
        addView(mControlsFrame, controlsLp);
        mControlsFrame.setVisibility(View.INVISIBLE);
        if (mControlsDisabled) {
            mClickFrame.setOnClickListener(null);
            mControlsFrame.setVisibility(View.GONE);
            mClickFrame.setOnLongClickListener(null);
        } else {
            mClickFrame.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleControls();
                }
            });
            mClickFrame.setOnLongClickListener(isLongClickable ? this : null);
        }


        // Retrieve controls
        mSeeker = mControlsFrame.findViewById(R.id.seeker);
        mSeeker.setOnSeekBarChangeListener(this);

        mCurrent = mControlsFrame.findViewById(R.id.current);
        mCurrent.setText(Util.getDurationString(0));

        mTime = mControlsFrame.findViewById(R.id.time);
        mTime.setText(Util.getDurationString(0));


        mBtnPlayPause = mControlsFrame.findViewById(R.id.btnPlayPause);
        mBtnPlayPause.setOnClickListener(this);
        mBtnPlayPause.setImageResource(R.drawable.rc_ic_sight_pause);

        invalidateThemeColors();

        setControlsEnabled(false);
        prepare();
    }

    private void initPlayView() {
        mImageViewPlay = new ImageView(getContext());
        FrameLayout.LayoutParams imageViewPlayParam = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        imageViewPlayParam.gravity = Gravity.CENTER;
        mImageViewPlay.setImageResource(R.drawable.rc_ic_sight_player_paly);
        addView(mImageViewPlay, imageViewPlayParam);
        mImageViewPlay.setVisibility(View.GONE);
        mImageViewPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
    }

    private void initCloseView() {
        mImageViewClose = new ImageView(getContext());
        FrameLayout.LayoutParams imageViewCloseParam = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        imageViewCloseParam.gravity = Gravity.TOP | Gravity.LEFT;
        int iconPadding = (int) getResources().getDimension(R.dimen.sight_record_icon_padding);
        mImageViewClose.setImageResource(R.drawable.rc_ic_sight_close);
        mImageViewClose.setPadding(iconPadding,iconPadding,iconPadding,iconPadding);
        addView(mImageViewClose, imageViewCloseParam);
        mImageViewClose.setVisibility(View.GONE);
        currentPlayerStatus = PLAYER_STATUS_CLOSE;
        mImageViewClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onClose();
                }
            }
        });
    }

    private void initSightListView() {
        mImageViewSightList = new ImageView(getContext());
        FrameLayout.LayoutParams imageViewSightListParam = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        imageViewSightListParam.gravity = Gravity.TOP | Gravity.RIGHT;
        int iconMargin = (int) getResources().getDimension(R.dimen.sight_record_top_icon_margin);
        imageViewSightListParam.setMargins(0, iconMargin, iconMargin, 0);
        mImageViewSightList.setImageResource(R.drawable.rc_ic_sight_list);
        addView(mImageViewSightList, imageViewSightListParam);
        mImageViewSightList.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onSightListRequest();
                }
            }
        });
    }

    @Override
    public void setSource(@NonNull Uri source) {
        boolean hadSource = mSource != null;
        if (hadSource) stop();
        mSource = source;
        RLog.i(TAG, "mSource = " + mSource);
        if (mPlayer != null) {
            if (hadSource) {
                sourceChanged();
            } else {
                prepare();
            }
        }
    }

    @Override
    public void setCallback(@NonNull EasyVideoCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setProgressCallback(@NonNull EasyVideoProgressCallback callback) {
        mProgressCallback = callback;
    }

    @Override
    public void setHideControlsOnPlay(boolean hide) {
        mHideControlsOnPlay = hide;
    }

    @Override
    public void setAutoPlay(boolean autoPlay) {
        mAutoPlay = autoPlay;
    }

    @Override
    public void setInitialPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        mInitialPosition = pos;
    }

    private void sourceChanged() {
        setControlsEnabled(false);
        mSeeker.setProgress(0);
        mSeeker.setEnabled(false);
        mPlayer.reset();
        currentPlayerStatus = PLAYER_STATUS_PREPARING;
        if (mCallback != null)
            mCallback.onPreparing(this);
        try {
            setSourceInternal();
        } catch (IOException e) {
            throwError(e);
        }
    }

    private void setSourceInternal() throws IOException {
        if (mSource.getScheme() != null &&
                (mSource.getScheme().equals("http") || mSource.getScheme().equals("https"))) {
            RLog.d(TAG, "Loading web URI: " + mSource.toString());
            mPlayer.setDataSource(mSource.toString());
        } else if (mSource.getScheme() != null && (mSource.getScheme().equals("file") && mSource.getPath().contains("/android_assets/"))) {
            RLog.d(TAG, "Loading assets URI: " + mSource.toString());
            AssetFileDescriptor afd;
            afd = getContext().getAssets().openFd(mSource.toString().replace("file:///android_assets/", ""));
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } else if (mSource.getScheme() != null && mSource.getScheme().equals("asset")) {
            RLog.d(TAG, "Loading assets URI: " + mSource.toString());
            AssetFileDescriptor afd;
            afd = getContext().getAssets().openFd(mSource.toString().replace("asset://", ""));
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } else {
            RLog.d(TAG, "Loading local URI: " + mSource.toString());
            mPlayer.setDataSource(getContext(), mSource);
        }
        mPlayer.prepareAsync();
    }

    private void prepare() {
        if (!mSurfaceAvailable || mSource == null || mPlayer == null || mIsPrepared) {
            return;
        }
        if (mCallback != null)
            mCallback.onPreparing(this);
        try {
            mPlayer.setSurface(mSurface);
            setSourceInternal();
        } catch (IOException e) {
            throwError(e);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        if (mSeeker == null) return;
        mSeeker.setEnabled(enabled);
        mBtnPlayPause.setEnabled(enabled);

        final float disabledAlpha = .4f;
        mBtnPlayPause.setAlpha(enabled ? 1f : disabledAlpha);

        mClickFrame.setEnabled(enabled);
    }

    public void setFromSightListImageInVisible() {
        mImageViewSightList.setVisibility(INVISIBLE);
    }

    @Override
    public void showControls() {
        if (mControlsDisabled || isControlsShown() || mSeeker == null) {
            return;
        }

        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(0f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mImageViewClose.setVisibility(View.VISIBLE);
        mControlsFrame.animate().alpha(1f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (mAutoFullscreen) {
                            setFullscreen(false);
                        }
                    }
                }).start();
    }

    @Override
    public void hideControls() {
        if (mControlsDisabled || !isControlsShown() || mSeeker == null) {
            return;
        }
        mControlsFrame.animate().cancel();
        mControlsFrame.setAlpha(1f);
        mControlsFrame.setVisibility(View.VISIBLE);
        mControlsFrame.animate().alpha(0f)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setFullscreen(true);

                        if (mControlsFrame != null) {
                            mImageViewClose.setVisibility(View.INVISIBLE);
                            mControlsFrame.setVisibility(View.INVISIBLE);
                        }

                    }
                }).start();
    }

    @CheckResult
    @Override
    public boolean isControlsShown() {
        return !mControlsDisabled && mControlsFrame != null && mControlsFrame.getAlpha() > .5f;
    }

    @Override
    public void toggleControls() {
        if (mControlsDisabled) return;
        if (isControlsShown()) {
            hideControls();
        } else {
            showControls();
        }
    }

    @Override
    public void enableControls(boolean andShow) {
        mControlsDisabled = false;
        if (andShow) showControls();
        mClickFrame.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleControls();
            }
        });
        mClickFrame.setClickable(true);
    }

    @Override
    public void disableControls() {
        mControlsDisabled = true;
        mControlsFrame.setVisibility(View.GONE);
        mClickFrame.setOnClickListener(null);
        mClickFrame.setClickable(false);
    }

    @CheckResult
    @Override
    public boolean isPrepared() {
        return mPlayer != null && mIsPrepared;
    }

    @CheckResult
    @Override
    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    @CheckResult
    @Override
    public int getCurrentPosition() {
        if (mPlayer == null) return -1;
        return mPlayer.getCurrentPosition();
    }

    @CheckResult
    @Override
    public int getDuration() {
        if (mPlayer == null) return -1;
        return mPlayer.getDuration();
    }

    @Override
    public void start() {
        if (mPlayer == null) return;
        audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        mPlayer.start();
        currentPlayerStatus = PLAYER_STATUS_STARTED;
        if (mCallback != null) mCallback.onStarted(this);
        if (mHandler == null) mHandler = new Handler();
        mHandler.post(mUpdateCounters);
        mBtnPlayPause.setImageResource(R.drawable.rc_ic_sight_pause);
        mImageViewPlay.setVisibility(View.GONE);
    }

    @Override
    public void seekTo(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos) {
        if (mPlayer == null) {
            return;
        }
        mPlayer.seekTo(pos);
    }

    public void setVolume(@FloatRange(from = 0f, to = 1f) float leftVolume, @FloatRange(from = 0f, to = 1f) float rightVolume) {
        if (mPlayer == null || !mIsPrepared) {
            throw new IllegalStateException("You cannot use setVolume(float, float) until the player is prepared.");
        }
        mPlayer.setVolume(leftVolume, rightVolume);
    }

    @Override
    public void pause() {
        beforePausePlayerStatus = currentPlayerStatus;
        currentPlayerStatus = PLAYER_STATUS_PAUSED;
        if (mPlayer == null || !isPlaying()) {
            return;
        }
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        mPlayer.pause();
        if (mCallback != null) {
            mCallback.onPaused(this);
        }
        if (mHandler == null) {
            return;
        }
        mHandler.removeCallbacks(mUpdateCounters);
        mBtnPlayPause.setImageResource(R.drawable.rc_ic_sight_play);
        mImageViewPlay.setVisibility(View.VISIBLE);
    }

    @Override
    public void stop() {
        if (mPlayer == null) return;
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        try {
            mPlayer.stop();
        } catch (Throwable ignored) {
        }
        if (mHandler == null) return;
        mHandler.removeCallbacks(mUpdateCounters);
        mBtnPlayPause.setImageResource(R.drawable.rc_ic_sight_pause);
    }

    @Override
    public void reset() {
        if (mPlayer == null) return;
        mPlayer.reset();
        mIsPrepared = false;
    }

    @Override
    public void release() {
        mIsPrepared = false;

        if (mPlayer != null) {
            try {
                mPlayer.release();
            } catch (Throwable ignored) {
            }
            mPlayer = null;
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateCounters);
            mHandler = null;
        }

        RLog.d(TAG, "Released player and Handler");
    }

    @Override
    public void setAutoFullscreen(boolean autoFullscreen) {
        this.mAutoFullscreen = autoFullscreen;
    }

    @Override
    public void setLoop(boolean loop) {
        mLoop = loop;
        if (mPlayer != null)
            mPlayer.setLooping(loop);
    }

    // Surface listeners

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        RLog.d(TAG, "Surface texture available: " + width + " " + height);
        mSurfaceAvailable = true;
        mSurface = new Surface(surfaceTexture);
        if (mIsPrepared) {
            mPlayer.setSurface(mSurface);
            if ("oppo".equals(Build.BRAND.toLowerCase()) && "OPPO R9sk".equals(Build.MODEL)) {
                mPlayer.seekTo(getCurrentPosition());
            }
        } else {
            prepare();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        RLog.d(TAG, "Surface texture changed: " + width + " " + height);
        adjustAspectRatio(width, height, mPlayer.getVideoWidth(), mPlayer.getVideoHeight());
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        RLog.d(TAG, "Surface texture destroyed");
        if ("oppo".equals(Build.BRAND.toLowerCase()) && "OPPO R9sk".equals(Build.MODEL)) {
            currentPos = getCurrentPosition();
        }
        mSurfaceAvailable = false;
        mSurface = null;
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    // Media player listeners

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        RLog.d(TAG, "onPrepared()");
        mIsPrepared = true;
        currentPlayerStatus = PLAYER_STATUS_PREPARED;
        if (mCallback != null)
            mCallback.onPrepared(this);
        mCurrent.setText(Util.getDurationString(0));
        mTime.setText(Util.getDurationString(Math.round(mediaPlayer.getDuration() * 1f / 1000) * 1000));
        mSeeker.setProgress(0);
        mSeeker.setMax(mediaPlayer.getDuration());
        setControlsEnabled(true);

        if (!mControlsDisabled && mHideControlsOnPlay) {
            hideControls();
        }
        start();
        if (mInitialPosition > 0) {
            seekTo(mInitialPosition);
        }
        if (!mAutoPlay) {
            mCurrent.setText(Util.getDurationString(mInitialPosition));
            mSeeker.setProgress(mInitialPosition);
            pause();
        }
        mInitialPosition = -1;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        RLog.d(TAG, "Buffering: " + percent + "%");
        if (mCallback != null)
            mCallback.onBuffering(percent);
        if (mSeeker != null) {
            if (percent == 100) {
                mSeeker.setSecondaryProgress(0);
            } else {
                mSeeker.setSecondaryProgress(mSeeker.getMax() * (percent / 100));
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        RLog.d(TAG, "onCompletion()");
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        if ("oppo".equals(Build.BRAND.toLowerCase()) && "OPPO R9sk".equals(Build.MODEL)) {
            currentPos = 0;
        }
        if (mLoop) {
            if (mHandler != null) {
                mHandler.removeCallbacks(mUpdateCounters);
            }
            mSeeker.setProgress(mSeeker.getMax());
            showControls();
        } else {
            seekTo(0);
            mSeeker.setProgress(0);
            mCurrent.setText(Util.getDurationString(0));
            mBtnPlayPause.setImageResource(R.drawable.rc_ic_sight_play);
            mImageViewPlay.setVisibility(View.VISIBLE);
        }
        currentPlayerStatus = PLAYER_STATUS_COMPLETION;
        if (mCallback != null) {
            mCallback.onCompletion(this);
            if (mLoop) {
                mCallback.onStarted(this);
            }
        }
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        RLog.d(TAG, "Video size changed: " + width + " " + height);
        setFitToFillAspectRatio(mediaPlayer, width, height);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        if (what == -38) {
            // Error code -38 happens on some Samsung devices
            // Just ignore it
            return false;
        }
        String errorMsg = "Preparation/playback error (" + what + "): ";
        switch (what) {
            default:
                errorMsg += "Unknown error";
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                errorMsg += "I/O error";
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                errorMsg += "Malformed";
                break;
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                errorMsg += "Not valid for progressive playback";
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                errorMsg += "Server died";
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                errorMsg += "Timed out";
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                errorMsg += "Unsupported";
                break;
        }
        throwError(new Exception(errorMsg));
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnPlayPause) {
            if (mPlayer.isPlaying()) {
                pause();
            } else {
                if (mHideControlsOnPlay && !mControlsDisabled)
                    hideControls();
                start();
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
        if (fromUser) {
            seekTo(value);
            mCurrent.setText(Util.getDurationString(value));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mWasPlaying = isPlaying();
        if (mWasPlaying && mPlayer != null)
            mPlayer.pause(); // keeps the time updater running, unlike pause()
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mWasPlaying && mPlayer != null) mPlayer.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RLog.d(TAG, "Detached from window");
        release();

        mSeeker = null;
        mCurrent = null;
        mTime = null;
        mBtnPlayPause = null;

        mControlsFrame = null;
        mClickFrame = null;

        if (mHandler != null) {
            mHandler.removeCallbacks(mUpdateCounters);
            mHandler = null;
        }
    }

    private void setFitToFillAspectRatio(MediaPlayer mp, int videoWidth, int videoHeight) {
        if (mp != null && mTextureView != null) {
            int screenWidth = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getWidth();
            int screenHeight = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getHeight();
            android.view.ViewGroup.LayoutParams videoParams = mTextureView.getLayoutParams();
            if (videoWidth > videoHeight) {
                int screenWidthFillHeight = screenWidth * videoHeight / videoWidth;     //按屏幕宽度缩放后的视频高度
                int screenFitHeight = Math.min(screenHeight, screenWidthFillHeight);    //屏幕高度能承载视频高度
                videoParams.width = screenWidth * screenFitHeight / screenWidthFillHeight;//按照屏幕高度缩放后的屏幕宽度
                videoParams.height = screenFitHeight;
            } else {
                int screenHeightFillWith = screenHeight * videoWidth / videoHeight;     //按屏幕高度缩放后的视频宽度
                int screenFitWidth = Math.min(screenWidth, screenHeightFillWith);       //屏幕宽度能承载视频宽度
                videoParams.width = screenFitWidth;
                videoParams.height = screenHeight * screenFitWidth / screenHeightFillWith;//按照屏幕宽度缩放后的屏幕高度
            }
            mTextureView.setLayoutParams(videoParams);
        }
    }

    private void adjustAspectRatio(int viewWidth, int viewHeight, int videoWidth, int videoHeight) {
        if (viewWidth < viewHeight) {
            return;
        }
        final double aspectRatio = (double) videoHeight / videoWidth;
        int newWidth, newHeight;

        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }

        final int xoff = (viewWidth - newWidth) / 2;
        final int yoff = (viewHeight - newHeight) / 2;

        final Matrix txform = new Matrix();
        mTextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
    }

    private void throwError(Exception e) {
        if (mCallback != null) {
            mCallback.onError(this, e);
        } else throw new RuntimeException(e);
    }

    private static void setTint(@NonNull SeekBar seekBar, @ColorInt int thumbTintColor, @ColorInt int progressTintColor) {
        ColorStateList thumbColor = ColorStateList.valueOf(thumbTintColor);
        ColorStateList progressColor = ColorStateList.valueOf(progressTintColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            seekBar.setThumbTintList(thumbColor);
            seekBar.setProgressTintList(progressColor);
            seekBar.setSecondaryProgressTintList(progressColor);
        } else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            Drawable progressDrawable = DrawableCompat.wrap(seekBar.getProgressDrawable());
            seekBar.setProgressDrawable(progressDrawable);
            DrawableCompat.setTintList(progressDrawable, progressColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Drawable thumbDrawable = DrawableCompat.wrap(seekBar.getThumb());
                DrawableCompat.setTintList(thumbDrawable, progressColor);
                seekBar.setThumb(thumbDrawable);
            }
        } else {
            PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
                mode = PorterDuff.Mode.MULTIPLY;
            }
            if (seekBar.getIndeterminateDrawable() != null) {
                seekBar.getIndeterminateDrawable().setColorFilter(thumbTintColor, mode);
            }
            if (seekBar.getProgressDrawable() != null) {
                seekBar.getProgressDrawable().setColorFilter(progressTintColor, mode);
            }
        }
    }

    private void invalidateThemeColors() {
        final int labelColor = Util.isColorDark(mThemeColor) ? Color.WHITE : Color.BLACK;
        mTime.setTextColor(labelColor);
        mTime.setTextColor(labelColor);
        setTint(mSeeker, labelColor, getResources().getColor(R.color.rc_main_theme));
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void setFullscreen(boolean fullscreen) {
        if (mAutoFullscreen) {
            int flags = !fullscreen ? 0 : View.SYSTEM_UI_FLAG_LOW_PROFILE;
            //ViewCompat.setFitsSystemWindows(mControlsFrame, !fullscreen);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                if (fullscreen) {
                    flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE;
                }
            }

            mClickFrame.setSystemUiVisibility(flags);
        }
    }

    public ImageView getImageViewSightList() {
        return mImageViewSightList;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == mp.MEDIA_INFO_VIDEO_RENDERING_START) {
            if (onInfoCallBack != null) {
                onInfoCallBack.onInfo();
            }
        }
        return false;
    }

    public void setOnInfoListener(OnInfoCallBack callBack) {
        onInfoCallBack = callBack;
    }

    @Override
    public boolean onLongClick(View view) {
        String[] items = new String[]{getContext().getString(R.string.rc_save_video)};
        OptionsPopupDialog.newInstance(getContext(), items).setOptionsPopupDialogListener(new OptionsPopupDialog.OnOptionsItemClickedListener() {
            @Override
            public void onOptionsItemClicked(int which) {
                if (which == 0) {
                    if (checkGrantedPermission()) return;
                    RLog.i(TAG, "onLongClick mSource = " + mSource);
                    if (mSource == null || TextUtils.isEmpty(mSource.toString())) {
                        return;
                    }
                    File file = new File(mSource.getPath());
                    boolean result = KitStorageUtils.saveMediaToPublicDir(getContext(), file, KitStorageUtils.MediaType.VIDEO);
                    if (result) {
                        Toast.makeText(getContext(), getContext().getString(R.string.rc_save_video_success), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), getContext().getString(R.string.rc_src_file_not_found), Toast.LENGTH_SHORT).show();
                    }
                }
            }


        }).show();
        return true;
    }

    private boolean checkGrantedPermission() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (PermissionCheckUtil.requestPermissions((Activity) getContext(), permissions)) {
            return false;
        }
        return true;
    }

    public interface OnInfoCallBack {
        void onInfo();
    }

    public void setplayBtnVisible(int visible) {
        if (mBtnPlayPause != null)
            mBtnPlayPause.setVisibility(visible);
    }

    public void setSeekBarClickable(boolean pIsClickable) {
        if (mSeeker != null) {
            mSeeker.setOnTouchListener(pIsClickable ? null : new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }

    }

    public void setLongClickable(boolean isLongClickable) {
        this.isLongClickable = isLongClickable;
        if (mClickFrame != null) {
            mClickFrame.setOnLongClickListener(isLongClickable ? this : null);
        }
    }

    public int getCurrentPlayerStatus() {
        return currentPlayerStatus;
    }

    public int getBeforePausePlayerStatus() {
        return beforePausePlayerStatus;
    }
}