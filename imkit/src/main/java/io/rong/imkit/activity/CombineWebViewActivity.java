package io.rong.imkit.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import io.rong.common.FileUtils;
import io.rong.common.LibStorageUtils;
import io.rong.common.RongWebView;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.KitMediaInterceptor;
import io.rong.imkit.R;
import io.rong.imkit.config.FeatureConfig;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.feature.forward.CombineMessage;
import io.rong.imkit.feature.forward.CombineMessageUtils;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.DeviceUtils;
import io.rong.imlib.common.NetUtils;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.GIFMessage;
import io.rong.message.ImageMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.SightMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import org.json.JSONObject;

public class CombineWebViewActivity extends RongBaseActivity {
    public static final String TYPE_LOCAL = "local";
    public static final String TYPE_MEDIA = "media";
    public static final int PROGRESS_100 = 100;
    private static final String TAG = CombineWebViewActivity.class.getSimpleName();
    // WebView加载视频时的默认背景宽高
    private static final int VIDEO_WIDTH = 300;
    private static final int VIDEO_HEIGHT = 600;
    private static final String COMBINE_FILE_PATH = "combine";
    private static final String FILE = "file://";
    private static final int BEGIN_INDEX = 7;

    protected RongWebView mWebView;
    private ProgressBar mProgress;
    private ImageView mImageView;
    private TextView mTextView;

