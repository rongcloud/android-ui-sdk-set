package io.rong.imkit.config;

import android.content.Context;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversationlist.model.BaseUiConversation;
import io.rong.imkit.conversationlist.provider.BaseConversationProvider;
import io.rong.imkit.conversationlist.provider.ConversationListEmptyProvider;
import io.rong.imkit.conversationlist.provider.PrivateConversationProvider;
import io.rong.imkit.widget.adapter.IViewProvider;
import io.rong.imkit.widget.adapter.ProviderManager;
import io.rong.imlib.model.Conversation;

public class ConversationListConfig {

    private final String TAG = "ConversationListConfig";
    private ConversationListBehaviorListener mListener;
    private boolean mIsEnableConnectStateNotice = true;
    //会话列表页是否自动下载高清语音
    private boolean mEnableAutomaticDownloadHQVoice = true;
    private int mConversationCountPerPage = 100; //每页拉取的会话条数, 默认 100.
    private ProviderManager<BaseUiConversation> mProviderManager;

    private Conversation.ConversationType[] mSupportedTypes = {Conversation.ConversationType.PRIVATE,
            Conversation.ConversationType.GROUP, Conversation.ConversationType.SYSTEM,
            Conversation.ConversationType.CUSTOMER_SERVICE, Conversation.ConversationType.CHATROOM,
            Conversation.ConversationType.APP_PUBLIC_SERVICE, Conversation.ConversationType.PUBLIC_SERVICE,
            Conversation.ConversationType.ENCRYPTED};

    private DataProcessor<Conversation> mDataProcessor = new DataProcessor<Conversation>() {
        @Override
        public Conversation.ConversationType[] supportedTypes() {
            return mSupportedTypes;
        }

        @Override
        public List<Conversation> filtered(List<Conversation> data) {
            return data;
        }

        @Override
        public boolean isGathered(Conversation.ConversationType type) {
            return false;
        }
    };

    public ConversationListConfig() {
        List<IViewProvider<BaseUiConversation>> providerList = new ArrayList<>();
        providerList.add(new PrivateConversationProvider());
        //        providerList.add(new GatheredConversationProvider());
        mProviderManager = new ProviderManager<>(providerList);
        //mProviderManager.setEmptyViewProvider(new ConversationListEmptyProvider());
        mProviderManager.setDefaultProvider(new BaseConversationProvider());
    }

    public void initConfig(Context context) {
        if (context != null) {
            Resources resources = context.getResources();
            try {
                mIsEnableConnectStateNotice = resources.getBoolean(R.bool.rc_is_show_warning_notification);
            } catch (Exception e) {
                RLog.e(TAG, "rc_is_show_warning_notification not get value", e);
            }
        }
    }

    public void setDataProcessor(DataProcessor<Conversation> dataFilter) {
        this.mDataProcessor = dataFilter;
    }

    public void setBehaviorListener(ConversationListBehaviorListener listener) {
        this.mListener = listener;
    }

    public void setCountPerPage(int count) {
        this.mConversationCountPerPage = count;
    }

    public void setConversationListProvider(ProviderManager<BaseUiConversation> providerManager) {
        this.mProviderManager = providerManager;
    }

    public void setEnableAutomaticDownloadHQVoice(boolean enable) {
        this.mEnableAutomaticDownloadHQVoice = enable;
    }

    public void setConversationProvider(BaseConversationProvider provider) {
        this.mProviderManager.addProvider(provider);
    }

    public ProviderManager<BaseUiConversation> getProviderManager() {
        return mProviderManager;
    }

    public boolean isEnableAutomaticDownloadHQVoice() {
        return mEnableAutomaticDownloadHQVoice;
    }

    public DataProcessor<Conversation> getDataProcessor() {
        return mDataProcessor;
    }

    public ConversationListBehaviorListener getListener() {
        return mListener;
    }

    public boolean isEnableConnectStateNotice() {
        return mIsEnableConnectStateNotice;
    }

    public int getConversationCountPerPage() {
        return mConversationCountPerPage;
    }

}

