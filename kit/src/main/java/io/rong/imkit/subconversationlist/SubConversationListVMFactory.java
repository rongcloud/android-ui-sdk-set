package io.rong.imkit.subconversationlist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import io.rong.imlib.model.Conversation;

public class SubConversationListVMFactory extends ViewModelProvider.AndroidViewModelFactory {
    private Application mApplication;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;

    public SubConversationListVMFactory(Application application, Conversation.ConversationType type) {
        super(application);
        mApplication = application;
        mConversationType = type;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SubConversationListViewModel(mApplication, mConversationType);
    }
}
