package io.rong.imkit.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.fragment.app.Fragment;
import io.rong.common.RLog;
import io.rong.imkit.activity.CombinePicturePagerActivity;
import io.rong.imkit.activity.CombineWebViewActivity;
import io.rong.imkit.activity.FilePreviewActivity;
import io.rong.imkit.activity.ForwardSelectConversationActivity;
import io.rong.imkit.activity.RongWebviewActivity;
import io.rong.imkit.activity.WebFilePreviewActivity;
import io.rong.imkit.conversation.ConversationFragment;
import io.rong.imkit.conversation.RongConversationActivity;
import io.rong.imkit.conversationlist.RongConversationListActivity;
import io.rong.imkit.feature.forward.ForwardClickActions;
import io.rong.imkit.feature.mention.MentionMemberSelectActivity;
import io.rong.imkit.subconversationlist.RongSubConversationListActivity;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import io.rong.message.FileMessage;
import java.util.ArrayList;
import java.util.HashMap;

public class RouteUtils {

    private static final String TAG = "RouteUtils";

    public static final String CONVERSATION_TYPE = "ConversationType";
    public static final String TARGET_ID = "targetId";
    public static final String CHANNEL_ID = "channelId";
    public static final String CONVERSATION_IDENTIFIER = "ConversationIdentifier";
    public static final String CREATE_CHATROOM = "createIfNotExist";
    public static final String TITLE = "title";
    public static final String INDEX_MESSAGE_TIME = "indexTime";
    public static final String CUSTOM_SERVICE_INFO = "customServiceInfo";
    public static final String FORWARD_TYPE = "forwardType";
    public static final String MESSAGE_IDS = "messageIds";
    public static final String MESSAGE_ID = "messageId";
    public static final String MESSAGE = "message";
    public static final String DISABLE_SYSTEM_EMOJI = "disableSystemEmoji";
    private static HashMap<RongActivityType, Class<? extends Activity>> sActivityMap =
            new HashMap<>();

