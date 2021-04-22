package io.rong.imkit.feature.forward;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.ExecutorHelper;
import io.rong.imlib.IRongCallback;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ImageMessage;
import io.rong.imlib.location.message.LocationMessage;
import io.rong.message.MediaMessageContent;
import io.rong.message.ReferenceMessage;

import static android.app.Activity.RESULT_OK;

public class ForwardManager {
    private static final String TAG = ForwardManager.class.getSimpleName();

    // 发送消息间隔
    private static final int TIME_DELAY = 400;
    //合并消息最多存储四条消息的文本信息
    private static final int SUMMARY_MAX_SIZE = 4;

    private ForwardManager() {

    }

    private static class SingletonHolder {
        static ForwardManager sInstance = new ForwardManager();
    }

    public static ForwardManager getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * 设置转发的会话选择结果。
     * 可以在自定义的转发选择联系人界面, 调用该方法启动合并转发.
     *
     * @param activity      选择联系人界面
     * @param conversations 会话类型,只能选择单聊和群聊会话
     */
    public static void setForwardMessageResult(Activity activity, ArrayList<Conversation> conversations) {
        Intent intent = activity.getIntent();
        intent.putParcelableArrayListExtra("conversations", conversations);
        activity.setResult(RESULT_OK, intent);
        activity.finish();
    }

