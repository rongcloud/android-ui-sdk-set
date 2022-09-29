package io.rong.imkit.notification;

import android.text.TextUtils;
import io.rong.imlib.ChannelClient;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;

public class MessageNotificationHelper {
    private static final ConcurrentHashMap<String, Integer> levelMap = new ConcurrentHashMap<>();
    private static Integer quietLevel = null;
    private static String mStartTime;
    private static int mSpanTime;
    private static NotifyListener mNotifyListener;

    public static void updateLevelMap(Conversation conversation) {
        String targetId =
                TextUtils.isEmpty(conversation.getTargetId()) ? "" : conversation.getTargetId();
        String channelId =
                TextUtils.isEmpty(conversation.getChannelId()) ? "" : conversation.getChannelId();

        levelMap.put(
                conversation.getConversationType().getValue() + ";;;" + targetId + ";" + channelId,
                conversation.getPushNotificationLevel());
    }

    public static void updateLevelMap(String key, int level) {
        levelMap.put(key, level);
    }

    public static void updateQuietHour(int level, String startTime, int spanTime) {
        quietLevel = level;
        mStartTime = startTime;
        mSpanTime = spanTime;
    }

    public static void clearCache() {
        levelMap.clear();
    }

    private static boolean containsLevelMap(Message message) {
        String targetId = TextUtils.isEmpty(message.getTargetId()) ? "" : message.getTargetId();
        String channelId = TextUtils.isEmpty(message.getChannelId()) ? "" : message.getChannelId();
        String key = message.getConversationType().getValue() + ";;;" + targetId + ";" + channelId;
        Integer level = levelMap.get(key);
        if (levelMap.containsKey(key)
                && level != null
                && level
                        != IRongCoreEnum.PushNotificationLevel.PUSH_NOTIFICATION_LEVEL_DEFAULT
                                .getValue()) {
            notifyMessage(level, message);
            return true;
        }
        return false;
    }

    public static void getNotificationQuietHoursLevel(
            RongIMClient.GetNotificationQuietHoursCallback callback) {
        if (quietLevel != null && callback != null) {
            callback.onSuccess(mStartTime, mSpanTime);
        } else {
            ChannelClient.getInstance()
                    .getNotificationQuietHoursLevel(
                            new IRongCoreCallback.GetNotificationQuietHoursCallbackEx() {
                                @Override
                                public void onSuccess(
                                        String startTime,
                                        int spanMinutes,
                                        IRongCoreEnum.PushNotificationQuietHoursLevel level) {
                                    mStartTime = startTime;
                                    mSpanTime = spanMinutes;
                                    quietLevel = level.getValue();
                                    if (callback != null) {
                                        callback.onSuccess(startTime, spanMinutes);
                                    }
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode coreErrorCode) {
                                    if (callback != null) {
                                        callback.onError(
                                                RongIMClient.ErrorCode.valueOf(coreErrorCode.code));
                                    }
                                }
                            });
        }
    }

    public static void getNotificationQuietHoursLevel(Message message) {
        if (message == null || message.getContent() == null) {
            return;
        }

        if (isInQuietTime(mStartTime, mSpanTime)) {
            if (quietLevel
                    == IRongCoreEnum.PushNotificationQuietHoursLevel
                            .PUSH_NOTIFICATION_QUIET_HOURS_LEVEL_BLOCKED
                            .getValue()) {
                return;
            }

            if (quietLevel
                    == IRongCoreEnum.PushNotificationQuietHoursLevel
                            .PUSH_NOTIFICATION_QUIET_HOURS_LEVEL_DEFAULT
                            .getValue()) {
                getConversationChannelNotificationLevel(message);
                return;
            }

            boolean notify = checkQuietHourAbility(message);
            if (notify) {
                if (mNotifyListener != null) {
                    mNotifyListener.onPreToNotify(message);
                }
            }
        } else {
            getConversationChannelNotificationLevel(message);
        }
    }

