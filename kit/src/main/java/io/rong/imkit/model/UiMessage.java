package io.rong.imkit.model;


import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.db.model.GroupMember;
import io.rong.imkit.userinfo.db.model.User;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageConfig;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.ReadReceiptInfo;
import io.rong.imlib.model.UserInfo;

public class UiMessage extends UiBaseBean {
    private Message message;
    private UserInfo userInfo;
    private @State.Value
    int state;
    private int progress;
    private String destructTime;
    private boolean isPlaying;
    private boolean isEdit;
    private boolean isSelected;

    /**
     * TextMessage 和 ReferenceMessage 的 content 字段
     */
    private SpannableStringBuilder contentSpannable;
    /**
     * ReferenceMessage 的 referMsg 为 TextMessage 时 的 content 字段
     */
    private SpannableStringBuilder referenceContentSpannable;

    public UiMessage(Message message) {
        this.message = message;
        if (message.getSentStatus() != null) {
            switch (message.getSentStatus()) {
                case SENDING:
                    state = State.PROGRESS;
                    break;
                case FAILED:
                    state = State.ERROR;
                    break;
                case CANCELED:
                    state = State.CANCEL;
                    break;
            }
        }
        initUserInfo();
        change();
    }

    public void setMessage(Message message) {
        this.message = message;
        change();
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
        change();
    }

    public void setState(int state) {
        this.state = state;
        change();
    }

    public void setProgress(int progress) {
        this.progress = progress;
        change();
    }

    public void setDestructTime(String destructTime) {
        this.destructTime = destructTime;
        change();
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        change();
    }

    public void setEdit(boolean edit) {
        this.isEdit = edit;
        change();
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        change();
    }

