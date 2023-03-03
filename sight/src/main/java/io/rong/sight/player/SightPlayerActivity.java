package io.rong.sight.player;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import com.bumptech.glide.Glide;
import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.activity.RongBaseNoActionbarActivity;
import io.rong.imkit.event.actionevent.BaseMessageEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.DownloadEvent;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.widget.CircleProgressView;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.SightMessage;
import io.rong.sight.R;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class SightPlayerActivity extends RongBaseNoActionbarActivity
        implements EasyVideoCallback, RongIMClient.OnRecallMessageListener {

    private static final String TAG = SightPlayerActivity.class.getSimpleName();

    private SightMessage mSightMessage;
    private Message mMessage;
    private int mProgress;
    private ImageView mThumbImageView;
    private FrameLayout mContainer;
    private RelativeLayout rlSightDownload;
    private CircleProgressView mSightDownloadProgress;
    private RelativeLayout mSightDownloadFailedReminder;
    private TextView mCountDownView;
    private boolean isFinishing = false;
    private boolean fromSightListImageVisible = true;
    private PlaybackVideoFragment mPlaybackVideoFragment;
    private DownloadMediaMessageCallback downloadMediaMessageCallback;
    private int currentSeek;
    private int currentPlayerStatus;
    private TextView mFailedText;
    private ImageView failedImageView;
    BaseMessageEvent mEvent =
            new BaseMessageEvent() {
                @Override
                public void onDownloadMessage(DownloadEvent event) {
                    processDownloadEvent(event);
                }

                @Override
                public void onDeleteMessage(DeleteEvent event) {
                    processMessageDelete(event);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow()
                .setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.rc_activity_sight_player);

        try {
            mSightMessage = getIntent().getParcelableExtra("SightMessage");
            mMessage = getIntent().getParcelableExtra("Message");
            mProgress = getIntent().getIntExtra("Progress", 0);
            fromSightListImageVisible =
                    getIntent().getBooleanExtra("fromSightListImageVisible", true);
        } catch (Exception exception) {
            RLog.i(TAG, "getIntent exception");
        }

        mContainer = findViewById(R.id.container);
        rlSightDownload = findViewById(R.id.rl_sight_download);
        mCountDownView = findViewById(R.id.rc_count_down);
        mFailedText = findViewById(R.id.rc_sight_download_failed_tv_reminder);
        failedImageView = findViewById(R.id.rc_sight_download_failed_iv_reminder);
        mSightDownloadFailedReminder = findViewById(R.id.rc_sight_download_failed_reminder);

        downloadMediaMessageCallback = new DownloadMediaMessageCallback(this);
        if (savedInstanceState != null) {
            currentSeek = savedInstanceState.getInt("seek", 0);
            currentPlayerStatus = savedInstanceState.getInt("status", 0);
            Message message = savedInstanceState.getParcelable("message");
            if (message != null) {
                mMessage = message;
                mSightMessage = (SightMessage) mMessage.getContent();
            }
        }
        if (mSightMessage == null) {
            return;
        }
        if (FileUtils.isFileExistsWithUri(this, mSightMessage.getLocalPath())) {
            initSightPlayer();
        } else {
            initDownloadView();
            if (mProgress == 0) {
                downloadSight();
            }
        }
        IMCenter.getInstance().addOnRecallMessageListener(this);
        IMCenter.getInstance().addMessageEventListener(mEvent);

        if (mSightMessage != null
                && mSightMessage.isDestruct()
                && mMessage != null
                && mMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
            DestructManager.getInstance().stopDestruct(mMessage);
            // EventBus.getDefault().post(new Event.changeDestructionReadTimeEvent(mMessage));
        }
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            isFinishing = true;
        }
        // todo 阅后即焚逻辑
        if (mSightMessage != null
                && mSightMessage.isDestruct()
                && mMessage != null
                && mMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
            DestructManager.getInstance().startDestruct(mMessage);
            // EventBus.getDefault().post(new Event.changeDestructionReadTimeEvent(mMessage));
        }
        IMCenter.getInstance().removeOnRecallMessageListener(this);
        IMCenter.getInstance().removeMessageEventListener(mEvent);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mPlaybackVideoFragment != null) {
            int status = mPlaybackVideoFragment.getBeforePausePlayerStatus();
            int seek = mPlaybackVideoFragment.getCurrentSeek();
            outState.putInt("seek", seek);
            outState.putInt("status", status);
            outState.putParcelable("message", mMessage);
        }
        super.onSaveInstanceState(outState);
    }

    private void initDownloadView() {
        rlSightDownload.setVisibility(View.VISIBLE);
        mThumbImageView = findViewById(R.id.rc_sight_thumb);
        if (mSightMessage.getThumbUri() != null && mSightMessage.getThumbUri().getPath() != null) {
            Glide.with(this)
                    .load(new File(mSightMessage.getThumbUri().getPath()))
                    .into(mThumbImageView);
        }
        mSightDownloadProgress = findViewById(R.id.rc_sight_download_progress);
        mSightDownloadProgress.setVisibility(View.VISIBLE);
        mSightDownloadProgress.setProgress(mProgress, true);
        findViewById(R.id.rc_sight_download_close)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                finish();
                            }
                        });
    }

    public void processRecallMessageRemote(Message message) {
        if (message.getMessageId() == mMessage.getMessageId()) {
            IMCenter.getInstance().cancelDownloadMediaMessage(mMessage, null);
            if (mPlaybackVideoFragment != null) {
                mPlaybackVideoFragment.pause();
            }
            new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                    .setMessage(getString(R.string.rc_recall_success))
                    .setPositiveButton(
                            getString(R.string.rc_dialog_ok),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .setCancelable(false)
                    .show();
        }
    }

    private void downloadSight() {
        RLog.e(TAG, "DownloadEvent:" + "***" + (mMessage == null));
        IMCenter.getInstance().downloadMediaMessage(mMessage, null);
    }

    @Override
    public boolean onMessageRecalled(
            Message message, RecallNotificationMessage recallNotificationMessage) {
        processRecallMessageRemote(message);
        return false;
    }

    private void initSightPlayer() {
        if (isFinishing()) {
            return;
        }
        mContainer.setVisibility(View.VISIBLE);
        mPlaybackVideoFragment =
                PlaybackVideoFragment.newInstance(
                        mSightMessage,
                        mSightMessage.getLocalPath().toString(),
                        mMessage.getTargetId(),
                        mMessage.getConversationType(),
                        getIntent().getBooleanExtra("fromList", false),
                        fromSightListImageVisible,
                        currentSeek,
                        currentPlayerStatus);
        mPlaybackVideoFragment.setVideoCallback(this);
        if (mSightMessage != null && mSightMessage.isDestruct()) {
            mPlaybackVideoFragment.setplayBtnVisible(View.GONE);
            mPlaybackVideoFragment.setSeekBarClickable(false);
        }
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, mPlaybackVideoFragment)
                .commitAllowingStateLoss();
    }

    public void processMessageDelete(DeleteEvent deleteEvent) {
        RLog.d(TAG, "MessageDeleteEvent");
        if (deleteEvent.getMessageIds() != null && mMessage != null) {
            for (int messageId : deleteEvent.getMessageIds()) {
                if (messageId == mMessage.getMessageId()) {
                    finish();
                    break;
                }
            }
        }
    }

    public void processDownloadEvent(DownloadEvent downloadEvent) {
        RLog.d(TAG, "FileMessageEvent");
        Message message = downloadEvent.getMessage();
        String uId = message.getUId();
        RLog.e(
                TAG,
                "DownloadEvent:" + downloadEvent.getProgress() + "===" + downloadEvent.getEvent());
        if (mMessage != null
                && downloadMediaMessageCallback != null
                && Objects.equals(uId, mMessage.getUId())) {
            int callBackType = downloadEvent.getEvent();
            switch (callBackType) {
                case DownloadEvent.SUCCESS:
                    downloadMediaMessageCallback.onSuccess(message);
                    break;
                case DownloadEvent.ERROR:
                    downloadMediaMessageCallback.onError(message, downloadEvent.getCode());
                    break;
                case DownloadEvent.CANCEL:
                    downloadMediaMessageCallback.onCanceled(message);
                    break;
                case DownloadEvent.PROGRESS:
                    downloadMediaMessageCallback.onProgress(message, downloadEvent.getProgress());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onStarted(EasyVideoPlayer player) {
        // default implementation ignored
    }

    @Override
    public void onPaused(EasyVideoPlayer player) {
        // default implementation ignored
    }

    @Override
    public void onPreparing(EasyVideoPlayer player) {
        // default implementation ignored
    }

    @Override
    public void onPrepared(EasyVideoPlayer player) {
        // default implementation ignored
    }

    @Override
    public void onBuffering(int percent) {
        // default implementation ignored
    }

    @Override
    public void onError(EasyVideoPlayer player, Exception e) {
        // default implementation ignored
    }

    @Override
    public void onPlayError(final Uri source, int what, int extra) {
        RLog.d(
                TAG,
                "onPlayError: " + "source = " + source + ", what = " + what + ", extra = " + extra);
        new AlertDialog.Builder(this)
                .setMessage(R.string.rc_video_play_error_open_system_player)
                .setPositiveButton(
                        R.string.rc_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openExternalPlayer(source);
                                finish();
                            }
                        })
                .setNegativeButton(
                        R.string.rc_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                .show();
    }

    private void openExternalPlayer(Uri source) {
        try {
            Uri uri = source;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && "file".equals(source.getScheme())) {
                uri =
                        FileProvider.getUriForFile(
                                getApplicationContext(),
                                getBaseContext().getApplicationContext().getPackageName()
                                        + getBaseContext()
                                                .getResources()
                                                .getString(
                                                        io.rong
                                                                .imkit
                                                                .R
                                                                .string
                                                                .rc_authorities_fileprovider),
                                new File(source.getPath()));
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(
                    uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension(source.toString()));
            startActivity(intent);
        } catch (Exception e) {
            RLog.e(TAG, "onPlayError: " + "Exception = " + e);
        }
    }

    @Override
    public void onCompletion(EasyVideoPlayer player) {
        // 播放完成且是阅后即焚消息关闭小视频
        if (mSightMessage != null && mSightMessage.isDestruct()) {
            finish();
        }
    }

    @Override
    public void onSightListRequest() {
        // default implementation ignored
    }

    @Override
    public void onClose() {
        finish();
    }

    public static class DownloadMediaMessageCallback
            implements IRongCallback.IDownloadMediaMessageCallback {
        WeakReference<SightPlayerActivity> reference;

        public DownloadMediaMessageCallback(SightPlayerActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(Message message) {
            SightPlayerActivity activity = reference.get();
            if (activity != null
                    && message != null
                    && message.getContent() instanceof SightMessage) {
                Uri uri = ((SightMessage) message.getContent()).getMediaUrl();
                if (uri != null
                        && activity.mSightMessage != null
                        && uri.equals(activity.mSightMessage.getMediaUrl())) {
                    if (activity.isFinishing) {
                        return;
                    }
                    activity.rlSightDownload.setVisibility(View.GONE);
                    activity.mThumbImageView.setVisibility(View.GONE);
                    activity.mSightDownloadProgress.setVisibility(View.GONE);
                    activity.mSightMessage = (SightMessage) message.getContent();
                    activity.mMessage = message;
                    activity.initSightPlayer();
                }
            }
        }

        @Override
        public void onProgress(Message message, int progress) {
            SightPlayerActivity activity = reference.get();
            if (message != null) {
                RLog.e(
                        TAG,
                        "DownloadEvent:"
                                + (activity != null)
                                + "***kk***"
                                + (message != null)
                                + "***"
                                + (message.getContent() instanceof SightMessage));
            }
            if (activity != null
                    && message != null
                    && message.getContent() instanceof SightMessage) {
                Uri uri = ((SightMessage) message.getContent()).getMediaUrl();
                RLog.e(
                        TAG,
                        "DownloadEvent:"
                                + (uri != null)
                                + "***jj***"
                                + (activity.mSightMessage != null)
                                + "***"
                                + (uri.equals(activity.mSightMessage.getMediaUrl())));
                if (uri != null
                        && activity.mSightMessage != null
                        && uri.equals(activity.mSightMessage.getMediaUrl())) {
                    RLog.e(TAG, "DownloadEvent:" + "coming ===");
                    activity.mProgress = progress;
                    activity.mSightDownloadProgress.setVisibility(View.VISIBLE);
                    activity.mSightDownloadProgress.setProgress(activity.mProgress, true);
                }
            }
        }

        @Override
        public void onError(Message message, RongIMClient.ErrorCode code) {
            RLog.e(TAG, "DownloadEvent:" + "Error===" + code.getMessage());
            final SightPlayerActivity activity = reference.get();
            if (activity != null
                    && message != null
                    && message.getContent() instanceof SightMessage) {
                Uri uri = ((SightMessage) message.getContent()).getMediaUrl();
                if (uri != null
                        && activity.mSightMessage != null
                        && uri.equals(activity.mSightMessage.getMediaUrl())) {
                    activity.mSightDownloadProgress.setVisibility(View.GONE);
                    activity.mSightDownloadFailedReminder.setVisibility(View.VISIBLE);
                    activity.mSightDownloadFailedReminder.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    activity.mSightDownloadFailedReminder.setVisibility(View.GONE);
                                    activity.mProgress = 0;
                                    activity.downloadSight();
                                }
                            });
                    if (code.equals(RongIMClient.ErrorCode.RC_FILE_EXPIRED)) {
                        activity.failedImageView.setVisibility(View.GONE);
                        activity.mFailedText.setText(R.string.rc_sight_file_expired);
                    } else {
                        activity.mFailedText.setText(R.string.rc_sight_download_failed);
                    }
                    //                    activity.initDownloadFailedReminder(code);
                }
            }
        }

        @Override
        public void onCanceled(Message message) {
            // default implementation ignored
        }
    }
}
