package io.rong.imkit.feature.editmessage;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.InputMode;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.conversation.extension.component.emoticon.EmoticonBoard;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.usermanage.interfaces.OnDataChangeEnhancedListener;
import io.rong.imkit.utils.RongViewUtils;
import io.rong.imkit.utils.keyboard.KeyboardHeightObserver;
import io.rong.imkit.widget.RongEditText;
import io.rong.imlib.IRongCoreCallback;
import io.rong.imlib.IRongCoreEnum;
import io.rong.imlib.model.ConversationIdentifier;

public class EditMessageInputPanel implements KeyboardHeightObserver {
    private View mRootView;
    private RongExtensionViewModel mExtensionViewModel;
    private ConversationIdentifier mConversationIdentifier;
    private Fragment mFragment;

    // 布局中的控件
    private TextView mReferenceContent;
    private RongEditText mEditText;
    private ImageView mExpandButton;
    private ImageView mEmojiButton;
    private LinearLayout mEditTimeoutContainer;
    private ImageView mCancelButton;
    private ImageView mConfirmButton;

    // 全屏输入的Dialog
    private Dialog mExpandDialog;
    // 是否编辑过期
    private boolean modifiable = false;

    @SuppressLint("ClickableViewAccessibility")
    EditMessageInputPanel(
            Fragment fragment, ViewGroup parent, ConversationIdentifier conversationIdentifier) {
        if (fragment == null || fragment.getContext() == null) {
            return;
        }
        mFragment = fragment;
        mConversationIdentifier = conversationIdentifier;
        mRootView =
                LayoutInflater.from(fragment.getContext())
                        .inflate(R.layout.rc_edit_message_input_panel, parent, false);

        // 初始化布局中的控件
        mReferenceContent = mRootView.findViewById(R.id.rc_reference_content);
        mEditText = mRootView.findViewById(R.id.rc_edit_btn);
        mExpandButton = mRootView.findViewById(R.id.rc_edit_message_expand_btn);
        mEmojiButton = mRootView.findViewById(R.id.rc_edit_message_emoji_btn);
        mEditTimeoutContainer = mRootView.findViewById(R.id.rc_edit_timeout_container);
        mCancelButton = mRootView.findViewById(R.id.rc_edit_message_cancel_btn);
        mConfirmButton = mRootView.findViewById(R.id.rc_edit_message_confirm_btn);

        mReferenceContent.setText("");
        mExpandButton.setOnClickListener(view -> expandInputView());
        mEmojiButton.setOnClickListener(
                view ->
                        mExtensionViewModel
                                .getInputModeLiveData()
                                .postValue(InputMode.EmoticonMode));
        mCancelButton.setOnClickListener(v -> EditMessageManager.getInstance().exitEditMode());
        mConfirmButton.setOnClickListener(
                view -> {
                    if (!modifiable) {
                        mEditTimeoutContainer.setVisibility(VISIBLE);
                        mConfirmButton.setImageResource(R.drawable.rc_edit_message_confirm_unable);
                        return;
                    }
                    IRongCoreCallback.OperationCallback callback =
                            new IRongCoreCallback.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    EditMessageManager.getInstance().exitEditMode();
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode code) {
                                    if (IRongCoreEnum.CoreErrorCode.RC_MODIFIED_MESSAGE_TIMEOUT
                                            == code) {
                                        modifiable = false;
                                        mEditTimeoutContainer.setVisibility(VISIBLE);
                                        mConfirmButton.setImageResource(
                                                R.drawable.rc_edit_message_confirm_unable);
                                    }
                                }
                            };
                    EditMessageManager.getInstance().editMessage(mEditText, callback);
                });
        mEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (!modifiable) {
                            mEditTimeoutContainer.setVisibility(VISIBLE);
                            mConfirmButton.setImageResource(
                                    R.drawable.rc_edit_message_confirm_unable);
                        } else {
                            boolean isClear = TextUtils.isEmpty(editable.toString());
                            mConfirmButton.setImageResource(
                                    isClear
                                            ? R.drawable.rc_edit_message_confirm_unable
                                            : R.drawable.rc_edit_message_confirm_enable);
                            mConfirmButton.setClickable(!isClear);
                        }
                    }
                });
        mEditText.setOnBackspaceListener(
                new OnDataChangeEnhancedListener<Integer>() {
                    @Override
                    public void onDataChange(Integer position) {
                        if (position == null || mFragment == null) {
                            return;
                        }
                        RongMentionManager.getInstance()
                                .onDeleteClick(
                                        mConversationIdentifier.getType(),
                                        mConversationIdentifier.getTargetId(),
                                        mEditText,
                                        position);
                    }
                });

        mExtensionViewModel = new ViewModelProvider(fragment).get(RongExtensionViewModel.class);

        EditMessageManager.getInstance().addKeyboardHeightObserver(this);
    }

    public void setContent(String content, String referContent, boolean showKeyBoard) {
        mEditText.setText(content, false);
        mEditText.setSelection(content.length());
        if (showKeyBoard) {
            mEditText.requestFocus();
            mExtensionViewModel.forceSetSoftInputKeyBoard(true);
        }
        if (!TextUtils.isEmpty(referContent)) {
            mReferenceContent.setVisibility(VISIBLE);
            mReferenceContent.setText(referContent);
        }
    }

    /** 展开全屏输入页面 展开过程：1，收起软键盘；2，停顿100ms，从下往上打开全屏输入页面，打开软键盘 */
    private void expandInputView() {
        if (mFragment == null || mFragment.getContext() == null) {
            return;
        }

        // 1. 收起软键盘
        if (mExtensionViewModel != null) {
            mExtensionViewModel.forceSetSoftInputKeyBoard(false);
        }

        // 2. 停顿100ms后执行展开动画，从下往上打开全屏输入页面
        mExpandButton.postDelayed(this::showExpandDialog, 200);
    }

    /**
     * 收起全屏输入页面
     *
     * @param expandView 全屏输入页面的视图，如果为null则通过Dialog获取
     */
    private void collapseExpandView(View expandView, boolean exitEditMode) {
        // 收起全屏输入页面，收起软键盘
        if (mExtensionViewModel != null) {
            mExtensionViewModel.forceSetSoftInputKeyBoard(false);
        }
        mExpandButton.postDelayed(() -> hideExpandDialog(expandView, exitEditMode), 300);
    }

    /** 显示全屏输入对话框 */
    private void showExpandDialog() {
        if (mFragment == null || mFragment.getContext() == null) {
            return;
        }
        EditMessageManager.getInstance().setEmoticonMode(false);
        Context context = mFragment.getContext();
        mExpandDialog = new Dialog(context, R.style.FullScreenDialogTheme);

        View expandView =
                LayoutInflater.from(context)
                        .inflate(R.layout.rc_edit_message_input_panel_expand, null);

        // 初始化全屏输入页面的控件并设置事件监听器
        ViewGroup expandTimeoutContainer =
                expandView.findViewById(R.id.rc_edit_timeout_expand_container);
        RongEditText expandEditText = expandView.findViewById(R.id.rc_edit_btn_expand);
        TextView expandReferContent = expandView.findViewById(R.id.rc_reference_content);
        ImageView expandCollapseButton = expandView.findViewById(R.id.rc_edit_message_collapse_btn);
        ImageView expandConfirmButton = expandView.findViewById(R.id.rc_edit_message_confirm_btn);
        ImageView expandCancelButton = expandView.findViewById(R.id.rc_edit_message_cancel_btn);
        ImageView expandEmojiButton = expandView.findViewById(R.id.rc_edit_message_emoji_btn);
        ViewGroup expandEmojiBoardContainer =
                expandView.findViewById(R.id.rc_emoji_board_container);
        // 设置emoji表情面板，但是隐藏状态的。
        EmoticonBoard mEmoticonBoard =
                new EmoticonBoard(
                        mFragment,
                        expandEmojiBoardContainer,
                        mConversationIdentifier.getType(),
                        mConversationIdentifier.getTargetId(),
                        false);
        RongViewUtils.addView(expandEmojiBoardContainer, mEmoticonBoard.getView());
        expandEmojiBoardContainer.setVisibility(GONE);
        // 同步当前输入内容到全屏页面
        if (mEditText != null && !TextUtils.isEmpty(mEditText.getText())) {
            int selectionStart = mEditText.getSelectionStart();
            int selectionEnd = mEditText.getSelectionEnd();
            expandEditText.setText(mEditText.getText().toString(), false);
            expandEditText.setSelection(selectionStart, selectionEnd);
        }
        if (mReferenceContent != null && !TextUtils.isEmpty(mReferenceContent.getText())) {
            expandReferContent.setText(mReferenceContent.getText().toString());
        }
        // 设置是否可以编辑状态
        if (!modifiable) {
            expandTimeoutContainer.setVisibility(VISIBLE);
            expandConfirmButton.setImageResource(R.drawable.rc_edit_message_confirm_unable);
            expandConfirmButton.setClickable(false);
        } else {
            boolean isClear =
                    mEditText == null
                            || mEditText.getText() == null
                            || TextUtils.isEmpty(mEditText.getText().toString());
            expandConfirmButton.setImageResource(
                    isClear
                            ? R.drawable.rc_edit_message_confirm_unable
                            : R.drawable.rc_edit_message_confirm_enable);
            expandConfirmButton.setClickable(!isClear);
        }
        expandEditText.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (!modifiable) {
                            expandTimeoutContainer.setVisibility(VISIBLE);
                            expandConfirmButton.setImageResource(
                                    R.drawable.rc_edit_message_confirm_unable);
                            expandConfirmButton.setClickable(false);
                        } else {
                            boolean isClear = TextUtils.isEmpty(editable.toString());
                            expandConfirmButton.setImageResource(
                                    isClear
                                            ? R.drawable.rc_edit_message_confirm_unable
                                            : R.drawable.rc_edit_message_confirm_enable);
                            expandConfirmButton.setClickable(!isClear);
                        }
                    }
                });
        expandEditText.setOnBackspaceListener(
                new OnDataChangeEnhancedListener<Integer>() {
                    @Override
                    public void onDataChange(Integer position) {
                        if (position == null || mFragment == null) {
                            return;
                        }
                        RongMentionManager.getInstance()
                                .onDeleteClick(
                                        mConversationIdentifier.getType(),
                                        mConversationIdentifier.getTargetId(),
                                        expandEditText,
                                        position);
                    }
                });

        // 设置收起按钮点击事件
        expandCollapseButton.setOnClickListener(v -> collapseExpandView(expandView, false));

        // 设置确认按钮点击事件
        expandConfirmButton.setOnClickListener(
                v -> {
                    if (!modifiable) {
                        expandTimeoutContainer.setVisibility(VISIBLE);
                        expandConfirmButton.setImageResource(
                                R.drawable.rc_edit_message_confirm_unable);
                        return;
                    }
                    IRongCoreCallback.OperationCallback callback =
                            new IRongCoreCallback.OperationCallback() {
                                @Override
                                public void onSuccess() {
                                    collapseExpandView(expandView, true);
                                }

                                @Override
                                public void onError(IRongCoreEnum.CoreErrorCode code) {
                                    if (IRongCoreEnum.CoreErrorCode.RC_MODIFIED_MESSAGE_TIMEOUT
                                            == code) {
                                        modifiable = false;
                                        expandTimeoutContainer.setVisibility(VISIBLE);
                                        expandConfirmButton.setImageResource(
                                                R.drawable.rc_edit_message_confirm_unable);
                                    }
                                }
                            };
                    EditMessageManager.getInstance().editMessage(expandEditText, callback);
                });
        // 设置取消按钮点击事件
        expandCancelButton.setOnClickListener(view -> collapseExpandView(expandView, true));
        // 设置Emoji按钮点击事件
        expandEmojiButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditMessageManager.getInstance().setEmoticonMode(true);
                        mExtensionViewModel.forceSetSoftInputKeyBoard(false);
                        ViewGroup emojiBoardContainer =
                                mExpandDialog.findViewById(R.id.rc_emoji_board_container);
                        emojiBoardContainer.setVisibility(VISIBLE);
                    }
                });

        mExpandDialog.setContentView(expandView);
        mExpandDialog.setCancelable(false);

        // 设置对话框属性
        if (mExpandDialog.getWindow() != null) {
            Window window = mExpandDialog.getWindow();
            WindowManager.LayoutParams lp = mExpandDialog.getWindow().getAttributes();
            lp.gravity = Gravity.BOTTOM;
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(lp);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // 取消全屏，允许状态栏显示
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            // 允许自定义状态栏颜色
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 取消半透明状态栏
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 设置状态栏颜色
            window.setStatusBarColor(Color.parseColor("#99000000"));
            // 设置键盘挤压模式
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }
        mExpandDialog.show();
        mExpandDialog.setOnDismissListener(
                dialog -> EditMessageManager.getInstance().setEmoticonMode(false));

        onKeyboardStatusChange(true);

        // 执行从下往上的展开动画
        TranslateAnimation slideUpAnimation =
                new TranslateAnimation(0, 0, expandView.getHeight(), 0);
        slideUpAnimation.setDuration(300);
        slideUpAnimation.setInterpolator(new DecelerateInterpolator());
        slideUpAnimation.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // 动画结束后打开软键盘
                        expandView.postDelayed(
                                () -> {
                                    if (mExtensionViewModel != null) {
                                        mExtensionViewModel.setEditTextWidget(expandEditText);
                                        EditMessageConfig config =
                                                EditMessageManager.getInstance()
                                                        .getEditMessageConfig();
                                        if (config != null) {
                                            // RongMentionManager 重新绑定Edittext对应的MentionList。
                                            EditMessageManager.getInstance()
                                                    .addMentionBlocks(
                                                            expandEditText, config.mentionBlocks);
                                        }
                                        mExtensionViewModel.forceSetSoftInputKeyBoard(true);
                                    }
                                },
                                150);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
        expandView.startAnimation(slideUpAnimation);
    }

    /**
     * 隐藏全屏输入对话框
     *
     * @param expandView 全屏输入页面的视图，如果为null则直接关闭Dialog
     */
    private void hideExpandDialog(View expandView, boolean exitEditMode) {
        if (mExpandDialog == null || !mExpandDialog.isShowing()) {
            return;
        }
        // 非退出编辑模式，同步全屏页面内容回主页面
        if (!exitEditMode) {
            // 恢复原始的EditText
            if (mExtensionViewModel != null && mEditText != null) {
                mExtensionViewModel.setEditTextWidget(mEditText);
                EditMessageConfig config = EditMessageManager.getInstance().getEditMessageConfig();
                if (config != null) {
                    // RongMentionManager 重新绑定Edittext对应的MentionList。
                    EditMessageManager.getInstance()
                            .addMentionBlocks(mEditText, config.mentionBlocks);
                }
                RongEditText expandEditText = expandView.findViewById(R.id.rc_edit_btn_expand);
                if (expandEditText != null) {
                    // 同步全屏页面输入内容到当前输入内容
                    String content = "";
                    if (!TextUtils.isEmpty(expandEditText.getText())) {
                        content = expandEditText.getText().toString();
                    }
                    int selectionStart = expandEditText.getSelectionStart();
                    int selectionEnd = expandEditText.getSelectionEnd();
                    mEditText.setText(content, false);
                    mEditText.setSelection(selectionStart, selectionEnd);
                    // 设置是否可以编辑状态
                    if (!modifiable) {
                        mEditTimeoutContainer.setVisibility(VISIBLE);
                        mConfirmButton.setImageResource(R.drawable.rc_edit_message_confirm_unable);
                        mConfirmButton.setClickable(false);
                    } else {
                        boolean isClear = TextUtils.isEmpty(content);
                        mConfirmButton.setImageResource(
                                isClear
                                        ? R.drawable.rc_edit_message_confirm_unable
                                        : R.drawable.rc_edit_message_confirm_enable);
                        mConfirmButton.setClickable(!isClear);
                    }
                }
            }
        }
        // 关闭全屏弹窗，执行动画
        if (mExpandDialog.getWindow() != null) {
            Window window = mExpandDialog.getWindow();
            // 设置状态栏颜色
            window.setStatusBarColor(Color.TRANSPARENT);
            mExpandDialog.findViewById(R.id.rc_edit_top_bar).setBackgroundColor(Color.TRANSPARENT);
        }
        // 执行从上往下的收起动画
        TranslateAnimation slideDownAnimation =
                new TranslateAnimation(0, 0, 0, expandView.getHeight());
        slideDownAnimation.setDuration(300);
        slideDownAnimation.setInterpolator(new DecelerateInterpolator());

        slideDownAnimation.setAnimationListener(
                new android.view.animation.Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(android.view.animation.Animation animation) {}

                    @Override
                    public void onAnimationEnd(android.view.animation.Animation animation) {
                        if (mExpandDialog != null) {
                            mExpandDialog.dismiss();
                            mExpandDialog = null;
                        }
                        // 退出编辑模式
                        if (exitEditMode) {
                            EditMessageManager.getInstance().exitEditMode();
                            return;
                        }
                        // 弹起软键盘
                        if (mExtensionViewModel != null && mEditText != null) {
                            mEditText.postDelayed(
                                    () -> mExtensionViewModel.forceSetSoftInputKeyBoard(true), 200);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(android.view.animation.Animation animation) {}
                });

        expandView.startAnimation(slideDownAnimation);
    }

    // 监听软键盘弹起落下
    public void onKeyboardStatusChange(boolean isKeyboardShow) {
        if (mExpandDialog == null) {
            return;
        }
        ViewGroup emojiBoardContainer = mExpandDialog.findViewById(R.id.rc_emoji_board_container);
        if (isKeyboardShow) {
            EditMessageManager.getInstance().setEmoticonMode(false);
            emojiBoardContainer.setVisibility(View.INVISIBLE);
        } else {
            emojiBoardContainer.setVisibility(
                    EditMessageManager.getInstance().isEmoticonMode() ? VISIBLE : GONE);
        }
    }

    public void onDestroy() {
        // 清理全屏对话框
        if (mExpandDialog != null && mExpandDialog.isShowing()) {
            mExpandDialog.dismiss();
            mExpandDialog = null;
        }
        mFragment = null;
        mExtensionViewModel = null;
        mExpandButton.setOnClickListener(null);
        mEmojiButton.setOnClickListener(null);
        mCancelButton.setOnClickListener(null);
        mConfirmButton.setOnClickListener(null);
        mEditText.removeAllTextChangedListener();
        mEditText.setOnBackspaceListener(null);
        EditMessageManager.getInstance().removeKeyboardHeightObserver(this);
    }

    View getRootView() {
        return mRootView;
    }

    EditText getEditText() {
        return mEditText;
    }

    public void setCheckMessageModifiableResult(boolean modifiable) {
        this.modifiable = modifiable;
        if (modifiable) {
            mEditTimeoutContainer.setVisibility(GONE);
            mConfirmButton.setImageResource(R.drawable.rc_edit_message_confirm_enable);
        } else {
            mEditTimeoutContainer.setVisibility(VISIBLE);
            mConfirmButton.setImageResource(R.drawable.rc_edit_message_confirm_unable);
        }
    }

    @Override
    public void onKeyboardHeightChanged(int orientation, boolean isOpen, int keyboardHeight) {
        onKeyboardStatusChange(isOpen);
    }
}
