package io.rong.sight.player;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.rong.common.rlog.RLog;
import io.rong.imlib.model.Conversation;
import io.rong.message.SightMessage;
import io.rong.sight.R;
import java.lang.ref.WeakReference;

public class PlaybackVideoFragment extends Fragment implements EasyVideoCallback {

    private static final String TAG = "PlaybackVideoFragment";

    private EasyVideoPlayer mPlayer;
    private static Conversation.ConversationType conversationType;
    private static String targetId;
    private static boolean isFromSightList;
    static boolean displayCurrentVideoOnly = false;
    private EasyVideoCallback mVideoCallback;
    private int playBtnVisible = View.VISIBLE;
    private boolean seekBarClickable = true;
    private static SightMessage mSightMessage;
    private static WeakReference<PlaybackVideoFragment> mPlaybackVideoFragment;
    private static int mInitialPosition;
    private static int mInitialPlayerStatus;
    private Activity mActivity;

    public void setVideoCallback(EasyVideoCallback pVideoCallback) {
        mVideoCallback = pVideoCallback;
    }

    @Override
    public void onAttach(Activity activity) {
        mActivity = activity;
        super.onAttach(activity);
    }

    public static PlaybackVideoFragment newInstance(
            SightMessage sightMessage,
            String outputUri,
            String id,
            Conversation.ConversationType type,
            boolean fromSightList,
            boolean displayCurrentVideo,
            int initialPosition,
            int initialPlayerStatus,
            boolean needAutoPlay) {
        PlaybackVideoFragment fragment = new PlaybackVideoFragment();
        mPlaybackVideoFragment = new WeakReference<>(fragment);
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("output_uri", outputUri);
        args.putBoolean("auto_play", needAutoPlay);
        mInitialPosition = initialPosition;
        mInitialPlayerStatus = initialPlayerStatus;
        mSightMessage = sightMessage;
        targetId = id;
        conversationType = type;
        isFromSightList = fromSightList;
        displayCurrentVideoOnly = displayCurrentVideo;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        if (mPlayer != null) {
            mPlayer.resume();
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        RLog.d(TAG, "onDestroy: ");
        super.onDestroy();
    }

    @Override
    public void onPause() {
        RLog.d(TAG, "onPause: ");
        super.onPause();
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.rc_fragment_sight_palyer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPlayer = view.findViewById(R.id.playbackView);
        if (mPlaybackVideoFragment != null) {
            mPlayer.setCallback(mPlaybackVideoFragment.get());
        }
        mPlayer.setplayBtnVisible(playBtnVisible);
        mPlayer.setSeekBarClickable(seekBarClickable);
        mPlayer.setSource(Uri.parse(getArguments().getString("output_uri")));
        mPlayer.setInitialPosition(mInitialPosition);
        boolean needAutoPlay = getArguments().getBoolean("auto_play", false);
        if (mInitialPlayerStatus == EasyVideoPlayer.PLAYER_STATUS_PAUSED
                || mInitialPlayerStatus == EasyVideoPlayer.PLAYER_STATUS_COMPLETION) {
            needAutoPlay = false;
        }
        mPlayer.setAutoPlay(needAutoPlay);
        if (displayCurrentVideoOnly) {
            mPlayer.setFromSightListImageInVisible();
        }
        if (mSightMessage != null && mSightMessage.isDestruct()) {
            setLongClickable(false);
            mPlayer.setFromSightListImageInVisible();
        } else {
            setLongClickable(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        mVideoCallback = null;
    }

    @Override
    public void onStarted(EasyVideoPlayer player) {
        if (mVideoCallback != null) {
            mVideoCallback.onStarted(player);
        }
    }

    @Override
    public void onPaused(EasyVideoPlayer player) {
        if (mVideoCallback != null) {
            mVideoCallback.onPaused(player);
        }
        if (mPlayer != null) {
            mPlayer.setAutoPlay(false);
        }
    }

    @Override
    public void onPreparing(EasyVideoPlayer player) {
        if (mVideoCallback != null) {
            mVideoCallback.onPreparing(player);
        }
    }

    @Override
    public void onPrepared(EasyVideoPlayer player) {
        if (mVideoCallback != null) {
            mVideoCallback.onPrepared(player);
        }
    }

    @Override
    public void onBuffering(int percent) {
        if (mVideoCallback != null) {
            mVideoCallback.onBuffering(percent);
        }
    }

    @Override
    public void onError(EasyVideoPlayer player, Exception e) {
        //// TODO: 16/12/5 error dialog
        if (mVideoCallback != null) {
            mVideoCallback.onError(player, e);
        }
    }

    @Override
    public void onCompletion(EasyVideoPlayer player) {
        if (mVideoCallback != null) {
            mVideoCallback.onCompletion(player);
        }
    }

    @Override
    public void onPlayError(Uri source, int what, int extra) {
        if (mVideoCallback != null) {
            mVideoCallback.onPlayError(source, what, extra);
        }
    }

    @Override
    public void onSightListRequest() {
        if (getActivity() == null) {
            return;
        }
        if (isFromSightList) {
            getActivity().finish();
        } else {
            if (conversationType == null) {
                return;
            }
            Intent intent = new Intent(getActivity(), SightListActivity.class);
            intent.putExtra("conversationType", conversationType.getValue());
            intent.putExtra("targetId", targetId);
            if (mSightMessage != null) intent.putExtra("isDestruct", mSightMessage.isDestruct());
            startActivity(intent);
        }
    }

    @Override
    public void onClose() {
        if (mActivity != null) {
            mActivity.finish();
        }
    }

    public void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void setplayBtnVisible(int visible) {
        playBtnVisible = visible;
        if (mPlayer != null) {
            mPlayer.setplayBtnVisible(visible);
        }
    }

    public void setSeekBarClickable(boolean pIsClickable) {
        seekBarClickable = pIsClickable;
        if (mPlayer != null) {
            mPlayer.setSeekBarClickable(pIsClickable);
        }
    }

    public void setLongClickable(boolean isLongClickable) {
        if (mPlayer != null) {
            mPlayer.setLongClickable(isLongClickable);
        }
    }

    public void setInitialPosition(int position) {
        if (mPlayer != null) {
            mPlayer.setInitialPosition(position);
        }
    }

    public int getCurrentSeek() {
        if (mPlayer != null) {
            return mPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getCurrentPlayerStatus() {
        return mPlayer.getCurrentPlayerStatus();
    }

    public int getBeforePausePlayerStatus() {
        return mPlayer.getBeforePausePlayerStatus();
    }
}
