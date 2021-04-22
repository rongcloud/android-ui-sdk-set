package io.rong.sight.record;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.utils.KitStorageUtils;
import io.rong.sight.R;
import io.rong.sight.util.ValueAnimatorUtil;

public class CameraView extends RelativeLayout implements SurfaceHolder.Callback,
        Camera.AutoFocusCallback, CameraFocusListener, SensorEventListener {

    public final String TAG = "Sight-CameraView";

    private PowerManager.WakeLock wakeLock;
    private Context mContext;
    private VideoView mVideoView;
    private ImageView mImageViewClose;
    private ImageView mImageViewSwitch;
    private FocusView mFocusView;
    private TextView mReminderToast;
    private TextView mTextViewProgress;
    private ImageView mImageViewRetry;
    private ImageView mImageViewSubmit;
    private ImageView mImageViewPlayControl;

    private CaptureButton mCaptureButton;

    private int iconWidth;
    private int iconMargin;
    private int controlIconWidth;
    private int controlIconMargin;
    private int controlIconMarginBottom;

    private String saveVideoPath = "";
    private String videoFileName = "";

    private MediaRecorder mediaRecorder;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.Parameters mParam;

    private boolean autoFocus;
    private boolean isPlay = false;
    private boolean isInPreviewState = false;
    //录制结束后，播放暂停状态且画面停在录制第一帧
    private boolean needPause;
    private int playbackPosition = 0;
    private boolean isRecorder = false;
    private float screenProp;
    private boolean supportCapture = false;
    private int maxDuration = 10;
    private long recordDuration = 0;
    //Activity onPaused;
    private boolean paused;

    private String fileName;
    private Bitmap pictureBitmap;

    private int SELECTED_CAMERA;

    private CameraViewListener cameraViewListener;
    private int nowScaleRate = 0;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    private int mSensorRotation = 0;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    //两次检测的时间间隔
    private static final int SENSOR_UPTATE_INTERVAL_TIME = 200;
    //上次检测时间
    private long lastSensorUpdateTime;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "sight:sightWakeLockTag");
        }
        SELECTED_CAMERA = Camera.CameraInfo.CAMERA_FACING_BACK;
        iconWidth = (int) getResources().getDimension(R.dimen.sight_record_top_icon_size);
        iconMargin = (int) getResources().getDimension(R.dimen.sight_record_top_icon_margin);
        controlIconWidth = (int) getResources().getDimension(R.dimen.sight_record_control_icon_size);
        controlIconMargin = (int) getResources().getDimension(R.dimen.sight_record_control_icon_margin_left);
        controlIconMarginBottom = (int) getResources().getDimension(R.dimen.sight_record_control_icon_margin_bottom);
        initView();
        mHolder = mVideoView.getHolder();
        mHolder.addCallback(this);
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                // do nothing
            }
        };
        mSensorManager = (SensorManager) mContext.getSystemService(Activity.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// 加速度
        }

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mCaptureButton.setCaptureListener(new CaptureButton.CaptureListener() {
            @Override
            public void capture() {
                if (supportCapture) {
                    CameraView.this.capture();
                }
            }

            @Override
            public void cancel() {
                mImageViewSwitch.setVisibility(VISIBLE);
                releaseCamera();
                mCamera = getCamera(SELECTED_CAMERA);
                setStartPreview(mCamera, mHolder);
                recordDuration = 0;
            }

            @Override
            public void determine() {

                if (cameraViewListener != null) {
                    cameraViewListener.captureSuccess(pictureBitmap);
                }
                mImageViewSwitch.setVisibility(VISIBLE);
                releaseCamera();
                mCamera = getCamera(SELECTED_CAMERA);
                setStartPreview(mCamera, mHolder);
            }

            @Override
            public void quit() {
                if (cameraViewListener != null) {
                    cameraViewListener.quit();
                }
            }

            @Override
            public void record() {
                recordDuration = 0;
                needPause = false;
                mReminderToast.setVisibility(View.GONE);
                startRecord();
                mImageViewClose.setVisibility(View.GONE);
                mImageViewSwitch.setVisibility(View.GONE);
            }

            @Override
            public void recordEnd(long duration) {
                recordDuration = duration;
                isInPreviewState = true;
                stopRecord();
                mTextViewProgress.setVisibility(View.GONE);
                needPause = true;
                playRecord();
                setRecordControlViewVisibility(true);
                mImageViewSubmit.setEnabled(true);
                mImageViewRetry.setEnabled(true);
            }

            @Override
            public void getRecordResult() {
                if (cameraViewListener != null) {
                    cameraViewListener.recordSuccess(fileName, Math.round(recordDuration * 1f / 1000));
                }
            }

            @Override
            public void deleteRecordResult() {
                deleteRecordFile();
                mVideoView.stopPlayback();
                releaseCamera();
                if (!paused) {
                    mCamera = getCamera(SELECTED_CAMERA);
                    setStartPreview(mCamera, mHolder);
                }
                isPlay = false;
                isInPreviewState = false;
                recordDuration = 0;
                needPause = false;
                setRecordControlViewVisibility(false);
                mTextViewProgress.setVisibility(View.GONE);
                updateReminderView();
            }

            @Override
            public void scale(float scaleValue) {
                if (mCamera == null || mParam == null || !mParam.isZoomSupported()) {
                    return;
                }
                if (scaleValue >= 0) {
                    int scaleRate = (int) (scaleValue / 50);
                    if (scaleRate < mParam.getMaxZoom() && scaleRate >= 0 && nowScaleRate != scaleRate) {
                        try {
                            mParam.setZoom(scaleRate);
                            mCamera.setParameters(mParam);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        nowScaleRate = scaleRate;
                    }
                }
            }

            @Override
            public void recordProgress(int progress) {
                updateProgressView(progress);
            }

            @Override
            public void retryRecord() {
                stopRecord();
                deleteRecordFile();
                mVideoView.stopPlayback();
                releaseCamera();
                if (!paused) {
                    mCamera = getCamera(SELECTED_CAMERA);
                    setStartPreview(mCamera, mHolder);
                    setRecordControlViewVisibility(false);
                }
                mTextViewProgress.setVisibility(View.GONE);
                recordDuration = 0;
            }
        });

    }

    public void setCameraViewListener(CameraViewListener cameraViewListener) {
        this.cameraViewListener = cameraViewListener;
    }

    private void initView() {
        setWillNotDraw(false);
        this.setBackgroundColor(Color.BLACK);
        //Surface
        mVideoView = new VideoView(mContext);
        LayoutParams videoViewParam = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        videoViewParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mVideoView.setLayoutParams(videoViewParam);
        mVideoView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    RLog.i(TAG, "Touch To Focus");
                    mCamera.autoFocus(CameraView.this);
                }
            }
        });
        //初始化为自动对焦
        autoFocus = true;

        //CaptureButton
        LayoutParams btnParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        btnParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        btnParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        mCaptureButton = new CaptureButton(mContext);
        mCaptureButton.setLayoutParams(btnParams);
        mCaptureButton.setId(R.id.rc_sight_record_bottom);
        mCaptureButton.setSupportCapture(supportCapture);
        ValueAnimatorUtil.resetDurationScaleIfDisable();

        mFocusView = new FocusView(mContext, 120);
        mFocusView.setVisibility(INVISIBLE);

        initReminderView();
        initCloseView();
        initSwitchView();
        initProgressView();
        initRetryView();
        initSubmitView();
        initPlayControlView();

        this.addView(mVideoView);
        this.addView(mCaptureButton);
        this.addView(mImageViewClose);
        this.addView(mImageViewSwitch);
        this.addView(mReminderToast);
        this.addView(mFocusView);
        this.addView(mTextViewProgress);
        this.addView(mImageViewRetry);
        this.addView(mImageViewSubmit);
        this.addView(mImageViewPlayControl);
        updateReminderView();
    }

    private void initReminderView() {
        mReminderToast = new TextView(mContext);
        mReminderToast.setText(R.string.rc_sight_reminder);
        mReminderToast.setTextColor(getResources().getColor(R.color.color_sight_white));
        mReminderToast.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.sight_text_size_14));
        mReminderToast.setShadowLayer(16F, 0F, 2F, getResources().getColor(R.color.color_sight_record_reminder_shadow));
        int paddingHorizontal = (int) getResources().getDimension(R.dimen.sight_text_view_padding_horizontal);
        int paddingVertical = (int) getResources().getDimension(R.dimen.sight_text_view_padding_vertical);
        mReminderToast.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
        LayoutParams toastParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        toastParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        toastParams.addRule(RelativeLayout.ABOVE, mCaptureButton.getId());
        mReminderToast.setLayoutParams(toastParams);
    }

    private void updateReminderView() {
        mReminderToast.setVisibility(View.VISIBLE);
        mReminderToast.postDelayed(new Runnable() {
            @Override
            public void run() {
                mReminderToast.setVisibility(View.GONE);
            }
        }, 5000);
    }

    private void initCloseView() {
        mImageViewClose = new ImageView(mContext);
        LayoutParams imageViewCloseParam = new LayoutParams(iconWidth, iconWidth);
        imageViewCloseParam.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        imageViewCloseParam.setMargins(iconMargin, iconMargin, 0, 0);
        mImageViewClose.setLayoutParams(imageViewCloseParam);
        mImageViewClose.setImageResource(R.drawable.rc_ic_sight_close);
        mImageViewClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                releaseCamera();
                if (cameraViewListener != null) {
                    cameraViewListener.quit();
                }
            }
        });
    }

    private void initSwitchView() {
        mImageViewSwitch = new ImageView(mContext);
        LayoutParams imageViewSwitchParam = new LayoutParams(iconWidth, iconWidth);
        imageViewSwitchParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        imageViewSwitchParam.setMargins(0, iconMargin, iconMargin, 0);
        mImageViewSwitch.setLayoutParams(imageViewSwitchParam);
        mImageViewSwitch.setImageResource(R.drawable.rc_ic_sight_switch);
        mImageViewSwitch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera != null) {
                    releaseCamera();
                    if (SELECTED_CAMERA == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        SELECTED_CAMERA = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    } else {
                        SELECTED_CAMERA = Camera.CameraInfo.CAMERA_FACING_BACK;
                    }
                    mCamera = getCamera(SELECTED_CAMERA);
                    setStartPreview(mCamera, mHolder);
                }
            }
        });
    }

    private void initProgressView() {
        mTextViewProgress = new TextView(mContext);
        mTextViewProgress.setVisibility(View.GONE);
        mTextViewProgress.setTextColor(getResources().getColor(R.color.color_sight_white));
        LayoutParams progressParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        progressParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        progressParams.addRule(RelativeLayout.ABOVE, mCaptureButton.getId());
        mTextViewProgress.setLayoutParams(progressParams);
    }

    private void initRetryView() {
        mImageViewRetry = new ImageView(mContext);
        RelativeLayout.LayoutParams imageViewRetryParam = new RelativeLayout.LayoutParams(controlIconWidth, controlIconWidth);
        imageViewRetryParam.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        imageViewRetryParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        imageViewRetryParam.setMargins(0, 0, 0, controlIconMarginBottom);
        imageViewRetryParam.setMarginStart(controlIconMargin);
        mImageViewRetry.setLayoutParams(imageViewRetryParam);
        mImageViewRetry.setImageResource(R.drawable.rc_ic_sight_record_retry);
        mImageViewRetry.setVisibility(View.GONE);
        mImageViewRetry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mImageViewSubmit.setEnabled(false);
                mCaptureButton.retryRecord();
            }
        });
    }

    private void initSubmitView() {
        mImageViewSubmit = new ImageView(mContext);
        RelativeLayout.LayoutParams imageViewSubmitParam = new RelativeLayout.LayoutParams(controlIconWidth, controlIconWidth);
        imageViewSubmitParam.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        imageViewSubmitParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        imageViewSubmitParam.setMargins(0, 0, 0, controlIconMarginBottom);
        imageViewSubmitParam.setMarginEnd(controlIconMarginBottom);
        mImageViewSubmit.setLayoutParams(imageViewSubmitParam);
        mImageViewSubmit.setImageResource(R.drawable.rc_ic_sight_record_submit);
        mImageViewSubmit.setVisibility(View.GONE);
        mImageViewSubmit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mImageViewRetry.setEnabled(false);
                mCaptureButton.submitRecord();
            }
        });
    }

    private void initPlayControlView() {
        mImageViewPlayControl = new ImageView(mContext);
        RelativeLayout.LayoutParams imageViewPlayControlParam = new RelativeLayout.LayoutParams(controlIconWidth, controlIconWidth);
        imageViewPlayControlParam.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        imageViewPlayControlParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        imageViewPlayControlParam.setMargins(0, 0, 0, controlIconMarginBottom);
        mImageViewPlayControl.setLayoutParams(imageViewPlayControlParam);
        mImageViewPlayControl.setImageResource(R.drawable.rc_ic_sight_record_play);
        mImageViewPlayControl.setVisibility(View.GONE);
        mImageViewPlayControl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlay) {
                    pauseRecord();
                } else {
                    playRecord();
                }
            }
        });
    }

    private void updateProgressView(int progress) {
        if (mTextViewProgress != null) {
            mTextViewProgress.setVisibility(View.VISIBLE);
            mTextViewProgress.setText(progress + "\"");
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    private Camera getCamera(int position) {
        Camera camera;
        try {
            camera = Camera.open(position);
        } catch (Exception e) {
            camera = null;
            e.printStackTrace();
        }
        return camera;
    }

    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        if (camera == null) {
            RLog.e(TAG, "Camera is null");
            return;
        }
        try {
            mParam = camera.getParameters();
            //适配全面屏预览被拉伸
            Camera.Size preferSize = getOptimalSize(mParam.getSupportedVideoSizes(), 1280, 720);
            Camera.Size previewSize = CameraParamUtil.getInstance().getPreviewSize(mParam.getSupportedPreviewSizes(), 1000, screenProp, preferSize);
            if (previewSize == null) {
                previewSize = mParam.getPreviewSize();
            }

            if (previewSize != null) {
                mParam.setPreviewSize(previewSize.width, previewSize.height);
            }

            if (supportCapture) {
                Camera.Size pictureSize = CameraParamUtil.getInstance().getPictureSize(mParam.getSupportedPictureSizes(), 1200, screenProp);
                mParam.setPictureSize(pictureSize.width, pictureSize.height);
                if (CameraParamUtil.getInstance().isSupportedPictureFormats(mParam.getSupportedPictureFormats(), ImageFormat.JPEG)) {
                    mParam.setPictureFormat(ImageFormat.JPEG);
                    mParam.setJpegQuality(100);
                }
            }
            cameraAutoFocus(camera, mParam);
            mParam = camera.getParameters();
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(calculateCameraPreviewOrientation());
            camera.startPreview();
        } catch (Exception e) {
            RLog.e(TAG, "startPreview failed");
            e.printStackTrace();
        }
    }

    private void cameraAutoFocus(Camera pCamera, Camera.Parameters pParam) {
        if (CameraParamUtil.getInstance().isSupportedFocusMode(mParam.getSupportedFocusModes(), Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            pParam.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        pCamera.setParameters(pParam);
        pCamera.cancelAutoFocus();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    public void capture() {
        if (!supportCapture) {
            return;
        }
        if (autoFocus) {
            mCamera.autoFocus(this);
        } else {
            if (SELECTED_CAMERA == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Matrix matrix = new Matrix();
                        matrix.setRotate(90);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        pictureBitmap = bitmap;
                        mImageViewSwitch.setVisibility(INVISIBLE);
                        mCaptureButton.captureSuccess();
                    }
                });
            } else if (SELECTED_CAMERA == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Matrix matrix = new Matrix();
                        matrix.setRotate(270);
                        matrix.postScale(-1, 1);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        pictureBitmap = bitmap;
                        mImageViewSwitch.setVisibility(INVISIBLE);
                        mCaptureButton.captureSuccess();
                    }
                });
            }
        }
    }

    //自动对焦
    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        cameraAutoFocus(camera, mParam);
        if (autoFocus) {
            if (SELECTED_CAMERA == Camera.CameraInfo.CAMERA_FACING_BACK && success) {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Matrix matrix = new Matrix();
                        matrix.setRotate(90);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        pictureBitmap = bitmap;
                        mImageViewSwitch.setVisibility(INVISIBLE);
                        mCaptureButton.captureSuccess();
                    }
                });
            } else if (SELECTED_CAMERA == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Matrix matrix = new Matrix();
                        matrix.setRotate(270);
                        matrix.postScale(-1, 1);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        pictureBitmap = bitmap;
                        mImageViewSwitch.setVisibility(INVISIBLE);
                        mCaptureButton.captureSuccess();
                    }
                });
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        float widthSize = MeasureSpec.getSize(widthMeasureSpec);
        float heightSize = MeasureSpec.getSize(heightMeasureSpec);
        screenProp = heightSize / widthSize;
        //RLog.i(TAG, "ScreenProp = " + screenProp + " " + widthSize + " " + heightSize);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        RLog.i(TAG, "surfaceCreated");
        if (!isInPreviewState && !paused) {
            setStartPreview(mCamera, holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        RLog.i(TAG, "surfaceChanged");
        mHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        RLog.i(TAG, "surfaceDestroyed");
        if (cameraViewListener != null) {
            cameraViewListener.quit();
        }
        releaseCamera();
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    public void onResume() {
        RLog.i(TAG, "onResume isInPreviewState = " + isInPreviewState);
        mCamera = getCamera(SELECTED_CAMERA);
        if (mCamera != null) {
            //setStartPreview(mCamera, mHolder);
            RLog.i(TAG, "Camera = " + mCamera);
        } else {
            RLog.i(TAG, "Camera is null!");
        }
        if (isInPreviewState) {
            mVideoView.resume();
        } else {
            paused = false;
        }
        if (wakeLock != null) {
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
        }

        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void onPause() {
        RLog.i(TAG, "onPause");
        paused = true;
        mCaptureButton.onPause();
        releaseCamera();
        if (isInPreviewState) {
            playbackPosition = mVideoView.getCurrentPosition();
        }
        if (wakeLock != null) {
            wakeLock.release();
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mSensor);
        }
    }

    private void startRecord() {
        RLog.i(TAG, "startRecord");
        if (isRecorder) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (IllegalStateException e) {
                RLog.e(TAG, "mediaRecorder got IllegalStateException", e);
                mediaRecorder = null;
                mediaRecorder = new MediaRecorder();
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                RLog.e(TAG, "mediaRecorder got exception", e);
            }

            mediaRecorder = null;
        }
        if (mCamera == null) {
            RLog.i(TAG, "Camera is null");
            stopRecord();
            return;
        }
        try {
            mCamera.unlock();
            if (mediaRecorder == null) {
                mediaRecorder = new MediaRecorder();
            }
            mediaRecorder.reset();
            mediaRecorder.setCamera(mCamera);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            if (mParam == null) {
                mParam = mCamera.getParameters();
            }
            Camera.Size preferSize = mCamera.new Size(1280, 720);
            Camera.Size videoSize = CameraParamUtil.getInstance().getVideoSize(mParam.getSupportedVideoSizes(), 1000, screenProp, preferSize);
            if (videoSize == null) {
                RLog.d(TAG, "mParam.getSupportedVideoSizes() return null");
                String defaultVideoSize = mParam.get("video-size");
                if (defaultVideoSize != null) {
                    String[] sizes = defaultVideoSize.split("x");
                    if (sizes.length == 2) {
                        try {
                            videoSize = mCamera.new Size(Integer.parseInt(sizes[0]), Integer.parseInt(sizes[1]));
                        } catch (NumberFormatException e) {
                            RLog.e(TAG, "get video-size got NumberFormatException");
                        }
                    }
                }
            }
            if (videoSize != null) {
                mediaRecorder.setVideoSize(videoSize.width, videoSize.height);
            }

            int rotation = (calculateCameraPreviewOrientation() + mSensorRotation) % 360;
            if (SELECTED_CAMERA == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                rotation = (360 - rotation) % 360;
            }
            mediaRecorder.setOrientationHint(rotation);


            mediaRecorder.setMaxDuration(maxDuration * 1000);
            mediaRecorder.setVideoEncodingBitRate(3 * 1024 * 1024);
            //mediaRecorder.setVideoFrameRate(15);
            mediaRecorder.setPreviewDisplay(mHolder.getSurface());

            videoFileName = "video_" + System.currentTimeMillis() + ".mp4";
            if (saveVideoPath.equals("")) {
                saveVideoPath = KitStorageUtils.getVideoSavePath(getContext());
            }
            mediaRecorder.setOutputFile(saveVideoPath + "/" + videoFileName);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecorder = true;
            updateProgressView(0);
        } catch (Exception e) {
            RLog.e(TAG, "startRecord got exception");
            e.printStackTrace();
        }
    }

    private void stopRecord() {
        RLog.i(TAG, "stopRecord");
        if (mediaRecorder != null) {
            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setOnInfoListener(null);
            mediaRecorder.setPreviewDisplay(null);
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                RLog.e(TAG, "stopRecord got exception");
                e.printStackTrace();
            } finally {
                isRecorder = false;
                mediaRecorder = null;
            }
            releaseCamera();
            fileName = saveVideoPath + "/" + videoFileName;
            if (isInPreviewState) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mVideoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
                }
                try {
                    mVideoView.setVideoPath(fileName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //录制结束后，暂停状态，画面停在第一帧
    private void playRecord() {
        RLog.i(TAG, "playRecord needPause:" + needPause);
        if (!needPause) {
            isPlay = true;
            audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
        mImageViewPlayControl.setImageResource(needPause ? R.drawable.rc_ic_sight_record_play :
                R.drawable.rc_ic_sight_record_pause);
        try {
            mVideoView.start();
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    RLog.i(TAG, "playRecord mVideoView onPrepared ");
                    if (mp == null) {
                        return;
                    }
                    int duration = mp.getDuration();
                    if (duration > 1000) {
                        recordDuration = duration;
                    }
                    //resume时VideoView会重新播，试图恢复之前的暂停状态出现各种错误，故而播放
                    //如果开发者认为这个是严重bug，另外一种处理方式是拍摄后预览时直接播放，去掉暂停按钮和暂停状态
                    if (paused) {
                        isPlay = true;
                        needPause = false;
                        paused = false;
                        mImageViewPlayControl.setImageResource(R.drawable.rc_ic_sight_record_pause);
                    }
                    try {
                        if (playbackPosition > 0 || needPause) {
                            mp.seekTo(playbackPosition);

                            mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                                @Override
                                public void onSeekComplete(MediaPlayer mp) {
                                    if ((isInPreviewState && !isPlay) || needPause) {
                                        needPause = false;
                                        mp.pause();
                                    }
                                }
                            });
                            playbackPosition = 0;
                        }
                        if (!needPause) {
                            mp.start();
                        }
                        mp.setLooping(true);
                        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                            @Override
                            public boolean onError(MediaPlayer mp, int what, int extra) {
                                RLog.e(TAG, "record play error on MediaPlayer onPrepared ,what = " + what + " extra = " + extra);
                                return true;
                            }
                        });
                    } catch (Exception e) {
                        RLog.e(TAG, "mVideoView onPrepared got error");
                        e.printStackTrace();
                    }
                }
            });
            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mp != null) {
                        mp.setDisplay(null);
                        mp.reset();
                        mp.setDisplay(mVideoView.getHolder());
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mVideoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
                    }
                    try {
                        mVideoView.setVideoPath(fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mVideoView.start();
                }
            });
            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    RLog.e(TAG, "record play error,what = " + what + " extra = " + extra);
                    mImageViewSubmit.setEnabled(false);
                    return true;
                }
            });
        } catch (Exception e) {
            mImageViewSubmit.setEnabled(false);
            RLog.e(TAG, "mVideoView play error");
            e.printStackTrace();
        }
    }

    private void pauseRecord() {
        RLog.i(TAG, "pauseRecord");
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        mImageViewPlayControl.setImageResource(R.drawable.rc_ic_sight_record_play);
        mVideoView.pause();
        isPlay = false;
    }

    public void setSaveVideoPath(String saveVideoPath) {
        this.saveVideoPath = saveVideoPath;
    }

    public void setAutoFocus(boolean autoFocus) {
        this.autoFocus = autoFocus;
    }

    @Override
    public void onFocusBegin(float x, float y) {
        mFocusView.setVisibility(VISIBLE);
        mFocusView.setX(x - mFocusView.getWidth() / 2f);
        mFocusView.setY(y - mFocusView.getHeight() / 2f);
        if (mCamera != null) {
            try {
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        cameraAutoFocus(camera, mParam);
                        if (success) {
                            onFocusEnd();
                        }
                    }
                });
            } catch (Exception e) {
                RLog.e(TAG, "autoFocus failed ");
                onFocusEnd();
            }
        }
    }

    @Override
    public void onFocusEnd() {
        mFocusView.setVisibility(INVISIBLE);
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        if (!autoFocus && event.getAction() == MotionEvent.ACTION_DOWN && SELECTED_CAMERA == Camera.CameraInfo.CAMERA_FACING_BACK && !isInPreviewState) {
            onFocusBegin(event.getX(), event.getY());
        }
        return super.onTouchEvent(event);
    }

    public void cancelAudio() {
        AudioUtil.setAudioManage(mContext);
    }

    public void setSupportCapture(boolean support) {
        supportCapture = support;
        if (mCaptureButton != null) {
            mCaptureButton.setSupportCapture(support);
        }
    }

    public void setMaxRecordDuration(int duration) {
        maxDuration = duration;
        if (mCaptureButton != null) {
            mCaptureButton.setMaxRecordDuration(duration);
        }
    }

    private void setRecordControlViewVisibility(boolean visual) {
        mImageViewClose.setVisibility(visual ? View.GONE : View.VISIBLE);
        mImageViewSwitch.setVisibility(visual ? View.GONE : View.VISIBLE);
        mImageViewSubmit.setVisibility(visual ? View.VISIBLE : View.GONE);
        mImageViewRetry.setVisibility(visual ? View.VISIBLE : View.GONE);
        mImageViewPlayControl.setVisibility(visual ? View.VISIBLE : View.GONE);
    }

    private void deleteRecordFile() {
        if (fileName == null || TextUtils.isEmpty(fileName)) {
            fileName = saveVideoPath + "/" + videoFileName;
        }
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 获取指定宽高区域内相机的所对应尺寸
     *
     * @param sizes 相机的成像尺寸
     * @param w     宽
     * @param h     高
     * @return 适配指定区域后的大小
     */
    private Camera.Size getOptimalSize(@NonNull List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }

        return optimalSize;
    }

    private int calculateCameraPreviewOrientation() {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(SELECTED_CAMERA, info);
        int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        RLog.d(TAG, "calculateCameraPreviewOrientation result:" + result);
        return result;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        long currentUpdateTime = System.currentTimeMillis();
        long timeInterval = currentUpdateTime - lastSensorUpdateTime;
        if (timeInterval < SENSOR_UPTATE_INTERVAL_TIME) {
            return;
        }

        lastSensorUpdateTime = currentUpdateTime;
        float x = event.values[0];
        float y = event.values[1];

        // x绝对值大于6为手机横屏（手机竖放顶端朝向左x为10，朝右x为-10）
        // y绝对值大于6为手机竖屏（手机竖放顶端朝向上y为10，朝下y为-10）
        if (Math.abs(x) > 6 && Math.abs(y) < 4) {
            if (x > 0) {
                mSensorRotation = 270;
            } else {
                mSensorRotation = 90;
            }
        } else if (Math.abs(y) > 6 && Math.abs(x) < 4) {
            if (y > 0) {
                mSensorRotation = 0;
            } else {
                mSensorRotation = 180;
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public interface CameraViewListener {
        void quit();

        void captureSuccess(Bitmap bitmap);

        void recordSuccess(String url, int recordDuration);
    }
}