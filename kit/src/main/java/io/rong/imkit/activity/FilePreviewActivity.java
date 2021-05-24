package io.rong.imkit.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.event.actionevent.BaseMessageEvent;
import io.rong.imkit.event.actionevent.DownloadEvent;
import io.rong.imkit.event.actionevent.MessageEventListener;
import io.rong.imkit.utils.FileTypeUtils;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.DownloadInfo;
import io.rong.imlib.model.Message;
import io.rong.message.FileMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;

import static android.widget.Toast.makeText;


public class FilePreviewActivity extends RongBaseActivity implements View.OnClickListener {
    private final static String TAG = "FilePreviewActivity";
    public static final int NOT_DOWNLOAD = 0;
    public static final int DOWNLOADED = 1;
    public static final int DOWNLOADING = 2;
    public static final int DELETED = 3;
    public static final int DOWNLOAD_ERROR = 4;
    public static final int DOWNLOAD_CANCEL = 5;
    public static final int DOWNLOAD_SUCCESS = 6;
    public static final int DOWNLOAD_PAUSE = 7;

    public static final int REQUEST_CODE_PERMISSION = 104;

    private static final String TXT_FILE = ".txt";
    private static final String APK_FILE = ".apk";

    private ImageView mFileTypeImage;
    private TextView mFileNameView;
    private TextView mFileSizeView;
    private Button mFileButton;

    protected FileDownloadInfo mFileDownloadInfo;
    protected FileMessage mFileMessage;
    protected Message mMessage;
    private int mProgress;

    private String mFileName;
    private long mFileSize;
    private List<Toast> mToasts;
    private FrameLayout contentContainer;
    private DownloadInfo info = null;
    private long downloadedFileLength;
    private MessageEventListener mEventListener = new BaseMessageEvent() {
        @Override
        public void onDownloadMessage(DownloadEvent event) {
            updateDownloadStatus(event);
        }
    };
    private IRongCoreCallback.ResultCallback<DownloadInfo> callback = new DownloadInfoCallBack(this);

    private static class DownloadInfoCallBack extends IRongCoreCallback.ResultCallback<DownloadInfo> {
        WeakReference<FilePreviewActivity> weakActivity;

        public DownloadInfoCallBack(FilePreviewActivity activity) {
            this.weakActivity = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(DownloadInfo downloadInfo) {

            if (weakActivity.get() == null) {
                return;
            }
            FilePreviewActivity activity = weakActivity.get();
            activity.info = downloadInfo;
            if (downloadInfo != null) {
                activity.mFileDownloadInfo.progress = downloadInfo.currentProgress();
            }
            activity.getInfoNow = false;
            activity.setViewStatusForResumeTransfer();
            activity.getFileDownloadInfoForResumeTransfer();
            activity.mFileButton.setBackgroundResource(R.drawable.rc_ac_btn_file_download_open_button);
            activity.mFileButton.setEnabled(true);
            RLog.d("getDownloadInfo", "getFileInfo finish");
        }

        @Override
        public void onError(IRongCoreEnum.CoreErrorCode e) {

        }
    }

