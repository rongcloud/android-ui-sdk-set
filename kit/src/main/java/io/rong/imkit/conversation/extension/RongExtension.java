package io.rong.imkit.conversation.extension;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.component.emoticon.EmoticonBoard;
import io.rong.imkit.conversation.extension.component.inputpanel.InputPanel;
import io.rong.imkit.conversation.extension.component.moreaction.MoreInputPanel;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.extension.component.plugin.IPluginRequestPermissionResultCallback;
import io.rong.imkit.conversation.extension.component.plugin.PluginBoard;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.uievent.InputBarEvent;
import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imkit.feature.mention.IExtensionEventWatcher;
import io.rong.imkit.feature.mention.RongMentionManager;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imlib.model.Conversation;

public class RongExtension extends LinearLayout {
    private String TAG = RongExtension.class.getSimpleName();
    private Fragment mFragment;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private ViewGroup mRoot;
    private RongExtensionViewModel mExtensionViewModel;
    private MessageViewModel mMessageViewModel;

    private RelativeLayout mAttachedInfoContainer;
    private RelativeLayout mBoardContainer;
    private RelativeLayout mInputPanelContainer;
    private InputPanel mInputPanel;
    private EmoticonBoard mEmoticonBoard;
    private PluginBoard mPluginBoard;
    private InputPanel.InputStyle mInputStyle;
    private MoreInputPanel mMoreInputPanel;
    private InputMode mPreInputMode;

    /**
     * RongExtension 构造方法.
     *
     * @param context 上下文
     */
    public RongExtension(Context context) {
        super(context);
        initView(context);
    }

