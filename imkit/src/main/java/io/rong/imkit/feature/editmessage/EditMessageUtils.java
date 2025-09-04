package io.rong.imkit.feature.editmessage;

import android.content.Context;
import android.text.Spannable;
import android.text.TextUtils;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.feature.mention.MentionBlock;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.StringUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.FileMessage;
import io.rong.message.RecallNotificationMessage;
import io.rong.message.ReferenceMessage;
import io.rong.message.RichContentMessage;
import io.rong.message.TextMessage;
import java.util.ArrayList;
import java.util.List;

public class EditMessageUtils {

    private static final String TAG = "EditMessageUtils";

    static String getDisplayName(Message message) {
        if (message == null || TextUtils.isEmpty(message.getSenderUserId())) {
            return "";
        }
        String sendId = "";
        MessageContent content = null;
        if (message.getContent() instanceof ReferenceMessage) {
            ReferenceMessage reference = (ReferenceMessage) message.getContent();
            sendId = reference.getUserId();
            content = reference;
        } else if (message.getContent() instanceof RecallNotificationMessage) {
            RecallNotificationMessage recall = (RecallNotificationMessage) message.getContent();
            sendId = message.getSenderUserId();
            content = recall.getOriginalMessageContent();
        } else if (message.getContent() instanceof TextMessage) {
            sendId = message.getSenderUserId();
            content = message.getContent();
        }
        if (content == null) {
            return "";
        }
        UserInfo userInfo = getUserInfo(sendId, content);
        String groupMemberName = "";
        if (message.getConversationType().equals(Conversation.ConversationType.GROUP)) {
            GroupUserInfo groupUserInfo =
                    RongUserInfoManager.getInstance()
                            .getGroupUserInfo(message.getTargetId(), sendId);
            groupMemberName = groupUserInfo != null ? groupUserInfo.getNickname() : "";
        }
        return RongUserInfoManager.getInstance().getUserDisplayName(userInfo, groupMemberName);
    }

    private static UserInfo getUserInfo(String userId, MessageContent messageContent) {
        boolean isInfoManagement =
                RongUserInfoManager.getInstance().getDataSourceType()
                        == RongUserInfoManager.DataSourceType.INFO_MANAGEMENT;
        if (isInfoManagement
                && messageContent != null
                && messageContent.getUserInfo() != null
                && messageContent.getUserInfo().getUserId() != null
                && messageContent.getUserInfo().getUserId().equals(userId)) {
            return messageContent.getUserInfo();
        }
        return RongUserInfoManager.getInstance().getUserInfo(userId);
    }