    public static void routeToConversationListActivity(Context context, String title) {
        if (context == null) {
            RLog.e(TAG, "routeToConversationListActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = RongConversationListActivity.class;
        if (sActivityMap.get(RongActivityType.ConversationListActivity) != null) {
            activity = sActivityMap.get(RongActivityType.ConversationListActivity);
        }
        Intent intent = new Intent(context, activity);
        if (!TextUtils.isEmpty(title)) {
            intent.putExtra(TITLE, title);
        }
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void routeToConversationActivity(
            Context context, Conversation.ConversationType type, String targetId) {
        ConversationIdentifier identifier = ConversationIdentifier.obtain(type, targetId, "");
        routeToConversationActivity(context, identifier, false, null);
    }

    public static void routeToConversationActivity(
            Context context,
            Conversation.ConversationType type,
            String targetId,
            boolean disableSystemEmoji) {
        ConversationIdentifier identifier = ConversationIdentifier.obtain(type, targetId, "");
        routeToConversationActivity(context, identifier, disableSystemEmoji, null);
    }

    /**
     * 启动会话页面
     *
     * @param context 上下文
     * @param type 会话类型
     * @param targetId 目标 ID
     * @param bundle 启动 activity 时 intent 里需要携带的 bundle 信息。
     */
    public static void routeToConversationActivity(
            Context context, Conversation.ConversationType type, String targetId, Bundle bundle) {
        ConversationIdentifier identifier = ConversationIdentifier.obtain(type, targetId, "");
        routeToConversationActivity(context, identifier, false, bundle);
    }

    /**
     * 启动会话页面
     *
     * @param context 上下文
     * @param type 会话类型
     * @param targetId 目标 ID
     * @param disableSystemEmoji 是否隐藏融云自带表情
     * @param bundle 启动 activity 时 intent 里需要携带的 bundle 信息。
     */
    public static void routeToConversationActivity(
            Context context,
            Conversation.ConversationType type,
            String targetId,
            boolean disableSystemEmoji,
            Bundle bundle) {
        ConversationIdentifier identifier = ConversationIdentifier.obtain(type, targetId, "");
        routeToConversationActivity(context, identifier, disableSystemEmoji, bundle);
    }

    /**
     * 启动会话页面
     *
     * @param context 上下文
     * @param conversationIdentifier 会话标识
     */
    public static void routeToConversationActivity(
            Context context, ConversationIdentifier conversationIdentifier) {
        routeToConversationActivity(context, conversationIdentifier, false, null);
    }

    /**
     * 启动会话页面
     *
     * @param context 上下文
     * @param conversationIdentifier 会话标识
     * @param bundle 启动 activity 时 intent 里需要携带的 bundle 信息。
     */
    public static void routeToConversationActivity(
            Context context, ConversationIdentifier conversationIdentifier, Bundle bundle) {
        routeToConversationActivity(context, conversationIdentifier, false, bundle);
    }

    /**
     * 启动会话页面
     *
     * @param context 上下文
     * @param conversationIdentifier 会话标识
     * @param disableSystemEmoji 是否隐藏融云自带表情
     * @param bundle 启动 activity 时 intent 里需要携带的 bundle 信息。
     */
    public static void routeToConversationActivity(
            Context context,
            ConversationIdentifier conversationIdentifier,
            boolean disableSystemEmoji,
            Bundle bundle) {
        if (context == null) {
            RLog.e(TAG, "routeToConversationActivity: context is null");
            return;
        }
        if (conversationIdentifier == null) {
            RLog.e(TAG, "routeToConversationActivity: conversationIdentifier is empty");
            return;
        }
        if (TextUtils.isEmpty(conversationIdentifier.getTargetId())) {
            RLog.e(TAG, "routeToConversationActivity: targetId is empty");
            return;
        }
        if (conversationIdentifier.getType() == null) {
            RLog.e(TAG, "routeToConversationActivity: type is empty");
            return;
        }
        Class<? extends Activity> activity = RongConversationActivity.class;
        if (sActivityMap.get(RongActivityType.ConversationActivity) != null) {
            activity = sActivityMap.get(RongActivityType.ConversationActivity);
        }
        Intent intent = new Intent(context, activity);
        // 旧字段依旧传，兼容旧版本逻辑
        intent.putExtra(TARGET_ID, conversationIdentifier.getTargetId());
        intent.putExtra(
                CONVERSATION_TYPE, conversationIdentifier.getType().getName().toLowerCase());
        intent.putExtra(CONVERSATION_IDENTIFIER, conversationIdentifier);
        intent.putExtra(DISABLE_SYSTEM_EMOJI, disableSystemEmoji);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * 启动聚合会话页面
     *
     * @param context 上下文
     * @param type 聚合会话类型
     * @param title 标题
     */
    public static void routeToSubConversationListActivity(
            Context context, Conversation.ConversationType type, String title) {
        if (context == null) {
            RLog.e(TAG, "routeToSubConversationListActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = RongSubConversationListActivity.class;
        if (sActivityMap.get(RongActivityType.SubConversationListActivity) != null) {
            activity = sActivityMap.get(RongActivityType.SubConversationListActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.putExtra(CONVERSATION_TYPE, type);
        intent.putExtra(TITLE, title);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * 启动 @ 功能选人页面
     *
     * @param context 上下文
     * @param targetId 目标 ID
     * @param type 会话类型
     */
    public static void routeToMentionMemberSelectActivity(
            Context context, String targetId, Conversation.ConversationType type) {
        if (context == null) {
            RLog.e(TAG, "routeToMentionMemberSelectActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = MentionMemberSelectActivity.class;
        if (sActivityMap.get(RongActivityType.MentionMemberSelectActivity) != null) {
            activity = sActivityMap.get(RongActivityType.MentionMemberSelectActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.putExtra(CONVERSATION_TYPE, type.getValue());
        intent.putExtra(TARGET_ID, targetId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void routeToWebActivity(Context context, String url) {
        routeToWebActivity(context, url, null);
    }

    /**
     * 启动 web view 页面
     *
     * @param context 上下文
     * @param url 远端 url 地址
     * @param title 标题
     */
    public static void routeToWebActivity(Context context, String url, String title) {
        if (context == null) {
            RLog.e(TAG, "routeToWebActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = RongWebviewActivity.class;
        if (sActivityMap.get(RongActivityType.RongWebViewActivity) != null) {
            activity = sActivityMap.get(RongActivityType.RongWebViewActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void routeToFilePreviewActivity(
            Context context, Message message, FileMessage content, int progress) {
        if (context == null) {
            RLog.e(TAG, "routeToFilePreviewActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = FilePreviewActivity.class;
        if (sActivityMap.get(RongActivityType.FilePreviewActivity) != null) {
            activity = sActivityMap.get(RongActivityType.FilePreviewActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.putExtra("FileMessage", content);
        intent.putExtra("Message", message);
        intent.putExtra("Progress", progress);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * 启动转发时选择会话页面
     *
     * @param fragment 当前 fragment
     * @param type 转发类型。{@link ForwardClickActions.ForwardType}
     * @param messageIds 转发的消息 id 列表。
     */
    public static void routeToForwardSelectConversationActivity(
            Fragment fragment,
            ForwardClickActions.ForwardType type,
            ArrayList<Integer> messageIds) {
        if (fragment == null || fragment.getContext() == null) {
            RLog.e(TAG, "routeToForwardSelectConversationActivity: fragment or context is null");
            return;
        }
        Class<? extends Activity> activity = ForwardSelectConversationActivity.class;
        if (sActivityMap.get(RongActivityType.ForwardSelectConversationActivity) != null) {
            activity = sActivityMap.get(RongActivityType.ForwardSelectConversationActivity);
        }
        Intent intent = new Intent(fragment.getContext(), activity);
        intent.putExtra(FORWARD_TYPE, type.getValue());
        intent.putIntegerArrayListExtra(MESSAGE_IDS, messageIds);
        if (!(fragment.getContext() instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        fragment.startActivityForResult(intent, ConversationFragment.REQUEST_CODE_FORWARD);
    }

    /**
     * 启动合并转发消息的图片展示页面
     *
     * @param context 上下文
     * @param message 合并转发时携带的原始消息
     */
    public static void routeToCombinePicturePagerActivity(Context context, Message message) {
        if (context == null) {
            RLog.e(TAG, "routeToCombinePicturePagerActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = CombinePicturePagerActivity.class;
        if (sActivityMap.get(RongActivityType.CombinePicturePagerActivity) != null) {
            activity = sActivityMap.get(RongActivityType.CombinePicturePagerActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.setPackage(context.getApplicationContext().getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("message", message);
        context.startActivity(intent);
    }

    /**
     * 启动合并转发消息的在线展示页面
     *
     * @param context 上下文
     * @param messageId 消息 id
     * @param uri 远端 url 地址
     * @param type
     * @param title 标题
     */
    public static void routeToCombineWebViewActivity(
            Context context, int messageId, String uri, String type, String title) {
        if (context == null) {
            RLog.e(TAG, "routeToCombineWebViewActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = CombineWebViewActivity.class;
        if (sActivityMap.get(RongActivityType.CombineWebViewActivity) != null) {
            activity = sActivityMap.get(RongActivityType.CombineWebViewActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.setPackage(context.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("messageId", messageId);
        intent.putExtra("uri", uri);
        intent.putExtra("type", type);
        intent.putExtra("title", title);
        context.startActivity(intent);
    }

    /**
     * 启动文件在线浏览页面
     *
     * @param context 上下文
     * @param fileUrl 文件远端地址
     * @param fileName 文件名称
     * @param fileSize 文件大小
     */
    public static void routeToWebFilePreviewActivity(
            Context context, String fileUrl, String fileName, String fileSize) {
        if (context == null) {
            RLog.e(TAG, "routeToWebFilePreviewActivity: context is null");
            return;
        }
        Class<? extends Activity> activity = WebFilePreviewActivity.class;
        if (sActivityMap.get(RongActivityType.WebFilePreviewActivity) != null) {
            activity = sActivityMap.get(RongActivityType.WebFilePreviewActivity);
        }
        Intent intent = new Intent(context, activity);
        intent.setPackage(context.getPackageName());
        intent.putExtra("fileUrl", fileUrl);
        intent.putExtra("fileName", fileName);
        intent.putExtra("fileSize", fileSize);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void registerActivity(
            RongActivityType activityType, Class<? extends Activity> activity) {
        sActivityMap.put(activityType, activity);
    }

    public static Class<? extends Activity> getActivity(RongActivityType type) {
        return sActivityMap.get(type);
    }

    public enum RongActivityType {
        ConversationListActivity,
        SubConversationListActivity,
        ConversationActivity,
        MentionMemberSelectActivity,
        RongWebViewActivity,
        FilePreviewActivity,
        CombineWebViewActivity,
        CombinePicturePagerActivity,
        ForwardSelectConversationActivity,
        WebFilePreviewActivity,
    }
}