    private RongIMClient.OnRecallMessageListener mRecallListener = new RongIMClient.OnRecallMessageListener() {
        @Override
        public boolean onMessageRecalled(Message message, RecallNotificationMessage recallNotificationMessage) {
            if (mMessage == null) {
                return false;
            }
            int messageId = mMessage.getMessageId();
            if (messageId == message.getMessageId()) {
                new AlertDialog.Builder(FilePreviewActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                        .setMessage(getString(R.string.rc_recall_success))
                        .setPositiveButton(getString(R.string.rc_dialog_ok), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.rc_ac_file_download);
        initStatusBar(R.color.app_color_white);
        initView();
        initData();
        initListener();
        getFileMessageStatus();
    }


    private void initListener() {
        IMCenter.getInstance().addMessageEventListener(mEventListener);
        IMCenter.getInstance().addOnRecallMessageListener(mRecallListener);
    }

    private void getFileMessageStatus() {
        Uri fileUrl = mFileMessage.getFileUrl();
        Uri localUri = mFileMessage.getLocalPath();
        boolean isLocalPathExist = false;
        if (localUri != null) {
            if (FileUtils.isFileExistsWithUri(this, localUri)) {
                isLocalPathExist = true;
            }
        }
        if (!isLocalPathExist && fileUrl != null && !TextUtils.isEmpty(fileUrl.toString())) {
            String url = fileUrl.toString();
            final long init = System.currentTimeMillis();
            mFileButton.setEnabled(false);
            mFileButton.setText(R.string.rc_picture_please);
            mFileButton.setBackgroundResource(R.drawable.rc_ac_btn_file_download_open_uncheck);
            RLog.d("test", "init time" + init + ",url" + url);
            getFileDownloadInfoInSubThread();
        } else {
            setViewStatus();
            getFileDownloadInfo();
        }
    }

    @Override
    public void setContentView(int resId) {
        contentContainer.removeAllViews();
        View view = LayoutInflater.from(this).inflate(resId, null);
        contentContainer.addView(view);
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) return;

        mFileDownloadInfo = new FileDownloadInfo();
        mFileMessage = getIntent().getParcelableExtra("FileMessage");
        mMessage = getIntent().getParcelableExtra("Message");
        mProgress = getIntent().getIntExtra("Progress", 0);

        mToasts = new ArrayList<>();
        mFileName = mFileMessage.getName();
        mFileTypeImage.setImageResource(FileTypeUtils.fileTypeImageId(this, mFileName));
        mFileNameView.setText(mFileName);
        mFileSize = mFileMessage.getSize();
        mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileSize));
        mFileButton.setOnClickListener(this);
    }

    private void initView() {
        contentContainer = (FrameLayout) findViewById(R.id.rc_ac_ll_content_container);
        View view = LayoutInflater.from(this).inflate(R.layout.rc_ac_file_preview_content, null);
        contentContainer.addView(view);
        mFileTypeImage = (ImageView) findViewById(R.id.rc_ac_iv_file_type_image);
        mFileNameView = (TextView) findViewById(R.id.rc_ac_tv_file_name);
        mFileSizeView = (TextView) findViewById(R.id.rc_ac_tv_file_size);
        mFileButton = (Button) findViewById(R.id.rc_ac_btn_download_button);
        mTitleBar.setTitle(R.string.rc_ac_file_download_preview);
        mTitleBar.setRightVisible(false);
    }

    private void setViewStatus() {
        if (mMessage.getMessageDirection() == Message.MessageDirection.RECEIVE) {
            if (mProgress == 0) {
                mFileButton.setVisibility(View.VISIBLE);
            } else if (mProgress == 100) {
                mFileButton.setVisibility(View.VISIBLE);
            } else {
                mFileButton.setVisibility(View.GONE);
            }
        }
    }

