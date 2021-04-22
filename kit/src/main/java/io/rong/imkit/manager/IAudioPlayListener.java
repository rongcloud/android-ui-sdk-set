package io.rong.imkit.manager;

import android.net.Uri;

public interface IAudioPlayListener {
    void onStart(Uri uri);
    void onStop(Uri uri);
    void onComplete(Uri uri);
}
