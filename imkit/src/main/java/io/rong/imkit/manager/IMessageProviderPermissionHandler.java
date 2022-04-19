package io.rong.imkit.manager;

import android.app.Activity;
import io.rong.imkit.model.UiMessage;

/**
 * 各消息类型展示类ItemProvider在onItemClick中需要实现权限申请时实现此接口.
 *
 * <p>Created by yanke on 2021/8/30
 */
public interface IMessageProviderPermissionHandler {
    void handleRequestPermissionsResult(
            Activity activity, UiMessage uiMessage, String[] permissions, int[] grantResults);
}
