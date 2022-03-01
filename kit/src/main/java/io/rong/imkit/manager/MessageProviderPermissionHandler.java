package io.rong.imkit.manager;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.messgelist.provider.BaseMessageItemProvider;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import java.util.HashMap;
import java.util.Map;

/** Created by yanke on 2021/8/30 */
public class MessageProviderPermissionHandler {
    private static final MessageProviderPermissionHandler ourInstance =
            new MessageProviderPermissionHandler();
    public static final int REQUEST_CODE_ITEM_PROVIDER_PERMISSIONS = 3000;
    private Map<Class<? extends MessageContent>, String[]> messageContentList = new HashMap<>();
    private UiMessage uiMessage;

    public static MessageProviderPermissionHandler getInstance() {
        return ourInstance;
    }

    private MessageProviderPermissionHandler() {}

    public boolean handleMessageClickPermission(UiMessage uiMessage, Fragment fragment) {
        if (uiMessage == null || uiMessage.getMessage() == null || fragment == null) {
            return false;
        }

        this.uiMessage = uiMessage;
        Message message = uiMessage.getMessage();

        MessageContent messageContent = message.getContent();
        if (!messageContentList.containsKey(messageContent.getClass())) {
            return false;
        }

        String[] permissions = messageContentList.get(messageContent.getClass());
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            return false;
        } else {
            PermissionCheckUtil.requestPermissions(
                    fragment, permissions, REQUEST_CODE_ITEM_PROVIDER_PERMISSIONS);
            return true;
        }
    }

    public void addMessageContent(
            Class<? extends MessageContent> messageContentClass, String[] permissions) {
        if (messageContentClass == null || permissions == null || permissions.length == 0) {
            return;
        }

        if (!messageContentList.containsKey(messageContentClass)) {
            messageContentList.put(messageContentClass, permissions);
        }
    }

    public void onRequestPermissionsResult(
            Activity activity, String[] permissions, int[] grantResults) {
        Message message = uiMessage.getMessage();
        MessageContent messageContent = message.getContent();
        if (!messageContentList.containsKey(messageContent.getClass())) {
            return;
        }
        BaseMessageItemProvider provider =
                (BaseMessageItemProvider)
                        RongConfigCenter.conversationConfig()
                                .getMessageListProvider()
                                .getProvider(uiMessage);
        ((IMessageProviderPermissionHandler) provider)
                .handleRequestPermissionsResult(activity, uiMessage, permissions, grantResults);
    }
}