    private void setViewStatusForResumeTransfer() {
        mFileButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        if (v == mFileButton) {
            switch (mFileDownloadInfo.state) {
                case NOT_DOWNLOAD:
                case DOWNLOAD_CANCEL:
                case DOWNLOAD_ERROR:
                case DELETED:
                    startToDownload();
                    break;
                case DOWNLOAD_SUCCESS:
                case DOWNLOADED:
                    openFile(mFileName, mFileMessage.getLocalPath());
                    break;
                case DOWNLOADING:
                    mFileDownloadInfo.state = DOWNLOAD_PAUSE;
                    IMCenter.getInstance().pauseDownloadMediaMessage(mMessage, null);
                    downloadedFileLength = (long) (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0) + 0.5f);
                    mFileSizeView.setText(getString(R.string.rc_ac_file_download_progress_pause) + "(" + FileTypeUtils.formatFileSize(downloadedFileLength)
                            + "/" + FileTypeUtils.formatFileSize(mFileSize) + ")");
                    mFileButton.setText(getResources().getString(R.string.rc_ac_file_preview_download_resume));
                    break;
                case DOWNLOAD_PAUSE:
                    if (IMCenter.getInstance().getCurrentConnectionStatus() == RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE) {
                        makeText(FilePreviewActivity.this, getString(R.string.rc_notice_network_unavailable), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mFileDownloadInfo.state = DOWNLOADING;
                    downloadFile();
                    if (mFileDownloadInfo.state != DOWNLOAD_ERROR && mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                        mFileButton.setText(getResources().getString(R.string.rc_cancel));
                    }
                    break;

            }
        }
    }

    private void startToDownload() {
        if ((mMessage.getContent() instanceof MediaMessageContent)) {
            resetMediaMessageLocalPath();
        } else {
            refreshDownloadState();
            return;
        }
        if (IMCenter.getInstance().getCurrentConnectionStatus() != RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED) {
            makeText(FilePreviewActivity.this, getString(R.string.rc_notice_network_unavailable), Toast.LENGTH_SHORT).show();
            return;
        }
        MediaMessageContent mediaMessage = (MediaMessageContent) (mMessage.getContent());
        if (mediaMessage != null && (mediaMessage.getMediaUrl() == null || TextUtils.isEmpty(mediaMessage.getMediaUrl().toString()))) {
            makeText(FilePreviewActivity.this, getString(R.string.rc_ac_file_url_error), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (mFileDownloadInfo.state == NOT_DOWNLOAD
                || mFileDownloadInfo.state == DOWNLOAD_ERROR
                || mFileDownloadInfo.state == DELETED
                || mFileDownloadInfo.state == DOWNLOAD_CANCEL) {
            downloadFile();
        }
    }

    protected void resetMediaMessageLocalPath() {
        FileMessage fileMessage = null;
        if (mMessage.getContent() instanceof FileMessage) {
            fileMessage = (FileMessage) mMessage.getContent();
        } else if (mMessage.getContent() instanceof ReferenceMessage) {
            ReferenceMessage referenceMessage = (ReferenceMessage) mMessage.getContent();
            fileMessage = (FileMessage) referenceMessage.getReferenceContent();
        }

        if (fileMessage != null) {
            if (fileMessage.getLocalPath() != null && !TextUtils.isEmpty(fileMessage.getLocalPath().toString())) {
                fileMessage.setLocalPath(null);
                mFileMessage.setLocalPath(null);
                IMCenter.getInstance().refreshMessage(mMessage);
            }
        }
    }

    public void openFile(String fileName, Uri fileSavePath) {
        try {
            if (!openInsidePreview(fileName, fileSavePath)) {
                Intent intent = FileTypeUtils.getOpenFileIntent(this, fileName, fileSavePath);

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } else {
                    makeText(FilePreviewActivity.this, getString(R.string.rc_ac_file_preview_can_not_open_file), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            RLog.e(TAG, "openFile", e);
            makeText(FilePreviewActivity.this, getString(R.string.rc_ac_file_preview_can_not_open_file), Toast.LENGTH_SHORT).show();
        }
    }

    protected boolean openInsidePreview(String fileName, Uri uri) {
        String fileSavePath = uri.toString();
        if (fileSavePath.endsWith(TXT_FILE)) {
            processTxtFile(fileName, uri);
            return true;
        } else if (fileSavePath.endsWith(APK_FILE)) {
            processApkFile(uri);
            return true;
        }
        return false;
    }

    /**
     * 处理三种情况
     *
     * @param fileName 文件名
     * @param uri      文件Uri
     */
    private void processTxtFile(String fileName, Uri uri) {
        Intent webIntent = new Intent(this, RongWebviewActivity.class);
        webIntent.setPackage(getPackageName());
        //如果是content
        if (FileUtils.uriStartWithContent(uri)) {
            webIntent.putExtra("url", uri);
        } else {
            //File开头
            String path = uri.toString();
            if (FileUtils.uriStartWithFile(uri)) {
                path = path.substring(7);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri txtUri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + getResources().getString(R.string.rc_authorities_fileprovider), new File(path));
                webIntent.putExtra("url", txtUri.toString());
            } else {
                webIntent.putExtra("url", "file://" + path);
            }
        }
        webIntent.putExtra("title", fileName);
        startActivity(webIntent);
    }

    /**
     * 处理三种情况
     *
     * @param uri 文件Uri
     */
    private void processApkFile(Uri uri) {
        if (FileUtils.uriStartWithContent(uri)) {
            Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(uri,
                    "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else {
            //File开头
            String path = uri.toString();
            if (FileUtils.uriStartWithFile(uri)) {
                path = path.substring(7);
            }
            File file = new File(path);
            if (!file.exists()) {
                makeText(FilePreviewActivity.this, getString(R.string.rc_file_not_exist), Toast.LENGTH_SHORT).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri downloaded_apk;
                try {
                    downloaded_apk = FileProvider.getUriForFile(this, getPackageName() + getString(R.string.rc_authorities_fileprovider), file);
                } catch (Exception e) {
                    throw new RuntimeException("Please check IMKit Manifest FileProvider config.");
                }
                Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(downloaded_apk,
                        "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } else {
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                installIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                startActivity(installIntent);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void downloadFile() {
        String[] permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!PermissionCheckUtil.checkPermissions(this, permission)) {
            PermissionCheckUtil.requestPermissions(this, permission, REQUEST_CODE_PERMISSION);
            return;
        }
        mFileDownloadInfo.state = DOWNLOADING;
        mFileButton.setText(getResources().getString(R.string.rc_cancel));
        downloadedFileLength = (long) (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0) + 0.5f);
        mFileSizeView.setText(getString(R.string.rc_ac_file_download_progress_tv) + "(" + FileTypeUtils.formatFileSize(downloadedFileLength)
                + "/" + FileTypeUtils.formatFileSize(mFileSize) + ")");
        IMCenter.getInstance().downloadMediaMessage(mMessage, null);
    }

    private void getFileDownloadInfo() {
        if (mFileMessage.getLocalPath() != null) {
            if (FileUtils.isFileExistsWithUri(this, mFileMessage.getLocalPath())) {
                mFileDownloadInfo.state = DOWNLOADED;
            } else {
                mFileDownloadInfo.state = DELETED;
            }
        } else {
            if (mProgress > 0 && mProgress < 100) {
                mFileDownloadInfo.state = DOWNLOADING;
                mFileDownloadInfo.progress = mProgress;
            } else {
                mFileDownloadInfo.state = NOT_DOWNLOAD;
            }
        }
        refreshDownloadState();
    }

    private void getFileDownloadInfoForResumeTransfer() {
        if (mFileMessage != null) {
            Uri path = mFileMessage.getLocalPath();
            if (path != null) {
                boolean exists = FileUtils.isFileExistsWithUri(FilePreviewActivity.this, path);
                if (exists) {
                    mFileDownloadInfo.state = DOWNLOADED;
                } else {
                    mFileDownloadInfo.state = DELETED;
                }
            } else if (info != null) { // localPath未生成，缓存数据存在的情况，可能为文件未下载完成，下了一部分
                if (info.isDownLoading()) {
                    mFileDownloadInfo.state = DOWNLOADING;
                } else {
                    mFileDownloadInfo.state = DOWNLOAD_PAUSE;
                }
            } else {
                mFileDownloadInfo.state = NOT_DOWNLOAD;
            }
        } else {
            mFileDownloadInfo.state = NOT_DOWNLOAD;
        }
        refreshDownloadState();
    }

    protected void refreshDownloadState() {
        switch (mFileDownloadInfo.state) {
            case NOT_DOWNLOAD:
                mFileButton.setText(getString(R.string.rc_ac_file_preview_begin_download));
                break;
            case DOWNLOADING:
                downloadedFileLength = (long) (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0) + 0.5f);
                mFileSizeView.setText(getString(R.string.rc_ac_file_download_progress_tv) + "(" + FileTypeUtils.formatFileSize(downloadedFileLength)
                        + "/" + FileTypeUtils.formatFileSize(mFileSize) + ")");
                mFileButton.setText(getString(R.string.rc_cancel));
                break;
            case DOWNLOADED:
                mFileButton.setText(getString(R.string.rc_ac_file_download_open_file_btn));
                break;
            case DOWNLOAD_SUCCESS:
//                mDownloadProgressView.setVisibility(View.GONE);
                mFileButton.setVisibility(View.VISIBLE);
                mFileButton.setText(getString(R.string.rc_ac_file_download_open_file_btn));
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileSize));
                makeText(FilePreviewActivity.this, getString(R.string.rc_ac_file_preview_downloaded) + mFileDownloadInfo.path, Toast.LENGTH_SHORT).show();
                break;
            case DOWNLOAD_ERROR:
                if (info != null) {
                    mFileDownloadInfo.progress = (int) (100L * info.currentFileLength() / info.getLength());
                }
                long downloadedFileLength = (long) (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0) + 0.5f);
                mFileSizeView.setText(getString(R.string.rc_ac_file_download_progress_pause) + "(" + FileTypeUtils.formatFileSize(downloadedFileLength)
                        + "/" + FileTypeUtils.formatFileSize(mFileSize) + ")");
                mFileButton.setText(getString(R.string.rc_ac_file_preview_download_resume));
                Toast toast = makeText(FilePreviewActivity.this, getString(R.string.rc_ac_file_preview_download_error), Toast.LENGTH_SHORT);
                if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                    toast.show();
                }
                mToasts.add(toast);
                break;
            case DOWNLOAD_CANCEL:
//                mDownloadProgressView.setVisibility(View.GONE);
//                mFileDownloadProgressBar.setProgress(0);
                mFileButton.setVisibility(View.VISIBLE);
                mFileButton.setText(getString(R.string.rc_ac_file_preview_begin_download));
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileSize));
                makeText(FilePreviewActivity.this, getString(R.string.rc_ac_file_preview_download_cancel), Toast.LENGTH_SHORT).show();
                break;
            case DELETED:
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileSize));
                mFileButton.setText(getString(R.string.rc_ac_file_preview_begin_download));
                break;
            case DOWNLOAD_PAUSE:
                downloadedFileLength = (long) (mFileMessage.getSize() * (mFileDownloadInfo.progress / 100.0) + 0.5f);
//                mFileDownloadProgressBar.setProgress(mFileDownloadInfo.progress);
                mFileSizeView.setText(getString(R.string.rc_ac_file_download_progress_pause) + "(" + FileTypeUtils.formatFileSize(downloadedFileLength)
                        + "/" + FileTypeUtils.formatFileSize(mFileSize) + ")");
                mFileButton.setText(getString(R.string.rc_ac_file_preview_download_resume));
                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getFileDownloadInfoInSubThread();
    }

