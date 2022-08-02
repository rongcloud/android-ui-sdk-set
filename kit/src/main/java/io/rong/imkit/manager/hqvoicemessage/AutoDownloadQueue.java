package io.rong.imkit.manager.hqvoicemessage;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import io.rong.imlib.model.Message;

class AutoDownloadQueue {

    private ConcurrentLinkedQueue<AutoDownloadEntry> highPriority = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<AutoDownloadEntry> normalPriority = new ConcurrentLinkedQueue<>();
    private HashMap<String,AutoDownloadEntry> autoDownloadEntryHashMap = new HashMap<>();
    private static final int MAX_QUEUE_COUNT = 100;

    void enqueue(AutoDownloadEntry autoDownloadEntry) {
        Message message = autoDownloadEntry.getMessage();

        if (autoDownloadEntry.getPriority() == AutoDownloadEntry.DownloadPriority.NORMAL) {
            normalPriority.add(autoDownloadEntry);
        } else if (autoDownloadEntry.getPriority() == AutoDownloadEntry.DownloadPriority.HIGH) {
            highPriority.add(autoDownloadEntry);
        }
        if (!autoDownloadEntryHashMap.containsKey(message.getUId())) {
            autoDownloadEntryHashMap.put(message.getUId(),autoDownloadEntry);
        }

        int doubleQueueSize = normalPriority.size() + highPriority.size();
        if (doubleQueueSize > MAX_QUEUE_COUNT) {
            if (!normalPriority.isEmpty()) {
                autoDownloadEntryHashMap.remove(normalPriority.poll().getMessage().getUId());
            } else {
                AutoDownloadEntry highItem = highPriority.poll();
                if (highItem != null) {
                    autoDownloadEntryHashMap.remove(highItem.getMessage().getUId());
                }
            }
        }
    }

    boolean ifMsgInHashMap(Message message) {
        return autoDownloadEntryHashMap.containsKey(message.getUId());
    }

    Message dequeue() {

        if (!highPriority.isEmpty()) {
            return highPriority.poll().getMessage();
        }

        if (!normalPriority.isEmpty()) {
            return normalPriority.poll().getMessage();
        }
        return null;
    }

    public boolean isEmpty() {
        return highPriority.isEmpty() && normalPriority.isEmpty();
    }

    HashMap<String, AutoDownloadEntry> getAutoDownloadEntryHashMap() {
         return autoDownloadEntryHashMap;
    }
 }
