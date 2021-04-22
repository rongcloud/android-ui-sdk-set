package io.rong.imkit.conversation.extension.component.inputpanel;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.InputMode;
import io.rong.imkit.conversation.extension.RongExtensionCacheHelper;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.manager.AudioRecordManager;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongUtils;
import io.rong.imlib.IMLibExtensionModuleManager;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.HardwareResource;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static io.rong.imkit.utils.PermissionCheckUtil.REQUEST_CODE_ASK_PERMISSIONS;


public class InputPanel {
    private final String TAG = this.getClass().getSimpleName();
    private Conversation.ConversationType mConversationType;
    private Context mContext;
    private String mTargetId;
    private InputStyle mInputStyle;
    private Fragment mFragment;
    private View mInputPanel;
    private boolean mIsVoiceInputMode; //语音和键盘切换按钮的当前显示状态是否为键盘模式
    private ImageView mVoiceToggleBtn;
    private EditText mEditText;
    private TextView mVoiceInputBtn;
    private ImageView mEmojiToggleBtn;
    private Button mSendBtn;
    private ImageView mAddBtn;
    private ViewGroup mAddOrSendBtn;
    private RongExtensionViewModel mExtensionViewModel;
    private String mInitialDraft = "";

    public InputPanel(Fragment fragment, ViewGroup parent, InputStyle inputStyle, Conversation.ConversationType type, String targetId) {
        mFragment = fragment;
        mInputStyle = inputStyle;
        mConversationType = type;
        mTargetId = targetId;
        initView(fragment.getContext(), parent);

        mExtensionViewModel = new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
        mExtensionViewModel.getInputModeLiveData().observe(fragment.getViewLifecycleOwner(), new Observer<InputMode>() {
            @Override
            public void onChanged(InputMode inputMode) {
                updateViewByInputMode(inputMode);
            }
        });
        if (fragment.getContext() != null) {
            mIsVoiceInputMode = RongExtensionCacheHelper.isVoiceInputMode(fragment.getContext(), mConversationType, mTargetId);
        }
        if (mIsVoiceInputMode) {
            mExtensionViewModel.getInputModeLiveData().setValue(InputMode.VoiceInput);
        } else {
            getDraft();
            mExtensionViewModel.getInputModeLiveData().setValue(InputMode.TextInput);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initView(final Context context, ViewGroup parent) {
        mContext = context;
        mInputPanel = LayoutInflater.from(context).inflate(R.layout.rc_extension_input_panel, parent, false);
        mVoiceToggleBtn = mInputPanel.findViewById(R.id.input_panel_voice_toggle);
        mEditText = mInputPanel.findViewById(R.id.edit_btn);
        mVoiceInputBtn = mInputPanel.findViewById(R.id.press_to_speech_btn);
        mEmojiToggleBtn = mInputPanel.findViewById(R.id.input_panel_emoji_btn);
        mAddOrSendBtn = mInputPanel.findViewById(R.id.input_panel_add_or_send);
        mSendBtn = mInputPanel.findViewById(R.id.input_panel_send_btn);
        mAddBtn = mInputPanel.findViewById(R.id.input_panel_add_btn);

        mSendBtn.setOnClickListener(mOnSendBtnClick);
        mEditText.setOnFocusChangeListener(mOnEditTextFocusChangeListener);
        mEditText.addTextChangedListener(mEditTextWatcher);
        mVoiceToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsVoiceInputMode) {
                    mIsVoiceInputMode = false;
                    mExtensionViewModel.getInputModeLiveData().setValue(InputMode.TextInput);
                    //切换到文本输入模式后需要弹出软键盘
                    mEditText.requestFocus();
                    if (TextUtils.isEmpty(mInitialDraft)) {
                        getDraft();
                    }
                } else {
                    mExtensionViewModel.getInputModeLiveData().postValue(InputMode.VoiceInput);
                    mIsVoiceInputMode = true;
                }
                RongExtensionCacheHelper.saveVoiceInputMode(context, mConversationType, mTargetId, mIsVoiceInputMode);
            }
        });
        mEmojiToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mExtensionViewModel == null) {
                    return;
                }
                if (mExtensionViewModel.getInputModeLiveData().getValue() != null
                        && mExtensionViewModel.getInputModeLiveData().getValue().equals(InputMode.EmoticonMode)) {
                    mEditText.requestFocus();
                    mExtensionViewModel.getInputModeLiveData().postValue(InputMode.TextInput);
                } else {
                    mExtensionViewModel.getInputModeLiveData().postValue(InputMode.EmoticonMode);
                }
            }
        });
        mAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mExtensionViewModel.getInputModeLiveData().getValue() != null
                        && mExtensionViewModel.getInputModeLiveData().getValue().equals(InputMode.PluginMode)) {
                    mEditText.requestFocus();
                    mExtensionViewModel.getInputModeLiveData().setValue(InputMode.TextInput);
                }
                mExtensionViewModel.getInputModeLiveData().setValue(InputMode.PluginMode);
            }
        });
        mVoiceInputBtn.setOnTouchListener(mOnVoiceBtnTouchListener);
        setInputPanelStyle(mInputStyle);
    }

    private void updateViewByInputMode(InputMode inputMode) {
        if (inputMode.equals(InputMode.TextInput) || inputMode.equals(InputMode.PluginMode)) {
            if (inputMode.equals(InputMode.TextInput)) {
                mIsVoiceInputMode = false;
            }
            mVoiceToggleBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.rc_ext_toggle_voice_btn));
            mEmojiToggleBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.rc_ext_input_panel_emoji));
            mEditText.setVisibility(VISIBLE);
            mVoiceInputBtn.setVisibility(GONE);
            mEmojiToggleBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.rc_ext_input_panel_emoji));
        } else if (inputMode.equals(InputMode.VoiceInput)) {
            mVoiceToggleBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.rc_ext_toggle_keyboard_btn));
            mVoiceInputBtn.setVisibility(VISIBLE);
            mEditText.setVisibility(GONE);
            mEmojiToggleBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.rc_ext_input_panel_emoji));
        } else if (inputMode.equals(InputMode.EmoticonMode)) {
            mVoiceToggleBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.rc_ext_toggle_voice_btn));
            mEmojiToggleBtn.setImageDrawable(mContext.getResources().getDrawable(R.drawable.rc_ext_toggle_keyboard_btn));
            mEditText.setVisibility(VISIBLE);
            mVoiceInputBtn.setVisibility(GONE);
        }
    }

    public EditText getEditText() {
        return mEditText;
    }

    public View getRootView() {
        return mInputPanel;
    }

    public void setVisible(int viewId, boolean visible) {
        mInputPanel.findViewById(viewId).setVisibility(visible ? VISIBLE : GONE);
    }

    /**
     * 设置 InputPanel 样式.
     *
     * @param style 目前支持 5 种样式，参照: {@link InputStyle}
     */
    public void setInputPanelStyle(InputStyle style) {
        switch (style) {
            case STYLE_SWITCH_CONTAINER_EXTENSION:
                setSCE();
                break;
            case STYLE_CONTAINER:
                setC();
                break;
            case STYLE_CONTAINER_EXTENSION:
                setCE();
                break;
            case STYLE_SWITCH_CONTAINER:
                setSC();
                break;
            default:
                setSCE();
                break;
        }
        mInputStyle = style;
    }

    private void setSCE() {
        if (mInputPanel != null) {
            mVoiceToggleBtn.setVisibility(VISIBLE);
            mEmojiToggleBtn.setVisibility(VISIBLE);
            mAddBtn.setVisibility(VISIBLE);
        }
    }

    private void setC() {
        if (mInputPanel != null) {
            mVoiceToggleBtn.setVisibility(GONE);
            mAddOrSendBtn.setVisibility(GONE);
            mEmojiToggleBtn.setVisibility(GONE);
            mAddBtn.setVisibility(GONE);
            mSendBtn.setVisibility(GONE);
        }
    }

    private void setCE() {
        if (mInputPanel != null) {
            mVoiceToggleBtn.setVisibility(GONE);
            mAddOrSendBtn.setVisibility(VISIBLE);
            mEmojiToggleBtn.setVisibility(VISIBLE);
            mAddBtn.setVisibility(VISIBLE);
        }
    }

    private void setSC() {
        if (mInputPanel != null) {
            mVoiceToggleBtn.setVisibility(VISIBLE);
            mAddOrSendBtn.setVisibility(GONE);
            mAddBtn.setVisibility(GONE);
        }
    }

    private void getDraft() {
        RongIMClient.getInstance().getTextMessageDraft(mConversationType, mTargetId, new RongIMClient.ResultCallback<String>() {
            @Override
            public void onSuccess(final String s) {
                if (!TextUtils.isEmpty(s)) {
                    mInitialDraft = s;
                    mEditText.setText(s);
                    mEditText.setSelection(s.length());
                    mEditText.requestFocus();
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode errorCode) {

            }
        });
    }

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
                if (RongUtils.phoneIsInUse(v.getContext()) || IMLibExtensionModuleManager.getInstance().onRequestHardwareResource(HardwareResource.ResourceType.VIDEO)
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

    private View.OnClickListener mOnSendBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mExtensionViewModel.onSendClick();
        }
    };

    private View.OnFocusChangeListener mOnEditTextFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                if (mExtensionViewModel != null && mExtensionViewModel.getInputModeLiveData() != null) {
                    mExtensionViewModel.getInputModeLiveData().postValue(InputMode.TextInput);
                }
                if (!TextUtils.isEmpty(mEditText.getText())) {
                    mSendBtn.setVisibility(VISIBLE);
                    mAddBtn.setVisibility(GONE);
                }
            } else {
                EditText editText = mExtensionViewModel.getEditTextWidget();
                if (editText.getText() != null && editText.getText().length() == 0) {
                    mSendBtn.setVisibility(GONE);
                    mAddBtn.setVisibility(VISIBLE);
                }
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
                IMCenter.getInstance().saveTextMessageDraft(mConversationType, mTargetId, mEditText.getText().toString(), null);
                if (mInputStyle.equals(InputStyle.STYLE_CONTAINER_EXTENSION) || mInputStyle.equals(InputStyle.STYLE_SWITCH_CONTAINER_EXTENSION)) {
                    mAddOrSendBtn.setVisibility(VISIBLE);
                    mAddBtn.setVisibility(VISIBLE);
                    mSendBtn.setVisibility(GONE);
                } else {
                    mAddOrSendBtn.setVisibility(GONE);
                }
            } else {
                mAddOrSendBtn.setVisibility(VISIBLE);
                mSendBtn.setVisibility(VISIBLE);
                mAddBtn.setVisibility(GONE);
            }

            int cursor, offset;
            if (count == 0) {
                cursor = start + before;
                offset = -before;
            } else {
                cursor = start;
                offset = count;
            }
            if ((Conversation.ConversationType.PRIVATE).equals(mConversationType) && offset != 0) {
                RongIMClient.getInstance().sendTypingStatus(mConversationType, mTargetId, "RC:TxtMsg");
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    public void onDestroy() {
        mFragment = null;
        mExtensionViewModel = null;
        if (mEditText != null && mEditText.getText() != null
                && !mInitialDraft.equals(mEditText.getText().toString())) {
            IMCenter.getInstance().saveTextMessageDraft(mConversationType, mTargetId, mEditText.getText().toString(), null);
        }
    }

    public enum InputStyle {
        /**
         * 录音切换-输入框-扩展
         */
        STYLE_SWITCH_CONTAINER_EXTENSION(0x123),
        /**
         * 录音切换-输入框
         */
        STYLE_SWITCH_CONTAINER(0x120),
        /**
         * 输入框-扩展
         */
        STYLE_CONTAINER_EXTENSION(0x023),
        /**
         * 仅有输入框
         */
        STYLE_CONTAINER(0x020);

        int v;

        InputStyle(int v) {
            this.v = v;
        }

        public static InputStyle getStyle(int v) {
            InputStyle result = null;
            for (InputStyle style : values()) {
                if (style.v == v) {
                    result = style;
                    break;
                }
            }
            return result;
        }
    }
}
