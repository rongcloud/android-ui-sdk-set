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

    public void setVideoCallback(EasyVideoCallback pVideoCallback) {
        mVideoCallback = pVideoCallback;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public static PlaybackVideoFragment newInstance(SightMessage sightMessage, String outputUri, String id, Conversation.ConversationType type, boolean fromSightList, boolean sightListImageVisible) {
        PlaybackVideoFragment fragment = new PlaybackVideoFragment();
        fragment.setRetainInstance(true);
        Bundle args = new Bundle();
        args.putString("output_uri", outputUri);
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
        mPlayer.setCallback(this);
        mPlayer.setplayBtnVisible(playBtnVisible);
        mPlayer.setSeekBarClickable(seekBarClickable);
        mPlayer.setSource(Uri.parse(getArguments().getString("output_uri")));
        if (!fromSightListImageVisible)
            mPlayer.setFromSightListImageInVisible();
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
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    public void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
    }

    public void setplayBtnVisible(int visible) {
        playBtnVisible = visible;
        if (mPlayer != null)
            mPlayer.setplayBtnVisible(visible);
    }

    public void setSeekBarClickable(boolean pIsClickable) {
        seekBarClickable = pIsClickable;
        if (mPlayer != null)
            mPlayer.setSeekBarClickable(pIsClickable);
    }

    public void setLongClickable(boolean isLongClickable) {
        if (mPlayer != null) {
            mPlayer.setLongClickable(isLongClickable);
        }
    }
}