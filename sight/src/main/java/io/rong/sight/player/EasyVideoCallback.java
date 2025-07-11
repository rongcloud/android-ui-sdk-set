package io.rong.sight.player;

import android.net.Uri;

/** @author Aidan Follestad (afollestad) */
public interface EasyVideoCallback {

    void onStarted(EasyVideoPlayer player);

    void onPaused(EasyVideoPlayer player);

    void onPreparing(EasyVideoPlayer player);

    void onPrepared(EasyVideoPlayer player);

    void onBuffering(int percent);

    void onError(EasyVideoPlayer player, Exception e);

    void onCompletion(EasyVideoPlayer player);

    void onSightListRequest();

    void onClose();

    default void onPlayError(Uri source, int what, int extra) {
        // do nothing
    }
}
