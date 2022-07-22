package io.rong.imkit.notification;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.RongConversationActivity;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.userinfo.model.GroupUserInfo;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.cache.RongCache;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationStatus;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.MentionedInfo;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageConfig;
import io.rong.imlib.model.MessagePushConfig;
import io.rong.imlib.model.UserInfo;
import io.rong.message.RecallNotificationMessage;
import io.rong.push.common.PushCacheHelper;
import io.rong.push.notification.RongNotificationHelper;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RongNotificationManager implements RongUserInfoManager.UserDataObserver {
    // 应用在前台，如果没有在会话界面，收消息时每间隔 3s 一次响铃、震动。
    private static final int SOUND_INTERVAL = 3000;
    private static boolean mIsInForeground;
    private final String TAG = this.getClass().getSimpleName();
    private final int MAX_NOTIFICATION_STATUS_CACHE = 128;
    private Application mApplication;
    private RongCache<String, Conversation.ConversationNotificationStatus> mNotificationCache;
    private boolean isQuietSettingSynced = false;
    private String mQuietStartTime; // 通知免打扰起始时间
    private int mQuietSpanTime; // 通知免打扰间隔时间
    private int requestCode = 1000;
    private long mLastSoundTime = 0;
    private Activity mTopForegroundActivity;
    private ConcurrentHashMap<String, Message> messageMap = new ConcurrentHashMap<>();
    private MediaPlayer mediaPlayer;

    private RongNotificationManager() {}

    public static RongNotificationManager getInstance() {
        return SingletonHolder.sInstance;
    }

    public void init(Application application) {
        messageMap.clear();
        mApplication = application;
        IMCenter.getInstance()
                .addConversationStatusListener(
                        new RongIMClient.ConversationStatusListener() {
                            @Override
                            public void onStatusChanged(ConversationStatus[] conversationStatuses) {
                                String stateType = "";
                                String key = "";
                                if (conversationStatuses != null
                                        && conversationStatuses.length > 0) {
                                    for (ConversationStatus status : conversationStatuses) {
                                        if (status.getStatus() == null) {
                                            continue;
                                        }
                                        for (Map.Entry<String, String> entry :
                                                status.getStatus().entrySet()) {
                                            stateType = entry.getKey();
                                            if (!TextUtils.isEmpty(stateType)) {
                                                break;
                                            }
                                        }
                                        key =
                                                getKey(
                                                        status.getTargetId(),
                                                        status.getConversationType(),
                                                        stateType);
                                        mNotificationCache.put(key, status.getNotifyStatus());
                                    }
                                }
                            }
                        });
        mNotificationCache = new RongCache<>(MAX_NOTIFICATION_STATUS_CACHE);
        getNotificationQuietHours(null);
        MessageNotificationHelper.setNotifyListener(
                new MessageNotificationHelper.NotifyListener() {
                    @Override
                    public void onPreToNotify(Message message) {
                        if (message == null) {
                            return;
                        }
                        preToNotify(message);
                    }
                });
        IMCenter.getInstance()
                .addOnReceiveMessageListener(
                        new RongIMClient.OnReceiveMessageWrapperListener() {
                            @Override
                            public boolean onReceived(
                                    final Message message,
                                    int left,
                                    boolean hasPackage,
                                    boolean offline) {
                                RLog.d(
                                        TAG,
                                        "onReceived. uid:"
                                                + message.getUId()
                                                + "; offline:"
                                                + offline);
                                if (shouldNotify(message, left, hasPackage, offline)) {
                                    // 高优先级消息不受免打扰和会话通知状态控制
                                    NotificationConfig.Interceptor interceptor =
                                            RongConfigCenter.notificationConfig().getInterceptor();
                                    if (interceptor != null) {
                                        boolean high = interceptor.isHighPriorityMessage(message);
                                        if (high) {
                                            preToNotify(message);
                                            return false;
                                        }
                                    }
                                    MessageNotificationHelper.getNotificationQuietHoursLevel(
                                            message);
                                }
                                return false;
                            }
                        });
        IMCenter.getInstance()
                .addOnRecallMessageListener(
                        new RongIMClient.OnRecallMessageListener() {
                            @Override
                            public boolean onMessageRecalled(
                                    final Message message,
                                    RecallNotificationMessage recallNotificationMessage) {
                                if (!isRecallFiltered(message)) {
                                    getConversationNotificationStatus(
                                            message.getConversationType(),
                                            message.getTargetId(),
                                            new RongIMClient.ResultCallback<
                                                    Conversation.ConversationNotificationStatus>() {
                                                @Override
                                                public void onSuccess(
                                                        Conversation.ConversationNotificationStatus
                                                                conversationNotificationStatus) {
                                                    if (conversationNotificationStatus.equals(
                                                            Conversation
                                                                    .ConversationNotificationStatus
                                                                    .NOTIFY)) {
                                                        preToNotify(message);
                                                    }
                                                }

                                                @Override
                                                public void onError(
                                                        RongIMClient.ErrorCode errorCode) {}
                                            });
                                }
                                return false;
                            }
                        });
        RongUserInfoManager.getInstance().addUserDataObserver(this);
        registerActivityLifecycleCallback();
    }

    void preToNotify(Message message) {
        if (!mIsInForeground) {
            prepareToSendNotification(message);
        } else if (!isInConversationPage()) {
            NotificationConfig.ForegroundOtherPageAction action =
                    RongConfigCenter.notificationConfig().getForegroundOtherPageAction();
            if (action.equals(NotificationConfig.ForegroundOtherPageAction.Notification)) {
                prepareToSendNotification(message);
            } else if (action.equals(NotificationConfig.ForegroundOtherPageAction.Sound)
                    && System.currentTimeMillis() - mLastSoundTime > SOUND_INTERVAL) {
                AudioManager audio =
                        (AudioManager) mApplication.getSystemService(Context.AUDIO_SERVICE);
                if (audio != null && audio.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                    mLastSoundTime = System.currentTimeMillis();
                    if (RongConfigCenter.featureConfig().isVibrateInForeground()) {
                        vibrate();
                    }
                    if (audio.getRingerMode() != AudioManager.RINGER_MODE_VIBRATE
                            && RongConfigCenter.featureConfig().isSoundInForeground()) {
                        sound();
                    }
                }
            }
        }
    }

    private void prepareToSendNotification(Message message) {
        String title;
        String content;
        int mNotificationId = RongNotificationHelper.getNotificationId(message.getUId());
        Conversation.ConversationType type = message.getConversationType();
        String targetId = message.getTargetId();

        ConversationKey targetKey =
                ConversationKey.obtain(message.getTargetId(), message.getConversationType());
        if (targetKey == null) {
            RLog.e(TAG, "onReceiveMessageFromApp targetKey is null");
        }
        if (type.equals(Conversation.ConversationType.GROUP)) {
            Group group = RongUserInfoManager.getInstance().getGroupInfo(targetId);
            title = group == null ? targetId : group.getName();
            UserInfo senderInfo =
                    RongUserInfoManager.getInstance().getUserInfo(message.getSenderUserId());
            if (message.getContent() instanceof RecallNotificationMessage) {
                content =
                        senderInfo == null
                                ? message.getSenderUserId()
                                : RongUserInfoManager.getInstance().getUserDisplayName(senderInfo)
                                        + IMCenter.getInstance()
                                                .getContext()
                                                .getString(R.string.rc_recalled_message);
            } else {
                if (senderInfo == null) {
                    if (targetKey != null) {
                        messageMap.put(targetKey.getKey(), message);
                    }
                }
                content =
                        senderInfo == null
                                ? message.getSenderUserId()
                                : RongUserInfoManager.getInstance().getUserDisplayName(senderInfo)
                                        + ":"
                                        + RongConfigCenter.conversationConfig()
                                                .getMessageSummary(
                                                        mApplication.getApplicationContext(),
                                                        message.getContent());
            }
        } else {
            UserInfo userInfo = RongUserInfoManager.getInstance().getUserInfo(targetId);
            title =
                    userInfo == null
                            ? targetId
                            : RongUserInfoManager.getInstance().getUserDisplayName(userInfo);
            if (userInfo == null) {
                if (targetKey != null) {
                    messageMap.put(targetKey.getKey(), message);
                }
            }
            if (message.getContent() instanceof RecallNotificationMessage) {
                content =
                        IMCenter.getInstance().getContext().getString(R.string.rc_recalled_message);
            } else {
                content =
                        RongConfigCenter.conversationConfig()
                                .getMessageSummary(
                                        mApplication.getApplicationContext(), message.getContent())
                                .toString();
            }
        }
        if (RongConfigCenter.notificationConfig()
                .getTitleType()
                .equals(NotificationConfig.TitleType.APP_NAME)) {
            title =
                    mApplication
                            .getPackageManager()
                            .getApplicationLabel(mApplication.getApplicationInfo())
                            .toString();
        }

        Class<? extends Activity> destination =
                RouteUtils.getActivity(RouteUtils.RongActivityType.ConversationActivity);
        Intent intent =
                new Intent(
                        mApplication,
                        destination == null ? RongConversationActivity.class : destination);
        intent.putExtra(RouteUtils.CONVERSATION_TYPE, type.getName().toLowerCase());
        intent.putExtra(RouteUtils.TARGET_ID, targetId);
        intent.putExtra(RouteUtils.MESSAGE_ID, message.getMessageId());
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent =
                    PendingIntent.getActivity(
                            mApplication,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent =
                    PendingIntent.getActivity(
                            mApplication, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        if (RongConfigCenter.notificationConfig().getInterceptor() != null) {
            pendingIntent =
                    RongConfigCenter.notificationConfig()
                            .getInterceptor()
                            .onPendingIntent(pendingIntent, intent);
        }

        MessagePushConfig messagePushConfig = message.getMessagePushConfig();
        // 配置 MessagePushConfig 属性
        if (messagePushConfig != null) {
            if (messagePushConfig.getAndroidConfig() != null) {
                String notificationId = messagePushConfig.getAndroidConfig().getNotificationId();
                if (!TextUtils.isEmpty(notificationId)) {
                    try {
                        mNotificationId = Integer.parseInt(notificationId);
                    } catch (Exception e) {
                        io.rong.push.common.RLog.d(
                                TAG, "parse notificationId exception:" + e.toString());
                    }
                }
            }
            if (!TextUtils.isEmpty(messagePushConfig.getPushTitle())) {
                title = messagePushConfig.getPushTitle();
            }
            if (!messagePushConfig.isForceShowDetailContent()
                    && !PushCacheHelper.getInstance()
                            .getPushContentShowStatus(IMCenter.getInstance().getContext())) {
                content =
                        IMCenter.getInstance()
                                .getContext()
                                .getString(R.string.rc_receive_new_message);
                title = null;
            } else {
                if (!TextUtils.isEmpty(messagePushConfig.getPushContent())) {
                    content = messagePushConfig.getPushContent();
                }
            }
        }

        NotificationUtil.getInstance()
                .showNotification(
                        mApplication.getApplicationContext(),
                        title,
                        content,
                        pendingIntent,
                        mNotificationId);
        requestCode++;
    }

    /**
     * 是否需要弹出本地通知。 SDK 默认不弹出本地通知的场景： 1. 聊天室消息没有本地通知。 2. 离线消息和不计数消息没有本地通知 3. 接受消息时处于免打扰状态，不弹通知。
     *
     * @return true 需要本地通知；false 不弹本地通知
     */
    private boolean shouldNotify(Message message, int left, boolean hasPackage, boolean offline) {
        MessageConfig messageConfig = message.getMessageConfig();
        if (messageConfig != null && messageConfig.isDisableNotification()) {
            return false;
        }
        // 离线消息和不计数消息，没有本地通知
        final MessageTag msgTag = message.getContent().getClass().getAnnotation(MessageTag.class);
        if (offline
                || msgTag != null
                        && (msgTag.flag() & MessageTag.ISCOUNTED) != MessageTag.ISCOUNTED) {
            return false;
        }

        // 通知被拦截，则 SDK 不再处理。
        if (RongConfigCenter.notificationConfig().getInterceptor() != null
                && RongConfigCenter.notificationConfig()
                        .getInterceptor()
                        .isNotificationIntercepted(message)) {
            return false;
        }
        // 聊天室或处于会话页面时，没有本地通知
        if (message.getConversationType().equals(Conversation.ConversationType.CHATROOM)
                || isInConversationPage()) {
            return false;
        }

        return true;
    }

    private boolean isRecallFiltered(Message message) {
        // 聊天室或处于会话页面时，没有本地通知
        if (message.getConversationType().equals(Conversation.ConversationType.CHATROOM)
                || isInConversationPage()) {
            return true;
        }
        MessageConfig messageConfig = message.getMessageConfig();
        if (messageConfig != null && messageConfig.isDisableNotification()) {
            return true;
        }
        if (!isQuietSettingSynced) { // 全局免打扰设置没有同步成功时，默认不弹通知。
            getNotificationQuietHours(null);
            return true;
        } else return isInQuietTime();
    }

    /**
     * 是否在会话列表，会话，音视频或者小视频录制界面
     *
     * @return 是否在会话列表，会话，音视频或者小视频录制界面
     */
    private boolean isInConversationPage() {
        return mTopForegroundActivity != null
                && (mTopForegroundActivity
                                .getClass()
                                .equals(
                                        RouteUtils.getActivity(
                                                RouteUtils.RongActivityType.ConversationActivity))
                        || mTopForegroundActivity
                                .getClass()
                                .equals(
                                        RouteUtils.getActivity(
                                                RouteUtils.RongActivityType
                                                        .ConversationListActivity))
                        || isRecordOrPlay()
                        || "io.rong.callkit.SingleCallActivity"
                                .equals(mTopForegroundActivity.getClass().getName()));
    }

    private boolean isRecordOrPlay() {
        boolean isRecordOrPlay = false;
        if ("io.rong.sight.player.SightPlayerActivity"
                .equals(mTopForegroundActivity.getClass().getName())) {
            return true;
        }

        if (!"io.rong.sight.record.SightRecordActivity"
                .equals(mTopForegroundActivity.getClass().getName())) {
            return false;
        }
        try {
            Class c = Class.forName("io.rong.sight.record.CameraView");
            Field field = c.getDeclaredField("isRecorder");
            Field fieldPlay = c.getDeclaredField("isPlay");
            field.setAccessible(true);
            fieldPlay.setAccessible(true);
            isRecordOrPlay = (boolean) field.get(c) || (boolean) fieldPlay.get(c);
        } catch (Exception e) {
            RLog.i(TAG, "isRecordOrPlay " + e);
        }
        return isRecordOrPlay;
    }

    private boolean isHighPriorityMessage(Message message) {
        NotificationConfig.Interceptor interceptor =
                RongConfigCenter.notificationConfig().getInterceptor();
        if (interceptor != null) {
            return interceptor.isHighPriorityMessage(message);
        } else if (message.getContent().getMentionedInfo() != null) {
            MentionedInfo mentionedInfo = message.getContent().getMentionedInfo();
            return mentionedInfo.getType().equals(MentionedInfo.MentionedType.ALL)
                    || (mentionedInfo.getType().equals(MentionedInfo.MentionedType.PART)
                            && mentionedInfo.getMentionedUserIdList() != null
                            && mentionedInfo
                                    .getMentionedUserIdList()
                                    .contains(RongIMClient.getInstance().getCurrentUserId()));
        }
        return false;
    }

    /**
     * 设置会话通知免打扰时间。
     *
     * @param startTime 起始时间 格式 HH:MM:SS。
     * @param spanMinutes 间隔分钟数大于 0 小于 1440。
     * @param callback 设置会话通知免打扰时间回调。
     */
    public void setNotificationQuietHours(
            final String startTime,
            final int spanMinutes,
            final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .setNotificationQuietHours(
                        startTime,
                        spanMinutes,
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {
                                mQuietStartTime = startTime;
                                mQuietSpanTime = spanMinutes;
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (callback != null) {
                                    callback.onError(errorCode);
                                }
                            }
                        });
    }

    /** 获取会话通知免打扰时间。 */
    public void getNotificationQuietHours(
            final RongIMClient.GetNotificationQuietHoursCallback callback) {
        MessageNotificationHelper.getNotificationQuietHoursLevel(
                new RongIMClient.GetNotificationQuietHoursCallback() {
                    @Override
                    public void onSuccess(String startTime, int spanMinutes) {
                        if (callback != null) {
                            callback.onSuccess(startTime, spanMinutes);
                        }
                        isQuietSettingSynced = true;
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {
                        if (callback != null) {
                            callback.onError(errorCode);
                        }
                    }
                });
    }

    public void removeNotificationQuietHours(final RongIMClient.OperationCallback callback) {
        RongIMClient.getInstance()
                .removeNotificationQuietHours(
                        new RongIMClient.OperationCallback() {
                            @Override
                            public void onSuccess() {

                                mQuietStartTime = null;
                                mQuietSpanTime = 0;

                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                                if (callback != null) {
                                    callback.onError(errorCode);
                                }
                            }
                        });
    }

    public void getConversationNotificationStatus(
            Conversation.ConversationType type,
            String targetId,
            final RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>
                    callback) {
        final String key = getKey(targetId, type, "1");
        if (mNotificationCache.get(key) != null && callback != null) {
            callback.onSuccess(mNotificationCache.get(key));
        } else {
            RongIMClient.getInstance()
                    .getConversationNotificationStatus(
                            type,
                            targetId,
                            new RongIMClient.ResultCallback<
                                    Conversation.ConversationNotificationStatus>() {
                                @Override
                                public void onSuccess(
                                        Conversation.ConversationNotificationStatus
                                                conversationNotificationStatus) {
                                    mNotificationCache.put(key, conversationNotificationStatus);
                                    if (callback != null) {
                                        callback.onSuccess(conversationNotificationStatus);
                                    }
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode errorCode) {
                                    if (callback != null) {
                                        callback.onError(errorCode);
                                    }
                                }
                            });
        }
    }

    private void sound() {
        Uri res = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && RongConfigCenter.notificationConfig().getInterceptor() != null) {
            NotificationChannel channel =
                    RongConfigCenter.notificationConfig()
                            .getInterceptor()
                            .onRegisterChannel(
                                    NotificationUtil.getInstance().getDefaultChannel(mApplication));
            res = channel.getSound();
        }
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(
                    new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            if (mp != null) {
                                try {
                                    mp.stop();
                                    mp.reset();
                                    mp.release();
                                } catch (Exception e) {
                                    RLog.e(TAG, "sound", e);
                                }
                            }
                            if (mediaPlayer != null) {
                                mediaPlayer = null;
                            }
                        }
                    });
            // 设置 STREAM_RING 模式：当系统设置震动时，使用系统设置方式是否播放收消息铃声。
            if (isWiredHeadsetOn(mApplication.getApplicationContext())) {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            } else if (isBluetoothA2dpOn(mApplication.getApplicationContext())) {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
            }
            mediaPlayer.setDataSource(mApplication, res);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(
                    new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                        }
                    });
        } catch (Exception e) {
            RLog.e(TAG, "sound", e);
            if (mediaPlayer != null) {
                mediaPlayer = null;
            }
        }
    }

    private boolean isWiredHeadsetOn(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                int deviceType = device.getType();
                if (deviceType == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || deviceType == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    return true;
                }
            }
        } else {
            return audioManager.isWiredHeadsetOn();
        }
        return false;
    }

    private boolean isBluetoothA2dpOn(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.isBluetoothA2dpOn();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) mApplication.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(new long[] {0, 200, 250, 200}, -1);
        }
    }

    private boolean isInQuietTime() {
        int hour = -1;
        int minute = -1;
        int second = -1;

        if (!TextUtils.isEmpty(mQuietStartTime) && mQuietStartTime.contains(":")) {
            String[] time = mQuietStartTime.split(":");

            try {
                if (time.length >= 3) {
                    hour = Integer.parseInt(time[0]);
                    minute = Integer.parseInt(time[1]);
                    second = Integer.parseInt(time[2]);
                }
            } catch (NumberFormatException e) {
                RLog.e(TAG, "getConversationNotificationStatus NumberFormatException");
            }
        }

        if (hour == -1 || minute == -1 || second == -1) {
            return false;
        }

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.HOUR_OF_DAY, hour);
        startCalendar.set(Calendar.MINUTE, minute);
        startCalendar.set(Calendar.SECOND, second);

        long startTime = startCalendar.getTimeInMillis();

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(startTime + mQuietSpanTime * 60 * 1000);

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

    private void registerActivityLifecycleCallback() {
        mApplication.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(
                            @NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

                    @Override
                    public void onActivityStarted(@NonNull Activity activity) {}

                    @Override
                    public void onActivityResumed(@NonNull Activity activity) {
                        if (mTopForegroundActivity == null) {
                            mIsInForeground = true;
                        }
                        mTopForegroundActivity = activity;
                    }

                    @Override
                    public void onActivityPaused(@NonNull Activity activity) {}

                    @Override
                    public void onActivityStopped(@NonNull Activity activity) {
                        if (mTopForegroundActivity == activity) {
                            mIsInForeground = false;
                            mTopForegroundActivity = null;
                        }
                    }

                    @Override
                    public void onActivitySaveInstanceState(
                            @NonNull Activity activity, @NonNull Bundle outState) {}

                    @Override
                    public void onActivityDestroyed(@NonNull Activity activity) {}
                });
    }

    /** 清除所有通知 */
    public void clearAllNotification() {
        if (mApplication == null) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) mApplication.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    /**
     * @param targetId
     * @param conversationType
     * @param stateType 状态类型 1 免打扰， 2 置顶；
     * @return
     */
    private String getKey(
            String targetId,
            Conversation.ConversationType conversationType,
            final String stateType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(conversationType.getName());
        stringBuilder.append(stateType);
        stringBuilder.append(targetId);
        return stringBuilder.toString();
    }

    @Override
    public void onUserUpdate(UserInfo user) {
        if (user == null) {
            return;
        }
        resendNotificationOnInfoUpdate(user.getUserId());
    }

    @Override
    public void onGroupUpdate(Group group) {
        if (group == null) {
            return;
        }
        resendNotificationOnInfoUpdate(group.getId());
    }

    /**
     * 当targetId对应的缓存更新时，更新通知
     *
     * @param targetId
     */
    private void resendNotificationOnInfoUpdate(String targetId) {
        Conversation.ConversationType[] types =
                new Conversation.ConversationType[] {
                    Conversation.ConversationType.PRIVATE,
                    Conversation.ConversationType.GROUP,
                    Conversation.ConversationType.DISCUSSION,
                    Conversation.ConversationType.CUSTOMER_SERVICE,
                    Conversation.ConversationType.CHATROOM,
                    Conversation.ConversationType.SYSTEM
                };
        Message message;
        for (Conversation.ConversationType type : types) {
            ConversationKey conversationKey = ConversationKey.obtain(targetId, type);
            if (conversationKey == null) {
                continue;
            }
            String key = conversationKey.getKey();
            if (messageMap.containsKey(key)) {
                message = messageMap.get(key);
                messageMap.remove(key);
                if (message != null) {
                    prepareToSendNotification(message);
                }
            }
        }
    }

    @Override
    public void onGroupUserInfoUpdate(GroupUserInfo groupUserInfo) {}

    private static class SingletonHolder {
        static RongNotificationManager sInstance = new RongNotificationManager();
    }
}
