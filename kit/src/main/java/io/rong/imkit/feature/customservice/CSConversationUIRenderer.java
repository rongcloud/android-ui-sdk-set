package io.rong.imkit.feature.customservice;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.conversation.ConversationFragment;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.inputpanel.InputPanel;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.messgelist.processor.IConversationUIRenderer;
import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imkit.feature.customservice.event.CSEvaluateEvent;
import io.rong.imkit.feature.customservice.event.CSExtensionConfigEvent;
import io.rong.imkit.feature.customservice.event.CSExtensionModeEvent;
import io.rong.imkit.feature.customservice.event.CSSelectGroupEvent;
import io.rong.imkit.feature.customservice.event.CSWarningEvent;
import io.rong.imkit.feature.location.plugin.CombineLocationPlugin;
import io.rong.imkit.feature.location.plugin.DefaultLocationPlugin;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.cs.CustomServiceConfig;
import io.rong.imlib.cs.model.CSGroupItem;
import io.rong.imlib.cs.model.CustomServiceMode;
import io.rong.imlib.model.Conversation;

public class CSConversationUIRenderer implements IConversationUIRenderer {
    private final String TAG = CSConversationUIRenderer.class.getSimpleName();
    ConversationFragment mFragment;
    RongExtension mRongExtension;
    Conversation.ConversationType mConversationType;
    String mTargetId;
    CSEvaluateDialog mEvaluateDialog;
    boolean mRobotType = true;

    @Override
    public void init(ConversationFragment fragment, RongExtension rongExtension, Conversation.ConversationType conversationType, String targetId) {
        mFragment = fragment;
        mRongExtension = rongExtension;
        mConversationType = conversationType;
        mTargetId = targetId;
    }

    @Override
    public boolean handlePageEvent(PageEvent pageEvent) {
        if (pageEvent instanceof CSWarningEvent) {
            CSWarningEvent csWarningEvent = (CSWarningEvent) pageEvent;
            showCustomWarning(csWarningEvent.mCSMessage, csWarningEvent.mClickListener);
            return true;
        } else if (pageEvent instanceof CSEvaluateEvent) {
            CSEvaluateDialog.EvaluateDialogType type = ((CSEvaluateEvent) pageEvent).mDialogType;
            boolean hasResolved = ((CSEvaluateEvent) pageEvent).isResolved;
            showCSEvaluate(type, hasResolved);
            return true;
        } else if (pageEvent instanceof CSSelectGroupEvent) {
            onSelectCustomerServiceGroup(((CSSelectGroupEvent) pageEvent).mGroupList);
            return true;
        } else if (pageEvent instanceof CSExtensionConfigEvent) {
            configRongExtension(((CSExtensionConfigEvent) pageEvent).mConfig);
        } else if (pageEvent instanceof CSExtensionModeEvent) {
            setRongExtensionBarMode(((CSExtensionModeEvent) pageEvent).mCustomServiceMode);
        }
        return false;
    }

