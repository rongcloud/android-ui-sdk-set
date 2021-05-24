package io.rong.sight.player;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import io.rong.imlib.model.Conversation;
import io.rong.message.SightMessage;
import io.rong.sight.R;


public class PlaybackVideoFragment extends Fragment implements EasyVideoCallback {

    private EasyVideoPlayer mPlayer;
    static private Conversation.ConversationType conversationType;
    static private String targetId;
    static private boolean isFromSightList;
    static boolean fromSightListImageVisible = true;
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

    public static PlaybackVideoFragment newInstance(SightMessage sightMessage, String outputUri, String id,
                                                    Conversation.ConversationType type, boolean fromSightList,
                                                    boolean sightListImageVisible, int initialPosition, int initialPlayerStatus) {
        PlaybackVideoFragment fragment = new PlaybackVideoFragment();
        mPlaybackVideoFragment = new WeakReference<>(fragment);
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("output_uri", outputUri);
        mInitialPosition = initialPosition;
        mInitialPlayerStatus = initialPlayerStatus;
        mSightMessage = sightMessage;
        targetId = id;
        conversationType = type;
        isFromSightList = fromSightList;
        fromSightListImageVisible = sightListImageVisible;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.rc_fragment_sight_palyer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPlayer = view.findViewById(R.id.playbackView);
        mPlayer.setCallback(mPlaybackVideoFragment.get());
        mPlayer.setplayBtnVisible(playBtnVisible);
        mPlayer.setSeekBarClickable(seekBarClickable);
        mPlayer.setSource(Uri.parse(getArguments().getString("output_uri")));
        mPlayer.setInitialPosition(mInitialPosition);
        boolean needAutoPlay = true;
        if (mInitialPlayerStatus == EasyVideoPlayer.PLAYER_STATUS_PAUSED || mInitialPlayerStatus == EasyVideoPlayer.PLAYER_STATUS_COMPLETION) {
            needAutoPlay = false;
        }
        mPlayer.setAutoPlay(needAutoPlay);
        if (!fromSightListImageVisible) {
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
    public void onSightListRequest() {
        if (isFromSightList) {
            if (getActivity() != null) {
                getActivity().finish();
            }
        } else {
            Intent intent = new Intent(getActivity(), SightListActivity.class);
            intent.putExtra("conversationType", conversationType.getValue());
            intent.putExtra("targetId", targetId);
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