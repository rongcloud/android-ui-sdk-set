package io.rong.imkit.picture;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;

public class PictureVideoPlayActivity extends PictureBaseActivity
        implements MediaPlayer.OnErrorListener,
                MediaPlayer.OnPreparedListener,
                MediaPlayer.OnCompletionListener,
                View.OnClickListener {
    private final String TAG = PictureVideoPlayActivity.class.getCanonicalName();
    private String video_path = "";
    private ImageView picture_left_back;
    private MediaController mMediaController;
    private VideoView mVideoView;
    private ImageView iv_play;
    private int mPositionWhenPaused = -1;

    @Override
    public int getResourceId() {
        return R.layout.rc_picture_activity_video_play;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        video_path = getIntent().getStringExtra("video_path");
        if (TextUtils.isEmpty(video_path)) {
            RLog.d(TAG, "video_path is empty! return directly!");
            return;
        }
        picture_left_back = findViewById(R.id.picture_left_back);
        mVideoView = findViewById(R.id.video_view);
        mVideoView.setBackgroundColor(Color.BLACK);
        iv_play = findViewById(R.id.iv_play);
        mMediaController = new MediaController(this);
        mVideoView.setOnCompletionListener(this);
        mVideoView.setOnPreparedListener(this);
        mVideoView.setMediaController(mMediaController);
        picture_left_back.setOnClickListener(this);
        iv_play.setOnClickListener(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                new ContextWrapper(newBase) {
                    @Override
                    public Object getSystemService(String name) {
                        if (Context.AUDIO_SERVICE.equals(name)) {
                            return getApplicationContext().getSystemService(name);
                        }
                        return super.getSystemService(name);
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        mMediaController = null;
        mVideoView = null;
        iv_play = null;
        super.onDestroy();
    }

    @Override
    public void onStart() {
        // Play Video
        if (mVideoView != null) {
            mVideoView.setVideoPath(video_path);
            mVideoView.start();
        }
        super.onStart();
    }

    @Override
    public void onPause() {
        // Stop video when the activity is pause.
        if (mVideoView != null) {
            mPositionWhenPaused = mVideoView.getCurrentPosition();
            mVideoView.stopPlayback();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        // Resume video player
        if (mPositionWhenPaused >= 0) {
            if (mVideoView != null) {
                mVideoView.seekTo(mPositionWhenPaused);
            }
            mPositionWhenPaused = -1;
        }

        super.onResume();
    }

    @Override
    public boolean onError(MediaPlayer player, int arg1, int arg2) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (null != iv_play) {
            iv_play.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.picture_left_back) {
            finish();
        } else if (id == R.id.iv_play) {
            if (mVideoView != null) {
                mVideoView.start();
            }
            iv_play.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setOnInfoListener(
                new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mp, int what, int extra) {
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            // video started
                            if (mVideoView != null) {
                                mVideoView.setBackgroundColor(Color.TRANSPARENT);
                            }
                            return true;
                        }
                        return false;
                    }
                });
    }
}
