package io.rong.imkit.conversation.extension.component.plugin;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import io.rong.imkit.conversation.extension.RongExtension;

/** Created by jiangecho on 2017/11/2. */
public interface IPluginRequestPermissionResultCallback {
    /** /~chinese 所有plugin的申请权限时，都应当用此request code */

    /** /~english This request code should be used for all plugin application permissions. */
    int REQUEST_CODE_PERMISSION_PLUGIN = 255;

    /**
     * /~chinese
     *
     * @param fragment Fragment
     * @param extension RongExtension
     * @param permissions 申请的权限
     * @param requestCode 请求码
     * @param grantResults 受权的结果
     * @return true， plugin自己已处理用户授权结果；false，plugin自己没处理用户授权结果
     */

    /**
     * /~english boolean onRequestPermissionResult(Fragment fragment, RongExtension extension, int
     * requestCode, java.lang.String[] permissions, int[] grantResults)
     *
     * @param fragment Fragment
     * @param extension RongExtension
     * @param requestCode Request code
     * @param permissions Permission to apply
     * @param grantResults Results of authorization
     * @return True, plugin has handled the user authorization result by itself; false,plugin has
     *     not handled the user authorization result by itself.
     */
    boolean onRequestPermissionResult(
            Fragment fragment,
            RongExtension extension,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults);
}
