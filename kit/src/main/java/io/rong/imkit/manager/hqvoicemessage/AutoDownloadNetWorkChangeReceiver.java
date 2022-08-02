package io.rong.imkit.manager.hqvoicemessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import io.rong.common.RLog;

public class AutoDownloadNetWorkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = AutoDownloadNetWorkChangeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        try {
            networkInfo = cm.getActiveNetworkInfo();
        } catch (Exception e) {
            RLog.e(TAG, "onReceive", e);
        }

        boolean networkAvailable = networkInfo != null && (networkInfo.isAvailable() && networkInfo.isConnected());
        if ((ConnectivityManager.CONNECTIVITY_ACTION).equals(intent.getAction()) && networkAvailable) {
            HQVoiceMsgDownloadManager.getInstance().resumeDownloadService();
        } else {
            HQVoiceMsgDownloadManager.getInstance().pauseDownloadService();
        }
    }
}
