package io.rong.imkit.conversation.extension;

import android.app.Application;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.component.emoticon.AndroidEmoji;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.feature.mention.IExtensionEventWatcher;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imlib.model.Conversation;
import io.rong.message.TextMessage;

public class RongExtensionViewModel extends AndroidViewModel {
    private final String TAG = this.getClass().getSimpleName();
    private MutableLiveData<Boolean> mExtensionBoardState;
    private MutableLiveData<InputMode> mInputModeLiveData;
    private MutableLiveData<Boolean> mAttachedInfoState;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private EditText mEditText;
    private boolean isSoftInputShow;

    public RongExtensionViewModel(@NonNull Application application) {
        super(application);
        mExtensionBoardState = new MutableLiveData<>();
        mInputModeLiveData = new MutableLiveData<>();
        mAttachedInfoState = new MutableLiveData<>();
    }

    void setAttachedConversation(Conversation.ConversationType type, String targetId, EditText editText) {
        mConversationType = type;
        mTargetId = targetId;
        mEditText = editText;
        mEditText.addTextChangedListener(mTextWatcher);
        if (type.equals(Conversation.ConversationType.GROUP)) {
            RongMentionManager.getInstance().createInstance(type, targetId, mEditText); //todo 更改实现方式，由 mention 模块 addTextWatcher.
        }
    }

    public void setEditTextWidget(EditText editText) {
        mEditText.setText("");
        mEditText = null;
        mEditText = editText;
        mEditText.addTextChangedListener(mTextWatcher);
    }

    public void onSendClick() {
        if (TextUtils.isEmpty(mEditText.getText()) || TextUtils.isEmpty(mEditText.getText().toString().trim())) {
            RLog.d(TAG, "can't send empty content.");
            mEditText.setText("");
            return;
        }

        String text = mEditText.getText().toString();
        mEditText.setText("");
        TextMessage textMessage = TextMessage.obtain(text);
        if (DestructManager.isActive()) {
            int length = text.length();
            long time;
            if (length <= 20) {
                time = 10;
            } else {
                time = Math.round((length - 20) * 0.5 + 10);
            }
            textMessage.setDestruct(true);
            textMessage.setDestructTime(time);
        }
        io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(mTargetId, mConversationType, textMessage);

        if (RongExtensionManager.getInstance().getExtensionEventWatcher().size() > 0) {
            for (IExtensionEventWatcher watcher : RongExtensionManager.getInstance().getExtensionEventWatcher()) {
                watcher.onSendToggleClick(message);
            }
        }
        IMCenter.getInstance().sendMessage(message, DestructManager.isActive() ? getApplication().getResources().getString(R.string.rc_conversation_summary_content_burn) : null, null, null);
    }

    public void forceSetSoftInputKeyBoard(boolean isShow) {
        InputMethodManager imm = (InputMethodManager) getApplication().getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (isShow) {
                mEditText.requestFocus();
                imm.showSoftInput(mEditText, 0);
            } else {
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
                mEditText.clearFocus();
            }
            isSoftInputShow = isShow;
        }
        if (isShow && mExtensionBoardState.getValue() != null && mExtensionBoardState.getValue().equals(false)) {
            mExtensionBoardState.setValue(true);
        }
    }

    public void setSoftInputKeyBoard(boolean isShow) {
        if (isSoftInputShow == isShow) {
            return;
        }
        forceSetSoftInputKeyBoard(isShow);
    }

    /**
     * 收起面板，RongExtension 仅显示 InputPanel。
     */
    public void collapseExtensionBoard() {
        if (mExtensionBoardState.getValue() != null && mExtensionBoardState.getValue().equals(false)) {
            RLog.d(TAG, "already collapsed, return directly.");
            return;
        }
        RLog.d(TAG, "collapseExtensionBoard");
        setSoftInputKeyBoard(false);
        mExtensionBoardState.postValue(false);
    }

    public boolean isSoftInputShow() {
        return isSoftInputShow;
    }

    /**
     * 退出"更多"模式
     *
     * @param context 上下文
     */
    public void exitMoreInputMode(Context context) {
        if (context == null) {
            return;
        }
        if (RongExtensionCacheHelper.isVoiceInputMode(context, mConversationType, mTargetId)) {
            mInputModeLiveData.postValue(InputMode.VoiceInput);
        } else {
            collapseExtensionBoard();
        }
    }

    /**
     * 获取 EditText 控件
     *
     * @return EditText 控件
     */
    public EditText getEditTextWidget() {
        return mEditText;
    }

    MutableLiveData<Boolean> getAttachedInfoState() {
        return mAttachedInfoState;
    }

    /**
     * 获取面板打开状态。 value < 0 面板收起； value > 0, 代表面板打开，value 为面板打开后的高度。
     *
     * @return 面板状态 LiveData
     */
    public MutableLiveData<Boolean> getExtensionBoardState() {
        return mExtensionBoardState;
    }

    /**
     * 获取输入模式的 LiveData
     *
     * @return 输入模式对应的  LiveData
     */
    public MutableLiveData<InputMode> getInputModeLiveData() {
        return mInputModeLiveData;
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        private int start;
        private int count;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            this.start = start;
            this.count = count;

            int cursor, offset;
            if (count == 0) {
                cursor = start + before;
                offset = -before;
            } else {
                cursor = start;
                offset = count;
            }
            for (IExtensionEventWatcher watcher : RongExtensionManager.getInstance().getExtensionEventWatcher()) {
                watcher.onTextChanged(getApplication().getApplicationContext(), mConversationType, mTargetId, cursor, offset, s.toString());
            }

            if (mInputModeLiveData.getValue() != InputMode.EmoticonMode
                    && mInputModeLiveData.getValue() != InputMode.RecognizeMode) {
                mInputModeLiveData.postValue(InputMode.TextInput);
                if (mEditText.getText() != null && mEditText.getText().length() > 0) {
                    mEditText.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setSoftInputKeyBoard(true);
                        }
                    }, 100);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (AndroidEmoji.isEmoji(s.subSequence(start, start + count).toString())) {
                mEditText.removeTextChangedListener(this);
                String resultStr = AndroidEmoji.replaceEmojiWithText(s.toString());
                mEditText.setText(AndroidEmoji.ensure(resultStr), TextView.BufferType.SPANNABLE);
                mEditText.setSelection(mEditText.getText().length());
                mEditText.addTextChangedListener(this);
            }
        }
    };

}
