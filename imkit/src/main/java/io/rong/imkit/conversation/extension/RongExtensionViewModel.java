package io.rong.imkit.conversation.extension;

import android.app.Application;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import io.rong.common.rlog.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.feature.destruct.DestructManager;
import io.rong.imkit.feature.mention.IExtensionEventWatcher;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.picture.tools.ToastUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.ConversationIdentifier;
import io.rong.message.TextMessage;
import java.util.Objects;

public class RongExtensionViewModel extends AndroidViewModel {
    private final String TAG = this.getClass().getSimpleName();
    private MutableLiveData<Boolean> mExtensionBoardState;
    private MutableLiveData<InputMode> mInputModeLiveData;
    private MutableLiveData<Boolean> mAttachedInfoState;
    private ConversationIdentifier mConversationIdentifier;
    private EditText mEditText;
    private boolean isSoftInputShow;
    private static final int MAX_MESSAGE_LENGTH_TO_SEND = 5500;
    private TextWatcher mTextWatcher =
            new TextWatcher() {
                private int start;
                private int count;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // do nothing
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
                    RongMentionManager.getInstance()
                            .onTextChanged(
                                    getApplication().getApplicationContext(),
                                    mConversationIdentifier.getType(),
                                    mConversationIdentifier.getTargetId(),
                                    cursor,
                                    offset,
                                    s.toString(),
                                    mEditText);
                    for (IExtensionEventWatcher watcher :
                            RongExtensionManager.getInstance().getExtensionEventWatcher()) {
                        watcher.onTextChanged(
                                getApplication().getApplicationContext(),
                                mConversationIdentifier.getType(),
                                mConversationIdentifier.getTargetId(),
                                cursor,
                                offset,
                                s.toString());
                    }

                    if (mInputModeLiveData.getValue() != InputMode.EmoticonMode
                            && mInputModeLiveData.getValue() != InputMode.RecognizeMode) {
                        mInputModeLiveData.postValue(InputMode.TextInput);
                        if (mEditText.getText() != null && mEditText.getText().length() > 0) {
                            mEditText.postDelayed(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            setSoftInputKeyBoard(true);
                                        }
                                    },
                                    100);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            };

    public RongExtensionViewModel(@NonNull Application application) {
        super(application);
        mExtensionBoardState = new MutableLiveData<>();
        mInputModeLiveData = new MutableLiveData<>();
        mAttachedInfoState = new MutableLiveData<>();
    }

    void setAttachedConversation(ConversationIdentifier conversationIdentifier, EditText editText) {
        mConversationIdentifier = conversationIdentifier;
        mEditText = editText;
        mEditText.addTextChangedListener(mTextWatcher);
        if (mConversationIdentifier.getType().equals(Conversation.ConversationType.GROUP)
                || mConversationIdentifier
                        .getType()
                        .equals(Conversation.ConversationType.ULTRA_GROUP)) {
            RongMentionManager.getInstance()
                    .createInstance(
                            mConversationIdentifier.getType(),
                            mConversationIdentifier.getTargetId(),
                            mEditText); // todo 更改实现方式，由 mention 模块 addTextWatcher.
        }
    }

    public void onSendClick() {
        if (TextUtils.isEmpty(mEditText.getText())
                || TextUtils.isEmpty(mEditText.getText().toString().trim())) {
            RLog.d(TAG, "can't send empty content.");
            mEditText.setText("");
            return;
        }

        String text = mEditText.getText().toString();
        if (text.length() > MAX_MESSAGE_LENGTH_TO_SEND) {
            ToastUtils.s(
                    getApplication().getApplicationContext(),
                    getApplication().getString(R.string.rc_message_too_long));
            RLog.d(TAG, "The text you entered is too long to send.");
            return;
        }
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
        io.rong.imlib.model.Message message =
                io.rong.imlib.model.Message.obtain(mConversationIdentifier, textMessage);
        RongMentionManager.getInstance().onSendToggleClick(message, mEditText);
        if (RongExtensionManager.getInstance().getExtensionEventWatcher().size() > 0) {
            for (IExtensionEventWatcher watcher :
                    RongExtensionManager.getInstance().getExtensionEventWatcher()) {
                watcher.onSendToggleClick(message);
            }
        }
        IMCenter.getInstance()
                .sendMessage(
                        message,
                        DestructManager.isActive()
                                ? getApplication()
                                        .getResources()
                                        .getString(R.string.rc_conversation_summary_content_burn)
                                : null,
                        null,
                        null);
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
        if (RongExtensionCacheHelper.isVoiceInputMode(
                context,
                mConversationIdentifier.getType(),
                mConversationIdentifier.getTargetId())) {
            mInputModeLiveData.postValue(InputMode.VoiceInput);
        } else {
            collapseExtensionBoard();
        }
    }

    /** 收起面板，RongExtension 仅显示 InputPanel。 */
    public void collapseExtensionBoard() {
        if (mExtensionBoardState.getValue() != null
                && mExtensionBoardState.getValue().equals(false)) {
            RLog.d(TAG, "already collapsed, return directly.");
            return;
        }
        RLog.d(TAG, "collapseExtensionBoard");
        setSoftInputKeyBoard(false);
        mExtensionBoardState.postValue(false);
        if (!DestructManager.isActive()) {
            mInputModeLiveData.postValue(InputMode.NormalMode);
        }
    }

    public void setSoftInputKeyBoard(boolean isShow) {
        forceSetSoftInputKeyBoard(isShow);
    }

    public void setSoftInputKeyBoard(boolean isShow, boolean clearFocus) {
        forceSetSoftInputKeyBoard(isShow, clearFocus);
    }

    public void forceSetSoftInputKeyBoard(boolean isShow) {
        forceSetSoftInputKeyBoard(isShow, true);
    }

    public void forceSetSoftInputKeyBoard(boolean isShow, boolean clearFocus) {
        if (mEditText == null) {
            return;
        }
        InputMethodManager imm =
                (InputMethodManager)
                        getApplication()
                                .getApplicationContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (isShow) {
                mEditText.requestFocus();
                imm.showSoftInput(mEditText, 0);
            } else {
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
                if (clearFocus) {
                    mEditText.clearFocus();
                }
            }
            isSoftInputShow = isShow;
        }
        if (isShow
                && mExtensionBoardState.getValue() != null
                && mExtensionBoardState.getValue().equals(false)) {
            mExtensionBoardState.setValue(true);
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

    public void setEditTextWidget(EditText editText) {
        if (!Objects.equals(mEditText, editText)) {
            mEditText = editText;
            mEditText.addTextChangedListener(mTextWatcher);
        }
    }

    MutableLiveData<Boolean> getAttachedInfoState() {
        return mAttachedInfoState;
    }

    /**
     * 获取面板打开状态。 {@code value < 0 } 面板收起； {@code value > 0}, 代表面板打开，value 为面板打开后的高度。
     *
     * @return 面板状态 LiveData
     */
    public MutableLiveData<Boolean> getExtensionBoardState() {
        return mExtensionBoardState;
    }

    /**
     * 获取输入模式的 LiveData
     *
     * @return 输入模式对应的 LiveData
     */
    public MutableLiveData<InputMode> getInputModeLiveData() {
        return mInputModeLiveData;
    }
}
