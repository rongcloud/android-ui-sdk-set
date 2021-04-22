package io.rong.imkit.manager.hqvoicemessage;

import io.rong.imlib.model.Message;

public class AutoDownloadEntry {
    private Message message;
    private DownloadPriority priority;

    public AutoDownloadEntry(Message message, DownloadPriority priority) {
        this.message = message;
        this.priority = priority;
    }

    /**
     * 文件下载优先级
     *
     * @return 当前文件的下载优先级
     */
    DownloadPriority getPriority() {
        return priority;
    }

    /**
     * 消息
     * @return 当前设置的消息
     */
    public Message getMessage() {
        return message;
    }

    /**
     * 设置消息
     * @param message 消息
     */
    public void setMessage(Message message) {
        this.message = message;
    }

    /**
     * 下载优先级
     */
    public enum DownloadPriority {
        /**
         * 正常优先级
         */
        NORMAL,
        /**
         * 高优先级
         */
        HIGH

    }
}
