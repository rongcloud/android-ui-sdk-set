package io.rong.sight.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.event.actionevent.BaseMessageEvent;
import io.rong.imkit.event.actionevent.DeleteEvent;
import io.rong.imkit.event.actionevent.DownloadEvent;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.widget.CircleProgressView;
import io.rong.imlib.ErrorCodes;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.SightMessage;
import io.rong.sight.R;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Objects;

/** @author gusd @Date 2023/03/21 */
public class SightPlayerFragment extends Fragment implements EasyVideoCallback {
    private static final String TAG = "SightPlayerFragment";

    private SightMessage mSightMessage;
    private Message mMessage;
    private int mProgress;
    private ImageView mThumbImageView;
    private FrameLayout mContainer;
    private RelativeLayout rlSightDownload;
    private CircleProgressView mSightDownloadProgress;
    private RelativeLayout mSightDownloadFailedReminder;
    private TextView mCountDownView;
    private boolean fromSightListImageVisible = true;
    private PlaybackVideoFragment mPlaybackVideoFragment;
    private SightPlayerFragment.DownloadMediaMessageCallback downloadMediaMessageCallback;
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

    private View mRootView;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.rc_fragment_sight_player, container, false);
        Bundle arguments = getArguments();
        if (arguments == null) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
            return mRootView;
        }
        try {
            mSightMessage = arguments.getParcelable("SightMessage");
            mMessage = arguments.getParcelable("Message");
            mProgress = arguments.getInt("Progress", 0);
            fromSightListImageVisible = arguments.getBoolean("fromSightListImageVisible", true);
        } catch (Exception exception) {
            RLog.i(TAG, "getIntent exception");
        }

        mContainer = findViewById(R.id.container);
        rlSightDownload = findViewById(R.id.rl_sight_download);
        mCountDownView = findViewById(R.id.rc_count_down);
        mFailedText = findViewById(R.id.rc_sight_download_failed_tv_reminder);
        failedImageView = findViewById(R.id.rc_sight_download_failed_iv_reminder);
        mSightDownloadFailedReminder = findViewById(R.id.rc_sight_download_failed_reminder);

        downloadMediaMessageCallback = new SightPlayerFragment.DownloadMediaMessageCallback(this);
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
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
            return mRootView;
        }
        if (FileUtils.isFileExistsWithUri(this.getActivity(), mSightMessage.getLocalPath())) {
            initSightPlayer();
        } else {
            initDownloadView();
        }
        IMCenter.getInstance().addMessageEventListener(mEvent);

        if (mSightMessage != null
                && mSightMessage.isDestruct()
                && mMessage != null
                && mMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
            DestructManager.getInstance().stopDestruct(mMessage);
            // EventBus.getDefault().post(new Event.changeDestructionReadTimeEvent(mMessage));
        }
        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!FileUtils.isFileExistsWithUri(this.getActivity(), mSightMessage.getLocalPath())) {
            if (mProgress == 0) {
                downloadSight();
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (mSightMessage != null
                && mSightMessage.isDestruct()
                && mMessage != null
                && mMessage.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {
            DestructManager.getInstance().startDestruct(mMessage);
            // EventBus.getDefault().post(new Event.changeDestructionReadTimeEvent(mMessage));
        }
        IMCenter.getInstance().removeMessageEventListener(mEvent);
        super.onDestroyView();
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
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.finish();
                                }
                            }
                        });
    }

    private <T extends View> T findViewById(@IdRes int id) {
        return mRootView.findViewById(id);
    }

    private void downloadSight() {
        RLog.e(TAG, "DownloadEvent:" + "***" + (mMessage == null));
        IMCenter.getInstance().downloadMediaMessage(mMessage, null);
    }

    private void initSightPlayer() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        mContainer.setVisibility(View.VISIBLE);
        mPlaybackVideoFragment =
                PlaybackVideoFragment.newInstance(
                        mSightMessage,
                        mSightMessage.getLocalPath().toString(),
                        mMessage.getTargetId(),
                        mMessage.getConversationType(),
                        getArguments().getBoolean("fromList", false),
                        fromSightListImageVisible,
                        currentSeek,
                        currentPlayerStatus,
                        getArguments().getBoolean("auto_play", false));
        mPlaybackVideoFragment.setVideoCallback(this);
        if (mSightMessage != null && mSightMessage.isDestruct()) {
            mPlaybackVideoFragment.setplayBtnVisible(View.GONE);
            mPlaybackVideoFragment.setSeekBarClickable(false);
        }
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.container, mPlaybackVideoFragment)
                .commitAllowingStateLoss();
    }

    public void processMessageDelete(DeleteEvent deleteEvent) {
        RLog.d(TAG, "MessageDeleteEvent");
        if (deleteEvent.getMessageIds() != null && mMessage != null) {
            for (int messageId : deleteEvent.getMessageIds()) {
                if (messageId == mMessage.getMessageId()) {
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.finish();
                    }
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
        new AlertDialog.Builder(this.getActivity())
                .setMessage(R.string.rc_video_play_error_open_system_player)
                .setPositiveButton(
                        R.string.rc_confirm,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openExternalPlayer(source);
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.finish();
                                }
                            }
                        })
                .setNegativeButton(
                        R.string.rc_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Activity activity = getActivity();
                                if (activity != null) {
                                    activity.finish();
                                }
                            }
                        })
                .show();
    }

    private void openExternalPlayer(Uri source) {
        try {

            Context context = getActivity();
            if (context == null) {
                return;
            }
            Uri uri = source;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && "file".equals(source.getScheme())) {
                uri =
                        FileProvider.getUriForFile(
                                context.getApplicationContext(),
                                context.getApplicationContext().getPackageName()
                                        + context.getResources()
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
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    @Override
    public void onSightListRequest() {
        // default implementation ignored
    }

    @Override
    public void onClose() {
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    public static class DownloadMediaMessageCallback
            implements IRongCallback.IDownloadMediaMessageCallback {
        WeakReference<SightPlayerFragment> reference;

        public DownloadMediaMessageCallback(SightPlayerFragment fragment) {
            reference = new WeakReference<>(fragment);
        }

        @Override
        public void onSuccess(Message message) {
            SightPlayerFragment fragment = reference.get();
            if (fragment != null
                    && message != null
                    && message.getContent() instanceof SightMessage) {
                Uri uri = ((SightMessage) message.getContent()).getMediaUrl();
                if (uri != null
                        && fragment.mSightMessage != null
                        && uri.equals(fragment.mSightMessage.getMediaUrl())) {
                    if (fragment.getActivity() == null || fragment.getActivity().isFinishing()) {
                        return;
                    }
                    fragment.rlSightDownload.setVisibility(View.GONE);
                    fragment.mThumbImageView.setVisibility(View.GONE);
                    fragment.mSightDownloadProgress.setVisibility(View.GONE);
                    fragment.mSightMessage = (SightMessage) message.getContent();
                    fragment.mMessage = message;
                    fragment.initSightPlayer();
                }
            }
        }

        @Override
        public void onProgress(Message message, int progress) {
            SightPlayerFragment fragment = reference.get();
            if (message != null) {
                RLog.e(
                        TAG,
                        "DownloadEvent:"
                                + (fragment != null)
                                + "***kk***"
                                + (message != null)
                                + "***"
                                + (message.getContent() instanceof SightMessage));
            }
            if (fragment != null
                    && message != null
                    && message.getContent() instanceof SightMessage) {
                Uri uri = ((SightMessage) message.getContent()).getMediaUrl();
                RLog.e(
                        TAG,
                        "DownloadEvent:"
                                + (uri != null)
                                + "***jj***"
                                + (fragment.mSightMessage != null)
                                + "***"
                                + (uri.equals(fragment.mSightMessage.getMediaUrl())));
                if (uri != null
                        && fragment.mSightMessage != null
                        && uri.equals(fragment.mSightMessage.getMediaUrl())) {
                    RLog.e(TAG, "DownloadEvent:" + "coming ===");
                    fragment.mProgress = progress;
                    fragment.mSightDownloadProgress.setVisibility(View.VISIBLE);
                    fragment.mSightDownloadProgress.setProgress(fragment.mProgress, true);
                }
            }
        }

        @Override
        public void onError(Message message, RongIMClient.ErrorCode code) {
            RLog.e(TAG, "DownloadEvent:" + "Error===" + code.getMessage());
            final SightPlayerFragment fragment = reference.get();
            if (fragment != null
                    && message != null
                    && message.getContent() instanceof SightMessage) {
                Uri uri = ((SightMessage) message.getContent()).getMediaUrl();
                if (uri != null
                        && fragment.mSightMessage != null
                        && uri.equals(fragment.mSightMessage.getMediaUrl())) {
                    fragment.mSightDownloadProgress.setVisibility(View.GONE);
                    fragment.mSightDownloadFailedReminder.setVisibility(View.VISIBLE);
                    fragment.mSightDownloadFailedReminder.setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    fragment.mSightDownloadFailedReminder.setVisibility(View.GONE);
                                    fragment.mProgress = 0;
                                    fragment.downloadSight();
                                }
                            });
                    if (code.getValue() == ErrorCodes.FILE_EXPIRED.getCode()) {
                        fragment.failedImageView.setVisibility(View.GONE);
                        fragment.mFailedText.setText(R.string.rc_sight_file_expired);
                    } else {
                        fragment.mFailedText.setText(R.string.rc_sight_download_failed);
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