    private String mType;
    private int mMessageId;
    private String mPrevUrl;
    private boolean mWebViewError = false;
    private boolean mNeedInjectDarkCSS = false;
    private RongIMClient.OnRecallMessageListener mRecallMessageListener =
            new RongIMClient.OnRecallMessageListener() {
                @Override
                public boolean onMessageRecalled(
                        Message message, RecallNotificationMessage recallNotificationMessage) {
                    if (mMessageId != -1 && mMessageId == message.getMessageId()) {
                        new AlertDialog.Builder(
                                        CombineWebViewActivity.this,
                                        AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                                .setMessage(getString(R.string.rc_recall_success))
                                .setPositiveButton(
                                        getString(R.string.rc_dialog_ok),
                                        new DialogInterface.OnClickListener() {

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

    private static boolean isImageFile(byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        return options.outWidth != -1;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_combine_webview);
        initStatusBar(R.color.app_color_white);
        initUI();
        initData();
        IMCenter.getInstance().addOnRecallMessageListener(mRecallMessageListener);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void initUI() {
        mWebView = findViewById(R.id.rc_webview);
        mProgress = findViewById(R.id.rc_web_progress);
        mImageView = findViewById(R.id.rc_web_download_failed);
        mTextView = findViewById(R.id.rc_web_download_text);

        mTitleBar.setRightVisible(false);
        mWebView.setVerticalScrollbarOverlay(true);
        mWebView.setWebViewClient(new CombineWebViewClient());
        mWebView.setWebChromeClient(new CombineWebChromeClient());

        if (RongConfigCenter.featureConfig().rc_set_java_script_enabled) {
            mWebView.addJavascriptInterface(new JsInterface(), "interface");
            mWebView.getSettings().setJavaScriptEnabled(true);
        } else {
            RLog.e(TAG, "js interface is disabled! This may cause some problems of this page!");
        }

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDefaultTextEncodingName("utf-8");
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setAllowFileAccess(true);
        // 允许小视频自动播放
        mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mWebView.getSettings().setSavePassword(false);

        // 允许混合内容 解决部分手机https请求里面加载不出http的图片
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 设置 WebView 暗黑模式
        setupWebViewDarkMode();
    }

    /** 设置 WebView 暗黑模式 */
    private void setupWebViewDarkMode() {
        // 非传统主题 且 系统处于暗黑模式时，需要注入暗色 CSS
        if (!IMKitThemeManager.isTraditionTheme() && IMKitThemeManager.isSystemInDarkMode(this)) {
            mNeedInjectDarkCSS = true;
            RLog.d(TAG, "setupWebViewDarkMode: will inject dark CSS");
        }
    }

    /** 注入自定义 CSS 样式，解决 Icon 背景及暗黑模式适配 */
    private void injectCustomCSS() {
        if (mWebView == null) {
            return;
        }
        String fileIconStyle = CombineMessageUtils.getInstance().getFileIconStyle();
        String darkStyle = CombineMessageUtils.getInstance().getDarkStyle();

        StringBuilder cssBuilder = new StringBuilder();
        // 1. 基础修复：无论是否暗黑模式，都将文件消息区域内的 Icon 背景设为透明
        if (!TextUtils.isEmpty(fileIconStyle)) {
            cssBuilder.append(fileIconStyle);
        }

        // 2. 暗黑模式适配：如果系统处于暗黑模式，追加暗色样式
        if (mNeedInjectDarkCSS && !TextUtils.isEmpty(darkStyle)) {
            cssBuilder.append(darkStyle);
        }

        if (cssBuilder.length() > 0) {
            String js =
                    "var style = document.createElement('style');"
                            + "style.type = 'text/css';"
                            + "style.innerHTML = '"
                            + cssBuilder
                            + "';"
                            + "document.head.appendChild(style);";

            mWebView.evaluateJavascript(js, null);
        }
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) {
            return;
        }

        mMessageId = intent.getIntExtra("messageId", -1);
        String uri = intent.getStringExtra("uri");
        mType = intent.getStringExtra("type");
        String title = intent.getStringExtra("title");

        mPrevUrl = uri;
        mWebViewError = false;
        if (mTitleBar != null && !TextUtils.isEmpty(title)) {
            mTitleBar.setTitle(title);
        }
        firstLoadUrl(uri);
        //        onCreateActionbar(new ActionBar());
    }

    /**
     * 第一次加载url
     *
     * @param url 需要加载的 URL
     * @discussion 由于私有云鉴权的问题，第一次加载的时候需要先下载好，然后加载本地合并转发文件，和文件消息的逻辑一致
     */
    private void firstLoadUrl(String url) {
        // 不是远端路径，直接加载
        if (!isRemoteUri(url)) {
            replacePortraitUrl(url);
            return;
        }
        showLoading();
        // 下载成功后加载本地路径
        if (mMessageId > 0) {
            downloadFileByMessageId(
                    mMessageId,
                    new IRongCoreCallback.ResultCallback<String>() {
                        @Override
                        public void onSuccess(String s) {
                            RLog.e(TAG, "downloadFileByMessageId onSuccess:" + s);
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            showLoadSuccess();
                                            replacePortraitUrl(s);
                                        }
                                    });
                        }

                        @Override
                        public void onError(IRongCoreEnum.CoreErrorCode e) {
                            RLog.e(TAG, "downloadFileByMessageId error:" + e);
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            showLoadError();
                                        }
                                    });
                        }
                    });
        } else {
            downloadFileByUri(
                    url,
                    new IRongCoreCallback.ResultCallback<String>() {
                        @Override
                        public void onSuccess(String s) {
                            RLog.e(TAG, "downloadFileByUri onSuccess:" + s);
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            showLoadSuccess();
                                            replacePortraitUrl(s);
                                        }
                                    });
                        }