    void showCustomWarning(String content, final View.OnClickListener clickListener) {
        if (mFragment != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mFragment.getContext());
            builder.setCancelable(false);
            final AlertDialog alertDialog = builder.create();
            alertDialog.show();
            Window window = alertDialog.getWindow();
            if (window == null) {
                return;
            }
            window.setContentView(R.layout.rc_cs_alert_warning);
            TextView tv = window.findViewById(R.id.rc_cs_msg);
            tv.setText(content);

            window.findViewById(R.id.rc_btn_ok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (clickListener != null) {
                        clickListener.onClick(v);
                    } else {
                        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                        alertDialog.dismiss();
                        FragmentManager fm = mFragment.getChildFragmentManager();
                        if (fm.getBackStackEntryCount() > 0) {
                            fm.popBackStack();
                        } else {
                            if (mFragment != null && mFragment.getActivity() != null) {
                                mFragment.getActivity().finish();
                            }
                        }
                    }
                }
            });

        }
    }

    void showCSEvaluate(CSEvaluateDialog.EvaluateDialogType dialogType, boolean isResolved) {
        if (mEvaluateDialog == null && mFragment != null) {
            mEvaluateDialog = new CSEvaluateDialog(mFragment.getContext(), mFragment.getRongExtension().getTargetId());
            mEvaluateDialog.setClickListener(new CSEvaluateDialog.EvaluateClickListener() {
                @Override
                public void onEvaluateSubmit() {
                    if (mEvaluateDialog != null) {
                        mEvaluateDialog.destroy();
                        mEvaluateDialog = null;
                    }
                    destroyConversation();
                }

                @Override
                public void onEvaluateCanceled() {
                    if (mEvaluateDialog != null) {
                        mEvaluateDialog.destroy();
                        mEvaluateDialog = null;
                    }
                    destroyConversation();
                }
            });
            mEvaluateDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (mEvaluateDialog != null) {
                        mEvaluateDialog = null;
                    }
                }
            });
            if (dialogType.equals(CSEvaluateDialog.EvaluateDialogType.STAR)) {
                mEvaluateDialog.showStar("");
            } else if (dialogType.equals(CSEvaluateDialog.EvaluateDialogType.ROBOT)) {
                mEvaluateDialog.showRobot(isResolved);
            } else if (dialogType.equals(CSEvaluateDialog.EvaluateDialogType.STAR_MESSAGE)) {
                mEvaluateDialog.showStarMessage(isResolved);
            }
        }
    }

    /**
     * 如果客服后台有分组,会弹出此对话框选择分组
     * 可以通过自定义类继承自 ConversationFragment 并重写此方法来自定义弹窗
     */
    public void onSelectCustomerServiceGroup(final List<CSGroupItem> groupList) {
        if (mFragment == null || mFragment.getActivity() == null) {
            RLog.w(TAG, "onSelectCustomerServiceGroup Activity has finished");
            return;
        }

        final SingleChoiceDialog singleChoiceDialog;
        List<String> singleDataList = new ArrayList<>();
        singleDataList.clear();
        for (int i = 0; i < groupList.size(); i++) {
            if (groupList.get(i).getOnline()) {
                singleDataList.add(groupList.get(i).getName());
            }
        }
        if (singleDataList.size() == 0) {
            RongIMClient.getInstance().selectCustomServiceGroup(mTargetId, null);
            return;
        }
        singleChoiceDialog = new SingleChoiceDialog(mFragment.getContext(), singleDataList);
        singleChoiceDialog.setTitle(mFragment.getResources().getString(R.string.rc_cs_select_group));
        singleChoiceDialog.setOnOKButtonListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int selItem = singleChoiceDialog.getSelectItem();
                RongIMClient.getInstance().selectCustomServiceGroup(mTargetId, groupList.get(selItem).getId());
            }

        });
        singleChoiceDialog.setOnCancelButtonListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RongIMClient.getInstance().selectCustomServiceGroup(mTargetId, null);
            }
        });
        singleChoiceDialog.show();
    }

    private void configRongExtension(CustomServiceConfig customServiceConfig) {
        if (customServiceConfig.evaEntryPoint.equals(CustomServiceConfig.CSEvaEntryPoint.EVA_EXTENSION)) {
            mRongExtension.getPluginBoard().addPlugin(new EvaluatePlugin(customServiceConfig.isReportResolveStatus));
        }
        if (customServiceConfig.isDisableLocation) {
            List<IPluginModule> defaultPlugins = mRongExtension.getPluginBoard().getPluginModules();
            IPluginModule location = null;
            for (int i = 0; i < defaultPlugins.size(); i++) {
                if (defaultPlugins.get(i) instanceof DefaultLocationPlugin
                        || defaultPlugins.get(i) instanceof CombineLocationPlugin) {
                    location = defaultPlugins.get(i);
                }
            }
            mRongExtension.getPluginBoard().removePlugin(location);
        }
    }

    private void setRongExtensionBarMode(CustomServiceMode customServiceMode) {
        InputPanel inputPanel = mRongExtension.getInputPanel();

        switch (customServiceMode) {
            case CUSTOM_SERVICE_MODE_NO_SERVICE:
            case CUSTOM_SERVICE_MODE_ROBOT:
                inputPanel.setInputPanelStyle(InputPanel.InputStyle.STYLE_CONTAINER);
                break;
            case CUSTOM_SERVICE_MODE_ROBOT_FIRST:
                inputPanel.setInputPanelStyle(InputPanel.InputStyle.STYLE_SWITCH_CONTAINER);
                mRobotType = true;
                ImageView voiceToggle = inputPanel.getRootView().findViewById(R.id.input_panel_voice_toggle);
                voiceToggle.setImageResource(R.drawable.rc_cs_admin_selector);
                voiceToggle.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mRobotType) {
                            RongIMClient.getInstance().switchToHumanMode(mTargetId);
                        }
                    }
                });
                break;
            case CUSTOM_SERVICE_MODE_HUMAN:
            case CUSTOM_SERVICE_MODE_HUMAN_FIRST:
                mRobotType = false;
                break;
        }
    }

    private void destroyConversation() {
        FragmentManager fm = mFragment.getChildFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            if (mFragment != null && mFragment.getActivity() != null) {
                mFragment.getActivity().finish();
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onDestroy() {
        mRongExtension = null;
        mFragment = null;
    }
}
