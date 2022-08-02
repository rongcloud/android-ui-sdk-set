package io.rong.imkit.widget.cache;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * 消息展示列表专用 list
 * 当达到存储最大值后，底部插入新数据时，顶部数据丢弃，
 * 顶部插入数据时，不考虑最大值，底部数据保留。
 *
 * @param <T> 范型
 */
public class MessageList<T> extends ArrayList<T> {
    /**
     * 列表存储最大值
     */
    private int mMaxCount;

    public MessageList(int maxCount, int initialCapacity) {
        super(initialCapacity);
        mMaxCount = maxCount;
    }

    public MessageList(int maxCount) {
        mMaxCount = maxCount;
    }

    public MessageList(int maxCount, @NonNull Collection<? extends T> c) {
        super(c);
        mMaxCount = maxCount;
    }

    @Override
    public boolean add(T t) {
        boolean result = super.add(t);
        int overCount = size() - mMaxCount;
        //超出最大值，先移除
        if (overCount > 0) {
            removeRange(0, overCount);
        }
        return result;
    }


    @Override
    public void add(int index, T element) {
        super.add(index, element);
        int overCount = size() - mMaxCount;
        //超出最大值，先移除
        if (overCount > 0) {
            //取中位值，大于中位移除首个索引，小于中位移除末尾索引
            if (index > mMaxCount / 2) {
                removeRange(0, overCount);
            }
        }
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
        boolean result = super.addAll(c);
        int overCount = size() - mMaxCount;
        //超出最大值，先移除
        if (overCount > 0) {
            removeRange(0, overCount);
        }
        return result;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
        boolean result = super.addAll(index, c);
        int overCount = size() - mMaxCount;
        //超出最大值，先移除
        if (overCount > 0) {
            //取中位值，大于中位移除首个索引，小于中位移除末尾索引
            if (index > mMaxCount / 2) {
                removeRange(0, overCount);
            }
        }
        return result;
    }
}
