package io.rong.imkit.activity;

import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import io.rong.imkit.R;
import io.rong.imkit.picture.tools.ToastUtils;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.language.RongConfigurationManager;

public class RongBaseNoActionbarActivity extends FragmentActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = RongConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (PermissionCheckUtil.checkPermissionResultIncompatible(permissions, grantResults)) {
            ToastUtils.s(this, getString(R.string.rc_permission_request_failed));
            return;
        }

        if (!PermissionCheckUtil.checkPermissions(this, permissions)) {
            PermissionCheckUtil.showRequestPermissionFailedAlter(this, permissions, grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
