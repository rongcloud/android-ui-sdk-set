package io.rong.imkit.activity;

import static android.widget.Toast.makeText;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import io.rong.common.FileUtils;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.utils.FileTypeUtils;
import io.rong.imkit.utils.RongUtils;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.filetransfer.FtUtilities;
import io.rong.imlib.model.DownloadInfo;
import java.io.File;

public class WebFilePreviewActivity extends RongBaseActivity implements View.OnClickListener {
    public static final int NOT_DOWNLOAD = 0;
    public static final int DOWNLOADED = 1;
    public static final int DOWNLOADING = 2;
    public static final int DELETED = 3;
    public static final int DOWNLOAD_ERROR = 4;
    public static final int DOWNLOAD_CANCEL = 5;
    public static final int DOWNLOAD_SUCCESS = 6;
    public static final int DOWNLOAD_PAUSE = 7;
    public static final int REQUEST_CODE_PERMISSION = 104;
    private static final String TAG = "WebFilePreviewActivity";
    private static final String PATH = "webfile";
    private static final String TXT_FILE = ".txt";
    private static final String APK_FILE = ".apk";
    private static final String FILE = "file://";
    //    private ProgressBar mFileDownloadProgressBar;
    //    private LinearLayout mDownloadProgressView;
    //    protected TextView mDownloadProgressTextView;
    protected View mCancel;
    protected FileDownloadInfo mFileDownloadInfo;
    private ImageView mFileTypeImage;
    private TextView mFileNameView;
    private TextView mFileSizeView;
    private TextView mFileDownloadOpenView;
    private File mAttachFile;
    private FrameLayout mContentContainer;
    private SupportResumeStatus supportResumeTransfer = SupportResumeStatus.NOT_SET;
    private DownloadInfo mDownloadInfo;
    private String pausedPath;
    private long downloadedFileLength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.rc_ac_file_download);
        initView();
        initData();
    }

    @Override
    public void setContentView(int resId) {
        mContentContainer.removeAllViews();
        mContentContainer.addView(LayoutInflater.from(this).inflate(resId, null));
    }

    private void initView() {
        mContentContainer = findViewById(R.id.rc_ac_ll_content_container);
        View view = LayoutInflater.from(this).inflate(R.layout.rc_ac_file_preview_content, null);
        mContentContainer.addView(view);

        mFileTypeImage = findViewById(R.id.rc_ac_iv_file_type_image);
        mFileNameView = findViewById(R.id.rc_ac_tv_file_name);
        mFileSizeView = findViewById(R.id.rc_ac_tv_file_size);
        mFileDownloadOpenView = findViewById(R.id.rc_ac_btn_download_button);
        //        mDownloadProgressView = findViewById(R.id.rc_ac_ll_progress_view);
        //        mDownloadProgressTextView = findViewById(R.id.rc_ac_tv_download_progress);
        //        mFileDownloadProgressBar = findViewById(R.id.rc_ac_pb_download_progress);

        mTitleBar.setTitle(R.string.rc_ac_file_download_preview);
        mTitleBar.setRightVisible(false);
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        mFileDownloadInfo = new FileDownloadInfo();
        mFileDownloadInfo.url = intent.getStringExtra("fileUrl");
        mFileDownloadInfo.fileName = intent.getStringExtra("fileName");
        try {
            mFileDownloadInfo.size = Long.parseLong(intent.getStringExtra("fileSize"));
        } catch (NumberFormatException e) {
            RLog.e(TAG, "NumberFormatException, default value is 0L");
            mFileDownloadInfo.size = 0L;
        }

        mFileDownloadInfo.uid = RongUtils.md5(mFileDownloadInfo.fileName + mFileDownloadInfo.size);
        mFileDownloadInfo.path = FileUtils.getCachePath(this, PATH);

        mFileTypeImage.setImageResource(
                FileTypeUtils.fileTypeImageId(this, mFileDownloadInfo.fileName));
        mFileNameView.setText(mFileDownloadInfo.fileName);
        mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
        mFileDownloadOpenView.setOnClickListener(this);
        //        mCancel.setOnClickListener(this);
        String savedPath =
                FtUtilities.getFileName(mFileDownloadInfo.path, mFileDownloadInfo.fileName, false);
        mAttachFile = new File(savedPath);
        if (mAttachFile.exists()) {
            mFileDownloadOpenView.setText(getOpenFileShowText());
        }

        IMCenter.getInstance()
                .supportResumeBrokenTransfer(
                        mFileDownloadInfo.url,
                        new RongIMClient.ResultCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                supportResumeTransfer =
                                        aBoolean
                                                ? SupportResumeStatus.SUPPORT
                                                : SupportResumeStatus.NOT_SUPPORT;
                                getFileDownloadInfo();
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                                mFileDownloadInfo.state = DOWNLOAD_ERROR;
                                refreshDownloadState();
                            }
                        });
    }

    private String getOpenFileShowText() {
        return getString(
                mFileDownloadInfo != null && isOpenInsideApp(mFileDownloadInfo.fileName)
                        ? R.string.rc_ac_file_download_open_file_direct_btn
                        : R.string.rc_ac_file_download_open_file_btn);
    }

    private void getFileDownloadInfo() {
        pausedPath = FileUtils.getTempFilePath(this, mFileDownloadInfo.uid);

        getFileInfo(
                new IRongCoreCallback.ResultCallback<DownloadInfo>() {
                    @Override
                    public void onSuccess(DownloadInfo downloadInfo) {
                        mDownloadInfo = downloadInfo;
                        if ((!mAttachFile.exists())) {
                            if (mDownloadInfo != null) {
                                FileUtils.removeFile(pausedPath);
                            }
                            mFileDownloadInfo.state = NOT_DOWNLOAD;
                        } else {
                            if (mDownloadInfo == null) { // 已经下载完毕
                                mFileDownloadInfo.state = DOWNLOADED;
                            } else { // 正在下载/暂停下载
                                if (mDownloadInfo.isDownLoading()) {
                                    mFileDownloadInfo.state = DOWNLOADING;
                                } else {
                                    mFileDownloadInfo.state = DOWNLOAD_PAUSE;
                                }
                            }
                        }
                        refreshDownloadState();
                    }

                    @Override
                    public void onError(IRongCoreEnum.CoreErrorCode e) {
                        // do nothing
                    }
                });
    }

    protected void refreshDownloadState() {
        switch (mFileDownloadInfo.state) {
            case NOT_DOWNLOAD:
                mFileDownloadOpenView.setText(
                        getString(R.string.rc_ac_file_preview_begin_download));
                break;
            case DOWNLOADING:
                //                mDownloadProgressView.setVisibility(View.VISIBLE);
                //                mFileDownloadProgressBar.setProgress(mFileDownloadInfo.progress);
                downloadedFileLength =
                        (long)
                                (mFileDownloadInfo.size * (mFileDownloadInfo.progress / 100.0)
                                        + 0.5f);
                mFileSizeView.setText(
                        getString(R.string.rc_ac_file_download_progress_tv)
                                + "("
                                + FileTypeUtils.formatFileSize(downloadedFileLength)
                                + "/"
                                + FileTypeUtils.formatFileSize(mFileDownloadInfo.size)
                                + ")");
                if (supportResumeTransfer == SupportResumeStatus.SUPPORT) {
                    //                    mDownloadProgressTextView.setVisibility(View.GONE);
                    mFileDownloadOpenView.setText(getString(R.string.rc_cancel));
                } else {
                    mFileDownloadOpenView.setVisibility(View.GONE);
                }
                break;
            case DOWNLOADED:
                mFileDownloadOpenView.setText(getOpenFileShowText());
                break;
            case DOWNLOAD_SUCCESS:
                //                mDownloadProgressView.setVisibility(View.GONE);
                mFileDownloadOpenView.setVisibility(View.VISIBLE);
                mFileDownloadOpenView.setText(getOpenFileShowText());
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
                makeText(
                                WebFilePreviewActivity.this,
                                getString(R.string.rc_ac_file_preview_downloaded)
                                        + mFileDownloadInfo.path,
                                Toast.LENGTH_SHORT)
                        .show();
                break;
            case DOWNLOAD_ERROR:
                if (supportResumeTransfer == SupportResumeStatus.SUPPORT) {
                    //                    mDownloadProgressView.setVisibility(View.VISIBLE);
                    //
                    // mFileDownloadProgressBar.setProgress(mFileDownloadInfo.progress);
                    long downloadedFileLength =
                            (long)
                                    (mFileDownloadInfo.size * (mFileDownloadInfo.progress / 100.0)
                                            + 0.5f);
                    mFileSizeView.setText(
                            getString(R.string.rc_ac_file_download_progress_pause)
                                    + "("
                                    + FileTypeUtils.formatFileSize(downloadedFileLength)
                                    + "/"
                                    + FileTypeUtils.formatFileSize(mFileDownloadInfo.size)
                                    + ")");
                    mFileDownloadOpenView.setText(
                            getString(R.string.rc_ac_file_preview_download_resume));
                } else {
                    //                    mDownloadProgressView.setVisibility(View.GONE);
                    mFileDownloadOpenView.setVisibility(View.VISIBLE);
                    mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
                    mFileDownloadOpenView.setText(
                            getString(R.string.rc_ac_file_preview_begin_download));
                }
                makeText(
                                WebFilePreviewActivity.this,
                                getString(R.string.rc_ac_file_preview_download_error),
                                Toast.LENGTH_SHORT)
                        .show();
                break;
            case DOWNLOAD_CANCEL:
                //                mDownloadProgressView.setVisibility(View.GONE);
                //                mFileDownloadProgressBar.setProgress(0);
                mFileDownloadOpenView.setVisibility(View.VISIBLE);
                mFileDownloadOpenView.setText(
                        getString(R.string.rc_ac_file_preview_begin_download));
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
                makeText(
                                WebFilePreviewActivity.this,
                                getString(R.string.rc_ac_file_preview_download_cancel),
                                Toast.LENGTH_SHORT)
                        .show();
                break;
            case DELETED:
                mFileSizeView.setText(FileTypeUtils.formatFileSize(mFileDownloadInfo.size));
                mFileDownloadOpenView.setText(
                        getString(R.string.rc_ac_file_preview_begin_download));
                break;
            case DOWNLOAD_PAUSE:
                //                mDownloadProgressView.setVisibility(View.VISIBLE);
                if (mDownloadInfo != null) {
                    mFileDownloadInfo.progress =
                            (int)
                                    (100L
                                            * mDownloadInfo.currentFileLength()
                                            / mDownloadInfo.getLength());
                    downloadedFileLength = mDownloadInfo.currentFileLength();
                } else {
                    downloadedFileLength =
                            (long)
                                    (mFileDownloadInfo.size * (mFileDownloadInfo.progress / 100.0)
                                            + 0.5f);
                }
                //                mFileDownloadProgressBar.setProgress(mFileDownloadInfo.progress);
                mFileSizeView.setText(
                        getString(R.string.rc_ac_file_download_progress_pause)
                                + "("
                                + FileTypeUtils.formatFileSize(downloadedFileLength)
                                + "/"
                                + FileTypeUtils.formatFileSize(mFileDownloadInfo.size)
                                + ")");
                mFileDownloadOpenView.setText(
                        getString(R.string.rc_ac_file_preview_download_resume));
                break;
            default:
                break;
        }
    }

    private void getFileInfo(IRongCoreCallback.ResultCallback<DownloadInfo> callback) {
        RongCoreClient.getInstance().getDownloadInfo(mFileDownloadInfo.uid, callback);
    }

    @Override
    public void onClick(View v) {
        if (v == mFileDownloadOpenView) {
            switch (mFileDownloadInfo.state) {
                case NOT_DOWNLOAD:
                case DOWNLOAD_CANCEL:
                case DOWNLOAD_ERROR:
                case DELETED:
                    startToDownload();
                    break;
                case DOWNLOAD_SUCCESS:
                case DOWNLOADED:
                    if (mAttachFile != null) {
                        openFile(mFileDownloadInfo.fileName, mAttachFile.getAbsolutePath());
                    }
                    break;
                case DOWNLOADING:
                    mFileDownloadInfo.state = DOWNLOAD_PAUSE;
                    RongIMClient.getInstance().pauseDownloadMediaFile(mFileDownloadInfo.uid, null);
                    mFileDownloadOpenView.setText(
                            getResources().getString(R.string.rc_ac_file_preview_download_resume));
                    if (mDownloadInfo != null) {
                        downloadedFileLength = mDownloadInfo.currentFileLength();
                    } else {
                        downloadedFileLength =
                                (long)
                                        (mFileDownloadInfo.size
                                                        * (mFileDownloadInfo.progress / 100.0)
                                                + 0.5f);
                    }
                    mFileSizeView.setText(
                            getString(R.string.rc_ac_file_download_progress_pause)
                                    + "("
                                    + FileTypeUtils.formatFileSize(downloadedFileLength)
                                    + "/"
                                    + FileTypeUtils.formatFileSize(mFileDownloadInfo.size)
                                    + ")");

                    break;
                case DOWNLOAD_PAUSE:
                    if (IMCenter.getInstance().getCurrentConnectionStatus()
                            == RongIMClient.ConnectionStatusListener.ConnectionStatus
                                    .NETWORK_UNAVAILABLE) {
                        makeText(
                                        WebFilePreviewActivity.this,
                                        getString(R.string.rc_notice_network_unavailable),
                                        Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    if (supportResumeTransfer == SupportResumeStatus.SUPPORT) {
                        mFileDownloadInfo.state = DOWNLOADING;
                        downloadFile();
                        if (mFileDownloadInfo.state != DOWNLOAD_ERROR
                                && mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                            mFileDownloadOpenView.setText(
                                    getResources().getString(R.string.rc_cancel));
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void startToDownload() {
        if (IMCenter.getInstance().getCurrentConnectionStatus()
                == RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE) {
            makeText(
                            WebFilePreviewActivity.this,
                            getString(R.string.rc_notice_network_unavailable),
                            Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (supportResumeTransfer == SupportResumeStatus.NOT_SET) {
            IMCenter.getInstance()
                    .supportResumeBrokenTransfer(
                            mFileDownloadInfo.url,
                            new RongIMClient.ResultCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean aBoolean) {
                                    if (mFileDownloadInfo.state == NOT_DOWNLOAD
                                            || mFileDownloadInfo.state == DELETED
                                            || mFileDownloadInfo.state == DOWNLOAD_ERROR
                                            || mFileDownloadInfo.state == DOWNLOAD_CANCEL) {
                                        supportResumeTransfer =
                                                SupportResumeStatus.valueOf(aBoolean ? 1 : 0);
                                        downloadFile();
                                    }
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {
                                    mFileDownloadInfo.state = DOWNLOAD_ERROR;
                                    refreshDownloadState();
                                }
                            });
        } else {
            if (mFileDownloadInfo.state == NOT_DOWNLOAD
                    || mFileDownloadInfo.state == DOWNLOAD_ERROR
                    || mFileDownloadInfo.state == DELETED
                    || mFileDownloadInfo.state == DOWNLOAD_CANCEL) {
                downloadFile();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void downloadFile() {
        // KNOTE: 2021/8/18下载文件使用应用私有目录  不需要存储权限
        mFileDownloadInfo.state = DOWNLOADING;
        if (supportResumeTransfer == SupportResumeStatus.SUPPORT) {
            mFileDownloadOpenView.setText(getResources().getString(R.string.rc_cancel));
            //            mCancel.setVisibility(View.GONE);
            //            mDownloadProgressView.setVisibility(View.VISIBLE);
            //            mDownloadProgressTextView.setVisibility(View.GONE);
            if (mDownloadInfo != null) {
                downloadedFileLength = mDownloadInfo.currentFileLength();
            } else {
                downloadedFileLength =
                        (long)
                                (mFileDownloadInfo.size * (mFileDownloadInfo.progress / 100.0)
                                        + 0.5f);
            }
            mFileSizeView.setText(
                    getString(R.string.rc_ac_file_download_progress_tv)
                            + "("
                            + FileTypeUtils.formatFileSize(downloadedFileLength)
                            + "/"
                            + FileTypeUtils.formatFileSize(mFileDownloadInfo.size)
                            + ")");
        } else {
            mFileDownloadOpenView.setVisibility(View.GONE);
            //            mDownloadProgressView.setVisibility(View.VISIBLE);
            //
            // mDownloadProgressTextView.setText(getString(R.string.rc_ac_file_download_progress_tv,
            //                    FileTypeUtils.formatFileSize(0),
            // FileTypeUtils.formatFileSize(mFileDownloadInfo.size)));
        }

        RongIMClient.getInstance()
                .downloadMediaFile(
                        mFileDownloadInfo.uid,
                        mFileDownloadInfo.url,
                        mFileDownloadInfo.fileName,
                        mFileDownloadInfo.path,
                        new IRongCallback.IDownloadMediaFileCallback() {

                            @Override
                            public void onFileNameChanged(String newFileName) {
                                mFileDownloadInfo.fileName = newFileName;
                            }

                            @Override
                            public void onSuccess() {
                                mFileDownloadInfo.state = DOWNLOAD_SUCCESS;
                                try {
                                    mAttachFile =
                                            new File(
                                                    mFileDownloadInfo.path,
                                                    mFileDownloadInfo.fileName);
                                } catch (Exception e) {
                                    RLog.e(TAG, "downloadFile" + e);
                                }
                                refreshDownloadState();
                            }

                            @Override
                            public void onProgress(int progress) {
                                if (mFileDownloadInfo.state != DOWNLOAD_CANCEL
                                        && mFileDownloadInfo.state != DOWNLOAD_PAUSE) {
                                    mFileDownloadInfo.progress = progress;
                                    mFileDownloadInfo.state = DOWNLOADING;
                                    refreshDownloadState();
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode code) {
                                if (mFileDownloadInfo.state != DOWNLOAD_CANCEL) {
                                    mFileDownloadInfo.state = DOWNLOAD_ERROR;
                                    refreshDownloadState();
                                }
                            }

                            @Override
                            public void onCanceled() {
                                mFileDownloadInfo.state = DOWNLOAD_CANCEL;
                                refreshDownloadState();
                            }
                        });
    }

    public void openFile(String fileName, String fileSavePath) {
        if (!openInsidePreview(fileName, fileSavePath)) {
            Intent intent = FileTypeUtils.getOpenFileIntent(this, fileName, fileSavePath);
            try {
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } else {
                    makeText(
                                    WebFilePreviewActivity.this,
                                    getString(R.string.rc_ac_file_preview_can_not_open_file),
                                    Toast.LENGTH_SHORT)
                            .show();
                }
            } catch (Exception e) {
                makeText(
                                WebFilePreviewActivity.this,
                                getString(R.string.rc_ac_file_preview_can_not_open_file),
                                Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    protected boolean openInsidePreview(String fileName, String fileSavePath) {
        if (isOpenInsideApp(fileSavePath)) {
            Intent webIntent = new Intent(this, RongWebviewActivity.class);
            webIntent.setPackage(getPackageName());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri uri =
                        FileProvider.getUriForFile(
                                this,
                                getPackageName() + getString(R.string.rc_authorities_fileprovider),
                                new File(fileSavePath));
                webIntent.putExtra("url", uri.toString());
            } else {
                webIntent.putExtra("url", FILE + fileSavePath);
            }
            webIntent.putExtra("title", fileName);
            startActivity(webIntent);
            return true;
        }
        return false;
    }

    private boolean isOpenInsideApp(String fileSavePath) {
        return fileSavePath != null && fileSavePath.endsWith(TXT_FILE);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getFileDownloadInfo();
    }

    private enum SupportResumeStatus {
        NOT_SET(-1),
        NOT_SUPPORT(0),
        SUPPORT(1);

        int value;

        SupportResumeStatus(int value) {
            this.value = value;
        }

        public static SupportResumeStatus valueOf(int code) {
            for (SupportResumeStatus c : SupportResumeStatus.values()) {
                if (code == c.getValue()) {
                    return c;
                }
            }
            SupportResumeStatus c = NOT_SET;
            c.value = code;
            return c;
        }

        public int getValue() {
            return this.value;
        }
    }

    private class FileDownloadInfo {
        int state;
        int progress;
        String path;
        String fileName;
        String url;
        String uid;
        long size;
    }
}
