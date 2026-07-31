package io.rong.imkit;

import static org.junit.Assert.assertFalse;

import io.rong.imkit.model.UiMessage;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.TextMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;

public class MessageItemLongClickActionManagerTest {
    private static final String MANAGER_CLASS_NAME =
            "io.rong.imkit.MessageItemLongClickActionManager";
    private static final String OPTIONAL_LOCATION_MESSAGE_CLASS_NAME =
            "io.rong.imlib.location.message.RealTimeLocationStartMessage";

    @Test
    public void recallFilterDoesNotResolveOptionalLocationClassWhenLocationModuleIsMissing()
            throws Exception {
        Message message =
                Message.obtain(
                        "target",
                        Conversation.ConversationType.PRIVATE,
                        TextMessage.obtain("hello"));
        message.setObjectName("RC:TxtMsg");
        message.setSenderUserId("sender");
        message.setSentStatus(Message.SentStatus.CANCELED);

        assertFalse(
                newRecallFilterWithoutOptionalLocationClass()
                        .filter(newUiMessageWithoutInitialization(message)));
    }

    @Test
    public void recallFilterRejectsRealTimeLocationObjectNameWhenLocationModuleIsMissing()
            throws Exception {
        Message message =
                Message.obtain(
                        "target",
                        Conversation.ConversationType.PRIVATE,
                        TextMessage.obtain("real-time location"));
        message.setObjectName("RC:RLStart");
        message.setSenderUserId("sender");
        message.setSentStatus(Message.SentStatus.SENT);

        assertFalse(
                newRecallFilterWithoutOptionalLocationClass()
                        .filter(newUiMessageWithoutInitialization(message)));
    }

    private static MessageItemLongClickAction.Filter newRecallFilterWithoutOptionalLocationClass()
            throws Exception {
        ClassLoader classLoader =
                new OptionalLocationBlockingClassLoader(
                        MessageItemLongClickActionManagerTest.class.getClassLoader());
        Class<?> recallFilterClass = classLoader.loadClass(MANAGER_CLASS_NAME + "$4");
        return (MessageItemLongClickAction.Filter)
                newInstanceWithoutInitialization(recallFilterClass);
    }

    private static Object newInstanceWithoutInitialization(Class<?> type) throws Exception {
        return newSerializationConstructor(type).newInstance();
    }

    private static UiMessage newUiMessageWithoutInitialization(Message message) throws Exception {
        UiMessage uiMessage = (UiMessage) newInstanceWithoutInitialization(UiMessage.class);
        Field messageField = UiMessage.class.getDeclaredField("message");
        messageField.setAccessible(true);
        messageField.set(uiMessage, message);
        return uiMessage;
    }

    private static Constructor<?> newSerializationConstructor(Class<?> type) throws Exception {
        Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
        Method getReflectionFactory =
                reflectionFactoryClass.getDeclaredMethod("getReflectionFactory");
        Object reflectionFactory = getReflectionFactory.invoke(null);
        Method newConstructorForSerialization =
                reflectionFactoryClass.getDeclaredMethod(
                        "newConstructorForSerialization", Class.class, Constructor.class);
        Constructor<Object> objectConstructor = Object.class.getDeclaredConstructor();
        Constructor<?> serializationConstructor =
                (Constructor<?>)
                        newConstructorForSerialization.invoke(
                                reflectionFactory, type, objectConstructor);
        serializationConstructor.setAccessible(true);
        return serializationConstructor;
    }

    private static final class OptionalLocationBlockingClassLoader extends ClassLoader {
        OptionalLocationBlockingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (this) {
                if (OPTIONAL_LOCATION_MESSAGE_CLASS_NAME.equals(name)) {
                    throw new ClassNotFoundException(name);
                }
                if (name.equals(MANAGER_CLASS_NAME) || name.startsWith(MANAGER_CLASS_NAME + "$")) {
                    Class<?> loadedClass = findLoadedClass(name);
                    if (loadedClass == null) {
                        loadedClass = defineManagerClass(name);
                    }
                    if (resolve) {
                        resolveClass(loadedClass);
                    }
                    return loadedClass;
                }
                return super.loadClass(name, resolve);
            }
        }

        private Class<?> defineManagerClass(String name) throws ClassNotFoundException {
            String resourceName = name.replace('.', '/') + ".class";
            try (InputStream inputStream = getParent().getResourceAsStream(resourceName)) {
                if (inputStream == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] classBytes = readAllBytes(inputStream);
                return defineClass(name, classBytes, 0, classBytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        private static byte[] readAllBytes(InputStream inputStream) throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }
}
