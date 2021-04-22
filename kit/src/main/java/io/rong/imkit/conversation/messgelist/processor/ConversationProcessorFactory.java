package io.rong.imkit.conversation.messgelist.processor;

import android.util.ArrayMap;

import io.rong.common.RLog;
import io.rong.imkit.feature.customservice.CustomServiceBusinessProcessor;
import io.rong.imkit.feature.publicservice.PublicServiceBusinessProcessor;
import io.rong.imlib.model.Conversation;

/**
 * 会话处理器工厂类。根据根据类型，生成不同的处理器，在此处理器中处理各类型会话的独立业务。
 * 可以通过设置自定义会话处理器，进行自定义业务处理。
 */
public class ConversationProcessorFactory {
    private static final String TAG = ConversationProcessorFactory.class.getSimpleName();

    public static ConversationProcessorFactory getInstance() {
        return Holder.instance;
    }

    ArrayMap<Conversation.ConversationType, IConversationBusinessProcessor> mProcessorMap;

    private ConversationProcessorFactory() {
        mProcessorMap = new ArrayMap<>();
        mProcessorMap.put(Conversation.ConversationType.PRIVATE, new PrivateBusinessProcessor());
        mProcessorMap.put(Conversation.ConversationType.SYSTEM, new PrivateBusinessProcessor());
        mProcessorMap.put(Conversation.ConversationType.GROUP, new GroupBusinessProcessor());
        mProcessorMap.put(Conversation.ConversationType.DISCUSSION, new GroupBusinessProcessor());
        mProcessorMap.put(Conversation.ConversationType.CHATROOM, new ChatRoomBusinessProcessor());
        mProcessorMap.put(Conversation.ConversationType.CUSTOMER_SERVICE, new CustomServiceBusinessProcessor());
        PublicServiceBusinessProcessor publicServiceProcessor = new PublicServiceBusinessProcessor();
        mProcessorMap.put(Conversation.ConversationType.PUBLIC_SERVICE, publicServiceProcessor);
        mProcessorMap.put(Conversation.ConversationType.APP_PUBLIC_SERVICE, publicServiceProcessor);
    }

    private static class Holder {
        private static ConversationProcessorFactory instance = new ConversationProcessorFactory();
    }

    public IConversationBusinessProcessor getProcessor(Conversation.ConversationType type) {
        IConversationBusinessProcessor processor = mProcessorMap.get(type);
        if (processor == null) {
            RLog.e(TAG, "No processor defined for type :" + type.getName() + "; Using private processor as default.");
            processor = mProcessorMap.get(Conversation.ConversationType.PRIVATE);
        }
        return processor;
    }

    public void setProcessor(Conversation.ConversationType type, IConversationBusinessProcessor processor) {
        mProcessorMap.put(type, processor);
    }
}