    public static List<Message> filterMessagesList(Context context, List<Message> messages, int index) {
        List<Message> forwardMessagesList = new ArrayList<>();
        if (context == null) {
            RLog.e(TAG, "filterMessagesList context is null");
            return forwardMessagesList;
        }

        for (Message message : messages) {
            if (!allowForward(message, index)) {
                String msg = context.getString(R.string.rc_combine_unsupported);
                if (index == 0) {
                    msg = context.getString(R.string.rc_combine_unsupported_step);
                }
                new AlertDialog.Builder(context)
                        .setMessage(msg)
                        .setPositiveButton(context.getString(R.string.rc_dialog_ok), null)
                        .show();
                forwardMessagesList.clear();
                return forwardMessagesList;
            }
            forwardMessagesList.add(message);
        }

        // 根据消息发送时间排序
        Collections.sort(forwardMessagesList, new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return (int) (o1.getSentTime() - o2.getSentTime());
            }
        });
        return forwardMessagesList;
    }

    // index: 0:逐步转发  1:合并转发
    private static boolean allowForward(Message message, int index) {
        if (message == null) {
            RLog.d(TAG, "Forwarding is not allowed, message is null");
            return false;
        }

        // 发送失败和撤回的消息不允许转发
        if (message.getSentStatus() == Message.SentStatus.SENDING
                || message.getSentStatus() == Message.SentStatus.FAILED ||
                message.getSentStatus() == Message.SentStatus.CANCELED) {
            RLog.d(TAG, "Forwarding is not allowed, status:" + message.getSentStatus());
            return false;
        }

        MessageContent messageContent = message.getContent();
        if (messageContent == null) {
            RLog.d(TAG, "Forwarding is not allowed, message:" + message);
            return false;
        }

        // 阅后即焚消息不允许转发
        if (messageContent.isDestruct()) {
            RLog.d(TAG, "Destruct message not allow forward");
            return false;
        }

        MessageTag tag = messageContent.getClass().getAnnotation(MessageTag.class);
        if (tag == null) {
            RLog.d(TAG, "Forwarding is not allowed, tag is null");
            return false;
        }

        String messageTag = tag.value();
        boolean allow = false;

        if (index == 0) {
            allow = allowForwardForStep(messageTag);
        } else if (index == 1) {
            allow = allowForwardForCombine(messageTag);
        }

        RLog.d(TAG, "Forwarding allowed:" + allow + ", type:" + tag.value());
        return allow;
    }

    //允许逐条转发的消息类型：文本,语音,小视频,图片,文件,图文,表情,位置,合并,引用
    private static boolean allowForwardForStep(String tag) {
        switch (tag) {
            case "RC:TxtMsg": // 文本
            case "RC:VcMsg": // 语音
            case "RC:HQVCMsg": // 语音
            case "RC:SightMsg": // 小视频
            case "RC:ImgMsg": // 图片
            case "RC:GIFMsg": // gif图片
            case "RC:FileMsg": // 文件
            case "RC:ImgTextMsg": // 图文
            case "RC:StkMsg": // 表情
            case "RC:CardMsg":// 名片
            case "RC:LBSMsg": // 位置
            case "RC:CombineMsg": // 合并
            case "RC:ReferenceMsg": // 引用
                return true;
        }
        return false;
    }

    //允许合并转发的消息类型：文本,语音,小视频,图片,文件,图文,表情,位置,合并,音视频通话
    private static boolean allowForwardForCombine(String tag) {
        switch (tag) {
            case "RC:TxtMsg": // 文本
            case "RC:VcMsg": // 语音
            case "RC:HQVCMsg": // 语音
            case "RC:SightMsg": // 小视频
            case "RC:ImgMsg": // 图片
            case "RC:GIFMsg": // gif图片
            case "RC:FileMsg": // 文件
            case "RC:ImgTextMsg": // 图文
            case "RC:StkMsg": // 表情
            case "RC:CardMsg":// 名片
            case "RC:LBSMsg": // 位置
            case "RC:CombineMsg": // 合并
            case "RC:VSTMsg": // 音视频通话
            case "RC:VCSummary": // 音视频通话
                return true;
        }
        return false;
    }

    // index: 0:逐步转发  1:合并转发
    public void forwardMessages(final int index, final List<Conversation> conversations,
                                final List<Integer> messageIds, List<Message> messages) {
        final List<Message> forwardMessages = new ArrayList<>();
        for (Message msg : messages) {
            if (messageIds.contains(msg.getMessageId())) {
                forwardMessages.add(msg);
            }
        }
        forwardMessages(index, conversations, forwardMessages);
    }

    private void forwardMessages(final int index, final List<Conversation> conversations, final List<Message> messages) {
        ExecutorHelper.getInstance().networkIO().execute(new Runnable() {
            @Override
            public void run() {
                if (index == 0) {
                    forwardMessageByStep(conversations, messages);
                } else if (index == 1) {
                    forwardMessageByCombine(conversations, messages);
                }
            }
        });
    }

    // 逐步转发
    private void forwardMessageByStep(List<Conversation> conversations, final List<Message> messages) {
        for (Conversation conversation : conversations) {
            for (Message msg : messages) {
                startForwardMessageByStep(conversation.getTargetId(), conversation.getConversationType(), msg);
                try {
                    Thread.sleep(TIME_DELAY);
                } catch (InterruptedException e) {
                    RLog.e(TAG, "forwardMessageByStep e:" + e.toString());
                    Thread.currentThread().interrupt();
                }

            }
        }

    }

    private void startForwardMessageByStep(String id, Conversation.ConversationType type, Message fwdMessage) {
        MessageContent messageContent = fwdMessage.getContent();
        //有些消息携带了用户信息，转发的消息必须把用户信息去掉
        messageContent.setUserInfo(null);
        Message message = Message.obtain(id, type, messageContent);

        if (messageContent instanceof ImageMessage) {
            ImageMessage imageMessage = (ImageMessage) messageContent;
            if (imageMessage.getRemoteUri() != null && !imageMessage.getRemoteUri().toString().startsWith("file")) {
                IMCenter.getInstance().sendMessage(message, null, null, null);
            } else {
                IMCenter.getInstance().sendMediaMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
            }
        } else if (messageContent instanceof LocationMessage) {
            IMCenter.getInstance().sendLocationMessage(message, null, null, null);
        } else if (messageContent instanceof ReferenceMessage) {
            IMCenter.getInstance().sendMessage(message, null, null, null);
        } else if (messageContent instanceof MediaMessageContent) {
            MediaMessageContent mediaMessageContent = (MediaMessageContent) messageContent;
            if (mediaMessageContent.getMediaUrl() != null) {
                IMCenter.getInstance().sendMessage(message, null, null, null);
            } else {
                IMCenter.getInstance().sendMediaMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
            }
        } else {
            IMCenter.getInstance().sendMessage(message, null, null, null);
        }
    }

    // 合并转发
    private void forwardMessageByCombine(List<Conversation> conversations, List<Message> messages) {
        //拼写H5界面,上传html文件,并回传文件地址uri
        Uri uri = CombineMessageUtils.getInstance().getUrlFromMessageList(messages);
        Conversation.ConversationType type = messages.get(0).getConversationType();

        final CombineMessage combine = CombineMessage.obtain(uri);
        combine.setConversationType(type);

        combine.setNameList(getNameList(messages, type));
        combine.setTitle(getTitle(combine));
        combine.setSummaryList(getSummaryList(messages));

        for (int i = 0; i < conversations.size(); i++) {
            Conversation conversation = conversations.get(i);
            Message message = Message.obtain(conversation.getTargetId(), conversation.getConversationType(), combine);
            IMCenter.getInstance().sendMediaMessage(message, null, null, (IRongCallback.ISendMediaMessageCallback) null);
            try {
                Thread.sleep(TIME_DELAY);
            } catch (InterruptedException e) {
                RLog.e(TAG, "forwardMessageByStep e:" + e.toString());
                Thread.currentThread().interrupt();
            }
        }
    }

    private String getTitle(CombineMessage content) {
        Context context = IMCenter.getInstance().getContext();
        String title = context.getString(R.string.rc_combine_chat_history);

        if (Conversation.ConversationType.GROUP.equals(content.getConversationType())) {
            title = context.getString(R.string.rc_combine_group_chat);
        } else if (Conversation.ConversationType.PRIVATE.equals(content.getConversationType())) {
            List<String> nameList = content.getNameList();
            if (nameList == null) return title;

            if (nameList.size() == 1) {
                title = String.format(context.getString(R.string.rc_combine_the_group_chat_of), nameList.get(0));
            } else if (nameList.size() == 2) {
                title = String.format(context.getString(R.string.rc_combine_the_group_chat_of),
                        nameList.get(0) + " " + context.getString(R.string.rc_combine_and) + " " + nameList.get(1));
            }
        }

        return title;
    }

    private List<String> getNameList(List<Message> messages, Conversation.ConversationType type) {
        List<String> names = new ArrayList<>();
        if ((Conversation.ConversationType.GROUP).equals(type)) {
            Group group = RongUserInfoManager.getInstance().getGroupInfo(messages.get(0).getTargetId());
            if (group != null) {
                String name = group.getName();
                if (!TextUtils.isEmpty(name) && !names.contains(name)) {
                    names.add(name);
                }
            }
        } else {
            for (Message msg : messages) {
                if (names.size() == 2) return names;

                UserInfo info = RongUserInfoManager.getInstance().getUserInfo(msg.getSenderUserId());
                if (info == null) {
                    RLog.d(TAG, "getNameList name is null, msg:" + msg);
                    break;
                }

                String name = info.getName();
                if (!TextUtils.isEmpty(name) && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private List<String> getSummaryList(List<Message> messages) {
        List<String> summaryList = new ArrayList<>();
        Conversation.ConversationType type = messages.get(0).getConversationType();
        for (int i = 0; i < messages.size() && i < SUMMARY_MAX_SIZE; i++) {
            Message message = messages.get(i);
            MessageContent content = message.getContent();
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            String userName = "";
            if (type.equals(Conversation.ConversationType.GROUP)) {
                GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(message.getTargetId(), message.getSenderUserId());
                if (groupUserInfo != null) {
                    userName = groupUserInfo.getNickname();
                }
            }

            if (TextUtils.isEmpty(userName) && userInfo != null) {
                userName = userInfo.getName();
            }

            String text;
            MessageTag tag = content.getClass().getAnnotation(MessageTag.class);
            String tagValue = tag != null ? tag.value() : null;
            if ("RC:CardMsg".equals(tagValue)) {
                text = IMCenter.getInstance().getContext().getString(R.string.rc_message_content_card);
            } else if ("RC:StkMsg".equals(tagValue)) {
                text = IMCenter.getInstance().getContext().getString(R.string.rc_message_content_sticker);
            } else if ("RC:VCSummary".equals(tagValue) || "RC:VSTMsg".equals(tagValue)) {
                text = IMCenter.getInstance().getContext().getString(R.string.rc_message_content_vst);
            } else if ("RCJrmf:RpMsg".equals(tagValue)) {
                text = IMCenter.getInstance().getContext().getString(R.string.rc_message_content_rp);
            } else {
                Spannable spannable = RongConfigCenter.conversationConfig().getMessageSummary(IMCenter.getInstance().getContext(), content);
                text = spannable.toString();
            }

            summaryList.add(userName + " : " + text);
        }
        return summaryList;
    }


    //todo
    public void exitDestructMode() {
        RongExtension extension = ForwardExtensionModule.sRongExtension.get();
        extension.resetToDefaultView();
    }

}