    public static void getConversationChannelNotificationLevel(Message message) {
        if (message == null || message.getContent() == null) {
            return;
        }

        if (containsLevelMap(message)) {
            return;
        }
        ChannelClient.getInstance()
                .getConversationChannelNotificationLevel(
                        message.getConversationType(),
                        message.getTargetId(),
                        message.getChannelId(),
                        new IRongCoreCallback.ResultCallback<
                                IRongCoreEnum.PushNotificationLevel>() {
                            @Override
                            public void onSuccess(IRongCoreEnum.PushNotificationLevel level) {
                                updateLevelMap(
                                        message.getConversationType().getValue()
                                                + ";;;"
                                                + message.getTargetId()
                                                + ";"
                                                + message.getChannelId(),
                                        level.getValue());
                                if (IRongCoreEnum.PushNotificationLevel
                                        .PUSH_NOTIFICATION_LEVEL_DEFAULT
                                        .equals(level)) {
                                    getConversationNotificationLevel(message);
                                    return;
                                }

                                notifyMessage(level.getValue(), message);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                getConversationNotificationLevel(message);
                            }
                        });
    }

    private static void notifyMessage(int level, Message message) {
        boolean notify = checkNotifyAbility(message, level);
        if (notify) {
            if (mNotifyListener != null) {
                mNotifyListener.onPreToNotify(message);
            }
        }
    }

