package io.rong.imkit.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.util.List;

import io.rong.common.RLog;
import io.rong.common.RongWebView;
import io.rong.imkit.R;
import io.rong.imkit.config.FeatureConfig;
import io.rong.imkit.config.RongConfigCenter;

public class RongWebviewActivity extends RongBaseActivity {
    private final static String TAG = "RongWebviewActivity";

    private String mPrevUrl;
    protected RongWebView mWebView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_ac_webview);
        initStatusBar(R.color.app_color_white);
        Intent intent = getIntent();
        mWebView = findViewById(R.id.rc_webview);
        mProgressBar = findViewById(R.id.rc_web_progressbar);
        mWebView.setVerticalScrollbarOverlay(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT > 11) {
            mWebView.getSettings().setDisplayZoomControls(false);
        }
        mWebView.getSettings().setSupportZoom(true);
        mWebView.setWebViewClient(new RongWebviewClient());
        mWebView.setWebChromeClient(new RongWebChromeClient());
        mWebView.setDownloadListener(new RongWebViewDownLoadListener());
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setDefaultTextEncodingName("utf-8");

        String url = intent.getStringExtra("url");
        Uri data = intent.getData();
        if (url != null) {
            if (RongConfigCenter.featureConfig().rc_set_java_script_enabled) {
                if (url.startsWith("file://")) {
                    mWebView.getSettings().setJavaScriptEnabled(false);
                } else {
                    mWebView.getSettings().setJavaScriptEnabled(true);
                }
            }
            mPrevUrl = url;
            mWebView.loadUrl(url);
            String title = intent.getStringExtra("title");
            if (mTitleBar != null && !TextUtils.isEmpty(title)) {
                mTitleBar.setTitle(title);
            }
        } else if (data != null) {
            mPrevUrl = data.toString();
            mWebView.loadUrl(data.toString());
        }
    }

    public void setOnTitleReceivedListener(OnTitleReceivedListener onTitleReceivedListener) {
        this.onTitleReceivedListener = onTitleReceivedListener;
    }

    private OnTitleReceivedListener onTitleReceivedListener;

    public interface OnTitleReceivedListener {
        void onTitleReceived(String title);
    }

    private class RongWebviewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mPrevUrl != null) {
                if (!mPrevUrl.equals(url)) {
                    if (!(url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://"))) {
                        Intent intent = new Intent("android.intent.action.VIEW");
                        Uri content_url = Uri.parse(url);
                        intent.setData(content_url);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            RLog.e(TAG, "not apps install for this intent =" + e.toString());
                            RLog.e(TAG, "RongWebviewClient", e);
                        }
                        return true;
                    }
                    mPrevUrl = url;
                    mWebView.loadUrl(url);
                    return true;
                } else {
                    return false;
                }
            } else {
                mPrevUrl = url;
                mWebView.loadUrl(url);
                return true;
            }
        }

        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(RongWebviewActivity.this);
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

    private class RongWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                mProgressBar.setVisibility(View.GONE);
            } else {
                if (mProgressBar.getVisibility() == View.GONE) {
                    mProgressBar.setVisibility(View.VISIBLE);
                }
                mProgressBar.setProgress(newProgress);
            }
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            if (mTitleBar != null && TextUtils.isEmpty(mTitleBar.getTitle())) {
                mTitleBar.setTitle(title);
            }
            if (onTitleReceivedListener != null) {
                onTitleReceivedListener.onTitleReceived(title);
            }
        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            if (!isFinishing()) {
                finish();
            }
        }
    }

    private class RongWebViewDownLoadListener implements DownloadListener {

        @Override
        public void onDownloadStart(String url, String userAgent,
                                    String contentDisposition, String mimetype, long contentLength) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (checkIntent(RongWebviewActivity.this, intent)) {
                startActivity(intent);
                if (("file").equals(uri.getScheme()) && uri.toString().endsWith(".txt")) {
                    finish();
                }
            }
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

    public boolean checkIntent(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> apps = packageManager.queryIntentActivities(intent, 0);
        return apps.size() > 0;
    }
}