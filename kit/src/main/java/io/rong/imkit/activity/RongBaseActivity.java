package io.rong.imkit.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import io.rong.imkit.R;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.StatusBarUtil;
import io.rong.imkit.utils.language.RongConfigurationManager;
import io.rong.imkit.widget.TitleBar;

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
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        super.setContentView(R.layout.rc_base_activity_layout);
        mTitleBar = findViewById(R.id.rc_title_bar);
        mTitleBar.setOnBackClickListener(new TitleBar.OnBackClickListener() {
            @Override
            public void onBackClick() {
                finish();
            }
        });
        mTitleBar.setBackgroundColor(getResources().getColor(R.color.rc_white_color));
        mContentView = findViewById(R.id.rc_base_container);
    }

    @Override
    public void setContentView(int resId) {
        View view = LayoutInflater.from(this).inflate(resId, null);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        mContentView.addView(view, lp);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (!PermissionCheckUtil.checkPermissions(this, permissions)) {
            PermissionCheckUtil.showRequestPermissionFailedAlter(this, permissions, grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void initStatusBar(int colorResId) {
        StatusBarUtil.setRootViewFitsSystemWindows(this, true);
        StatusBarUtil.setStatusBarColor(this, colorResId == 0 ? getResources().getColor(R.color.rc_background_main_color) : getResources().getColor(colorResId)); //Color.parseColor("#F5F6F9")
    }
}
