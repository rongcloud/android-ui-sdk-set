package io.rong.imkit.config;

import static org.junit.Assert.assertFalse;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.Test;

public class ConversationConfigMultiDeviceSyncTest {

    @Test
    public void multiDeviceSyncDoesNotSupportNullConversationType() {
        boolean previousValue = ConversationConfig.enableMultiDeviceSync;
        try {
            ConversationConfig config = newConfigWithoutConstructor();
            config.setEnableMultiDeviceSync(true);

            assertFalse(config.isEnableMultiDeviceSync(null));
        } finally {
            ConversationConfig.enableMultiDeviceSync = previousValue;
        }
    }

    private static ConversationConfig newConfigWithoutConstructor() {
        try {
            Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
            Method getReflectionFactory =
                    reflectionFactoryClass.getDeclaredMethod("getReflectionFactory");
            Object reflectionFactory = getReflectionFactory.invoke(null);
            Method newConstructorForSerialization =
                    reflectionFactoryClass.getDeclaredMethod(
                            "newConstructorForSerialization", Class.class, Constructor.class);
            Constructor<Object> objectConstructor = Object.class.getDeclaredConstructor();
            Constructor<?> constructor =
                    (Constructor<?>)
                            newConstructorForSerialization.invoke(
                                    reflectionFactory, ConversationConfig.class, objectConstructor);
            constructor.setAccessible(true);
            return (ConversationConfig) constructor.newInstance();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