    private void getFileDownloadInfoInSubThread() {
        getInfoNow = true;
        getFileInfo();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        try {
            for (Toast toast : mToasts) {
                toast.cancel();
            }
        } catch (Exception e) {
            RLog.e(TAG, "onDestroy", e);
        }
        IMCenter.getInstance().removeMessageEventListener(mEventListener);
        IMCenter.getInstance().removeOnRecallMessageListener(mRecallListener);
        super.onDestroy();
    }

    private boolean getInfoNow = false;

    public void updateDownloadStatus(DownloadEvent event) {
        if (mMessage.getMessageId() == event.getMessage().getMessageId()) {
            switch (event.getEvent()) {
                case DownloadEvent.SUCCESS:
                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                        if (event.getMessage() == null || event.getMessage().getContent() == null)
                            return;
                        if (event.getMessage().getContent() instanceof FileMessage) {
                            mFileMessage = (FileMessage) event.getMessage().getContent();
                            mFileMessage.setLocalPath(Uri.parse(mFileMessage.getLocalPath().toString()));
                            mFileDownloadInfo.path = mFileMessage.getLocalPath().toString();
                        } else {
                            ReferenceMessage referenceMessage = (ReferenceMessage) event.getMessage().getContent();
                            mFileMessage.setLocalPath(Uri.parse(referenceMessage.getLocalPath().toString()));
                            mFileDownloadInfo.path = referenceMessage.getLocalPath().toString();
                        }

                        mFileDownloadInfo.state = DOWNLOAD_SUCCESS;
                        refreshDownloadState();
                    }
                    break;
                case DownloadEvent.PROGRESS:
                    if (info == null && !getInfoNow) {
                        getFileDownloadInfoInSubThread();
                    }
                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL
                            && mFileDownloadInfo.state != DOWNLOAD_PAUSE) {
                        mFileDownloadInfo.state = DOWNLOADING;
                        mFileDownloadInfo.progress = event.getProgress();
                        refreshDownloadState();
                    }
                    break;
                case DownloadEvent.ERROR:
                    if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                        mFileDownloadInfo.state = DOWNLOAD_ERROR;
                        refreshDownloadState();
                    }
                    break;
                case DownloadEvent.CANCEL:
                    mFileDownloadInfo.state = DOWNLOAD_CANCEL;
                    refreshDownloadState();
                    break;
            }
        }
    }

    public class FileDownloadInfo {
        public int state;
        public int progress;
        public String path;
    }

    public Message getMessage() {
        return mMessage;
    }


    private void getFileInfo() {
        RLog.d("getDownloadInfo", "getFileInfo start");
        RongCoreClient.getInstance().getDownloadInfo(String.valueOf(mMessage.getMessageId()), callback);
    }


}
