package io.rong.imkit.feature.publicservice;

import android.content.Context;

import io.rong.imkit.utils.StringUtils;
import io.rong.imkit.widget.cache.RongCache;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.publicservice.model.PublicServiceProfile;

public class PublicServiceManager {
    private final int MAX_SIZE = 128;
    private PublicServiceProfileProvider mProfileProvider;
    private IPublicServiceMenuClickListener mPublicServiceMenuClickListener;
    private RongCache<String, PublicServiceProfile> mCache;
    private PublicServiceExtensionModule mExtensionModule;
    private PublicServiceBehaviorListener mPubBehaviorListener;

    private PublicServiceManager() {
        mCache = new RongCache<>(MAX_SIZE);
        mExtensionModule = new PublicServiceExtensionModule();
    }

    private static class SingletonHolder {
        static PublicServiceManager sInstance = new PublicServiceManager();
    }

    public static PublicServiceManager getInstance() {
        return SingletonHolder.sInstance;
    }

    /**
     * <p>
     * 设置公众服务账号信息的提供者，供 RongIM 调用获公众服务账号名称，头像信息和公众服务号菜单。
     * 目前 sdk 默认的公众号服务不需要开发者设置，这个接口提供了另外一种从 app 层设置公众服务账号信息的方式
     * 设置后，当 sdk 界面展示用户信息时，会回调 {@link PublicServiceProfileProvider#getPublicServiceProfile(Conversation.PublicServiceType, String)}
     * 使用者只需要根据对应的publicServiceType, publicServiceId 提供对应的公众服务账号信息。
     * 如果需要异步从服务器获取公众服务账号信息，使用者可以在此方法中发起异步请求，然后返回 null 信息。
     * 在异步请求结果返回后，根据返回的结果调用 {@link #refreshPublicServiceProfile(PublicServiceProfile)} 刷新公众号信息。
     * </p>
     *
     * @param publicServiceProfileProvider 公众服务账号信息的提供者 {@link PublicServiceProfileProvider}。
     */
    public void setPublicServiceProfileProvider(PublicServiceProfileProvider publicServiceProfileProvider) {
        mProfileProvider = publicServiceProfileProvider;
    }

    /**
     * 设置公众号界面操作的监听器。
     *
     * @param listener 会话公众号界面操作的监听器。
     */
    public void setPublicServiceBehaviorListener(PublicServiceBehaviorListener listener) {
        mPubBehaviorListener = listener;
    }

    /**
     * 刷新公众服务账号缓存数据。
     *
     * @param publicServiceProfile 需要更新的公众服务账号缓存数据。
     */
    public void refreshPublicServiceProfile(PublicServiceProfile publicServiceProfile) {
        if (publicServiceProfile == null)
            return;

        Conversation.ConversationType type = publicServiceProfile.getConversationType();
        String targetId = publicServiceProfile.getTargetId();
        String key = StringUtils.getKey(type.getName(), targetId);
        mCache.put(key, publicServiceProfile);
    }

    /**
     * 设置公众服务菜单点击监听。
     * 建议使用方法：在进入对应公众服务会话时，设置监听。当退出会话时，重置监听为 null，这样可以防止内存泄露。
     *
     * @param menuClickListener 监听。
     */
    public void setPublicServiceMenuClickListener(IPublicServiceMenuClickListener menuClickListener) {
        mPublicServiceMenuClickListener = menuClickListener;
    }

    public IPublicServiceMenuClickListener getPublicServiceMenuClickListener() {
        return mPublicServiceMenuClickListener;
    }

    public PublicServiceProfile getPublicServiceProfile(Conversation.PublicServiceType publicServiceType, String targetId) {
        String key = StringUtils.getKey(publicServiceType.getName(), targetId);
        if (mCache.get(key) != null) {
            return mCache.get(key);
        } else if (mProfileProvider != null) {
            return mProfileProvider.getPublicServiceProfile(publicServiceType, targetId);
        }
        return null;
    }

    public void getPublicServiceProfile(final Conversation.PublicServiceType publicServiceType, final String targetId, final RongIMClient.ResultCallback<PublicServiceProfile> callback) {
        RongIMClient.getInstance().getPublicServiceProfile(publicServiceType, targetId, new RongIMClient.ResultCallback<PublicServiceProfile>() {
            @Override
            public void onSuccess(PublicServiceProfile publicServiceProfile) {
                if (callback != null) {
                    callback.onSuccess(publicServiceProfile);
                }
                String key = StringUtils.getKey(publicServiceType.getName(), targetId);
                mCache.put(key, publicServiceProfile);
            }

            @Override
            public void onError(RongIMClient.ErrorCode coreErrorCode) {
                if (callback != null) {
                    callback.onError(RongIMClient.ErrorCode.valueOf(coreErrorCode.getValue()));
                }
            }
        });
    }

    public PublicServiceBehaviorListener getPubBehaviorListener() {
        return mPubBehaviorListener;
    }

    public PublicServiceExtensionModule getExtensionModule() {
        return mExtensionModule;
    }

    /**
     * PublicServiceProfile提供者。
     */
    public interface PublicServiceProfileProvider {
        /**
         * 获取PublicServiceProfile。
         *
         * @param publicServiceType 公众服务类型。
         * @param id                公众服务 id。
         * @return PublicServiceProfile。
         */
        PublicServiceProfile getPublicServiceProfile(Conversation.PublicServiceType publicServiceType, String id);
    }

    /**
     * 公众号界面操作的监听器
     */
    public interface PublicServiceBehaviorListener {
        /**
         * 当点击关注后执行。
         *
         * @param context 上下文。
         * @param info    公众号信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onFollowClick(Context context, PublicServiceProfile info);

        /**
         * 当点击取消关注后执行。
         *
         * @param context 上下文。
         * @param info    公众号信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onUnFollowClick(Context context, PublicServiceProfile info);

        /**
         * 当点击进入进入会话后执行。
         *
         * @param context 上下文。
         * @param info    公众号信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        boolean onEnterConversationClick(Context context, PublicServiceProfile info);
    }
}
