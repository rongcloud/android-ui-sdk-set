package io.rong.imkit.manager.hqvoicemessage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.NetUtils;
import io.rong.imlib.model.Message;
import io.rong.message.HQVoiceMessage;

public class HQVoiceMsgDownloadManager {

    private static final String TAG = HQVoiceMsgDownloadManager.class.getSimpleName();
    private Context mContext;
    private final AutoDownloadQueue autoDownloadQueue = new AutoDownloadQueue();
    private ExecutorService executorService;
    private static final int REQUEST_MSG_DOWNLOAD_PERMISSION = 1001;
    private Future<?> future = null;
    private List<AutoDownloadEntry> errorList = null;

    private HQVoiceMsgDownloadManager() {

    }

    public void init(final Context context) {
        AutoDownloadNetWorkChangeReceiver autoDownloadNetWorkChangeReceiver;
        autoDownloadNetWorkChangeReceiver = new AutoDownloadNetWorkChangeReceiver();
        mContext = context.getApplicationContext();
        executorService = Executors.newSingleThreadExecutor();
        errorList = new ArrayList<>();
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            context.getApplicationContext().registerReceiver(autoDownloadNetWorkChangeReceiver, intentFilter);
        } catch (Exception e) {
            RLog.e(TAG, "registerReceiver Exception", e);
        }
        downloadHQVoiceMessage();
        final String[] writePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        IMCenter.getInstance().addOnReceiveMessageListener(new RongIMClient.OnReceiveMessageWrapperListener() {
            @Override
            public boolean onReceived(Message message, int left, boolean hasPackage, boolean offline) {
                if (!offline && message.getContent() instanceof HQVoiceMessage
                        && RongConfigCenter.conversationListConfig().isEnableAutomaticDownloadHQVoice()) {
                    if (PermissionCheckUtil.checkPermissions(context, writePermission)) {
                        enqueue(new AutoDownloadEntry(message, AutoDownloadEntry.DownloadPriority.NORMAL));
                    }
                }
                return false;
            }
        });
    }

    private static class HQVoiceMsgDownloadManagerHolder {
        @SuppressLint("StaticFieldLeak")
        private static HQVoiceMsgDownloadManager instance = new HQVoiceMsgDownloadManager();
    }

    public static HQVoiceMsgDownloadManager getInstance() {
        return HQVoiceMsgDownloadManagerHolder.instance;
    }

    public void enqueue(Fragment fragment, AutoDownloadEntry autoDownloadEntry) {

        if (autoDownloadEntry == null) {
            return;
        }

        Message message = autoDownloadEntry.getMessage();
        if (!(message.getContent() instanceof HQVoiceMessage)
                || (ifMsgInHashMap(message) && fragment != null)) {
            return;
        }

        String[] permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (fragment != null) {
            if (!PermissionCheckUtil.checkPermissions(fragment.getActivity(), permission)) {
                PermissionCheckUtil.requestPermissions(fragment, permission, REQUEST_MSG_DOWNLOAD_PERMISSION);
            }
        }

        HQVoiceMessage hqVoiceMessage = (HQVoiceMessage) message.getContent();

        if (!(hqVoiceMessage.getLocalPath() == null
                || TextUtils.isEmpty(hqVoiceMessage.getLocalPath().toString()))) {
            return;
        }

        synchronized (autoDownloadQueue) {
            boolean isEmpty = autoDownloadQueue.isEmpty();
            autoDownloadQueue.enqueue(autoDownloadEntry);
            if (isEmpty) {
                autoDownloadQueue.notify();
            }

            if (future.isDone() && NetUtils.isNetWorkAvailable(mContext)) {
                downloadHQVoiceMessage();
            }
        }
    }

    public void enqueue(AutoDownloadEntry autoDownloadEntry) {
        enqueue(null, autoDownloadEntry);
    }

    private Message dequeue() {
        return autoDownloadQueue.dequeue();
    }

    private void removeUidInHashMap(String uid) {
        autoDownloadQueue.getAutoDownloadEntryHashMap().remove(uid);
    }

    private boolean ifMsgInHashMap(Message message) {
        return autoDownloadQueue.ifMsgInHashMap(message);
    }

    private AutoDownloadEntry getMsgEntry(Message message) {
        if (message == null) {
            return null;
        }
        AutoDownloadEntry autoDownloadEntry = null;
        if (autoDownloadQueue.getAutoDownloadEntryHashMap().containsKey(message.getUId())) {
            autoDownloadEntry = autoDownloadQueue.getAutoDownloadEntryHashMap().get(message.getUId());
        }
        return autoDownloadEntry;
    }

    private void downloadHQVoiceMessage() {
        future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                //noinspection InfiniteLoopStatement
                while (true) {
                    synchronized (autoDownloadQueue) {
                        if (autoDownloadQueue.isEmpty()) {
                            try {
                                autoDownloadQueue.wait();
                            } catch (InterruptedException e) {
                                RLog.e(TAG, "downloadHQVoiceMessage e:" + e.toString());
                                Thread.currentThread().interrupt();
                            }
                        }
                    }

                    Message message = dequeue();
                    IMCenter.getInstance().downloadMediaMessage(message, new IRongCallback.IDownloadMediaMessageCallback() {
                        @Override
                        public void onSuccess(Message message) {
                            RLog.d(TAG, "downloadMediaMessage success");
                            if (errorList != null) {
                                errorList.remove(getMsgEntry(message));
                            }
                            removeUidInHashMap(message.getUId());
                        }

                        @Override
                        public void onProgress(Message message, int progress) {
                            RLog.d(TAG, "downloadMediaMessage onProgress");
                        }

                        @Override
                        public void onError(Message message, RongIMClient.ErrorCode code) {
                            if (errorList != null && !errorList.contains(getMsgEntry(message))) {
                                errorList.add(getMsgEntry(message));
                                RLog.i(TAG, "onError = " + code.getValue() + " errorList size = " + errorList.size());
                            }
                        }

                        @Override
                        public void onCanceled(Message message) {

                        }
                    });
                }
            }
        });
    }

    void pauseDownloadService() {
        //TODO 网络中断后处理
    }

    public void resumeDownloadService() {
        if (errorList == null || errorList.size() == 0) {
            return;
        }
        if (future.isDone() && NetUtils.isNetWorkAvailable(mContext)) {
            downloadHQVoiceMessage();
        }

        for (int i = errorList.size() - 1; i >= 0; i--) {
            enqueue(errorList.get(i));
        }
    }
}
