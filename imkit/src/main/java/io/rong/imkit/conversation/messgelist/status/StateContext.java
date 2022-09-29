package io.rong.imkit.conversation.messgelist.status;

import android.os.Bundle;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.model.UiMessage;

public class StateContext {
    IMessageState historyState = new HistoryState(this);
    IMessageState normalState = new NormalState(this);
    IMessageState chatroomNormalSate = new ChatroomNormalState(this);
    public static final int NORMAL_STATE = 0;
    public static final int HISTORY_STATE = 1;
    public static final int CHATROOM_NORMAL_STATE = 2;
    private IMessageState currentState;

    public StateContext(int state) {
        switch (state) {
            case NORMAL_STATE:
                this.currentState = normalState;
                break;
            case HISTORY_STATE:
                this.currentState = historyState;
                break;
            case CHATROOM_NORMAL_STATE:
                this.currentState = chatroomNormalSate;
                break;
        }
    }

    public void setCurrentState(IMessageState state) {
        this.currentState = state;
    }

    public void init(MessageViewModel messageViewModel, Bundle bundle) {
        currentState.init(messageViewModel, bundle);
    }

    public void onLoadMore(MessageViewModel viewModel) {
        currentState.onLoadMore(viewModel);
    }

    public void onRefresh(MessageViewModel viewModel) {
        currentState.onRefresh(viewModel);
    }

    public void onNewMessageBarClick(MessageViewModel viewModel) {
        currentState.onNewMessageBarClick(viewModel);
    }

    public void onHistoryBarClick(MessageViewModel viewModel) {
        currentState.onHistoryBarClick(viewModel);
    }

    public void newMentionMessageBarClick(MessageViewModel viewModel) {
        currentState.onNewMentionMessageBarClick(viewModel);
    }

    public void onScrollToBottom(MessageViewModel viewModel) {
        currentState.onScrollToBottom(viewModel);
    }

    public void onClearMessage(MessageViewModel viewModel) {
        currentState = normalState;
    }

    public boolean isNormalState(MessageViewModel viewModel) {
        return normalState.equals(currentState);
    }

    public void onReceived(
            MessageViewModel messageViewModel,
            UiMessage message,
            int left,
            boolean hasPackage,
            boolean offline) {
        currentState.onReceived(messageViewModel, message, left, hasPackage, offline);
    }
}