    public static void getConversationNotificationLevel(Message message) {
        if (message == null || message.getContent() == null) {
            return;
        }

        if (containsLevelMap(message)) {
            return;
        }
        ChannelClient.getInstance()
                .getConversationNotificationLevel(
                        message.getConversationType(),
                        message.getTargetId(),
                        new IRongCoreCallback.ResultCallback<
                                IRongCoreEnum.PushNotificationLevel>() {
                            @Override
                            public void onSuccess(IRongCoreEnum.PushNotificationLevel level) {
                                updateLevelMap(
                                        message.getConversationType().getValue()
                                                + ";;;"
                                                + message.getTargetId()
                                                + ";"
                                                + "",
                                        level.getValue());
                                if (IRongCoreEnum.PushNotificationLevel
                                        .PUSH_NOTIFICATION_LEVEL_DEFAULT
                                        .equals(level)) {
                                    getConversationTypeNotificationLevel(message);
                                    return;
                                }

                                notifyMessage(level.getValue(), message);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {
                                getConversationTypeNotificationLevel(message);
                            }
                        });
    }

    public static void getConversationTypeNotificationLevel(Message message) {
        if (message == null || message.getContent() == null) {
            return;
        }

        if (containsLevelMap(message)) {
            return;
        }

        ChannelClient.getInstance()
                .getConversationTypeNotificationLevel(
                        message.getConversationType(),
                        new IRongCoreCallback.ResultCallback<
                                IRongCoreEnum.PushNotificationLevel>() {
                            @Override
                            public void onSuccess(IRongCoreEnum.PushNotificationLevel level) {
                                updateLevelMap(
                                        message.getConversationType().getValue() + ";;;" + "" + ";",
                                        level.getValue());
                                if (IRongCoreEnum.PushNotificationLevel
                                        .PUSH_NOTIFICATION_LEVEL_DEFAULT
                                        .equals(level)) {
                                    if (mNotifyListener != null) {
                                        mNotifyListener.onPreToNotify(message);
                                    }
                                    return;
                                }

                                notifyMessage(level.getValue(), message);
                            }

                            @Override
                            public void onError(IRongCoreEnum.CoreErrorCode e) {}
                        });
    }

    private static boolean checkNotifyAbility(Message message, int level) {
        MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
        if (IRongCoreEnum.PushNotificationLevel.PUSH_NOTIFICATION_LEVEL_ALL_MESSAGE.getValue()
                == level) {
            return true;
        }

        if (IRongCoreEnum.PushNotificationLevel.PUSH_NOTIFICATION_LEVEL_BLOCKED.getValue()
                == level) {
            return false;
        }

        if (IRongCoreEnum.PushNotificationLevel.PUSH_NOTIFICATION_LEVEL_MENTION.getValue()
                == level) {
            if (Conversation.ConversationType.ULTRA_GROUP.equals(message.getConversationType())
                    || Conversation.ConversationType.GROUP.equals(message.getConversationType())) {
                return mentionedInfo != null
                        && (MentionedInfo.MentionedType.ALL.equals(mentionedInfo.getType())
                                || (MentionedInfo.MentionedType.PART.equals(
                                                mentionedInfo.getType()))
                                        && mentionedInfo.getMentionedUserIdList() != null
                                        && mentionedInfo
                                                .getMentionedUserIdList()
                                                .contains(
                                                        RongIMClient.getInstance()
                                                                .getCurrentUserId()));
            }

            if (Conversation.ConversationType.PRIVATE.equals(message.getConversationType())
                    || Conversation.ConversationType.SYSTEM.equals(message.getConversationType())) {
                return false;
            }
        }

        if (IRongCoreEnum.PushNotificationLevel.PUSH_NOTIFICATION_LEVEL_MENTION_USERS.getValue()
                == level) {
            return mentionedInfo != null
                    && MentionedInfo.MentionedType.PART.equals(mentionedInfo.getType())
                    && mentionedInfo.getMentionedUserIdList() != null
                    && mentionedInfo
                            .getMentionedUserIdList()
                            .contains(RongIMClient.getInstance().getCurrentUserId());
        }

        if (IRongCoreEnum.PushNotificationLevel.PUSH_NOTIFICATION_LEVEL_MENTION_ALL.getValue()
                == level) {
            return mentionedInfo != null
                    && MentionedInfo.MentionedType.ALL.equals(mentionedInfo.getType());
        }
        return false;
    }

    private static boolean checkQuietHourAbility(Message message) {
        MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();

        if (Conversation.ConversationType.PRIVATE.equals(message.getConversationType())
                || Conversation.ConversationType.SYSTEM.equals(message.getConversationType())) {
            return false;
        }

        if (Conversation.ConversationType.ULTRA_GROUP.equals(message.getConversationType())
                || Conversation.ConversationType.GROUP.equals(message.getConversationType())) {
            return mentionedInfo != null
                    && (MentionedInfo.MentionedType.ALL.equals(mentionedInfo.getType())
                            || (MentionedInfo.MentionedType.PART).equals(mentionedInfo.getType()));
        }
        return false;
    }

    private static boolean isInQuietTime(String startTime, int spanMinutes) {
        int hour = -1;
        int minute = -1;
        int second = -1;

        if (!TextUtils.isEmpty(startTime) && startTime.contains(":")) {
            String[] time = startTime.split(":");

            try {
                if (time.length >= 3) {
                    hour = Integer.parseInt(time[0]);
                    minute = Integer.parseInt(time[1]);
                    second = Integer.parseInt(time[2]);
                }
            } catch (NumberFormatException e) {
                // todo
            }
        }

        if (hour == -1 || minute == -1 || second == -1) {
            return false;
        }

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.HOUR_OF_DAY, hour);
        startCalendar.set(Calendar.MINUTE, minute);
        startCalendar.set(Calendar.SECOND, second);

        long start = startCalendar.getTimeInMillis();

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(start + (long) spanMinutes * 60 * 1000);

        Calendar currentCalendar = Calendar.getInstance();
        // 免打扰时段分为 不跨天（比如12：00--14：00）和 跨天（比如22：00 -- 第二天07：00）两种情况，不跨天走if里的逻辑，跨天走else里的逻辑
        if (currentCalendar.get(Calendar.DAY_OF_MONTH) == endCalendar.get(Calendar.DAY_OF_MONTH)) {

            return currentCalendar.after(startCalendar) && currentCalendar.before(endCalendar);
        } else {

            // 跨天 且 currentCalendar 在 startCalendar 之前 ，需要判断 currentCalendar 是否在00：00到 endCalendar
            // 之间
            if (currentCalendar.before(startCalendar)) {

                endCalendar.add(Calendar.DAY_OF_MONTH, -1); // 将endCalendar日期-1 ，再与currentCalendar比较

                return currentCalendar.before(endCalendar);
            } else {
                // 跨天 且 currentCalendar 在 startCalendar 之后，则当前时间一定在免打扰时段，return true
                return true;
            }
        }
    }

    static void setNotifyListener(NotifyListener notifyListener) {
        mNotifyListener = notifyListener;
    }

    interface NotifyListener {
        void onPreToNotify(Message message);
    }
}
