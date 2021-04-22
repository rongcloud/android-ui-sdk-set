package io.rong.imkit.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import io.rong.common.FileUtils;
import io.rong.common.LibStorageUtils;
import io.rong.common.RLog;
import io.rong.common.RongWebView;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.feature.forward.CombineMessageUtils;
import io.rong.imkit.feature.location.AMapPreviewActivity;
import io.rong.imkit.feature.location.AMapPreviewActivity2D;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.common.DeviceUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.ImageMessage;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.SightMessage;

public class CombineWebViewActivity extends RongBaseActivity {
    private final static String TAG = CombineWebViewActivity.class.getSimpleName();

    // WebView加载视频时的默认背景宽高
    private static final int VIDEO_WIDTH = 300;
    private static final int VIDEO_HEIGHT = 600;

    public static final String TYPE_LOCAL = "local";
    public static final String TYPE_MEDIA = "media";
    private static final String COMBINE_FILE_PATH = "combine";

    protected RongWebView mWebView;
    private ProgressBar mProgress;
    private ImageView mImageView;
    private TextView mTextView;

    private String mType;
    private int mMessageId;
    private String mPrevUrl;
    private boolean mWebViewError = false;
    private RongIMClient.OnRecallMessageListener mRecallMessageListener = new RongIMClient.OnRecallMessageListener() {
        @Override
        public boolean onMessageRecalled(Message message, RecallNotificationMessage recallNotificationMessage) {
            if (mMessageId != -1 && mMessageId == message.getMessageId()) {
                new AlertDialog.Builder(CombineWebViewActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
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

        if (Build.VERSION.SDK_INT >= 17) {
            mWebView.addJavascriptInterface(new JsInterface(), "interface");
        }

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDefaultTextEncodingName("utf-8");
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setAllowFileAccess(true);
        // 允许小视频自动播放
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }
        //允许混合内容 解决部分手机https请求里面加载不出http的图片
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) return;

        mMessageId = intent.getIntExtra("messageId", -1);
        String uri = intent.getStringExtra("uri");
        mType = intent.getStringExtra("type");
        String title = intent.getStringExtra("title");

        mPrevUrl = uri;
        mWebViewError = false;
        mWebView.loadUrl(mPrevUrl);
        if (mTitleBar != null && !TextUtils.isEmpty(title)) {
            mTitleBar.setTitle(title);
        }
//        onCreateActionbar(new ActionBar());
    }

    private class CombineWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            RLog.d(TAG, "shouldOverrideUrlLoading mPrevUrl: " + mPrevUrl + ", url:" + url);
            if (mPrevUrl != null && mPrevUrl.equals(url)) {
                return false;
            } else {
                if (TYPE_MEDIA.equals(mType)
                        && url != null
                        && (url.toLowerCase().startsWith("http") || url.toLowerCase().startsWith("ftp"))) {
                    String filePath = CombineMessageUtils.getInstance().getCombineFilePath(url);
                    if (new File(filePath).exists()) {
                        url = Uri.parse("file://" + filePath).toString();
                        mType = TYPE_LOCAL;
                    }
                }
                mPrevUrl = url;
                mWebView.loadUrl(mPrevUrl);
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            RLog.d(TAG, "onPageStarted url:" + url);
            if (TYPE_MEDIA.equals(mType)
                    && url != null
                    && (url.toLowerCase().startsWith("http") || url.toLowerCase().startsWith("ftp"))) {
                String filePath = CombineMessageUtils.getInstance().getCombineFilePath(url);
                if (!new File(filePath).exists()) {
                    new DownloadTask().execute(url, filePath);
                }
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            RLog.d(TAG, "onReceivedError errorCode:" + errorCode);
            mWebViewError = true;
            mProgress.setVisibility(View.GONE);
            mImageView.setVisibility(View.VISIBLE);
            mTextView.setVisibility(View.VISIBLE);
            mWebView.setVisibility(View.GONE);
            mTextView.setText(getString(R.string.rc_combine_webview_download_failed));
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(CombineWebViewActivity.this);
            builder.setMessage(R.string.rc_notification_error_ssl_cert_invalid);
            builder.setNegativeButton(R.string.rc_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    handler.cancel();
                }
            });
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private class CombineWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            RLog.d(TAG, "CombineWebChromeClient onProgressChanged:" + newProgress);
            if (mWebViewError) {
                return;
            }
            if (newProgress == 100) {
                mProgress.setVisibility(View.GONE);
                mImageView.setVisibility(View.GONE);
                mTextView.setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
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
                }
            } catch (Exception e) {
                RLog.e(TAG, "sendInfoToAndroid", e);
            }
        }
    }

    private void openSight(JSONObject jsonObj) {
        String mediaUrl = jsonObj.optString("fileUrl");
        int duration = jsonObj.optInt("duration");
        String base64 = jsonObj.optString("imageBase64");
        int dotIndex = base64.indexOf(",");
        base64 = base64.substring(dotIndex + 1);

        EncodeFile encodeFile = new EncodeFile(mediaUrl, base64).invoke();
        String sightThumb = encodeFile.getThumb();
        String sightName = encodeFile.getName();

        SightMessage sightMessage = new SightMessage();
        sightMessage.setThumbUri(Uri.parse("file://" + sightThumb + sightName));
        sightMessage.setMediaUrl(Uri.parse(mediaUrl));
        sightMessage.setDuration(duration);
        if (new IsSightFileExists(sightMessage).invoke()) {
            String sightPath = LibStorageUtils.getMediaDownloadDir(getApplicationContext(), LibStorageUtils.VIDEO);
            String name = DeviceUtils.ShortMD5(Base64.NO_WRAP, mediaUrl);
            if (sightPath.startsWith("file://")) {
                sightPath = sightPath.substring(7);
            }
            sightMessage.setLocalPath(Uri.parse(sightPath + File.separator + name));
        }

        Message message = new Message();
        message.setContent(sightMessage);
        message.setTargetId(RongIMClient.getInstance().getCurrentUserId());
        message.setConversationType(Conversation.ConversationType.PRIVATE);

        ComponentName cn = new ComponentName(CombineWebViewActivity.this, "io.rong.sight.player.SightPlayerActivity");
        Intent intent = new Intent();
        intent.setComponent(cn);
        intent.putExtra("Message", message);
        intent.putExtra("SightMessage", sightMessage);
        startActivity(intent);
    }

    private class IsSightFileExists {
        private SightMessage sightMessage;

        public IsSightFileExists(SightMessage sightMessage) {
            this.sightMessage = sightMessage;
        }

        public boolean invoke() {
            String sightPath = LibStorageUtils.getMediaDownloadDir(getApplicationContext(), LibStorageUtils.VIDEO);
            String name = DeviceUtils.ShortMD5(Base64.NO_WRAP, sightMessage.getMediaUrl().toString());
            if (sightPath.startsWith("file://")) {
                sightPath = sightPath.substring(7);
            }
            File file = new File(sightPath + File.separator + name);
            return file.exists();
        }
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
        imageMessage.setThumUri(Uri.parse("file://" + thumb + name));
        imageMessage.setRemoteUri(Uri.parse(mediaUrl));

        Message message = new Message();
        message.setContent(imageMessage);
        message.setConversationType(Conversation.ConversationType.PRIVATE);
        RouteUtils.routeToCombinePicturePagerActivity(this, message);
    }

    // 根据链接地址，获取合并转发消息下载路径
    public String getCombineFilePath(String uri) {
        return FileUtils.getCachePath(IMCenter.getInstance().getContext())
                + File.separator + COMBINE_FILE_PATH + File.separator;
    }

    private static boolean isImageFile(byte[] data) {
        if (data == null || data.length == 0)
            return false;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        return options.outWidth != -1;
    }

    private void openFile(JSONObject jsonObj) {
        Log.e("openFile", jsonObj.toString());
//        Intent intent = new Intent(RongKitIntent.RONG_INTENT_ACTION_OPENWEBFILE);
//        intent.setPackage(getPackageName());
//        intent.putExtra("fileUrl", jsonObj.optString("fileUrl"));
//        intent.putExtra("fileName", jsonObj.optString("fileName"));
//        intent.putExtra("fileSize", jsonObj.optString("fileSize"));
//        startActivity(intent);
        RouteUtils.routeToWebFilePreviewActivity(this, jsonObj.optString("fileUrl"), jsonObj.optString("fileName"), jsonObj.optString("fileSize"));
    }

    private void openMap(JSONObject jsonObj) {
        double lat = Double.parseDouble(jsonObj.optString("latitude"));
        double lng = Double.parseDouble(jsonObj.optString("longitude"));
        String poi = jsonObj.optString("locationName");
        LocationMessage content = LocationMessage.obtain(lat, lng, poi, null);
        try {
            Intent intent;
            if (this.getResources().getBoolean(R.bool.rc_location_2D)) {
                intent = new Intent(CombineWebViewActivity.this, AMapPreviewActivity2D.class);
            } else {
                intent = new Intent(CombineWebViewActivity.this, AMapPreviewActivity.class);
            }
            intent.putExtra("location", content);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            RLog.e(TAG, "openMap", e);
        }
    }

    private void openCombine(JSONObject jsonObj) {
        String type = CombineWebViewActivity.TYPE_MEDIA;
        String uri = jsonObj.optString("fileUrl");
        String filePath = CombineMessageUtils.getInstance().getCombineFilePath(uri);
        if (new File(filePath).exists()) {
            uri = Uri.parse("file://" + filePath).toString();
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

    private static class DownloadTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(String... params) {
            OutputStream out = null;
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);

                int code = urlConnection.getResponseCode();
                if (code < 200 || code >= 300) {
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
                    RLog.d(TAG, "DownloadTask successMkdir:" + successMkdir + ",isCreateNewFile:" + isCreateNewFile);
                }
                out = new FileOutputStream(file);
                byte[] buffer = new byte[10 * 1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                in.close();
            } catch (IOException e) {
                RLog.e(TAG, "DownloadTask", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        RLog.e(TAG, "DownloadTask", e);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            mWebView.destroy();
        }
        IMCenter.getInstance().removeOnRecallMessageListener(mRecallMessageListener);
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
                    RLog.e(TAG, "afterDecodeMessage Not Base64 Content!");
                    RLog.e(TAG, "IllegalArgumentException ", e);
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