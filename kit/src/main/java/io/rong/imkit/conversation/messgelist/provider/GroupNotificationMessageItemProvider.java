package io.rong.imkit.conversation.messgelist.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.GroupNotificationMessageData;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.widget.adapter.IViewProviderListener;
import io.rong.imkit.widget.adapter.ViewHolder;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.GroupNotificationMessage;

public class GroupNotificationMessageItemProvider extends BaseNotificationMessageItemProvider<GroupNotificationMessage> {
    private static final String TAG = "GroupNotificationMessageItemProvider";


    @Override
    protected ViewHolder onCreateMessageContentViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rc_item_group_information_notification_message, parent, false);
        return new ViewHolder(parent.getContext(), view);
    }

    @Override
    protected void bindMessageContentViewHolder(ViewHolder holder, ViewHolder parentHolder, GroupNotificationMessage groupNotificationMessage, UiMessage uiMessage, int position, List<UiMessage> list, IViewProviderListener<UiMessage> listener) {
        try {
            if (groupNotificationMessage != null && uiMessage != null) {
                if (groupNotificationMessage.getData() == null) {
                    return;
                }
                GroupNotificationMessageData data;
                try {
                    data = jsonToBean(groupNotificationMessage.getData());
                } catch (Exception e) {
                    RLog.e(TAG, "bindView", e);
                    return;
                }
                String operation = groupNotificationMessage.getOperation();
                String operatorNickname = data.getOperatorNickname();
                String operatorUserId = groupNotificationMessage.getOperatorUserId();
                String currentUserId = RongIM.getInstance().getCurrentUserId();
                if (operatorNickname == null) {
                    UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(operatorUserId);
                    if (userInfo != null) {
                        operatorNickname = userInfo.getName();
                        if (operatorNickname == null) {
                            operatorNickname = groupNotificationMessage.getOperatorUserId();
                        }
                    }
                }
                List<String> memberList = data.getTargetUserDisplayNames();
                List<String> memberIdList = data.getTargetUserIds();
                String memberName = null;
                String memberUserId = null;
                Context context = holder.getContext();
                if (memberIdList != null) {
                    if (memberIdList.size() == 1) {
                        memberUserId = memberIdList.get(0);
                    }
                }
                if (memberList != null) {
                    if (memberList.size() == 1) {
                        memberName = memberList.get(0);
                    } else if (memberIdList != null && memberIdList.size() > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (String s : memberList) {
                            sb.append(s);
                            sb.append(context.getString(R.string.rc_item_divided_string));
                        }
                        String str = sb.toString();
                        memberName = str.substring(0, str.length() - 1);
                    }
                }

                if (!TextUtils.isEmpty(operation))
                    if (operation.equals("Add")) {
                        if (operatorUserId.equals(memberUserId)) {
                            holder.setText(R.id.rc_msg, memberName + context.getString(R.string.rc_item_join_group));
                        } else {
                            String inviteName;
                            String invitedName;
                            String inviteMsg;
                            if (memberUserId != null && memberUserId.equals(currentUserId)) {
                                invitedName = context.getString(R.string.rc_item_you);
                            } else {
                                invitedName = memberName;
                            }
                            if (!groupNotificationMessage.getOperatorUserId().equals(RongIM.getInstance().getCurrentUserId())) {
                                inviteName = operatorNickname;
                                inviteMsg = inviteName + context.getString(R.string.rc_item_invitation) + " " + invitedName + " " + context.getString(R.string.rc_join_group);
                            } else {
                                inviteMsg = context.getString(R.string.rc_item_you_invitation) + " " + invitedName + " " + context.getString(R.string.rc_join_group);
                            }
                            holder.setText(R.id.rc_msg, inviteMsg);
                        }
                    } else if (operation.equals("Kicked")) {
                        String operator;
                        String kickedName;
                        if (memberIdList != null) {
                            for (String userId : memberIdList) {
                                if (currentUserId.equals(userId)) {
                                    operator = operatorNickname;
                                    holder.setText(R.id.rc_msg, context.getString(R.string.rc_item_you_remove_self) + " " + operator
                                            + " " + context.getString(R.string.rc_item_remove));
                                } else {
                                    String removeMsg;
                                    if (!operatorUserId.equals(currentUserId)) {
                                        operator = operatorNickname;
                                        kickedName = memberName;
                                        removeMsg = operator + context.getString(R.string.rc_item_remove_group_member)
                                                + " " + kickedName + " " + context.getString(R.string.rc_item_remove);
                                    } else {
                                        kickedName = memberName;
                                        removeMsg = context.getString(R.string.rc_item_you_remove_group_member) +
                                                " " + kickedName + " " + context.getString(R.string.rc_item_remove);
                                    }
                                    holder.setText(R.id.rc_msg, removeMsg);
                                }
                            }
                        }
                    } else if (operation.equals("Create")) {
                        GroupNotificationMessageData createGroupData = new GroupNotificationMessageData();
                        try {
                            createGroupData = jsonToBean(groupNotificationMessage.getData());
                        } catch (Exception e) {
                            RLog.e(TAG, "bindView", e);
                            return;
                        }
                        String name;
                        String createMsg;
                        if (!operatorUserId.equals(currentUserId)) {
                            name = operatorNickname;
                            createMsg = name + context.getString(R.string.rc_item_created_group);
                        } else {
                            createMsg = context.getString(R.string.rc_item_you_created_group);
                        }
                        holder.setText(R.id.rc_msg, createMsg);
                    } else if (operation.equals("Dismiss")) {
                        holder.setText(R.id.rc_msg, operatorNickname + context.getString(R.string.rc_item_dismiss_groups));
                    } else if (operation.equals("Quit")) {
                        holder.setText(R.id.rc_msg, operatorNickname + context.getString(R.string.rc_item_quit_groups));
                    } else if (operation.equals("Rename")) {
                        String operator;
                        String groupName;
                        String changeMsg;
                        if (!operatorUserId.equals(currentUserId)) {
                            operator = operatorNickname;
                            groupName = data.getTargetGroupName();
                            changeMsg = operator + context.getString(R.string.rc_item_change_group_name) + "\"" + groupName + "\"";
                        } else {
                            groupName = data.getTargetGroupName();
                            changeMsg = context.getString(R.string.rc_item_you_change_group_name) + "\"" + groupName + "\"";
                        }
                        holder.setText(R.id.rc_msg, changeMsg);
                    }
            }
        } catch (Exception e) {
            RLog.e(TAG, "bindView", e);
        }
    }

    @Override
    protected boolean isMessageViewType(MessageContent messageContent) {
        return messageContent instanceof GroupNotificationMessage;
    }

    @Override
    public Spannable getSummarySpannable(Context context, GroupNotificationMessage groupNotificationMessage) {
        try {
            GroupNotificationMessageData data;
            if (groupNotificationMessage == null || groupNotificationMessage.getData() == null)
                return null;
            try {
                data = jsonToBean(groupNotificationMessage.getData());
            } catch (Exception e) {
                RLog.e(TAG, "getContentSummary", e);
                return null;
            }
            String operation = groupNotificationMessage.getOperation();
            String operatorNickname = data.getOperatorNickname();
            String operatorUserId = groupNotificationMessage.getOperatorUserId();
            String currentUserId = RongIM.getInstance().getCurrentUserId();

            if (operatorNickname == null) {
                UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(operatorUserId);
                if (userInfo != null) {
                    operatorNickname = userInfo.getName();
                }
                if (operatorNickname == null) {
                    operatorNickname = groupNotificationMessage.getOperatorUserId();
                }
            }
            List<String> memberList = data.getTargetUserDisplayNames();
            List<String> memberIdList = data.getTargetUserIds();
            String memberName = null;
            String memberUserId = null;
            if (memberIdList != null) {
                if (memberIdList.size() == 1) {
                    memberUserId = memberIdList.get(0);
                }
            }
            if (memberList != null) {
                if (memberList.size() == 1) {
                    memberName = memberList.get(0);
                } else if (memberIdList != null && memberIdList.size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (String s : memberList) {
                        sb.append(s);
                        sb.append(context.getString(R.string.rc_item_divided_string));
                    }
                    String str = sb.toString();
                    memberName = str.substring(0, str.length() - 1);
                }
            }


            SpannableString spannableStringSummary = new SpannableString("");
            switch (operation) {
                case "Add":
                    try {
                        if (operatorUserId.equals(memberUserId)) {
                            spannableStringSummary = new SpannableString(operatorNickname + context.getString(R.string.rc_item_join_group));
                        } else {
                            String inviteName;
                            String invitedName;
                            String invitationMsg;
                            if (memberUserId != null && memberUserId.equals(currentUserId)) {
                                invitedName = context.getString(R.string.rc_item_you);
                            } else {
                                invitedName = memberName;
                            }
                            if (!operatorUserId.equals(currentUserId)) {
                                inviteName = operatorNickname;
                                invitationMsg = inviteName + context.getString(R.string.rc_item_invitation) + " " + invitedName + " " + context.getString(R.string.rc_join_group);
                            } else {
                                invitationMsg = context.getString(R.string.rc_item_you_invitation) + " " + invitedName + " " + context.getString(R.string.rc_join_group);
                            }
                            spannableStringSummary = new SpannableString(invitationMsg);
                        }
                    } catch (Exception e) {
                        RLog.e(TAG, "getContentSummary", e);
                    }
                    break;
                case "Kicked": {
                    String operator;
                    String kickedName;
                    if (memberIdList != null) {
                        for (String userId : memberIdList) {
                            if (currentUserId.equals(userId)) {
                                operator = operatorNickname;
                                spannableStringSummary = new SpannableString(context.getString(R.string.rc_item_you_remove_self) + " " + operator
                                        + " " + context.getString(R.string.rc_item_remove));
                            } else {
                                String removeMsg;
                                if (!operatorUserId.equals(currentUserId)) {
                                    operator = operatorNickname;
                                    kickedName = memberName;
                                    removeMsg = operator + context.getString(R.string.rc_item_remove_group_member)
                                            + " " + kickedName + " " + context.getString(R.string.rc_item_remove);
                                } else {
                                    kickedName = memberName;
                                    removeMsg = context.getString(R.string.rc_item_you_remove_group_member) +
                                            " " + kickedName + " " + context.getString(R.string.rc_item_remove);
                                }
                                spannableStringSummary = new SpannableString(removeMsg);
                            }
                        }
                    }
                    break;
                }
                case "Create":
                    String name;
                    String createMsg;
                    if (!operatorUserId.equals(currentUserId)) {
                        name = operatorNickname;
                        createMsg = name + context.getString(R.string.rc_item_created_group);
                    } else {
                        createMsg = context.getString(R.string.rc_item_you_created_group);
                    }
                    spannableStringSummary = new SpannableString(createMsg);

                    break;
                case "Dismiss":
                    spannableStringSummary = new SpannableString(operatorNickname + context.getString(R.string.rc_item_dismiss_groups));
                    break;
                case "Quit":
                    spannableStringSummary = new SpannableString(operatorNickname + context.getString(R.string.rc_item_quit_groups));
                    break;
                case "Rename": {
                    String operator;
                    String groupName;
                    String changeMsg;
                    if (!operatorUserId.equals(currentUserId)) {
                        operator = operatorNickname;
                        groupName = data.getTargetGroupName();
                        changeMsg = operator + context.getString(R.string.rc_item_change_group_name) + "\"" + groupName + "\"";
                    } else {
                        groupName = data.getTargetGroupName();
                        changeMsg = context.getString(R.string.rc_item_you_change_group_name) + "\"" + groupName + "\"";
                    }
                    spannableStringSummary = new SpannableString(changeMsg);
                    break;
                }
            }

            return spannableStringSummary;
        } catch (Exception e) {
            RLog.e(TAG, "getContentSummary", e);
        }
        return new SpannableString(context.getString(R.string.rc_item_group_notification_summary));
    }

    private GroupNotificationMessageData jsonToBean(String data) {
        GroupNotificationMessageData dataEntity = new GroupNotificationMessageData();
        try {
            JSONObject jsonObject = new JSONObject(data);
            if (jsonObject.has("operatorNickname")) {
                dataEntity.setOperatorNickname(jsonObject.getString("operatorNickname"));
            }
            if (jsonObject.has("targetGroupName")) {
                dataEntity.setTargetGroupName(jsonObject.getString("targetGroupName"));
            }
            if (jsonObject.has("timestamp")) {
                dataEntity.setTimestamp(jsonObject.getLong("timestamp"));
            }
            if (jsonObject.has("targetUserIds")) {
                JSONArray jsonArray = jsonObject.getJSONArray("targetUserIds");
                for (int i = 0; i < jsonArray.length(); i++) {
                    dataEntity.getTargetUserIds().add(jsonArray.getString(i));
                }
            }
            if (jsonObject.has("targetUserDisplayNames")) {
                JSONArray jsonArray = jsonObject.getJSONArray("targetUserDisplayNames");
                for (int i = 0; i < jsonArray.length(); i++) {
                    dataEntity.getTargetUserDisplayNames().add(jsonArray.getString(i));
                }
            }
            if (jsonObject.has("oldCreatorId")) {
                dataEntity.setOldCreatorId(jsonObject.getString("oldCreatorId"));
            }
            if (jsonObject.has("oldCreatorName")) {
                dataEntity.setOldCreatorName(jsonObject.getString("oldCreatorName"));
            }
            if (jsonObject.has("newCreatorId")) {
                dataEntity.setNewCreatorId(jsonObject.getString("newCreatorId"));
            }
            if (jsonObject.has("newCreatorName")) {
                dataEntity.setNewCreatorName(jsonObject.getString("newCreatorName"));
            }

        } catch (Exception e) {
            RLog.e(TAG, "jsonToBean", e);
        }
        return dataEntity;
    }
}