                        @Override
                        public void onError(IRongCoreEnum.CoreErrorCode e) {
                            RLog.e(TAG, "downloadFileByUri error:" + e);
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            showLoadError();
                                        }
                                    });
                        }
                    });
        }
    }

    private void replacePortraitUrl(String url) {
        if (!TextUtils.isEmpty(url) && (!url.startsWith("file://"))) {
            url = "file://" + url;
        }
        mWebView.loadUrl(url);
    }

    private boolean isRemoteUri(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return false;
        }
        return uri.toLowerCase().startsWith("http") || uri.toLowerCase().startsWith("ftp");
    }

    private void openSight(JSONObject jsonObj) {
        SightMessage sightMessage = getSightMessage(jsonObj);
        Message message = new Message();
        message.setContent(sightMessage);

        // 此处的 会话类型和会话 id 都是不正确的，仅当做占位使用
        message.setConversationType(Conversation.ConversationType.PRIVATE);
        message.setTargetId(RongIMClient.getInstance().getCurrentUserId());

        if (mMessageId <= 0) {
            // 非法的消息 id，那么直接跳转
            routeToSightPlayerActivity(message, sightMessage);
            return;
        }
        RongCoreClient.getInstance()
                .getMessage(
                        mMessageId,
                        new IRongCoreCallback.ResultCallback<Message>() {
                            @Override
                            public void onSuccess(Message msg) {
                                // 拿到正确消息的时候再修正会话类型和会话 id
                                message.setConversationType(msg.getConversationType());
                                message.setTargetId(msg.getTargetId());
                                routeToSightPlayerActivity(message, sightMessage);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                routeToSightPlayerActivity(message, sightMessage);
                            }
                        });
    }

    private void openImage(JSONObject jsonObj) {

        String mediaUrl = jsonObj.optString("fileUrl");
        String base64 = jsonObj.optString("imgUrl");

        int dotIndex = base64.indexOf(",");
        base64 = base64.substring(dotIndex + 1);

        EncodeFile encodeFile = new EncodeFile(mediaUrl, base64).invoke();
        String thumb = encodeFile.getThumb();
        String name = encodeFile.getName();

        ImageMessage imageMessage = ImageMessage.obtain();
        imageMessage.setThumUri(Uri.parse(FILE + thumb + name));
        imageMessage.setRemoteUri(Uri.parse(mediaUrl));

        Message message = new Message();
        message.setContent(imageMessage);
        message.setConversationType(Conversation.ConversationType.PRIVATE);
        RouteUtils.routeToCombinePicturePagerActivity(this, message);
    }

    private void openGif(JSONObject jsonObj) {
        String mediaUrl = jsonObj.optString("fileUrl");
        GIFMessage gifMessage = GIFMessage.obtain(null);
        gifMessage.setRemoteUri(Uri.parse(mediaUrl));

        Message message = new Message();
        message.setConversationType(Conversation.ConversationType.PRIVATE);
        message.setTargetId(RongIMClient.getInstance().getCurrentUserId());
        message.setContent(gifMessage);

        Context context = getApplicationContext();
        Intent intent = new Intent(context, GIFPreviewActivity.class);
        intent.setPackage(context.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("message", message);
        context.startActivity(intent);
    }

    // 根据链接地址，获取合并转发消息下载路径
    public String getCombineFilePath(String uri) {
        return FileUtils.getCachePath(IMCenter.getInstance().getContext())
                + File.separator
                + COMBINE_FILE_PATH
                + File.separator;
    }

    private void openFile(JSONObject jsonObj) {
        RLog.e("openFile", jsonObj.toString());
        //        Intent intent = new Intent(RongKitIntent.RONG_INTENT_ACTION_OPENWEBFILE);
        //        intent.setPackage(getPackageName());
        //        intent.putExtra("fileUrl", jsonObj.optString("fileUrl"));
        //        intent.putExtra("fileName", jsonObj.optString("fileName"));
        //        intent.putExtra("fileSize", jsonObj.optString("fileSize"));
        //        startActivity(intent);
        RouteUtils.routeToWebFilePreviewActivity(
                this,
                jsonObj.optString("fileUrl"),
                jsonObj.optString("fileName"),
                jsonObj.optString("fileSize"));
    }

    private void openMap(JSONObject jsonObj) {
        double lat = Double.parseDouble(jsonObj.optString("latitude"));
        double lng = Double.parseDouble(jsonObj.optString("longitude"));
        String poi = jsonObj.optString("locationName");
        LocationMessage content = LocationMessage.obtain(lat, lng, poi, null);
        try {
            Intent intent = new Intent(this, Class.forName("io.rong.location.AMapPreviewActivity"));
            intent.putExtra("location", content);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            RLog.e(TAG, "openMap" + e.getMessage());
        }
    }

    private void openCombine(JSONObject jsonObj) {
        String type = CombineWebViewActivity.TYPE_MEDIA;
        String uri = jsonObj.optString("fileUrl");
        String filePath = CombineMessageUtils.getInstance().getCombineFilePath(uri);
        if (new File(filePath).exists()) {
            uri = Uri.parse(FILE + filePath).toString();
            type = CombineWebViewActivity.TYPE_LOCAL;
        }

        RouteUtils.routeToCombineWebViewActivity(this, -1, uri, type, jsonObj.optString("title"));
    }

    private void openLink(JSONObject jsonObj) {
        String link = jsonObj.optString("link");
        RouteUtils.routeToWebActivity(this, link);
    }

    private void openPhone(JSONObject jsonObj) {
        String phoneNumber = jsonObj.optString("phoneNum");
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.destroy();
        }
        IMCenter.getInstance().removeOnRecallMessageListener(mRecallMessageListener);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private SightMessage getSightMessage(JSONObject jsonObj) {
        String mediaUrl = jsonObj.optString("fileUrl");
        int duration = jsonObj.optInt("duration");
        String base64 = jsonObj.optString("imageBase64");
        int dotIndex = base64.indexOf(",");
        base64 = base64.substring(dotIndex + 1);

        EncodeFile encodeFile = new EncodeFile(mediaUrl, base64).invoke();
        String sightThumb = encodeFile.getThumb();
        String sightName = encodeFile.getName();

        Message message = new Message();

        SightMessage sightMessage = new SightMessage();
        sightMessage.setThumbUri(Uri.parse(FILE + sightThumb + sightName));
        sightMessage.setMediaUrl(Uri.parse(mediaUrl));
        sightMessage.setDuration(duration);
        if (new IsSightFileExists(sightMessage, message.getMessageId()).invoke()) {
            String sightPath =
                    LibStorageUtils.getMediaDownloadDir(
                            getApplicationContext(), LibStorageUtils.VIDEO);
            String name =
                    message.getMessageId() + "_" + DeviceUtils.ShortMD5(Base64.NO_WRAP, mediaUrl);
            if (sightPath.startsWith(FILE)) {
                sightPath = sightPath.substring(7);
            }
            sightMessage.setLocalPath(Uri.parse(sightPath + File.separator + name));
        }
        return sightMessage;
    }

    private void routeToSightPlayerActivity(Message message, SightMessage sightMessage) {
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        ComponentName cn =
                                new ComponentName(
                                        CombineWebViewActivity.this,
                                        "io.rong.sight.player.SightPlayerActivity");
                        Intent intent = new Intent();
                        intent.setComponent(cn);
                        intent.putExtra("Message", message);
                        intent.putExtra("SightMessage", sightMessage);
                        intent.putExtra("displayCurrentVideoOnly", true);
                        intent.putExtra("fromList", false);
                        startActivity(intent);
                    }
                });
    }

    private static class DownloadTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            OutputStream out = null;
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = NetUtils.createURLConnection(params[0]);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);

                int code = urlConnection.getResponseCode();
                if (code < HttpURLConnection.HTTP_OK
                        || code >= HttpURLConnection.HTTP_MULT_CHOICE) {
                    RLog.e(TAG, "DownloadTask failed! code:" + code);
                    return null;
                }

                InputStream in = urlConnection.getInputStream();
                File file = new File(params[1]);
                if (!file.exists()) {
                    if (file.getParent() == null) {
                        RLog.e(TAG, "DownloadTask failed! file.getParent is null.");
                        return null;
                    }
                    File dir = new File(file.getParent());
                    boolean successMkdir = dir.mkdirs();
                    boolean isCreateNewFile = file.createNewFile();
                    RLog.d(
                            TAG,
                            "DownloadTask successMkdir:"
                                    + successMkdir
                                    + ",isCreateNewFile:"
                                    + isCreateNewFile);
                }
                out = new FileOutputStream(file);
                byte[] buffer = new byte[10 * 1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                in.close();
            } catch (IOException e) {
                RLog.e(TAG, "DownloadTask" + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        RLog.e(TAG, "DownloadTask" + e.getMessage());
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            // default implementation ignored
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // default implementation ignored
        }
    }

    private void downloadFileByMessageId(
            int messageId, IRongCoreCallback.ResultCallback<String> callback) {
        if (messageId <= 0) {
            if (callback != null) {
                callback.onError(IRongCoreEnum.CoreErrorCode.RC_INVALID_PARAMETER_MESSAGE_ID);
            }
            return;
        }
        RongCoreClient.getInstance()
                .getMessage(
                        messageId,
                        new IRongCoreCallback.ResultCallback<Message>() {
                            @Override
                            public void onSuccess(Message message) {

                                IMCenter.getInstance()
                                        .downloadMediaMessage(
                                                message,
                                                new IRongCallback.IDownloadMediaMessageCallback() {
                                                    @Override
                                                    public void onSuccess(Message message) {
                                                        if (message == null) {
                                                            if (callback != null) {
                                                                callback.onError(
                                                                        IRongCoreEnum.CoreErrorCode
                                                                                .RC_INVALID_PARAMETER_MESSAGE);
                                                            }
                                                            return;
                                                        }
                                                        if (!(message.getContent()
                                                                instanceof CombineMessage)) {
                                                            if (callback != null) {
                                                                callback.onError(
                                                                        IRongCoreEnum.CoreErrorCode
                                                                                .RC_INVALID_PARAMETER_MESSAGE_CONTENT);
                                                            }
                                                            return;
                                                        }
                                                        CombineMessage combineMessage =
                                                                (CombineMessage)
                                                                        message.getContent();
                                                        String filePath =
                                                                CombineMessageUtils.getInstance()
                                                                        .getCombineFilePath(
                                                                                combineMessage
                                                                                        .getMediaUrl()
                                                                                        .toString());

                                                        saveToLocalPath(
                                                                combineMessage
                                                                        .getLocalPath()
                                                                        .toString(),
                                                                filePath);

                                                        if (callback != null) {
                                                            callback.onSuccess(filePath);
                                                        }
                                                    }

                                                    @Override
                                                    public void onProgress(
                                                            Message message, int progress) {
                                                        // do nothing
                                                    }

                                                    @Override
                                                    public void onError(
                                                            Message message,
                                                            RongIMClient.ErrorCode code) {
                                                        if (callback != null) {
                                                            callback.onError(
                                                                    IRongCoreEnum.CoreErrorCode
                                                                            .valueOf(
                                                                                    code
                                                                                            .getValue()));
                                                        }
                                                    }

                                                    @Override
                                                    public void onCanceled(Message message) {
                                                        // do nothing
                                                    }
                                                });
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                if (callback != null) {
                                    callback.onError(e);
                                }
                            }
                        });
    }

    private void downloadFileByUri(String uri, IRongCoreCallback.ResultCallback<String> callback) {
        if (TextUtils.isEmpty(uri)) {
            if (callback != null) {
                callback.onError(IRongCoreEnum.CoreErrorCode.RC_INVALID_PARAMETER_MEDIA_URL);
            }
            return;
        }
        String key = DeviceUtils.ShortMD5(Base64.NO_WRAP, uri);
        String name = key + ".html";
        String filePath = CombineMessageUtils.getInstance().getCombineFileDirectory();

        IMCenter.getInstance()
                .downloadMediaFile(
                        key,
                        uri,
                        name,
                        filePath,
                        new IRongCallback.IDownloadMediaFileCallback() {
                            String newChangeFileName = "";

                            @Override
                            public void onFileNameChanged(String newFileName) {
                                this.newChangeFileName = newFileName;
                            }

                            @Override
                            public void onSuccess() {
                                String newFileName =
                                        !TextUtils.isEmpty(this.newChangeFileName)
                                                ? this.newChangeFileName
                                                : name;
                                String retFilePath = filePath + File.separator + newFileName;
                                if (callback != null) {
                                    callback.onSuccess(retFilePath);
                                }
                            }

                            @Override
                            public void onProgress(int progress) {
                                // do nothing
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode code) {
                                if (callback != null) {
                                    callback.onError(
                                            IRongCoreEnum.CoreErrorCode.valueOf(code.getValue()));
                                }
                            }

                            @Override
                            public void onCanceled() {
                                // do nothing
                            }
                        });
    }

    private void saveToLocalPath(String srcPath, String targetPath) {
        if (TextUtils.isEmpty(srcPath) || TextUtils.isEmpty(targetPath)) {
            return;
        }

        srcPath = srcPath.replace("file://", "");
        File srcFile = new File(srcPath);
        if (!srcFile.exists()) {
            RLog.e(TAG, "saveToLocalPath failed! srcFile not exist. " + srcPath);
            return;
        }

        File targetFile = new File(targetPath);
        if (!targetFile.exists()) {
            if (targetFile.getParent() == null) {
                RLog.e(TAG, "createFileIfNeed failed! file.getParent is null.");
                return;
            }
            File dir = new File(targetFile.getParent());
            boolean successMkdir = dir.mkdirs();
            boolean isCreateNewFile = false;
            try {
                isCreateNewFile = targetFile.createNewFile();
                RLog.d(
                        TAG,
                        "createFileIfNeed successMkdir:"
                                + successMkdir
                                + ",isCreateNewFile:"
                                + isCreateNewFile);
            } catch (IOException e) {
                RLog.e(TAG, "createFileIfNeed" + e.getMessage());
            }
        }

        boolean isRenameSuccess = srcFile.renameTo(targetFile);
        if (!isRenameSuccess) {
            RLog.e(TAG, "saveToLocalPath failed! rename failed. " + srcPath + " -> " + targetPath);
        }
    }

    private void showLoadError() {
        mWebViewError = true;
        mProgress.setVisibility(View.GONE);
        mImageView.setVisibility(View.VISIBLE);
        mTextView.setVisibility(View.VISIBLE);
        mWebView.setVisibility(View.GONE);
        mTextView.setText(
                getString(
                        IMKitThemeManager.dynamicResource(
                                R.string.rc_combine_webview_load_failed,
                                R.string.rc_combine_webview_download_failed)));
    }

    private void showLoading() {
        mTextView.setText(getString(R.string.rc_combine_webview_loading));
        mTextView.setVisibility(View.VISIBLE);
    }

    private void showLoadSuccess() {

        mProgress.setVisibility(View.GONE);
        mImageView.setVisibility(View.GONE);
        mTextView.setVisibility(View.GONE);
        mWebView.setVisibility(View.VISIBLE);
    }

    private class CombineWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            RLog.d(TAG, "shouldOverrideUrlLoading mPrevUrl: " + mPrevUrl + ", url:" + url);
            if (mPrevUrl != null && mPrevUrl.equals(url)) {
                return false;
            } else {
                if (TYPE_MEDIA.equals(mType) && isRemoteUri(url)) {
                    String filePath = CombineMessageUtils.getInstance().getCombineFilePath(url);
                    if (new File(filePath).exists()) {
                        url = Uri.parse(FILE + filePath).toString();
                        mType = TYPE_LOCAL;
                    }
                    mPrevUrl = url;
                    mWebView.loadUrl(mPrevUrl);
                }
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            RLog.d(TAG, "onPageStarted url:" + url);
        }

        @Override
        public void onReceivedError(
                WebView view, int errorCode, String description, String failingUrl) {
            RLog.d(TAG, "onReceivedError errorCode:" + errorCode);
            showLoadError();
        }

        @Override
        public void onReceivedSslError(
                WebView view, final SslErrorHandler handler, SslError error) {
            FeatureConfig.SSLInterceptor interceptor =
                    RongConfigCenter.featureConfig().getSSLInterceptor();
            boolean check = false;
            if (interceptor != null) {
                check = interceptor.check(error.getCertificate());
            }
            if (check) {
                handler.proceed();
            } else {
                final AlertDialog.Builder builder =
                        new AlertDialog.Builder(CombineWebViewActivity.this);
                builder.setMessage(R.string.rc_notification_error_ssl_cert_invalid);
                builder.setNegativeButton(
                        R.string.rc_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                handler.cancel();
                            }
                        });

                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            KitMediaInterceptor interceptor =
                    RongConfigCenter.featureConfig().getKitMediaInterceptor();
            if (interceptor != null) {
                url = interceptor.onCombinePortraitLoad(url);
                if (interceptor.shouldInterceptRequest(view, url)) {
                    return new WebResourceResponse(null, null, null);
                }
            }
            return super.shouldInterceptRequest(view, url);
        }
    }

    private class CombineWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            RLog.d(TAG, "CombineWebChromeClient onProgressChanged:" + newProgress);
            if (mWebViewError) {
                return;
            }
            if (newProgress == PROGRESS_100) {
                showLoadSuccess();
                // 页面加载完成后注入自定义 CSS
                injectCustomCSS();
            } else {
                if (mProgress.getVisibility() == View.GONE) {
                    mProgress.setVisibility(View.VISIBLE);
                }
                if (mTextView.getVisibility() == View.GONE) {
                    mTextView.setText(getString(R.string.rc_combine_webview_loading));
                    mTextView.setVisibility(View.VISIBLE);
                }
                if (mWebView.getVisibility() == View.VISIBLE) {
                    mWebView.setVisibility(View.GONE);
                }
            }
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (mTitleBar != null && TextUtils.isEmpty(mTitleBar.getTitle())) {
                mTitleBar.setTitle(title);
            }
        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            if (!isFinishing()) {
                finish();
            }
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            // 去除播放小视频时的默认背景
            // WebView 加载video的时候默认有一个黑色暂停背景图，产品要求去掉该背景。
            // 使用一像素替换的话会在打开视频时出现一条黑线，折中使用300*600作为该背景大小
            return Bitmap.createBitmap(VIDEO_WIDTH, VIDEO_HEIGHT, Bitmap.Config.ALPHA_8);
        }
    }

    private class JsInterface {

        @JavascriptInterface
        public void sendInfoToAndroid(String uri) {
            try {
                RLog.d(TAG, "sendInfoToAndroid type start" + uri);
                JSONObject jsonObj = new JSONObject(uri);
                String type = jsonObj.optString("type");
                RLog.d(TAG, "sendInfoToAndroid type:" + type);
                switch (type) {
                    case "RC:FileMsg":
                        openFile(jsonObj);
                        break;
                    case "RC:LBSMsg":
                        openMap(jsonObj);
                        break;
                    case "RC:CombineMsg":
                        openCombine(jsonObj);
                        break;
                    case "link":
                        openLink(jsonObj);
                        break;
                    case "phone":
                        openPhone(jsonObj);
                        break;
                    case "RC:ImgMsg":
                        openImage(jsonObj);
                        break;
                    case "RC:SightMsg":
                        openSight(jsonObj);
                        break;
                    case "RC:GIFMsg":
                        openGif(jsonObj);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                RLog.e(TAG, "sendInfoToAndroid" + e.getMessage());
            }
        }
    }

    private class IsSightFileExists {
        private SightMessage sightMessage;
        private int messageId;

        public IsSightFileExists(SightMessage sightMessage, int messageId) {
            this.sightMessage = sightMessage;
            this.messageId = messageId;
        }

        public boolean invoke() {
            String sightPath =
                    LibStorageUtils.getMediaDownloadDir(
                            getApplicationContext(), LibStorageUtils.VIDEO);
            String name =
                    messageId
                            + "_"
                            + DeviceUtils.ShortMD5(
                                    Base64.NO_WRAP, sightMessage.getMediaUrl().toString());
            if (sightPath.startsWith(FILE)) {
                sightPath = sightPath.substring(BEGIN_INDEX);
            }
            File file = new File(sightPath + File.separator + name);
            return file.exists();
        }
    }

    private class EncodeFile {
        private String mediaUrl;
        private String base64;
        private String thumb;
        private String name;

        EncodeFile(String mediaUrl, String base64) {
            this.mediaUrl = mediaUrl;
            this.base64 = base64;
        }

        String getThumb() {
            return thumb;
        }

        public String getName() {
            return name;
        }

        public EncodeFile invoke() {
            thumb = getCombineFilePath(mediaUrl);
            name = RongUtils.md5(mediaUrl) + ".jpg";
            File thumbFile = new File(thumb + name);
            if (!TextUtils.isEmpty(base64) && !thumbFile.exists()) {
                byte[] data = null;
                try {
                    data = Base64.decode(base64, Base64.NO_WRAP);
                } catch (IllegalArgumentException e) {
                    RLog.e(TAG, "IllegalArgumentException " + e.getMessage());
                }

                if (!isImageFile(data)) {
                    RLog.e(TAG, "afterDecodeMessage Not Image File!");
                    return this;
                }

                FileUtils.byte2File(data, thumb, name);
            }
            return this;
        }
    }
}