    /**
     * RongExtension 构造方法.
     *
     * @param context 上下文
     * @param attrs   View 的属性集合
     */
    public RongExtension(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RongExtension);
        int attr = a.getInt(R.styleable.RongExtension_RCStyle, 0x123);
        a.recycle();
        mInputStyle = InputPanel.InputStyle.getStyle(attr);
        initView(context);
    }

    private void initView(Context context) {
        mRoot = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.rc_extension_board, this, true);
        mAttachedInfoContainer = mRoot.findViewById(R.id.rc_ext_attached_info_container);
        mInputPanelContainer = mRoot.findViewById(R.id.rc_ext_input_container);
        mBoardContainer = mRoot.findViewById(R.id.rc_ext_board_container);
    }

    public void bindToConversation(Fragment fragment, Conversation.ConversationType conversationType, String targetId) {
        mFragment = fragment;
        mConversationType = conversationType;
        mTargetId = targetId;

        mExtensionViewModel = new ViewModelProvider(mFragment).get(RongExtensionViewModel.class);
        mExtensionViewModel.getAttachedInfoState().observe(mFragment, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isVisible) {
                mAttachedInfoContainer.setVisibility(isVisible ? VISIBLE : GONE);
            }
        });
        mExtensionViewModel.getExtensionBoardState().observe(mFragment, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
                if (!value) {
                    mBoardContainer.setVisibility(GONE);
                }
            }
        });
        mMessageViewModel = new ViewModelProvider(mFragment).get(MessageViewModel.class);
        mMessageViewModel.getPageEventLiveData().observe(mFragment, new Observer<PageEvent>() {
            @Override
            public void onChanged(PageEvent pageEvent) {
                if (pageEvent instanceof InputBarEvent) {
                    if (((InputBarEvent) pageEvent).mType.equals(InputBarEvent.Type.ReEdit)) {
                        insertToEditText(((InputBarEvent) pageEvent).mExtra);
                    } else if (((InputBarEvent) pageEvent).mType.equals(InputBarEvent.Type.ShowMoreMenu)) {
                        mExtensionViewModel.getInputModeLiveData().postValue(InputMode.MoreInputMode);
                    } else if (((InputBarEvent) pageEvent).mType.equals(InputBarEvent.Type.HideMoreMenu)) {
                        resetToDefaultView();
                    } else if (((InputBarEvent) pageEvent).mType.equals(InputBarEvent.Type.ActiveMoreMenu) && mMoreInputPanel != null) {
                        mMoreInputPanel.refreshView(true);
                    } else if (((InputBarEvent) pageEvent).mType.equals(InputBarEvent.Type.InactiveMoreMenu) && mMoreInputPanel != null) {
                        mMoreInputPanel.refreshView(false);
                    }
                }
            }
        });
        mEmoticonBoard = new EmoticonBoard(fragment, mBoardContainer, conversationType, targetId);
        mPluginBoard = new PluginBoard(fragment, mBoardContainer, conversationType, targetId);
        mInputPanel = new InputPanel(fragment, mInputPanelContainer, mInputStyle,
                conversationType, targetId);

        if (mInputPanelContainer.getChildCount() <= 0) {
            mInputPanelContainer.addView(mInputPanel.getRootView());
        }
        mExtensionViewModel.setAttachedConversation(conversationType, targetId, mInputPanel.getEditText());
        mExtensionViewModel.getInputModeLiveData().observe(mFragment, new Observer<InputMode>() {
            @Override
            public void onChanged(InputMode inputMode) {
                mPreInputMode = inputMode;
                updateInputMode(inputMode);
            }
        });
        for (IExtensionModule module : RongExtensionManager.getInstance().getExtensionModules()) {
            module.onAttachedToExtension(fragment, this);
        }
    }

    /**
     * ConversationFragment onResume() 时的生命周期回调。
     */
    public void onResume() {
        if (mExtensionViewModel == null) {
            return;
        }
        final EditText editText = mExtensionViewModel.getEditTextWidget();
        if (editText != null && (editText.getText().length() > 0 || editText.isFocused())) {
            this.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mExtensionViewModel.forceSetSoftInputKeyBoard(true);
                }
            }, 100);

            editText.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                        int cursorPos = editText.getSelectionStart();
                        RongMentionManager.getInstance().onDeleteClick(mConversationType, mTargetId, editText, cursorPos);
                    }
                    return false;
                }
            });
        }
    }

    public void setAttachedInfo(View view) {
        mAttachedInfoContainer.removeAllViews();
        if (view != null) {
            mAttachedInfoContainer.addView(view);
        }
        mAttachedInfoContainer.setVisibility(VISIBLE);
    }

    /**
     * 获取 extension 各组成部分的容器
     *
     * @param type 容器类型
     * @return 容器
     */
    public RelativeLayout getContainer(ContainerType type) {
        if (type == null) {
            return null;
        }
        if (type.equals(ContainerType.ATTACH)) {
            return mAttachedInfoContainer;
        } else if (type.equals(ContainerType.INPUT)) {
            return mInputPanelContainer;
        } else {
            return mBoardContainer;
        }
    }

    public InputPanel getInputPanel() {
        return mInputPanel;
    }

    public PluginBoard getPluginBoard() {
        return mPluginBoard;
    }

    public EmoticonBoard getEmoticonBoard() {
        return mEmoticonBoard;
    }

    public void resetToDefaultView() {
        mInputPanelContainer.removeAllViews();
        if (mInputPanel == null) {
            mInputPanel = new InputPanel(mFragment, mInputPanelContainer, mInputStyle, mConversationType, mTargetId);
        }
        mExtensionViewModel.setEditTextWidget(mInputPanel.getEditText());
        mInputPanelContainer.addView(mInputPanel.getRootView());
        if (mFragment.getContext() != null) {
            mAttachedInfoContainer.removeAllViews();
            mAttachedInfoContainer.setVisibility(GONE);
            updateInputMode(RongExtensionCacheHelper.isVoiceInputMode(mFragment.getContext(), mConversationType, mTargetId) ? InputMode.VoiceInput : InputMode.TextInput);
        }
    }

    public void updateInputMode(InputMode inputMode) {
        if (inputMode == null) {
            return;
        }
        RLog.d(TAG, "update to inputMode:" + inputMode);
        if (inputMode.equals(InputMode.TextInput)) {
            //文本输入模式下，默认有焦点或有输入内容时，才会弹出软键盘。
            //若有特殊情况，需要各业务模块手动调用 RongExtensionViewModel 的 setSoftInputKeyBoard() 方法主动弹起软键盘
            EditText editText = mExtensionViewModel.getEditTextWidget();
            if (editText == null || editText.getText() == null) return;
            if (isEditTextSameProperty(editText)) return;
            RLog.d(TAG, "update for TextInput mode");
            mInputPanelContainer.setVisibility(VISIBLE);
            mBoardContainer.setVisibility(GONE);
            if ((editText.isFocused() || editText.getText().length() > 0)) {
                this.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mFragment != null && mFragment.getActivity() != null && !mFragment.getActivity().isFinishing()) {
                            mExtensionViewModel.setSoftInputKeyBoard(true);
                            mExtensionViewModel.getExtensionBoardState().setValue(true);
                        }
                    }
                }, 100);
            } else {
                mExtensionViewModel.setSoftInputKeyBoard(false);
                mExtensionViewModel.getExtensionBoardState().setValue(false);
            }
        } else if (inputMode.equals(InputMode.VoiceInput)) {
            mInputPanelContainer.setVisibility(VISIBLE);
            mBoardContainer.setVisibility(GONE);
            mExtensionViewModel.setSoftInputKeyBoard(false);
            mExtensionViewModel.getExtensionBoardState().setValue(false);
        } else if (inputMode.equals(InputMode.EmoticonMode)) {
            mExtensionViewModel.setSoftInputKeyBoard(false);
            this.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBoardContainer.removeAllViews();
                    mBoardContainer.addView(mEmoticonBoard.getView());
                    mBoardContainer.setVisibility(VISIBLE);
                    mExtensionViewModel.getExtensionBoardState().setValue(true);
                }
            }, 100);
        } else if (inputMode.equals(InputMode.PluginMode)) {
            mBoardContainer.removeAllViews();
            mBoardContainer.addView(mPluginBoard.getView());
            mBoardContainer.setVisibility(VISIBLE);
            mExtensionViewModel.setSoftInputKeyBoard(false);
            mExtensionViewModel.getExtensionBoardState().setValue(true);
        } else if (inputMode.equals(InputMode.MoreInputMode)) {
            mInputPanelContainer.setVisibility(GONE);
            mBoardContainer.setVisibility(GONE);
            if (mMoreInputPanel == null) {
                mMoreInputPanel = new MoreInputPanel(mFragment, mAttachedInfoContainer);
            }
            mAttachedInfoContainer.removeAllViews();
            mAttachedInfoContainer.addView(mMoreInputPanel.getRootView());
            mAttachedInfoContainer.setVisibility(VISIBLE);
            mExtensionViewModel.setSoftInputKeyBoard(false);
            mExtensionViewModel.getExtensionBoardState().setValue(false);
        }

    }

    /**
     * 收起面板。
     * 兼容旧版本，保留此方法。
     * 推荐使用 ViewModel 对应方法。
     */
    public void collapseExtension() {
        RLog.d(TAG, "collapseExtension");
        mExtensionViewModel.collapseExtensionBoard();
    }

    /**
     * 在 plugin 界面添加自定义 view，添加后，+ 号区域全部填充为自定义的 view。
     * 当自定义 view 可见时点击 ”+“ 会触发自定义 view 和默认 plugin 界面间进行切换。
     *
     * @param v 自定义 view
     */
    public void addPluginPager(View v) {
        if (null != mPluginBoard) {
            mPluginBoard.addPager(v);
        }
    }

    public EditText getInputEditText() {
        return mInputPanel.getEditText();
    }

    /**
     * 获取当前 Extension 所在会话的会话类型。
     *
     * @return 会话类型。
     */
    public Conversation.ConversationType getConversationType() {
        return mConversationType;
    }

    /**
     * 获取当前所在会话的 targetId。
     *
     * @return 目标 id。
     */
    public String getTargetId() {
        return mTargetId;
    }

    public void requestPermissionForPluginResult(String[] permissions, int requestCode, IPluginModule pluginModule) {
        if ((requestCode & 0xffffff00) != 0) {
            throw new IllegalArgumentException("requestCode must less than 256");
        }
        int position = mPluginBoard.getPluginPosition(pluginModule);
        int req = ((position + 1) << 8) + (requestCode & 0xff);
        PermissionCheckUtil.requestPermissions(mFragment, permissions, req);
    }

    public boolean onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int position = (requestCode >> 8) - 1;
        int reqCode = requestCode & 0xFF;
        IPluginModule pluginModule = mPluginBoard.getPluginModule(position);
        if (pluginModule instanceof IPluginRequestPermissionResultCallback) {
            return ((IPluginRequestPermissionResultCallback) pluginModule).onRequestPermissionResult(mFragment, this, reqCode, permissions, grantResults);
        }
        return false;
    }

    /**
     * @param intent      The intent to start.
     * @param requestCode If >= 0, this code will be returned in
     *                    onActivityResult() when the activity exits.
     */
    public void startActivityForPluginResult(Intent intent, int requestCode, IPluginModule pluginModule) {
        if ((requestCode & 0xffffff00) != 0) {
            throw new IllegalArgumentException("requestCode must less than 256.");
        }
        int position = mPluginBoard.getPluginPosition(pluginModule);
        mFragment.startActivityForResult(intent, ((position + 1) << 8) + (requestCode & 0xff));
    }

    /**
     * activity 结束返回结果。
     */
    public void onActivityPluginResult(int requestCode, int resultCode, Intent data) {
        int position = (requestCode >> 8) - 1;
        int reqCode = requestCode & 0xff;
        IPluginModule pluginModule = mPluginBoard.getPluginModule(position);
        if (pluginModule != null) {
            pluginModule.onActivityResult(reqCode, resultCode, data);
        }
    }

    public void onDestroy() {
        //todo 内部组件抽象出基础接口，统一调用。
        if (mInputPanel != null) {
            mInputPanel.onDestroy();
        }

        for (IExtensionEventWatcher watcher : RongExtensionManager.getInstance().getExtensionEventWatcher()) {
            watcher.onDestroy(mConversationType, mTargetId);
        }
        for (IExtensionModule extensionModule : RongExtensionManager.getInstance().getExtensionModules()) {
            extensionModule.onDetachedFromExtension();
        }
    }

    private void insertToEditText(String content) {
        EditText editText = mExtensionViewModel.getEditTextWidget();
        int len = content.length();
        int cursorPos = editText.getSelectionStart();
        editText.getEditableText().insert(cursorPos, content);
        editText.setSelection(cursorPos + len);
    }

    /**
     * 此方法是为了判断文本输入模式下，控件属性是否发生变化，避免频繁刷新。
     *
     * @param editText 输入控件
     * @return 属性是否和之前一致。如果一致，上层直接返回，不再进行刷新处理。
     */
    private boolean isEditTextSameProperty(EditText editText) {
        return (mPreInputMode.equals(InputMode.TextInput)
                && (editText.isFocused() || editText.getText().length() > 0)
                && mExtensionViewModel.isSoftInputShow());
    }

    public enum ContainerType {
        ATTACH,  // 附属容器
        INPUT,  // 输入条容器
        BOARD, //扩展面板容器
    }
}
