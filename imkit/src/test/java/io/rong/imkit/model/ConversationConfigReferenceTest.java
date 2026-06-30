package io.rong.imkit.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import io.rong.imkit.config.ConversationConfig;
import io.rong.imkit.feature.reference.ReferenceContentInputBarProvider;
import io.rong.imkit.feature.reference.ReferenceContentMessageItemProvider;
import io.rong.imkit.feature.reference.ReferenceInputBarAction;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.message.TextMessage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;

public class ConversationConfigReferenceTest {

    @Test
    public void defaultReferenceMenuItemFilterKeepsBuiltInTypesAndRejectsCustomTypes() {
        ConversationConfig config = newConfig();

        assertTrue(
                config.getReferenceMenuItemFilter()
                        .shouldShowReferenceMenuItem(uiMessage(TextMessage.obtain("hello"))));
        assertFalse(
                config.getReferenceMenuItemFilter()
                        .shouldShowReferenceMenuItem(uiMessage(new CustomMessageContent())));
    }

    @Test
    public void customReferenceMenuItemFilterCanAppendCustomTypesAndDelegateDefaultTypes() {
        ConversationConfig config = newConfig();
        config.setReferenceMenuItemFilter(
                uiMessage -> {
                    if (uiMessage != null
                            && uiMessage.getContent() instanceof CustomMessageContent) {
                        return true;
                    }
                    return config.getDefaultReferenceMenuItemFilter()
                            .shouldShowReferenceMenuItem(uiMessage);
                });

        assertTrue(
                config.getReferenceMenuItemFilter()
                        .shouldShowReferenceMenuItem(uiMessage(new CustomMessageContent())));
        assertTrue(
                config.getReferenceMenuItemFilter()
                        .shouldShowReferenceMenuItem(uiMessage(TextMessage.obtain("hello"))));

        config.setReferenceMenuItemFilter(null);

        assertSame(config.getDefaultReferenceMenuItemFilter(), config.getReferenceMenuItemFilter());
        assertFalse(
                config.getReferenceMenuItemFilter()
                        .shouldShowReferenceMenuItem(uiMessage(new CustomMessageContent())));
    }

    @Test
    public void referenceContentProvidersPreferObjectNameAndFallbackToContentType() {
        ConversationConfig config = newConfig();
        ReferenceContentMessageItemProvider<CustomMessageContent> objectNameMessageProvider =
                new TestMessageProvider(false);
        ReferenceContentMessageItemProvider<CustomMessageContent> contentMessageProvider =
                new TestMessageProvider(true);
        ReferenceContentInputBarProvider<CustomMessageContent> objectNameInputProvider =
                new TestInputProvider(false);
        ReferenceContentInputBarProvider<CustomMessageContent> contentInputProvider =
                new TestInputProvider(true);

        config.addReferenceContentMessageItemProvider("App:Card", objectNameMessageProvider);
        config.addReferenceContentMessageItemProvider(null, contentMessageProvider);
        config.addReferenceContentInputBarProvider("App:Card", objectNameInputProvider);
        config.addReferenceContentInputBarProvider(null, contentInputProvider);

        assertSame(
                objectNameMessageProvider,
                config.getReferenceContentMessageItemProvider(
                        "App:Card", new CustomMessageContent()));
        assertSame(
                contentMessageProvider,
                config.getReferenceContentMessageItemProvider(
                        "App:Other", new CustomMessageContent()));
        assertSame(
                objectNameInputProvider,
                config.getReferenceContentInputBarProvider("App:Card", new CustomMessageContent()));
        assertSame(
                contentInputProvider,
                config.getReferenceContentInputBarProvider(
                        "App:Other", new CustomMessageContent()));
    }

    private static UiMessage uiMessage(MessageContent content) {
        Message message = Message.obtain("target", Conversation.ConversationType.PRIVATE, content);
        message.setSenderUserId("sender");
        return silentUiMessage(message);
    }

    private static UiMessage silentUiMessage(Message message) {
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

    private static ConversationConfig newConfig() {
        try {
            Constructor<ConversationConfig> constructor =
                    ConversationConfig.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static class CustomMessageContent extends MessageContent {
        public static final Parcelable.Creator<CustomMessageContent> CREATOR =
                new Parcelable.Creator<CustomMessageContent>() {
                    @Override
                    public CustomMessageContent createFromParcel(Parcel source) {
                        return new CustomMessageContent();
                    }

                    @Override
                    public CustomMessageContent[] newArray(int size) {
                        return new CustomMessageContent[size];
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

    private static class TestMessageProvider
            extends ReferenceContentMessageItemProvider<CustomMessageContent> {
        private final boolean supportContentType;

        TestMessageProvider(boolean supportContentType) {
            this.supportContentType = supportContentType;
        }

        @Override
        public View onCreateView(Context context, ViewGroup parent) {
            return null;
        }

        @Override
        public void onBindView(
                View view, Message quotedMessage, CustomMessageContent quotedContent) {}

        @Override
        public boolean isReferenceContentType(MessageContent content) {
            return supportContentType && content instanceof CustomMessageContent;
        }
    }

    private static class TestInputProvider
            extends ReferenceContentInputBarProvider<CustomMessageContent> {
        private final boolean supportContentType;

        TestInputProvider(boolean supportContentType) {
            this.supportContentType = supportContentType;
        }

        @Override
        public View onCreateView(
                Context context, ViewGroup parent, ReferenceInputBarAction action) {
            return null;
        }

        @Override
        public void onBindView(
                View view, Message quotedMessage, CustomMessageContent quotedContent) {}

        @Override
        public boolean isReferenceContentType(MessageContent content) {
            return supportContentType && content instanceof CustomMessageContent;
        }
    }
}
