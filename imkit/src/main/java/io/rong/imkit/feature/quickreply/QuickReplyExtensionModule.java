package io.rong.imkit.feature.quickreply;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.config.RongConfigCenter;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.InputMode;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.feature.reference.ReferenceManager;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import java.lang.ref.WeakReference;
import java.util.List;

public class QuickReplyExtensionModule implements IExtensionModule {

    private View mQuickReplyIcon;
    private boolean isQuickReplyShow;
    private WeakReference<RongExtension> mExtension;
    private RelativeLayout attachContainer;

    @Override
    public void onInit(Context context, String appKey) {}

    @Override
    public void onAttachedToExtension(final Fragment fragment, final RongExtension extension) {
        final Conversation.ConversationType type = extension.getConversationType();
        final IQuickReplyProvider provider =
                RongConfigCenter.featureConfig().getQuickReplyProvider();
        mExtension = new WeakReference<>(extension);
        if (DestructManager.isActive()) {
            return;
        }
        if (provider != null
                && provider.getPhraseList(type) != null
                && provider.getPhraseList(type).size() > 0
                && fragment != null
                && fragment.getContext() != null
                && !fragment.isDetached()) {
            final RongExtensionViewModel rongExtensionViewModel =
                    new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
            attachContainer = extension.getContainer(RongExtension.ContainerType.ATTACH);
            attachContainer.removeAllViews();
            mQuickReplyIcon =
                    LayoutInflater.from(fragment.getContext())
                            .inflate(R.layout.rc_ext_quick_reply_icon, attachContainer, false);
            attachContainer.addView(mQuickReplyIcon);
            attachContainer.setVisibility(View.VISIBLE);
            rongExtensionViewModel
                    .getInputModeLiveData()
                    .observe(
                            fragment,
                            new Observer<InputMode>() {
                                @Override
                                public void onChanged(InputMode inputMode) {
                                    if (inputMode != InputMode.QuickReplyMode) {
                                        isQuickReplyShow = false;
                                    }
                                }
                            });
            mQuickReplyIcon
                    .findViewById(R.id.ext_common_phrases)
                    .setOnClickListener(
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (isQuickReplyShow
                                            && rongExtensionViewModel
                                                    .getExtensionBoardState()
                                                    .getValue()) {
                                        isQuickReplyShow = false;
                                        if (rongExtensionViewModel.getEditTextWidget() != null) {
                                            rongExtensionViewModel
                                                    .getEditTextWidget()
                                                    .requestFocus();
                                        }
                                        rongExtensionViewModel
                                                .getInputModeLiveData()
                                                .setValue(InputMode.TextInput);
                                    } else {
                                        isQuickReplyShow = true;
                                        rongExtensionViewModel
                                                .getInputModeLiveData()
                                                .setValue(InputMode.QuickReplyMode);
                                        RelativeLayout boardContainer =
                                                extension.getContainer(
                                                        RongExtension.ContainerType.BOARD);
                                        boardContainer.removeAllViews();
                                        QuickReplyBoard quickReplyBoard =
                                                new QuickReplyBoard(
                                                        v.getContext(),
                                                        boardContainer,
                                                        provider.getPhraseList(type),
                                                        new AdapterView.OnItemClickListener() {
                                                            @Override
                                                            public void onItemClick(
                                                                    AdapterView<?> parent,
                                                                    View view,
                                                                    int position,
                                                                    long id) {
                                                                isQuickReplyShow = false;
                                                                if (rongExtensionViewModel
                                                                                .getEditTextWidget()
                                                                        != null) {
                                                                    rongExtensionViewModel
                                                                            .getEditTextWidget()
                                                                            .requestFocus();
                                                                }
                                                                rongExtensionViewModel
                                                                        .getInputModeLiveData()
                                                                        .setValue(
                                                                                InputMode
                                                                                        .TextInput);
                                                            }
                                                        });
                                        quickReplyBoard.setAttachedConversation(extension);
                                        boardContainer.addView(quickReplyBoard.getRootView());
                                        boardContainer.setVisibility(View.VISIBLE);
                                    }
                                }
                            });
            ReferenceManager.getInstance().setReferenceStatusListener(ReferenceStatusListener);
        }
    }

    @Override
    public void onDetachedFromExtension() {
        ReferenceManager.getInstance().removeReferenceStatusListener(ReferenceStatusListener);
        // The following code is used to resolve memory leaks.
        if (mExtension != null && mExtension.get() != null) {
            mExtension.get().removeView(attachContainer);
            attachContainer = null;
        }

        if (mQuickReplyIcon != null) {
            mQuickReplyIcon = null;
        }
    }

    @Override
    public void onReceivedMessage(Message message) {}

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        return null;
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return null;
    }

    @Override
    public void onDisconnect() {}

    private final ReferenceManager.ReferenceStatusListener ReferenceStatusListener =
            new ReferenceManager.ReferenceStatusListener() {
                @Override
                public void onHide() {
                    RongExtension extension = mExtension.get();
                    if (extension != null) {
                        extension.setAttachedInfo(mQuickReplyIcon);
                    }
                }
            };
}
