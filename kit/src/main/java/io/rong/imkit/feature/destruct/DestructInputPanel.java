package io.rong.imkit.feature.destruct;

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

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.InputMode;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.conversation.extension.RongExtensionManager;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.extension.component.plugin.ImagePlugin;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.IMLibExtensionModuleManager;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.HardwareResource;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.rong.imkit.utils.PermissionCheckUtil.REQUEST_CODE_ASK_PERMISSIONS;

public class DestructInputPanel {
    private View mRootView;
    private RongExtensionViewModel mExtensionViewModel;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private Fragment mFragment;
    private ImageView mVoiceToggle;
    private EditText mEditText;
    private TextView mVoicePressButton;
    private ImageView mImageButton;
    private ImageView mCancelButton;
    private TextView mSendButton;
    private boolean isVoiceInputMode; //语音和键盘切换按钮的当前显示状态是否为键盘模式

    @SuppressLint("ClickableViewAccessibility")
    DestructInputPanel(Fragment fragment, ViewGroup parent, Conversation.ConversationType type, String targetId) {
        if (fragment == null || fragment.getContext() == null) {
            return;
        }
        mFragment = fragment;
        mConversationType = type;
        mTargetId = targetId;
        mRootView = LayoutInflater.from(fragment.getContext()).inflate(R.layout.rc_destruct_input_panel, parent, false);
        mVoiceToggle = mRootView.findViewById(R.id.input_panel_voice_toggle);
        mEditText = mRootView.findViewById(R.id.edit_btn);
        mVoicePressButton = mRootView.findViewById(R.id.press_to_speech_btn);
        mImageButton = mRootView.findViewById(R.id.input_panel_img_btn);
        mCancelButton = mRootView.findViewById(R.id.input_panel_cancel_btn);
        mSendButton = mRootView.findViewById(R.id.input_panel_send_btn);

        isVoiceInputMode = RongExtensionCacheHelper.isVoiceInputMode(fragment.getContext(), type, targetId);
        updateViewByVoiceToggle(fragment.getContext());
        mVoiceToggle.setOnClickListener(mVoiceToggleClickListener);
        mEditText.setOnFocusChangeListener(mOnEditTextFocusChangeListener);
        mEditText.addTextChangedListener(mEditTextWatcher);
        mVoicePressButton.setOnTouchListener(mOnVoiceBtnTouchListener);
        mImageButton.setOnClickListener(mImageIconClickListener);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExtensionViewModel.onSendClick();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DestructManager.getInstance().exitDestructMode();
            }
        });
        mExtensionViewModel = new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
        RongIMClient.getInstance().getTextMessageDraft(type, targetId, new RongIMClient.ResultCallback<String>() {
            @Override
            public void onSuccess(String s) {
                if (!TextUtils.isEmpty(s)) {
                    mEditText.setText(s);
                    mEditText.requestFocus();
                    mEditText.setSelection(s.length());
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });
    }

    public void onDestroy() {
        mFragment = null;
        mExtensionViewModel = null;
        IMCenter.getInstance().saveTextMessageDraft(mConversationType, mTargetId, mEditText.getText().toString(), null);
    }

    View getRootView() {
        return mRootView;
    }

    EditText getEditText() {
        return mEditText;
    }

    private void updateViewByVoiceToggle(Context context) {
        if (isVoiceInputMode) {
            mVoiceToggle.setImageDrawable(context.getResources().getDrawable(R.drawable.rc_destruct_ext_panel_key_icon));
            mEditText.setVisibility(GONE);
            mVoicePressButton.setVisibility(VISIBLE);
        } else {
            mVoiceToggle.setImageDrawable(context.getResources().getDrawable(R.drawable.rc_destruct_ext_panel_voice_icon));
            mEditText.setVisibility(VISIBLE);
            mVoicePressButton.setVisibility(GONE);
        }
    }

    private View.OnClickListener mVoiceToggleClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            isVoiceInputMode = !isVoiceInputMode;
            RongExtensionCacheHelper.saveDestructInputMode(v.getContext(), isVoiceInputMode);
            updateViewByVoiceToggle(v.getContext());
        }
    };

    private View.OnFocusChangeListener mOnEditTextFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
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

    private TextWatcher mEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s == null || s.length() == 0) {
                mSendButton.setVisibility(GONE);
                mCancelButton.setVisibility(VISIBLE);
                IMCenter.getInstance().saveTextMessageDraft(mConversationType, mTargetId, mEditText.getText().toString(), null);
            } else {
                mSendButton.setVisibility(VISIBLE);
                mCancelButton.setVisibility(GONE);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    private float mLastTouchY;
    private boolean mUpDirection;
    private View.OnTouchListener mOnVoiceBtnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            float mOffsetLimit = 70 * v.getContext().getResources().getDisplayMetrics().density;
            String[] permissions = {Manifest.permission.RECORD_AUDIO};

            if (!PermissionCheckUtil.checkPermissions(v.getContext(), permissions) && event.getAction() == MotionEvent.ACTION_DOWN) {
                PermissionCheckUtil.requestPermissions(mFragment, permissions, REQUEST_CODE_ASK_PERMISSIONS);
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (AudioPlayManager.getInstance().isPlaying()) {
                    AudioPlayManager.getInstance().stopPlay();
                }
                //判断正在视频通话和语音通话中不能进行语音消息发送
                if (IMLibExtensionModuleManager.getInstance().onRequestHardwareResource(HardwareResource.ResourceType.VIDEO)
                        || IMLibExtensionModuleManager.getInstance().onRequestHardwareResource(HardwareResource.ResourceType.AUDIO)) {
                    Toast.makeText(v.getContext(), v.getContext().getResources().getString(R.string.rc_voip_occupying),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                AudioRecordManager.getInstance().startRecord(v.getRootView(), mConversationType, mTargetId);
                mLastTouchY = event.getY();
                mUpDirection = false;
                ((TextView) v).setText(R.string.rc_voice_release_to_send);
                ((TextView) v).setBackground(v.getContext().getResources().getDrawable(R.drawable.rc_ext_voice_touched_button));
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (mLastTouchY - event.getY() > mOffsetLimit && !mUpDirection) {
                    AudioRecordManager.getInstance().willCancelRecord();
                    mUpDirection = true;
                    ((TextView) v).setText(R.string.rc_voice_press_to_input);
                    ((TextView) v).setBackground(v.getContext().getResources().getDrawable(R.drawable.rc_ext_voice_idle_button));
                } else if (event.getY() - mLastTouchY > -mOffsetLimit && mUpDirection) {
                    AudioRecordManager.getInstance().continueRecord();
                    mUpDirection = false;
                    ((TextView) v).setBackground(v.getContext().getResources().getDrawable(R.drawable.rc_ext_voice_touched_button));
                    ((TextView) v).setText(R.string.rc_voice_release_to_send);
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                AudioRecordManager.getInstance().stopRecord();
                ((TextView) v).setText(R.string.rc_voice_press_to_input);
                ((TextView) v).setBackground(v.getContext().getResources().getDrawable(R.drawable.rc_ext_voice_idle_button));
            }
            if (mConversationType.equals(Conversation.ConversationType.PRIVATE)) {
                RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:VcMsg");
            }
            return true;
        }
    };

    private View.OnClickListener mImageIconClickListener = new View.OnClickListener() {
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
                List<IPluginModule> pluginModules = extension.getPluginBoard().getPluginModules();
                for (int i = 0; i < pluginModules.size(); i++) {
                    if (pluginModules.get(i) instanceof ImagePlugin) {
                        imageIndex = i;
                        imagePlugin = pluginModules.get(i);
                    } else if (pluginModules.get(i).getClass().getName().equals("io.rong.sight.SightPlugin")) {
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
            imageClickDialog.setImageVideoDialogListener(new DestructImageDialog.ImageVideoDialogListener() {
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
