package io.rong.imkit.widget.adapter;

import static org.junit.Assert.assertFalse;

import android.util.SparseArray;
import android.view.View;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class ViewHolderTest {
    @Test
    public void getViewSerializesSparseArrayAccess() throws Exception {
        ViewHolder holder = allocate(ViewHolder.class);
        TrackingSparseArray cache = allocate(TrackingSparseArray.class);
        cache.activeReaders = new AtomicInteger();
        cache.value = allocate(View.class);
        setField(holder, "mViews", cache);
        setField(holder, "mConvertView", null);

        CountDownLatch start = new CountDownLatch(1);
        Thread first = new Thread(() -> invokeGetView(holder, start));
        Thread second = new Thread(() -> invokeGetView(holder, start));
        first.start();
        second.start();
        start.countDown();
        first.join();
        second.join();

        assertFalse(cache.overlapped);
    }

    private static void invokeGetView(ViewHolder holder, CountDownLatch start) {
        try {
            start.await();
            holder.getView(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocate(Class<T> type) throws Exception {
        Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
        Object reflectionFactory =
                reflectionFactoryClass.getMethod("getReflectionFactory").invoke(null);
        Method newConstructorForSerialization =
                reflectionFactoryClass.getDeclaredMethod(
                        "newConstructorForSerialization", Class.class, Constructor.class);
        Constructor<Object> objectConstructor = Object.class.getDeclaredConstructor();
        Constructor<?> constructor =
                (Constructor<?>)
                        newConstructorForSerialization.invoke(
                                reflectionFactory, type, objectConstructor);
        constructor.setAccessible(true);
        return (T) constructor.newInstance();
    }

    private static final class TrackingSparseArray extends SparseArray<View> {
        private AtomicInteger activeReaders;
        private View value;
        private volatile boolean overlapped;

        @Override
        public View get(int key) {
            if (activeReaders.incrementAndGet() > 1) {
                overlapped = true;
            }
            try {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return value;
            } finally {
                activeReaders.decrementAndGet();
            }
        }

        @Override
        public void put(int key, View value) {
            this.value = value;
        }
    }
}
