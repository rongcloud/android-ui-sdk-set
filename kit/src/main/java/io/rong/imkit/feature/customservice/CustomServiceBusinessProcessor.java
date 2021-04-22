package io.rong.imkit.feature.customservice;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.List;

import io.rong.common.RLog;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.conversation.messgelist.processor.BaseBusinessProcessor;
import io.rong.imkit.conversation.messgelist.viewmodel.MessageViewModel;
import io.rong.imkit.event.uievent.PageDestroyEvent;
import io.rong.imkit.feature.customservice.event.CSEvaluateEvent;
import io.rong.imkit.feature.customservice.event.CSExtensionConfigEvent;
import io.rong.imkit.feature.customservice.event.CSExtensionModeEvent;
import io.rong.imkit.feature.customservice.event.CSQuitEvent;
import io.rong.imkit.feature.customservice.event.CSSelectGroupEvent;
import io.rong.imkit.feature.customservice.event.CSWarningEvent;
import io.rong.imkit.model.UiMessage;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.cs.CustomServiceConfig;
import io.rong.imlib.cs.ICustomServiceListener;
import io.rong.imlib.cs.model.CSCustomServiceInfo;
import io.rong.imlib.cs.model.CSGroupItem;
import io.rong.imlib.cs.model.CustomServiceMode;
import io.rong.imlib.model.Conversation;
import io.rong.message.InformationNotificationMessage;


public class CustomServiceBusinessProcessor extends BaseBusinessProcessor implements Handler.Callback {
    private final String TAG = CustomServiceBusinessProcessor.class.getSimpleName();
    private final int CS_HUMAN_MODE_CUSTOMER_EXPIRE = 0;
    private final int CS_HUMAN_MODE_SEAT_EXPIRE = 1;
    /**
     * 进入客服会话，弹出评价菜单超时时间(单位：秒) 设置为 0 时，任何时候离开客服会话时，都会弹出评价菜单.
     */
    private long rc_custom_service_evaluation_interval = 60 * 1000L;
    private WeakReference<MessageViewModel> mWeakMessageViewModel;
    private CSCustomServiceInfo mCustomServiceInfo;
    private long mCSStartTime;
    private boolean mRobotType = true;
    private boolean mNeedEvaluate = true;
    private CustomServiceConfig mCustomServiceConfig;
    private Handler mMainHandler;
    private boolean mStopCSWhenQuit = true;
    private String mTargetId;

    @Override
    public boolean onReceived(MessageViewModel viewModel, UiMessage message, int left, boolean hasPackage, boolean offline) {
        if (mCustomServiceConfig != null && mCustomServiceConfig.userTipTime > 0) {
            startTimer(CS_HUMAN_MODE_CUSTOMER_EXPIRE, mCustomServiceConfig.userTipTime * 60 * 1000);
        }
        return super.onReceived(viewModel, message, left, hasPackage, offline);
    }

    @Override
    public void init(MessageViewModel messageViewModel, Bundle bundle) {
        if (bundle != null) {
            mCustomServiceInfo = (CSCustomServiceInfo) bundle.get(RouteUtils.CUSTOM_SERVICE_INFO);
            if (mCustomServiceInfo == null) {
                RLog.e(TAG, "Please set customServiceInfo to bundle when start custom service conversation!");
                CSCustomServiceInfo.Builder builder = new CSCustomServiceInfo.Builder();
                builder.nickName(messageViewModel.getCurTargetId());
            }
        }
        mWeakMessageViewModel = new WeakReference<>(messageViewModel);
        mTargetId = messageViewModel.getCurTargetId();
        mCSStartTime = System.currentTimeMillis();
        RongIMClient.getInstance().startCustomService(messageViewModel.getCurTargetId(), mCustomServiceListener, mCustomServiceInfo);
    }

