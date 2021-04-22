package io.rong.imkit.feature.mention;


import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import io.rong.common.RLog;
import io.rong.imkit.config.ConversationConfig;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.RongExtensionManager;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ReferenceMessage;
import io.rong.message.TextMessage;

public class RongMentionManager implements IExtensionEventWatcher {
    private static String TAG = "RongMentionManager";
    private Stack<MentionInstance> stack = new Stack<>();
    private IGroupMembersProvider mGroupMembersProvider;
    private IMentionedInputListener mMentionedInputListener;
    private IAddMentionedMemberListener mAddMentionedMemberListener;

    private static class SingletonHolder {
        static RongMentionManager sInstance = new RongMentionManager();
    }

    private RongMentionManager() {

    }

    public static RongMentionManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public void createInstance(Conversation.ConversationType conversationType, String targetId, EditText editText) {
        RLog.i(TAG, "createInstance");
        if (!RongConfigCenter.conversationConfig().rc_enable_mentioned_message) {
            RLog.e(TAG, "rc_enable_mentioned_message is disable");
            return;
        }
        String key = conversationType.getName() + targetId;
        MentionInstance mentionInstance;
        if (stack.size() > 0) {
            mentionInstance = stack.peek();
            if (mentionInstance.key.equals(key)) {
                return;
            }
        }

        mentionInstance = new MentionInstance();
        mentionInstance.key = key;
        mentionInstance.inputEditText = editText;
        mentionInstance.mentionBlocks = new ArrayList<>();
        stack.add(mentionInstance);
    }

    public void destroyInstance(Conversation.ConversationType conversationType, String targetId) {
        RLog.i(TAG, "destroyInstance");
        if (!RongConfigCenter.conversationConfig().rc_enable_mentioned_message) {
            RLog.e(TAG, "rc_enable_mentioned_message is disable");
            return;
        }
        if (stack.size() > 0) {
            MentionInstance instance = stack.peek();
            if (instance.key.equals(conversationType.getName() + targetId)) {
                stack.pop();
            } else {
                RLog.e(TAG, "Invalid MentionInstance : " + instance.key);
            }
        } else {
            RLog.e(TAG, "Invalid MentionInstance.");
        }
    }

    public void mentionMember(Conversation.ConversationType conversationType, String targetId, String userId) {
        RLog.d(TAG, "mentionMember " + userId);
        if (TextUtils.isEmpty(userId)
                || conversationType == null
                || TextUtils.isEmpty(targetId)
                || stack.size() == 0) {
            RLog.e(TAG, "Illegal argument");
            return;
        }
        String key = conversationType.getName() + targetId;
        MentionInstance instance = stack.peek();
        if (instance == null || !instance.key.equals(key)) {
            RLog.e(TAG, "Invalid mention instance : " + key);
            return;
        }
        UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(userId);

        if (userInfo == null || TextUtils.isEmpty(userInfo.getUserId())) {
            RLog.e(TAG, "Invalid userInfo");
            return;
        }

        if (conversationType == Conversation.ConversationType.GROUP) {
            GroupUserInfo groupUserInfo = RongUserInfoManager.getInstance().getGroupUserInfo(targetId, userId);
            if (groupUserInfo != null) {
                userInfo.setName(groupUserInfo.getNickname());
            }
        }
        addMentionedMember(userInfo, 0);
    }

    public void mentionMember(UserInfo userInfo) {
        if (userInfo == null || TextUtils.isEmpty(userInfo.getUserId())) {
            RLog.e(TAG, "Invalid userInfo");
            return;
        }
        addMentionedMember(userInfo, 1);
    }

