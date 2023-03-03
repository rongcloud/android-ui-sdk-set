package io.rong.imkit.manager.hqvoicemessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import io.rong.imlib.common.NetUtils;

public class AutoDownloadNetWorkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = AutoDownloadNetWorkChangeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean networkAvailable = NetUtils.isNetWorkAvailable(context);
        if ((ConnectivityManager.CONNECTIVITY_ACTION).equals(intent.getAction())
                && networkAvailable) {
            HQVoiceMsgDownloadManager.getInstance().resumeDownloadService();
        } else {
            HQVoiceMsgDownloadManager.getInstance().pauseDownloadService();
        }
    }
}
