package io.rong.imkit.manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.rtp.AudioStream;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.WindowManager;

import java.io.FileInputStream;
import java.io.IOException;

import io.rong.common.RLog;

public class AudioPlayManager implements SensorEventListener {
    private final static String TAG = "AudioPlayManager";

    private MediaPlayer mMediaPlayer;
    private IAudioPlayListener _playListener;
    private Uri mUriPlaying;
    private Sensor _sensor;
    private SensorManager _sensorManager;
    private AudioManager mAudioManager;
    private PowerManager _powerManager;
    private PowerManager.WakeLock _wakeLock;
    private AudioManager.OnAudioFocusChangeListener afChangeListener;
    private Context mContext;
    private Handler handler;
    private final Object mLock = new Object();

    private AudioPlayManager() {
        handler = new Handler(Looper.getMainLooper());
    }

    static class SingletonHolder {
        static AudioPlayManager sInstance = new AudioPlayManager();
    }

    public static AudioPlayManager getInstance() {
        return SingletonHolder.sInstance;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (mLock) {
            float range = event.values[0];
            RLog.d(TAG, "onSensorChanged. range:" + range + "; max range:" + event.sensor.getMaximumRange());
            double rangeJudgeValue = 0.0;
            boolean judge;
            if (_sensor == null || mMediaPlayer == null || mAudioManager == null) {
                return;
            }
            judge = judgeCondition(event, range, rangeJudgeValue);

            if (mMediaPlayer.isPlaying()) {
                FileInputStream fis = null;
                if (judge) {
                    //处理 sensor 出现异常后，持续回调 sensor 变化，导致声音播放卡顿
                    if (mAudioManager.getMode() == AudioManager.MODE_NORMAL) return;
                    mAudioManager.setMode(AudioManager.MODE_NORMAL);
                    mAudioManager.setSpeakerphoneOn(true);
                    final int positions = mMediaPlayer.getCurrentPosition();
                    try {
                        mMediaPlayer.reset();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            AudioAttributes attributes = new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .build();
                            mMediaPlayer.setAudioAttributes(attributes);
                        } else {
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        }
                        mMediaPlayer.setVolume(1, 1);
                        fis = new FileInputStream(mUriPlaying.getPath());
                        mMediaPlayer.setDataSource(fis.getFD());
                        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    mp.seekTo(positions, mp.SEEK_CLOSEST);
                                } else {
                                    mp.seekTo(positions);
                                }
                            }
                        });
                        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                            @Override
                            public void onSeekComplete(MediaPlayer mp) {
                                mp.start();
                            }
                        });
                        mMediaPlayer.prepareAsync();
                    } catch (IOException e) {
                        RLog.e(TAG, "onSensorChanged", e);
                    } finally {
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e) {
                                RLog.e(TAG, "startPlay", e);
                            }
                        }
                    }

                    setScreenOn();
                } else {
                    if (!(Build.BRAND.equals("samsung") && Build.MODEL.equals("SM-N9200"))) {
                        setScreenOff();
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        if (mAudioManager.getMode() == AudioManager.MODE_IN_COMMUNICATION) return;
                        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    } else {
                        if (mAudioManager.getMode() == AudioManager.MODE_IN_CALL) return;
                        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                    }
                    mAudioManager.setSpeakerphoneOn(false);
                    replay();
                }
            } else {
                if (range > 0.0) {
                    if (mAudioManager.getMode() == AudioManager.MODE_NORMAL) return;
                    mAudioManager.setMode(AudioManager.MODE_NORMAL);
                    mAudioManager.setSpeakerphoneOn(true);
                    setScreenOn();
                }
            }
        }
    }

    private boolean judgeCondition(SensorEvent event, float range, double rangeJudgeValue) {
        synchronized (mLock) {
            boolean judge;
            if (Build.BRAND.equalsIgnoreCase("HUAWEI")) {
                judge = (range >= event.sensor.getMaximumRange());
            } else {
                if (Build.BRAND.equalsIgnoreCase("ZTE")) {
                    rangeJudgeValue = 1.0;
                } else if (Build.BRAND.equalsIgnoreCase("nubia")) {
                    rangeJudgeValue = 3.0;
                }
                judge = (range > rangeJudgeValue);
            }
            return judge;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setScreenOff() {
        synchronized (mLock) {
            if (_wakeLock == null && _powerManager != null) {
                _wakeLock = _powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "AudioPlayManager:wakelockTag");
            }
            if (_wakeLock != null && !_wakeLock.isHeld()) {
                _wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
            }
        }
    }

    private void setScreenOn() {
        synchronized (mLock) {
            if (_wakeLock != null && _wakeLock.isHeld()) {
                _wakeLock.setReferenceCounted(false);
                _wakeLock.release();
                _wakeLock = null;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void replay() {
        synchronized (mLock) {
            if (mMediaPlayer == null) {
                return;
            }
            FileInputStream fis = null;
            try {
                final int positions = mMediaPlayer.getCurrentPosition();
                mMediaPlayer.reset();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                            .build();
                    mMediaPlayer.setAudioAttributes(attributes);
                } else {
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                }
                fis = new FileInputStream(mUriPlaying.getPath());
                mMediaPlayer.setDataSource(fis.getFD());
                mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        // 装载完毕回调
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            RLog.e(TAG, "replay", e);
                            // Restore interrupted state...
                            Thread.currentThread().interrupt();
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            mp.seekTo(positions, mp.SEEK_CLOSEST);
                        } else {
                            mp.seekTo(positions);
                        }
                    }
                });
                mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        mp.start();
                    }
                });
                // 通过异步的方式装载媒体资源
                mMediaPlayer.prepareAsync();
                mMediaPlayer.setVolume(1.0f, 1.0f);
            } catch (IOException e) {
                RLog.e(TAG, "replay", e);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        RLog.e(TAG, "replay", e);
                    }
                }
            }
        }
    }

    public void startPlay(final Context context, Uri audioUri, IAudioPlayListener playListener) {
        synchronized (mLock) {
            if (context == null || audioUri == null) {
                RLog.e(TAG, "startPlay context or audioUri is null.");
                return;
            }
            mContext = context;

            if (_playListener != null && mUriPlaying != null) {
                _playListener.onStop(mUriPlaying);
            }
            resetMediaPlayer();

            this.afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    synchronized (mLock) {
                        RLog.d(TAG, "OnAudioFocusChangeListener " + focusChange);
                        if (mAudioManager != null && focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            mAudioManager.abandonAudioFocus(afChangeListener);
                            afChangeListener = null;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    synchronized (mLock) {
                                        if (_playListener != null) {
                                            _playListener.onComplete(mUriPlaying);
                                            _playListener = null;
                                        }
                                    }
                                }
                            });
                            reset();
                        }
                    }
                }
            };

            FileInputStream fis = null;
            if (context instanceof Activity) {
                ((Activity) context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            try {
                _powerManager = (PowerManager) context.getApplicationContext().getSystemService(Context.POWER_SERVICE);
                mAudioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                if (!isHeadphonesPlugged(mAudioManager)) {
                    _sensorManager = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
                    if (_sensorManager != null) {
                        _sensor = _sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                        _sensorManager.registerListener(this, _sensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }
                }
                muteAudioFocus(mAudioManager, true);

                _playListener = playListener;
                mUriPlaying = audioUri;
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        synchronized (mLock) {
                            if (_playListener != null) {
                                _playListener.onComplete(mUriPlaying);
                                _playListener = null;
                            }
                            reset();
                            if (context instanceof Activity) {
                                ((Activity) context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                            }
                        }
                    }
                });
                mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        synchronized (mLock) {
                            reset();
                            return true;
                        }
                    }
                });
                fis = new FileInputStream(audioUri.getPath());
                mMediaPlayer.setDataSource(fis.getFD());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    AudioAttributes attributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build();
                    mMediaPlayer.setAudioAttributes(attributes);
                } else {
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                }
                //mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                if (_playListener != null)
                    _playListener.onStart(mUriPlaying);
            } catch (Exception e) {
                RLog.e(TAG, "startPlay", e);
                if (_playListener != null) {
                    _playListener.onStop(audioUri);
                    _playListener = null;
                }
                reset();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        RLog.e(TAG, "startPlay", e);
                    }
                }
            }
        }
    }

    private boolean isHeadphonesPlugged(AudioManager audioManager) {
        synchronized (mLock) {
            if (audioManager == null) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
                for (AudioDeviceInfo deviceInfo : audioDevices) {
                    if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                            || deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                        return true;
                    }
                }
                return false;
            } else {
                return audioManager.isWiredHeadsetOn();
            }
        }
    }

    public void setPlayListener(IAudioPlayListener listener) {
        synchronized (mLock) {
            this._playListener = listener;
        }
    }

    public void stopPlay() {
        synchronized (mLock) {
            if (mContext != null) {
                if (mContext instanceof Activity) {
                    ((Activity) mContext).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
            if (_playListener != null && mUriPlaying != null) {
                _playListener.onStop(mUriPlaying);
            }
            reset();
        }
    }

    private void reset() {
        resetMediaPlayer();
        resetAudioPlayManager();
    }

    private void resetAudioPlayManager() {
        if (mAudioManager != null) {
            mAudioManager.setMode(AudioManager.MODE_NORMAL);
            muteAudioFocus(mAudioManager, false);
        }
        if (_sensorManager != null) {
            setScreenOn();
            _sensorManager.unregisterListener(this);
        }
        _sensorManager = null;
        _sensor = null;
        _powerManager = null;
        mAudioManager = null;
        _wakeLock = null;
        mUriPlaying = null;
        _playListener = null;
    }

    private void resetMediaPlayer() {
        synchronized (mLock) {
            if (mMediaPlayer != null) {
                try {
                    mMediaPlayer.stop();
                    mMediaPlayer.reset();
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                } catch (IllegalStateException e) {
                    RLog.e(TAG, "resetMediaPlayer", e);
                }
            }
        }
    }

    public Uri getPlayingUri() {
        synchronized (mLock) {
            return mUriPlaying != null ? mUriPlaying : Uri.EMPTY;
        }
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void muteAudioFocus(AudioManager audioManager, boolean bMute) {
        synchronized (mLock) {
            if (audioManager == null) return;

            if (bMute) {
                audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            } else {
                audioManager.abandonAudioFocus(afChangeListener);
                afChangeListener = null;
            }
        }
    }

    /**
     * 检查AudioPlayManager是否处于通道正常的状态。
     *
     * @param context 上下文
     * @return 是否处于通道正常的状态
     */
    public boolean isInNormalMode(Context context) {
        synchronized (mLock) {
            if (mAudioManager == null) {
                mAudioManager = (AudioManager) context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            }
            return mAudioManager != null && mAudioManager.getMode() == AudioManager.MODE_NORMAL;
        }
    }

    private boolean isVOIPMode = false;

    public boolean isInVOIPMode(Context context) {
        return isVOIPMode;
    }

    public void setInVoipMode(boolean isVOIPMode) {
        this.isVOIPMode = isVOIPMode;
    }

    public boolean isPlaying() {
        synchronized (mLock) {
            return mMediaPlayer != null && mMediaPlayer.isPlaying();
        }
    }
}