    private ICustomServiceListener mCustomServiceListener = new ICustomServiceListener() {
        @Override
        public void onSuccess(CustomServiceConfig config) {
            mCustomServiceConfig = config;
            MessageViewModel messageViewModel = mWeakMessageViewModel.get();
            if (config.isBlack) {
                String csMessage = messageViewModel.getApplication().getResources().getString(R.string.rc_blacklist_prompt);
                if (messageViewModel != null) {
                    messageViewModel.executePageEvent(new CSWarningEvent(csMessage, null));
                }
            }
            if (config.robotSessionNoEva) {
                mNeedEvaluate = false;
            }
            if (messageViewModel != null) {
                messageViewModel.executePageEvent(new CSExtensionConfigEvent(mCustomServiceConfig));
            }
        }

        @Override
        public void onError(int code, String msg) {
            MessageViewModel messageViewModel = mWeakMessageViewModel.get();
            if (messageViewModel != null) {
                messageViewModel.executePageEvent(new CSWarningEvent(msg, null));
            }
        }

        @Override
        public void onModeChanged(CustomServiceMode mode) {
            MessageViewModel messageViewModel = mWeakMessageViewModel.get();
            if (mode.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_HUMAN)
                    || mode.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_HUMAN_FIRST)) {
                if (mCustomServiceConfig != null && mCustomServiceConfig.userTipTime > 0 && !TextUtils.isEmpty(mCustomServiceConfig.userTipWord)) {
                    startTimer(CS_HUMAN_MODE_CUSTOMER_EXPIRE, mCustomServiceConfig.userTipTime * 60 * 1000);
                }
                if (mCustomServiceConfig != null && mCustomServiceConfig.adminTipTime > 0 && !TextUtils.isEmpty(mCustomServiceConfig.adminTipWord)) {
                    startTimer(CS_HUMAN_MODE_SEAT_EXPIRE, mCustomServiceConfig.adminTipTime * 60 * 1000);
                }
                mRobotType = false;
                mNeedEvaluate = true;
            } else if (mode.equals(CustomServiceMode.CUSTOM_SERVICE_MODE_NO_SERVICE)) {
                mNeedEvaluate = false;
            }
            if (messageViewModel != null) {
                messageViewModel.executePageEvent(new CSExtensionModeEvent(mode));
            }
        }

        @Override
        public void onQuit(String msg) {
            MessageViewModel messageViewModel = mWeakMessageViewModel.get();
            RLog.i(TAG, "CustomService onQuit.");
            if (messageViewModel != null) {
                messageViewModel.executePageEvent(new CSQuitEvent(msg, false));
            }
            if (mCustomServiceConfig != null && mCustomServiceConfig.evaEntryPoint.equals(CustomServiceConfig.CSEvaEntryPoint.EVA_END)
                    && !mRobotType) {
                if (messageViewModel != null) {
                    CSEvaluateEvent csEvaluateEvent = new CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType.STAR, false);
                    messageViewModel.executePageEvent(csEvaluateEvent);
                }
            } else {
                CSWarningEvent csWarningEvent = new CSWarningEvent(msg, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MessageViewModel messageViewModel = mWeakMessageViewModel.get();
                        if (mCustomServiceConfig.quitSuspendType.equals(CustomServiceConfig.CSQuitSuspendType.NONE)) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - mCSStartTime <= rc_custom_service_evaluation_interval) {
                                if (messageViewModel != null) {
                                    messageViewModel.executePageEvent(new PageDestroyEvent());
                                }
                            } else {
                                if (messageViewModel != null) {
                                    if (mCustomServiceConfig != null && mCustomServiceConfig.evaluateType.equals(CustomServiceConfig.CSEvaType.EVA_UNIFIED)) {
                                        messageViewModel.executePageEvent(new CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType.STAR_MESSAGE, mCustomServiceConfig.isReportResolveStatus));
                                    } else if (mRobotType) {
                                        messageViewModel.executePageEvent(new CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType.ROBOT, true));
                                    } else {
                                        messageViewModel.executePageEvent(new CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType.STAR, false));
                                    }
                                }
                            }
                        }
                    }
                });
                if (messageViewModel != null) {
                    messageViewModel.executePageEvent(csWarningEvent);
                }
            }
        }

        @Override
        public void onPullEvaluation(String dialogId) {
            MessageViewModel messageViewModel = mWeakMessageViewModel.get();
            if (messageViewModel != null) {
                if (mNeedEvaluate) {
                    if (mCustomServiceConfig != null && mCustomServiceConfig.evaluateType.equals(CustomServiceConfig.CSEvaType.EVA_UNIFIED)) {
                        messageViewModel.executePageEvent(new CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType.STAR_MESSAGE, mCustomServiceConfig.isReportResolveStatus));
                    } else if (mRobotType) {
                        messageViewModel.executePageEvent(new CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType.ROBOT, true));
                    } else {
                        messageViewModel.executePageEvent(new CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType.STAR, false));
                    }
                } else {
                    messageViewModel.executePageEvent(new PageDestroyEvent());
                }
            }
        }

        @Override
        public void onSelectGroup(List<CSGroupItem> groups) {
            MessageViewModel messageViewModel = mWeakMessageViewModel.get();
            if (messageViewModel != null) {
                messageViewModel.executePageEvent(new CSSelectGroupEvent(groups));
            }
        }
    };

    @Override
    public boolean onBackPressed(MessageViewModel viewModel) {
        super.onBackPressed(viewModel);
        if (mCustomServiceConfig != null && CustomServiceConfig.CSQuitSuspendType.NONE.equals(mCustomServiceConfig.quitSuspendType) && mNeedEvaluate) {
            if (mRobotType) {
                viewModel.executePageEvent(new CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType.ROBOT, true));
            } else {
                viewModel.executePageEvent(new CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType.STAR_MESSAGE, true));
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy(MessageViewModel viewModel) {
        if (mCustomServiceConfig != null) {
            boolean shouldStop = mCustomServiceConfig.quitSuspendType.equals(CustomServiceConfig.CSQuitSuspendType.SUSPEND);
            if (!shouldStop && mCustomServiceConfig.quitSuspendType.equals(CustomServiceConfig.CSQuitSuspendType.NONE)) {
                shouldStop = mStopCSWhenQuit;
            }
            if (shouldStop) {
                RongIMClient.getInstance().stopCustomService(viewModel.getCurTargetId());
            }
        }
        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        mCustomServiceConfig = null;
    }

    /**
     * 进入客服会话，弹出评价菜单超时时间(单位：秒) 设置为 0 时，任何时候离开客服会话时，都会弹出评价菜单.
     *
     * @param time 超时时间
     */
    public void setCustomServiceEvaluateTime(long time) {
        rc_custom_service_evaluation_interval = time;
    }

    /**
     * 设置退出会话页面时是否退出客服
     *
     * @param value 是否关闭客服。
     */
    public void setStopCSWhenQuit(boolean value) {
        mStopCSWhenQuit = value;
    }

    private void startTimer(int event, int interval) {
        if (mMainHandler == null) {
            mMainHandler = new Handler(Looper.getMainLooper());
        }
        mMainHandler.removeMessages(event);
        mMainHandler.sendEmptyMessageDelayed(event, interval);
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case CS_HUMAN_MODE_CUSTOMER_EXPIRE: {
                if (mCustomServiceConfig == null) {
                    return true;
                }
                InformationNotificationMessage info = new InformationNotificationMessage(mCustomServiceConfig.userTipWord);
                IMCenter.getInstance().insertIncomingMessage(Conversation.ConversationType.CUSTOMER_SERVICE, mTargetId, mTargetId, null, info, null);
                return true;
            }
            case CS_HUMAN_MODE_SEAT_EXPIRE: {
                if (mCustomServiceConfig == null) {
                    return true;
                }
                InformationNotificationMessage info = new InformationNotificationMessage(mCustomServiceConfig.adminTipWord);
                IMCenter.getInstance().insertIncomingMessage(Conversation.ConversationType.CUSTOMER_SERVICE, mTargetId, mTargetId, null, info, null);
                return true;
            }
        }
        return false;
    }

    public CustomServiceConfig getCustomServiceConfig() {
        return mCustomServiceConfig;
    }
}
