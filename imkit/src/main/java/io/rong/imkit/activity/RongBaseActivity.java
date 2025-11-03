package io.rong.imkit.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import io.rong.imkit.R;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.picture.tools.ToastUtils;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.utils.StatusBarUtil;
import io.rong.imkit.utils.language.RongConfigurationManager;
import io.rong.imkit.widget.TitleBar;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import java.util.Locale;

public class RongBaseActivity extends AppCompatActivity {
    protected ViewFlipper mContentView;
    protected TitleBar mTitleBar;

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = RongConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        RongUtils.fixAndroid8ActivityCrash(this);
        super.onCreate(savedInstanceState);
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
        super.setContentView(R.layout.rc_base_activity_layout);
        mTitleBar = findViewById(R.id.rc_title_bar);
        mTitleBar.setOnBackClickListener(
                new TitleBar.OnBackClickListener() {
                    @Override
                    public void onBackClick() {
                        finish();
                    }
                });
        mTitleBar.setBackgroundColor(
                IMKitThemeManager.getColorFromAttrId(this, R.attr.rc_common_background_color));
        mContentView = findViewById(R.id.rc_base_container);
    }

    @Override
    public void setContentView(int resId) {
        View view = LayoutInflater.from(this).inflate(resId, null);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1);
        mContentView.addView(view, lp);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissionResultIncompatible(permissions, grantResults)) {
            ToastUtils.s(this, getString(R.string.rc_permission_request_failed));
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void initStatusBar(int colorResId) {
        StatusBarUtil.setRootViewFitsSystemWindows(this, true);
        StatusBarUtil.setStatusBarColor(
                this,
                colorResId == 0
                        ? getResources()
                                .getColor(
                                        IMKitThemeManager.getAttrResId(
                                                this, R.attr.rc_primary_color))
                        : getResources().getColor(colorResId)); // Color.parseColor("#F5F6F9")
    }

    /** 从Intent取出ConversationIdentifier，没有的话取ConversationType、targetId、channelId构建 */
    public ConversationIdentifier initConversationIdentifier() {
        Intent intent = getIntent();
        if (intent.hasExtra(RouteUtils.CONVERSATION_IDENTIFIER)) {
            ConversationIdentifier identifier =
                    intent.getParcelableExtra(RouteUtils.CONVERSATION_IDENTIFIER);
            if (identifier != null) {
                return identifier;
            }
        }

        String type = intent.getStringExtra(RouteUtils.CONVERSATION_TYPE);
        Conversation.ConversationType conversationType =
                Conversation.ConversationType.valueOf(type.toUpperCase(Locale.US));
        String targetId = intent.getStringExtra(RouteUtils.TARGET_ID);
        return ConversationIdentifier.obtain(conversationType, targetId, "");
    }
}
