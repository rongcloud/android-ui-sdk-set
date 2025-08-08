package io.rong.imkit.base;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.rong.imkit.utils.RongUtils;
import io.rong.imkit.utils.language.RongConfigurationManager;

/**
 * @author rongcloud
 * @since 5.10.4
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = RongConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        RongUtils.fixAndroid8ActivityCrash(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
