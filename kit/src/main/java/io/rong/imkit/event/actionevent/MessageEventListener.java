package io.rong.imkit.event.actionevent;

public interface MessageEventListener {

    void onSendMessage(SendEvent event);

    void onSendMediaMessage(SendMediaEvent event);

    void onDownloadMessage(DownloadEvent event);

    void onDeleteMessage(DeleteEvent event);

    void onRecallEvent(RecallEvent event);

    void onRefreshEvent(RefreshEvent event);

    void onInsertMessage(InsertEvent event);

    void onClearMessages(ClearEvent event);
}
