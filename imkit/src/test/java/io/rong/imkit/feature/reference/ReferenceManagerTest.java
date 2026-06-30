package io.rong.imkit.feature.reference;

import static org.junit.Assert.assertEquals;

import android.os.Parcel;
import android.os.Parcelable;
import io.rong.imkit.model.UiMessage;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.QuoteInfo;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;

public class ReferenceManagerTest {

    @Test
    public void buildQuoteInfoFallsBackToMessageTagWhenObjectNameMissing() throws Exception {
        Message quotedMessage =
                Message.obtain(
                        "target",
                        Conversation.ConversationType.PRIVATE,
                        new TaggedMessageContent());
        quotedMessage.setSenderUserId("sender");
        quotedMessage.setUId("quoted-uid");
        quotedMessage.setObjectName(null);

        UiMessage uiMessage = uiMessage(quotedMessage);

        Method method =
                ReferenceManager.class.getDeclaredMethod(
                        "buildQuoteInfoFromUiMessage", UiMessage.class);
        method.setAccessible(true);
        QuoteInfo quoteInfo = (QuoteInfo) method.invoke(ReferenceManager.getInstance(), uiMessage);

        assertEquals("App:TaggedMsg", quoteInfo.getObjectName());
    }

    private static UiMessage uiMessage(Message message) {
        try {
            Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
            Method getReflectionFactory =
                    reflectionFactoryClass.getDeclaredMethod("getReflectionFactory");
            Object reflectionFactory = getReflectionFactory.invoke(null);
            Method newConstructorForSerialization =
                    reflectionFactoryClass.getDeclaredMethod(
                            "newConstructorForSerialization", Class.class, Constructor.class);
            Constructor<Object> objectConstructor = Object.class.getDeclaredConstructor();
            Constructor<?> silentConstructor =
                    (Constructor<?>)
                            newConstructorForSerialization.invoke(
                                    reflectionFactory, UiMessage.class, objectConstructor);
            silentConstructor.setAccessible(true);
            UiMessage uiMessage = (UiMessage) silentConstructor.newInstance();
            Field messageField = UiMessage.class.getDeclaredField("message");
            messageField.setAccessible(true);
            messageField.set(uiMessage, message);
            return uiMessage;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @MessageTag(value = "App:TaggedMsg", flag = MessageTag.ISCOUNTED)
    private static class TaggedMessageContent extends MessageContent {
        public static final Parcelable.Creator<TaggedMessageContent> CREATOR =
                new Parcelable.Creator<TaggedMessageContent>() {
                    @Override
                    public TaggedMessageContent createFromParcel(Parcel source) {
                        return new TaggedMessageContent();
                    }

                    @Override
                    public TaggedMessageContent[] newArray(int size) {
                        return new TaggedMessageContent[size];
                    }
                };

        @Override
        public byte[] encode() {
            return new byte[0];
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {}
    }
}