    public Message getMessage() {
        return message;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public int getState() {
        return state;
    }

    public int getProgress() {
        return progress;
    }

    public String getDestructTime() {
        return destructTime;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isEdit() {
        return isEdit;
    }

    public boolean isSelected() {
        return isSelected;
    }


    public void onUserInfoUpdate(List<User> userList) {
        for (User user : userList) {
            if (user.id.equals(message.getSenderUserId())) {
                if (user.name != null) {
                    userInfo.setName(user.name);
                }
                userInfo.setPortraitUri(Uri.parse(user.portraitUrl));
                userInfo.setExtra(user.extra);
                change();
                break;
            }
        }
    }

    public void onGroupMemberInfoUpdate(GroupMember member) {
        if (member.userId.equals(message.getSenderUserId())) {
            if (!TextUtils.isEmpty(member.memberName)) {
                userInfo.setName(member.memberName);
                change();
            }
        }
    }

    public void initUserInfo() {
        UserInfo user = RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
        if (user != null) {
            userInfo = user;
            if (userInfo.getName() == null) {
                userInfo.setName(message.getSenderUserId());
            }
        } else {
            userInfo = new UserInfo(message.getSenderUserId(), message.getSenderUserId(), null);
        }
        if (message.getConversationType().equals(Conversation.ConversationType.GROUP)) {
            GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(message.getTargetId(), message.getSenderUserId());
            if (groupUserInfo != null && !TextUtils.isEmpty(groupUserInfo.getNickname())) {
                userInfo.setName(groupUserInfo.getNickname());
            }
        }
    }

    public int getMessageId() {
        return message != null ? message.getMessageId() : -1;
    }

    public void setUId(String UId) {
        if (message == null) {
            return;
        }
        message.setUId(UId);
        change();
    }


    public void setConversationType(Conversation.ConversationType conversationType) {
        if (message == null) {
            return;
        }
        message.setConversationType(conversationType);
        change();
    }

    public void setTargetId(String targetId) {
        if (message == null) {
            return;
        }
        message.setTargetId(targetId);
        change();
    }

    public void setMessageId(int messageId) {
        if (message == null) {
            return;
        }
        message.setMessageId(messageId);
        change();
    }

    public void setReadTime(long readTime) {
        if (message == null) {
            return;
        }
        message.setReadTime(readTime);
        change();
    }

    public void setMessageDirection(Message.MessageDirection messageDirection) {
        if (message == null) {
            return;
        }
        message.setMessageDirection(messageDirection);
        change();
    }

    public void setReceivedStatus(Message.ReceivedStatus receivedStatus) {
        if (message == null) {
            return;
        }
        message.setReceivedStatus(receivedStatus);
        change();
    }

    public void setSentStatus(Message.SentStatus sentStatus) {
        if (message == null) {
            return;
        }
        message.setSentStatus(sentStatus);
        change();
    }

    public void setReceivedTime(long receivedTime) {
        if (message == null) {
            return;
        }
        message.setReceivedTime(receivedTime);
        change();
    }

    public void setSentTime(long sentTime) {
        if (message == null) {
            return;
        }
        message.setSentTime(sentTime);
        change();
    }

    public void setObjectName(String objectName) {
        if (message == null) {
            return;
        }
        message.setObjectName(objectName);
        change();
    }

    public void setContent(MessageContent content) {
        if (message == null) {
            return;
        }
        message.setContent(content);
        change();
    }

    public void setExtra(String extra) {
        if (message == null) {
            return;
        }
        message.setExtra(extra);
        change();
    }

    public void setSenderUserId(String senderUserId) {
        if (message == null) {
            return;
        }
        message.setSenderUserId(senderUserId);
        change();
    }

    public void setReadReceiptInfo(ReadReceiptInfo readReceiptInfo) {
        if (message == null) {
            return;
        }
        message.setReadReceiptInfo(readReceiptInfo);
        change();
    }

    public void setMessageConfig(MessageConfig messageConfig) {
        if (message == null) {
            return;
        }
        message.setMessageConfig(messageConfig);
        change();
    }

    public void setCanIncludeExpansion(boolean canIncludeExpansion) {
        if (message == null) {
            return;
        }
        message.setCanIncludeExpansion(canIncludeExpansion);
        change();
    }

    public void setExpansion(HashMap<String, String> expansion) {
        if (message == null) {
            return;
        }
        message.setExpansion(expansion);
        change();
    }

    public String getUId() {
        return message != null ? message.getUId() : null;
    }

    public Conversation.ConversationType getConversationType() {
        return message != null ? message.getConversationType() : Conversation.ConversationType.NONE;
    }

    public String getTargetId() {
        return message != null ? message.getTargetId() : null;
    }

    public long getReadTime() {
        return message != null ? message.getReadTime() : 0;
    }

    public Message.MessageDirection getMessageDirection() {
        return message != null ? message.getMessageDirection() : null;
    }

    public Message.ReceivedStatus getReceivedStatus() {
        return message != null ? message.getReceivedStatus() : null;
    }

    public Message.SentStatus getSentStatus() {
        return message != null ? message.getSentStatus() : null;
    }

    public long getReceivedTime() {
        return message != null ? message.getReceivedTime() : 0;
    }

    public long getSentTime() {
        return message != null ? message.getSentTime() : 0;
    }

    public String getObjectName() {
        return message != null ? message.getObjectName() : null;
    }

    public MessageContent getContent() {
        return message != null ? message.getContent() : null;
    }

    public String getExtra() {
        return message != null ? message.getExtra() : null;
    }


    public String getSenderUserId() {
        return message != null ? message.getSenderUserId() : null;
    }


    public ReadReceiptInfo getReadReceiptInfo() {
        return message != null ? message.getReadReceiptInfo() : null;
    }


    public MessageConfig getMessageConfig() {
        return message != null ? message.getMessageConfig() : null;
    }


    public boolean isCanIncludeExpansion() {
        return message != null && message.isCanIncludeExpansion();
    }

    public Map<String, String> getExpansion() {
        return message != null ? message.getExpansion() : null;
    }

    public SpannableStringBuilder getContentSpannable() {
        return contentSpannable;
    }

    public void setContentSpannable(SpannableStringBuilder contentSpannable) {
        this.contentSpannable = contentSpannable;
    }

    public SpannableStringBuilder getReferenceContentSpannable() {
        return referenceContentSpannable;
    }

    public void setReferenceContentSpannable(SpannableStringBuilder referenceContentSpannable) {
        this.referenceContentSpannable = referenceContentSpannable;
    }
}
