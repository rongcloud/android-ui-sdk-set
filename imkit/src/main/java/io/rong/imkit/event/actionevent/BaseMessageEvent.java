package io.rong.imkit.event.actionevent;

public class BaseMessageEvent implements MessageEventListener {

    @Override
    public void onSendMessage(SendEvent event) {
        // do nothing
    }

    @Override
    public void onSendMediaMessage(SendMediaEvent event) {
        // do nothing
    }

    @Override
    public void onDownloadMessage(DownloadEvent event) {
        // do nothing
    }

    @Override
    public void onDeleteMessage(DeleteEvent event) {
        // do nothing
    }

    @Override
    public void onRecallEvent(RecallEvent event) {
        // do nothing
    }

    @Override
    public void onRefreshEvent(RefreshEvent event) {
        // do nothing
    }

    @Override
    public void onInsertMessage(InsertEvent event) {
        // do nothing
    }

    @Override
    public void onClearMessages(ClearEvent event) {
        // do nothing
    }
}
