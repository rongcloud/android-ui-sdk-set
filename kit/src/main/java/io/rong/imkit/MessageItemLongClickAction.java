package io.rong.imkit;

import android.content.Context;

import java.util.Objects;

import io.rong.imkit.model.UiMessage;


/**
 * Created by jiangecho on 2017/3/16.
 */

public class MessageItemLongClickAction {

    public static class Builder {
        private String title;
        private int titleResId;
        private MessageItemLongClickListener listener;
        private Filter filter;
        private int priority;

        public Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder titleResId(int resId) {
            this.titleResId = resId;
            return this;
        }

        public Builder actionListener(MessageItemLongClickListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * 是否显示
         *
         * @param filter 过滤器
         * @return Builder
         */
        public Builder showFilter(Filter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * 优先级越高，排在越前面, 默认全是0，按添加顺序排列
         *
         * @param priority 优先级
         * @return Builder
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public MessageItemLongClickAction build() {
            MessageItemLongClickAction action = new MessageItemLongClickAction(this.titleResId,
                    this.title, this.listener, this.filter);
            action.priority = priority;
            return action;
        }
    }

    private MessageItemLongClickAction(int titleResId, String title, MessageItemLongClickListener listener, Filter filter) {
        this.titleResId = titleResId;
        this.title = title;
        this.listener = listener;
        this.filter = filter;
    }

    private String title;
    private int titleResId;
    public int priority;
    public MessageItemLongClickListener listener;
    public Filter filter;

    public String getTitle(Context context) {
        if (context != null && titleResId > 0) {
            return context.getResources().getString(titleResId);
        }
        return title;
    }

    public boolean filter(UiMessage message) {
        return filter == null || filter.filter(message);
    }

    public interface MessageItemLongClickListener {
        boolean onMessageItemLongClick(Context context, UiMessage message);
    }

    public interface Filter {
        /**
         * @param message 消息
         * @return 返回true，表示显示
         */
        boolean filter(UiMessage message);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageItemLongClickAction that = (MessageItemLongClickAction) o;

        if (titleResId != that.titleResId) return false;
        return Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + titleResId;
        return result;
    }
}