    static List<MentionBlock> getMentionBlocks(UiMessage uiMessage) {
        String content = "";
        if (uiMessage.getContent() instanceof TextMessage) {
            content = ((TextMessage) uiMessage.getContent()).getContent();
        } else if (uiMessage.getContent() instanceof ReferenceMessage) {
            content = ((ReferenceMessage) uiMessage.getContent()).getEditSendText();
        }
        if (TextUtils.isEmpty(content)) {
            return new ArrayList<>();
        }

        MentionedInfo mentionedInfo = uiMessage.getContent().getMentionedInfo();
        if (mentionedInfo == null) {
            return new ArrayList<>();
        }
        List<String> mentionedUserIdList = new ArrayList<>();
        if (mentionedInfo.getMentionedUserIdList() != null
                && !mentionedInfo.getMentionedUserIdList().isEmpty()) {
            mentionedUserIdList.addAll(mentionedInfo.getMentionedUserIdList());
        }
        if (mentionedInfo.getType() == MentionedInfo.MentionedType.ALL) {
            boolean hasAll = false;
            if (!mentionedUserIdList.isEmpty()) {
                for (String uid : mentionedUserIdList) {
                    if (TextUtils.equals(uid, "-1")) {
                        hasAll = true;
                    }
                }
            }
            if (!hasAll) {
                mentionedUserIdList.add("-1");
            }
        }
        if (mentionedUserIdList.isEmpty()) {
            return new ArrayList<>();
        }
        String targetId = uiMessage.getTargetId();
        Conversation.ConversationType type = uiMessage.getConversationType();
        List<MentionBlock> mentionBlocks = new ArrayList<>();

        // 记录已处理的区间，避免重复匹配 - 使用二维数组存储[start, end]
        List<int[]> processedRanges = new ArrayList<>();

        for (String uid : mentionedUserIdList) {
            // 获取@的用户名
            String mentionUserName = EditMessageUtils.getMentionUserName(type, targetId, uid);
            // 检查用户名是否为空
            if (TextUtils.isEmpty(mentionUserName)) {
                RLog.e(TAG, "Empty user name for uid: " + uid);
                continue;
            }

            String searchPattern = "@" + mentionUserName + " ";
            int searchIndex = 0;

            // 查找所有匹配的位置
            while (searchIndex < content.length()) {
                int foundIndex = content.indexOf(searchPattern, searchIndex);
                if (foundIndex == -1) {
                    break; // 没有找到更多匹配
                }

                int foundEnd = foundIndex + searchPattern.length();

                // 检查这个位置是否已经被处理过（区间重叠检查）
                boolean alreadyProcessed = false;
                for (int[] processedRange : processedRanges) {
                    int processedStart = processedRange[0];
                    int processedEnd = processedRange[1];
                    // 检查两个区间是否重叠：foundIndex < processedEnd && foundEnd > processedStart
                    if (foundIndex < processedEnd && foundEnd > processedStart) {
                        alreadyProcessed = true;
                        break;
                    }
                }

                if (!alreadyProcessed) {
                    // 创建新的MentionBlock
                    MentionBlock block = new MentionBlock();
                    block.userId = uid;
                    block.name = mentionUserName;
                    block.offset = true;
                    block.start = foundIndex;
                    block.end = foundEnd;
                    mentionBlocks.add(block);

                    // 记录已处理的区间
                    processedRanges.add(new int[] {foundIndex, foundEnd});
                }

                // 移动到下一个搜索位置，避免重复匹配
                searchIndex = foundIndex + searchPattern.length();
            }
        }

        return mentionBlocks;
    }

    // 返回Mention中userID对应的名字，@所有人则返回"所有人"。
    private static String getMentionUserName(
            Conversation.ConversationType type, String targetId, String uid) {
        if (type == Conversation.ConversationType.GROUP) {
            if (TextUtils.equals(uid, "-1")) {
                return "";
            }
            GroupUserInfo groupUserInfo =
                    RongUserInfoManager.getInstance().getGroupUserInfo(targetId, uid);
            if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                return groupUserInfo.getNickname();
            }
        }
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(uid);
        if (userInfo == null
                || TextUtils.isEmpty(userInfo.getUserId())
                || userInfo.getName() == null) {
            return "";
        }
        return userInfo.getName();
    }

    static String getReferContent(Context context, Message message) {
        if (context == null
                || message == null
                || !(message.getContent() instanceof ReferenceMessage)) {
            return "";
        }
        ReferenceMessage referenceMessage = (ReferenceMessage) message.getContent();
        ReferenceMessage.ReferenceMessageStatus status = referenceMessage.getReferMsgStatus();
        if (ReferenceMessage.ReferenceMessageStatus.DELETE == status) {
            return context.getString(R.string.rc_reference_status_delete);
        } else if (ReferenceMessage.ReferenceMessageStatus.RECALLED == status) {
            return context.getString(R.string.rc_reference_status_recall);
        }
        MessageContent content = referenceMessage.getReferenceContent();
        Spannable messageSummary =
                RongConfigCenter.conversationConfig().getMessageSummary(context, content);
        if (content instanceof FileMessage) {
            return messageSummary.toString();
        } else if (content instanceof RichContentMessage) {
            return messageSummary.toString() + ((RichContentMessage) content).getTitle();
        } else {
            return StringUtils.getStringNoBlank(messageSummary.toString());
        }
    }

    static String getOriginalContent(Message message) {
        if (message.getContent() instanceof TextMessage) {
            return ((TextMessage) message.getContent()).getContent();
        }
        if (message.getContent() instanceof ReferenceMessage) {
            return ((ReferenceMessage) message.getContent()).getEditSendText();
        }
        return "";
    }

    static String getReferUid(Message message) {
        if (message == null || !(message.getContent() instanceof ReferenceMessage)) {
            return "";
        }
        return ((ReferenceMessage) message.getContent()).getReferMsgUid();
    }
}
