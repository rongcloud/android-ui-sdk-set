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
    private String nickname;
    /**
     * TextMessage 和 ReferenceMessage 的 content 字段
     */
    private SpannableStringBuilder contentSpannable;
    /**
     * ReferenceMessage 的 referMsg 为 TextMessage 时 的 content 字段
     */
    private SpannableStringBuilder referenceContentSpannable;

    public UiMessage(Message message) {
        setMessage(message);
        initUserInfo();
        change();
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
                nickname = groupUserInfo.getNickname();
            }
        }
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
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
        change();
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
        change();
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        change();
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        change();
    }

    public String getDestructTime() {
        return destructTime;
    }

    public void setDestructTime(String destructTime) {
        this.destructTime = destructTime;
        change();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        change();
    }

    public boolean isEdit() {
        return isEdit;
    }

    public void setEdit(boolean edit) {
        this.isEdit = edit;
        change();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        change();
    }

    public void onUserInfoUpdate(List<User> userList) {
        for (User user : userList) {
            if (user.id.equals(message.getSenderUserId())) {
                if (user.name != null) {
                    userInfo.setName(user.name);
                } else {
                    userInfo.setName("");
                }
                userInfo.setAlias(user.alias);
                userInfo.setPortraitUri(Uri.parse(user.portraitUrl));
                userInfo.setExtra(user.extra);
                change();
                break;
            }
        }
    }

    public void onGroupMemberInfoUpdate(GroupMember member) {
        if (member.userId.equals(message.getSenderUserId())) {
            nickname = member.memberName;
            change();
        }
    }

    public int getMessageId() {
        return message != null ? message.getMessageId() : -1;
    }

    public void setMessageId(int messageId) {
        if (message == null) {
            return;
        }
        message.setMessageId(messageId);
        change();
    }

    public String getUId() {
        return message != null ? message.getUId() : null;
    }

    public void setUId(String UId) {
        if (message == null) {
            return;
        }
        message.setUId(UId);
        change();
    }

    public Conversation.ConversationType getConversationType() {
        return message != null ? message.getConversationType() : Conversation.ConversationType.NONE;
    }

    public void setConversationType(Conversation.ConversationType conversationType) {
        if (message == null) {
            return;
        }
        message.setConversationType(conversationType);
        change();
    }

    public String getTargetId() {
        return message != null ? message.getTargetId() : null;
    }

    public void setTargetId(String targetId) {
        if (message == null) {
            return;
        }
        message.setTargetId(targetId);
        change();
    }

    public long getReadTime() {
        return message != null ? message.getReadTime() : 0;
    }

    public void setReadTime(long readTime) {
        if (message == null) {
            return;
        }
        message.setReadTime(readTime);
        change();
    }

    public Message.MessageDirection getMessageDirection() {
        return message != null ? message.getMessageDirection() : null;
    }

    public void setMessageDirection(Message.MessageDirection messageDirection) {
        if (message == null) {
            return;
        }
        message.setMessageDirection(messageDirection);
        change();
    }

    public Message.ReceivedStatus getReceivedStatus() {
        return message != null ? message.getReceivedStatus() : null;
    }

    public void setReceivedStatus(Message.ReceivedStatus receivedStatus) {
        if (message == null) {
            return;
        }
        message.setReceivedStatus(receivedStatus);
        change();
    }

    public Message.SentStatus getSentStatus() {
        return message != null ? message.getSentStatus() : null;
    }

    public void setSentStatus(Message.SentStatus sentStatus) {
        if (message == null) {
            return;
        }
        message.setSentStatus(sentStatus);
        change();
    }

    public long getReceivedTime() {
        return message != null ? message.getReceivedTime() : 0;
    }

    public void setReceivedTime(long receivedTime) {
        if (message == null) {
            return;
        }
        message.setReceivedTime(receivedTime);
        change();
    }

    public long getSentTime() {
        return message != null ? message.getSentTime() : 0;
    }

    public void setSentTime(long sentTime) {
        if (message == null) {
            return;
        }
        message.setSentTime(sentTime);
        change();
    }

    public String getObjectName() {
        return message != null ? message.getObjectName() : null;
    }

    public void setObjectName(String objectName) {
        if (message == null) {
            return;
        }
        message.setObjectName(objectName);
        change();
    }

    public MessageContent getContent() {
        return message != null ? message.getContent() : null;
    }

    public void setContent(MessageContent content) {
        if (message == null) {
            return;
        }
        message.setContent(content);
        change();
    }

    public String getExtra() {
        return message != null ? message.getExtra() : null;
    }

    public void setExtra(String extra) {
        if (message == null) {
            return;
        }
        message.setExtra(extra);
        change();
    }

    public String getSenderUserId() {
        return message != null ? message.getSenderUserId() : null;
    }

    public void setSenderUserId(String senderUserId) {
        if (message == null) {
            return;
        }
        message.setSenderUserId(senderUserId);
        change();
    }

    public ReadReceiptInfo getReadReceiptInfo() {
        return message != null ? message.getReadReceiptInfo() : null;
    }

    public void setReadReceiptInfo(ReadReceiptInfo readReceiptInfo) {
        if (message == null) {
            return;
        }
        message.setReadReceiptInfo(readReceiptInfo);
        change();
    }

    public MessageConfig getMessageConfig() {
        return message != null ? message.getMessageConfig() : null;
    }

    public void setMessageConfig(MessageConfig messageConfig) {
        if (message == null) {
            return;
        }
        message.setMessageConfig(messageConfig);
        change();
    }

    public boolean isCanIncludeExpansion() {
        return message != null && message.isCanIncludeExpansion();
    }

    public void setCanIncludeExpansion(boolean canIncludeExpansion) {
        if (message == null) {
            return;
        }
        message.setCanIncludeExpansion(canIncludeExpansion);
        change();
    }

    public Map<String, String> getExpansion() {
        return message != null ? message.getExpansion() : null;
    }

    public void setExpansion(HashMap<String, String> expansion) {
        if (message == null) {
            return;
        }
        message.setExpansion(expansion);
        change();
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }


    public String getDisplayName() {
        return RongUserInfoManager.getInstance().getUserDisplayName(userInfo, nickname);
    }
}