    public String getMentionBlockInfo() {
        if (stack.size() > 0) {
            MentionInstance mentionInstance = stack.peek();
            if (mentionInstance.mentionBlocks != null && !mentionInstance.mentionBlocks.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                for (MentionBlock mentionBlock : mentionInstance.mentionBlocks) {
                    jsonArray.put(mentionBlock.toJson());
                }
                return jsonArray.toString();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    void addMentionBlock(MentionBlock mentionBlock) {
        if (stack.size() > 0) {
            MentionInstance mentionInstance = stack.peek();
            mentionInstance.mentionBlocks.add(mentionBlock);
        }
    }

    /**
     * @param userInfo 用户信息
     * @param from     0 代表来自会话界面，1 来着群成员选择界面。
     */
    private void addMentionedMember(UserInfo userInfo, int from) {
        if (stack.size() > 0) {
            MentionInstance mentionInstance = stack.peek();
            EditText editText = mentionInstance.inputEditText;
            if (userInfo != null && editText != null) {
                String mentionContent;
                mentionContent = from == 0 ? "@" + userInfo.getName() + " " : userInfo.getName() + " ";
                int len = mentionContent.length();
                int cursorPos = editText.getSelectionStart();

                MentionBlock brokenBlock = getBrokenMentionedBlock(cursorPos, mentionInstance.mentionBlocks);
                if (brokenBlock != null) {
                    mentionInstance.mentionBlocks.remove(brokenBlock);
                }

                MentionBlock mentionBlock = new MentionBlock();
                mentionBlock.userId = userInfo.getUserId();
                mentionBlock.offset = false;
                mentionBlock.name = userInfo.getName();
                if (from == 1) {
                    mentionBlock.start = cursorPos - 1;
                } else {
                    mentionBlock.start = cursorPos;
                }
                mentionBlock.end = cursorPos + len;
                mentionInstance.mentionBlocks.add(mentionBlock);

                editText.getEditableText().insert(cursorPos, mentionContent);
                editText.setSelection(cursorPos + len);
                //@某人的时候弹出软键盘
                if (mAddMentionedMemberListener != null) {
                    mAddMentionedMemberListener.onAddMentionedMember(userInfo, from);
                }
                mentionBlock.offset = true;
            }
        }
    }

    private MentionBlock getBrokenMentionedBlock(int cursorPos, List<MentionBlock> blocks) {
        MentionBlock brokenBlock = null;
        for (MentionBlock block : blocks) {
            if (block.offset && cursorPos < block.end && cursorPos > block.start) {
                brokenBlock = block;
                break;
            }
        }
        return brokenBlock;
    }

    private void offsetMentionedBlocks(int cursorPos, int offset, List<MentionBlock> blocks) {
        for (MentionBlock block : blocks) {
            if (cursorPos <= block.start && block.offset) {
                block.start += offset;
                block.end += offset;
            }
            block.offset = true;
        }
    }

    private MentionBlock getDeleteMentionedBlock(int cursorPos, List<MentionBlock> blocks) {
        MentionBlock deleteBlock = null;
        for (MentionBlock block : blocks) {
            if (cursorPos == block.end) {
                deleteBlock = block;
                break;
            }
        }
        return deleteBlock;
    }

    /**
     * 当输入框文本变化时，回调此方法。
     *
     * @param conversationType 会话类型
     * @param targetId         目标 id
     * @param cursorPos        输入文本时，光标位置初始位置
     * @param offset           文本的变化量：增加时为正数，减少是为负数
     * @param text             文本内容
     */
    @Override
    public void onTextChanged(Context context, Conversation.ConversationType conversationType, String targetId, int cursorPos, int offset, String text) {
        RLog.d(TAG, "onTextEdit " + cursorPos + ", " + text);

        if (stack == null || stack.size() == 0) {
            RLog.w(TAG, "onTextEdit ignore.");
            return;
        }
        MentionInstance mentionInstance = stack.peek();
        if (!mentionInstance.key.equals(conversationType.getName() + targetId)) {
            RLog.w(TAG, "onTextEdit ignore conversation.");
            return;
        }
        //判断单个字符是否是@
        if (offset == 1) {
            if (!TextUtils.isEmpty(text)) {
                boolean showMention = false;
                String str;
                if (cursorPos == 0) {
                    str = text.substring(0, 1);
                    showMention = str.equals("@");
                } else {
                    String preChar = text.substring(cursorPos - 1, cursorPos);
                    str = text.substring(cursorPos, cursorPos + 1);
                    if (str.equals("@") && !preChar.matches("^[a-zA-Z]*") && !preChar.matches("^\\d+$")) {
                        showMention = true;
                    }
                }
                if (showMention && (mMentionedInputListener == null
                        || !mMentionedInputListener.onMentionedInput(conversationType, targetId))) {
                    RouteUtils.routeToMentionMemberSelectActivity(context, targetId, conversationType);
                }
            }
        }

        //判断输入光标位置是否破坏已有的“@块”。
        MentionBlock brokenBlock = getBrokenMentionedBlock(cursorPos, mentionInstance.mentionBlocks);
        if (brokenBlock != null) {
            mentionInstance.mentionBlocks.remove(brokenBlock);
        }
        //改变所有有效“@块”位置。
        offsetMentionedBlocks(cursorPos, offset, mentionInstance.mentionBlocks);
    }

    @Override
    public void onSendToggleClick(Message message) {
        MessageContent messageContent = message.getContent();
        if (stack.size() > 0) {
            List<String> userIds = new ArrayList<>();
            MentionInstance curInstance = stack.peek();
            for (MentionBlock block : curInstance.mentionBlocks) {
                if (!userIds.contains(block.userId)) {
                    userIds.add(block.userId);
                }
            }
            if (userIds.size() > 0) {
                curInstance.mentionBlocks.clear();
                MentionedInfo mentionedInfo = new MentionedInfo(MentionedInfo.MentionedType.PART, userIds, null);
                messageContent.setMentionedInfo(mentionedInfo);
                message.setContent(messageContent);
            }
        }
    }

    @Override
    public void onDeleteClick(Conversation.ConversationType type, String targetId, EditText editText, int cursorPos) {
        RLog.d(TAG, "onTextEdit " + cursorPos);

        if (stack.size() > 0 && cursorPos > 0) {
            MentionInstance mentionInstance = stack.peek();
            if (mentionInstance.key.equals(type.getName() + targetId)) {
                MentionBlock deleteBlock = getDeleteMentionedBlock(cursorPos, mentionInstance.mentionBlocks);
                if (deleteBlock != null) {
                    mentionInstance.mentionBlocks.remove(deleteBlock);
                    String delText = deleteBlock.name;
                    int start = cursorPos - delText.length() - 1;
                    editText.getEditableText().delete(start, cursorPos);
                    editText.setSelection(start);
                }
            }
        }
    }

    @Override
    public void onDestroy(Conversation.ConversationType type, String targetId) {
        destroyInstance(type, targetId);
    }

    public IGroupMembersProvider getGroupMembersProvider() {
        return mGroupMembersProvider;
    }

    public void setMentionedInputListener(IMentionedInputListener listener) {
        mMentionedInputListener = listener;
    }

    public void setAddMentionedMemberListener(IAddMentionedMemberListener listener) {
        mAddMentionedMemberListener = listener;
    }

    /**
     * 设置群组成员提供者。
     * <p/>
     * '@' 功能和VoIP功能在选人界面,需要知道群组内成员信息,开发者需要设置该提供者。 开发者需要在回调中获取到群成员信息
     * 并通过{@link IGroupMemberCallback}中的方法设置到 sdk 中
     * <p/>
     *
     * @param groupMembersProvider 群组成员提供者。
     */
    public void setGroupMembersProvider(final IGroupMembersProvider groupMembersProvider) {
        mGroupMembersProvider = groupMembersProvider;
    }

    public interface IGroupMembersProvider {
        void getGroupMembers(String groupId, IGroupMemberCallback callback);
    }

    public interface IGroupMemberCallback {
        void onGetGroupMembersResult(List<UserInfo> members);
    }
}
