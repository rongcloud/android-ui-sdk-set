package io.rong.sight.player;

import android.net.Uri;

import androidx.annotation.CheckResult;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

/**
 * @author Aidan Follestad (afollestad)
 */
@SuppressWarnings("unused")
interface IUserMethods {

    void setSource(@NonNull Uri source);

    void setCallback(@NonNull EasyVideoCallback callback);

    void setProgressCallback(@NonNull EasyVideoProgressCallback callback);

    void setHideControlsOnPlay(boolean hide);

    void setAutoPlay(boolean autoPlay);

    void setInitialPosition(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos);

    void showControls();

    void hideControls();

    @CheckResult
    boolean isControlsShown();

    void toggleControls();

    void enableControls(boolean andShow);

    void disableControls();

    @CheckResult
    boolean isPrepared();

    @CheckResult
    boolean isPlaying();

    @CheckResult
    int getCurrentPosition();

    @CheckResult
    int getDuration();

    void start();

    void seekTo(@IntRange(from = 0, to = Integer.MAX_VALUE) int pos);

    void setVolume(@FloatRange(from = 0f, to = 1f) float leftVolume, @FloatRange(from = 0f, to = 1f) float rightVolume);

    void pause();

    void stop();

    void reset();

    void release();

    void setAutoFullscreen(boolean autoFullScreen);

    void setLoop(boolean loop);
}