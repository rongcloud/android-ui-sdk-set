package io.rong.imkit.feature.destruct;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.rong.imkit.utils.PermissionCheckUtil.REQUEST_CODE_ASK_PERMISSIONS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.conversation.extension.InputMode;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.extension.component.plugin.ImagePlugin;
import io.rong.imkit.feature.reference.ReferenceManager;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongOperationPermissionUtils;
import io.rong.imkit.utils.ToastUtils;
import io.rong.imlib.ChannelClient;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.RongCoreClient;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.imlib.model.Message;
import java.lang.ref.WeakReference;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public class DestructInputPanel {
    private View mRootView;
    private RongExtensionViewModel mExtensionViewModel;
    private ConversationIdentifier mConversationIdentifier;
    private Fragment mFragment;
    private ImageView mVoiceToggle;
    private EditText mEditText;
    private TextView mVoicePressButton;
    private ImageView mImageButton;
    private ImageView mCancelButton;
    private TextView mSendButton;
    private boolean isVoiceInputMode; // 语音和键盘切换按钮的当前显示状态是否为键盘模式

    @SuppressLint("ClickableViewAccessibility")
    DestructInputPanel(
            Fragment fragment, ViewGroup parent, ConversationIdentifier conversationIdentifier) {
        if (fragment == null || fragment.getContext() == null) {
            return;
        }
        mFragment = fragment;
        mConversationIdentifier = conversationIdentifier;
        mRootView =
                LayoutInflater.from(fragment.getContext())
                        .inflate(R.layout.rc_destruct_input_panel, parent, false);
        mVoiceToggle = mRootView.findViewById(R.id.input_panel_voice_toggle);
        mEditText = mRootView.findViewById(R.id.edit_btn);
        mVoicePressButton = mRootView.findViewById(R.id.press_to_speech_btn);
        mImageButton = mRootView.findViewById(R.id.input_panel_img_btn);
        mCancelButton = mRootView.findViewById(R.id.input_panel_cancel_btn);
        mSendButton = mRootView.findViewById(R.id.input_panel_send_btn);
        if (!IMKitThemeManager.isTraditionTheme()) {
            mRootView.setBackgroundColor(
                    IMKitThemeManager.getColorFromAttrId(
                            fragment.getContext(), R.attr.rc_common_background_color));
            mEditText.setBackgroundResource(R.drawable.rc_lively_panel_input_background);
            mVoicePressButton.setTextColor(
                    IMKitThemeManager.getColorFromAttrId(
                            fragment.getContext(), R.attr.rc_text_primary_color));
        }
        mVoicePressButton.setBackgroundResource(
                IMKitThemeManager.dynamicResource(
                        R.drawable.rc_lively_auxiliary_background_1_radius_8,
                        R.drawable.rc_ext_voice_idle_button));

        isVoiceInputMode =
                RongExtensionCacheHelper.isVoiceInputMode(
                        fragment.getContext(),
                        conversationIdentifier.getType(),
                        conversationIdentifier.getTargetId());
        updateViewByVoiceToggle(fragment.getContext());
        mVoiceToggle.setOnClickListener(mVoiceToggleClickListener);
        mEditText.setOnFocusChangeListener(mOnEditTextFocusChangeListener);
        mEditText.addTextChangedListener(mEditTextWatcher);
        mVoicePressButton.setOnTouchListener(mOnVoiceBtnTouchListener);
        mImageButton.setOnClickListener(mImageIconClickListener);
        mSendButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mExtensionViewModel.onSendClick();
                    }
                });

        mCancelButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        IMCenter.getInstance()
                                .saveTextMessageDraft(
                                        mConversationIdentifier,
                                        mEditText.getText().toString(),
                                        null);
                        DestructManager.getInstance().exitDestructMode();
                    }
                });
        mExtensionViewModel = new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
        ChannelClient.getInstance()
                .getTextMessageDraft(
                        mConversationIdentifier.getType(),
                        mConversationIdentifier.getTargetId(),
                        mConversationIdentifier.getChannelId(),
                        new DestructInputPanel.GetDraftCallback(new WeakReference<>(this)));
    }

    public void onDestroy() {
        mFragment = null;
        mExtensionViewModel = null;
        if (DestructManager.isActive()) {
            IMCenter.getInstance()
                    .saveTextMessageDraft(
                            mConversationIdentifier,
                            getDraft(mEditText.getText().toString()),
                            null);
        }
    }

    View getRootView() {
        return mRootView;
    }

    EditText getEditText() {
        return mEditText;
    }

    private static class GetDraftCallback extends IRongCoreCallback.ResultCallback<String> {
        private final WeakReference<DestructInputPanel> mWeakInputPanel;

        GetDraftCallback(WeakReference<DestructInputPanel> weakInputPanel) {
            mWeakInputPanel = weakInputPanel;
        }

        @Override
        public void onSuccess(String content) {
            DestructInputPanel inputPanel = mWeakInputPanel != null ? mWeakInputPanel.get() : null;
            if (inputPanel == null) {
                return;
            }
            String draftContent = "";
            String referencedMessageUId = null;

            // 尝试解析为 JSON 格式
            try {
                JSONObject draftJson = new JSONObject(content);
                draftContent = draftJson.optString("draftContent", "");
                referencedMessageUId = draftJson.optString("referencedMessageUId", null);
            } catch (Exception e) {
                // 如果不是 JSON 格式，兼容原有的字符串草稿内容
                draftContent = content;
            }

            String finalDraftContent = draftContent;

            // 使用静态内部类防止泄漏
            RongCoreClient.getInstance()
                    .getMessageByUid(
                            referencedMessageUId,
                            new IRongCoreCallback.ResultCallback<Message>() {
                                private final WeakReference<DestructInputPanel>
                                        callbackWeakInputPanel = new WeakReference<>(inputPanel);

                                @Override
                                public void onSuccess(Message message) {
                                    DestructInputPanel callbackInputPanel =
                                            callbackWeakInputPanel.get();
                                    if (callbackInputPanel != null) {
                                        callbackInputPanel.updateMessageDraft(
                                                finalDraftContent, message);
                                    }
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode e) {
                                    DestructInputPanel callbackInputPanel =
                                            callbackWeakInputPanel.get();
                                    if (callbackInputPanel != null) {
                                        callbackInputPanel.updateMessageDraft(
                                                finalDraftContent, null);
                                    }
                                }
                            });
        }

        @Override
        public void onError(IRongCoreEnum.CoreErrorCode e) {
            // do nothing
        }
    }

    @NonNull
    private String getDraft(String draftContent) {
        UiMessage uiMessage = ReferenceManager.getInstance().getUiMessage();
        String referencedMessageUId = null;
        if (uiMessage != null && uiMessage.getMessage() != null) {
            referencedMessageUId = uiMessage.getMessage().getUId();
        }

        // 构造 JSON 格式的草稿数据
        JSONObject draftJson = new JSONObject();
        try {
            draftJson.put("draftContent", draftContent);
            if (referencedMessageUId != null && !referencedMessageUId.isEmpty()) {
                draftJson.put("referencedMessageUId", referencedMessageUId);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return draftJson.toString();
    }

    private void updateMessageDraft(final String draft, Message message) {
        if (TextUtils.isEmpty(draft) || mEditText == null || mFragment == null) {
            return;
        }
        if (message != null) {
            ReferenceManager.getInstance()
                    .showReferenceView(mFragment.getContext(), new UiMessage(message));
        }
        mEditText.postDelayed(
                () -> {
                    mEditText.setText(draft);
                    mEditText.setSelection(mEditText.length());
                    mEditText.requestFocus();
                },
                50);
    }

    private void updateViewByVoiceToggle(Context context) {
        if (isVoiceInputMode) {
            mVoiceToggle.setImageDrawable(
                    context.getResources()
                            .getDrawable(
                                    IMKitThemeManager.getAttrResId(
                                            context,
                                            R.attr
                                                    .rc_conversation_input_bar_keyboard_destruct_img)));
            mEditText.setVisibility(GONE);
            mVoicePressButton.setVisibility(VISIBLE);
        } else {
            mVoiceToggle.setImageDrawable(
                    context.getResources()
                            .getDrawable(
                                    IMKitThemeManager.getAttrResId(
                                            context,
                                            R.attr.rc_conversation_input_bar_voice_destruct_img)));
            mEditText.setVisibility(VISIBLE);
            mVoicePressButton.setVisibility(GONE);
        }
    }

    private View.OnClickListener mVoiceToggleClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    isVoiceInputMode = !isVoiceInputMode;
                    RongExtensionCacheHelper.saveDestructInputMode(
                            v.getContext(), isVoiceInputMode);
                    updateViewByVoiceToggle(v.getContext());
                }
            };

    private View.OnFocusChangeListener mOnEditTextFocusChangeListener =
            new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (mExtensionViewModel == null) {
                        return;
                    }
                    if (hasFocus) {
                        mExtensionViewModel.getInputModeLiveData().postValue(InputMode.TextInput);
                        if (!TextUtils.isEmpty(mEditText.getText())) {
                            mSendButton.setVisibility(VISIBLE);
                            mCancelButton.setVisibility(GONE);
                        }
                    } else {
                        mExtensionViewModel.collapseExtensionBoard();
                        mSendButton.setVisibility(GONE);
                        mCancelButton.setVisibility(VISIBLE);
                    }
                }
            };

    private TextWatcher mEditTextWatcher =
            new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // do nothing
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s == null || s.length() == 0) {
                        mSendButton.setVisibility(GONE);
                        mCancelButton.setVisibility(VISIBLE);
                    } else {
                        mSendButton.setVisibility(VISIBLE);
                        mCancelButton.setVisibility(GONE);
                    }

                    int offset = count == 0 ? -before : count;
                    if ((Conversation.ConversationType.PRIVATE)
                                    .equals(mConversationIdentifier.getType())
                            && offset != 0) {
                        RongIMClient.getInstance()
                                .sendTypingStatus(
                                        mConversationIdentifier.getType(),
                                        mConversationIdentifier.getTargetId(),
                                        "RC:TxtMsg");
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                    // do nothing
                }
            };

    private float mLastTouchY;
    private boolean mUpDirection;
    private View.OnTouchListener mOnVoiceBtnTouchListener =
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float mOffsetLimit =
                            70 * v.getContext().getResources().getDisplayMetrics().density;
                    String[] permissions = {Manifest.permission.RECORD_AUDIO};

                    if (!PermissionCheckUtil.checkPermissions(v.getContext(), permissions)
                            && event.getAction() == MotionEvent.ACTION_DOWN) {
                        PermissionCheckUtil.requestPermissions(
                                mFragment, permissions, REQUEST_CODE_ASK_PERMISSIONS);
                        return true;
                    }

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (AudioPlayManager.getInstance().isPlaying()) {
                            AudioPlayManager.getInstance().stopPlay();
                        }
                        // 判断正在视频通话和语音通话中不能进行语音消息发送
                        if (RongOperationPermissionUtils.isOnRequestHardwareResource()) {
                            String text =
                                    v.getContext()
                                            .getResources()
                                            .getString(R.string.rc_voip_occupying);
                            ToastUtils.show(v.getContext(), text, Toast.LENGTH_SHORT);
                            return true;
                        }
                        AudioRecordManager.getInstance()
                                .startRecord(v.getRootView(), mConversationIdentifier);
                        mLastTouchY = event.getY();
                        mUpDirection = false;
                        ((TextView) v).setText(R.string.rc_voice_release_to_send);
                        v.setBackgroundResource(
                                IMKitThemeManager.dynamicResource(
                                        R.drawable.rc_lively_auxiliary_background_2_radius_8,
                                        R.drawable.rc_ext_voice_touched_button));
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (mLastTouchY - event.getY() > mOffsetLimit && !mUpDirection) {
                            AudioRecordManager.getInstance().willCancelRecord();
                            mUpDirection = true;
                            ((TextView) v).setText(R.string.rc_voice_press_to_input);
                            v.setBackgroundResource(
                                    IMKitThemeManager.dynamicResource(
                                            R.drawable.rc_lively_auxiliary_background_1_radius_8,
                                            R.drawable.rc_ext_voice_idle_button));
                        } else if (event.getY() - mLastTouchY > -mOffsetLimit && mUpDirection) {
                            AudioRecordManager.getInstance().continueRecord();
                            mUpDirection = false;
                            v.setBackgroundResource(
                                    IMKitThemeManager.dynamicResource(
                                            R.drawable.rc_lively_auxiliary_background_2_radius_8,
                                            R.drawable.rc_ext_voice_touched_button));
                            ((TextView) v).setText(R.string.rc_voice_release_to_send);
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP
                            || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        AudioRecordManager.getInstance().stopRecord();
                        ((TextView) v).setText(R.string.rc_voice_press_to_input);
                        v.setBackgroundResource(
                                IMKitThemeManager.dynamicResource(
                                        R.drawable.rc_lively_auxiliary_background_1_radius_8,
                                        R.drawable.rc_ext_voice_idle_button));
                    }
                    if (mConversationIdentifier
                            .getType()
                            .equals(Conversation.ConversationType.PRIVATE)) {
                        RongIMClient.getInstance()
                                .sendTypingStatus(
                                        mConversationIdentifier.getType(),
                                        mConversationIdentifier.getTargetId(),
                                        "RC:VcMsg");
                    }
                    return true;
                }
            };

    private View.OnClickListener mImageIconClickListener =
            new View.OnClickListener() {
                private IPluginModule sightPlugin;
                private IPluginModule imagePlugin;
                private int sightIndex;
                private int imageIndex;

                @Override
                public void onClick(View v) {
                    final RongExtension extension = DestructExtensionModule.sRongExtension.get();
                    if (extension == null) {
                        return;
                    }
                    if (imagePlugin == null && sightPlugin == null) {
                        List<IPluginModule> pluginModules =
                                extension.getPluginBoard().getPluginModules();
                        for (int i = 0; i < pluginModules.size(); i++) {
                            if (pluginModules.get(i) instanceof ImagePlugin) {
                                imageIndex = i;
                                imagePlugin = pluginModules.get(i);
                            } else if (pluginModules
                                    .get(i)
                                    .getClass()
                                    .getName()
                                    .equals("io.rong.sight.SightPlugin")) {
                                sightIndex = i;
                                sightPlugin = pluginModules.get(i);
                            }
                            if (imagePlugin != null && sightPlugin != null) {
                                break;
                            }
                        }
                    }
                    DestructImageDialog imageClickDialog = new DestructImageDialog();
                    imageClickDialog.setHasImage(imagePlugin != null);
                    imageClickDialog.setHasSight(sightPlugin != null);
                    imageClickDialog.setImageVideoDialogListener(
                            new DestructImageDialog.ImageVideoDialogListener() {
                                @Override
                                public void onSightClick(View v) {
                                    if (sightPlugin != null)
                                        sightPlugin.onClick(mFragment, extension, sightIndex);
                                }

                                @Override
                                public void onImageClick(View v) {
                                    if (imagePlugin != null)
                                        imagePlugin.onClick(mFragment, extension, imageIndex);
                                }
                            });
                    if (mFragment.isAdded()) {
                        imageClickDialog.show(mFragment.getParentFragmentManager());
                    }
                }
            };
}
