package io.rong.imkit.feature.location;

import android.Manifest;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.conversation.ConversationFragment;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.messgelist.processor.IConversationUIRenderer;
import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imkit.userinfo.RongUserInfoManager;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.widget.dialog.PromptPopupDialog;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.location.RealTimeLocationConstant;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

import static io.rong.imkit.utils.PermissionCheckUtil.REQUEST_CODE_ASK_PERMISSIONS;

public class LocationUiRender implements IConversationUIRenderer {
    private ConversationFragment mFragment;
    private Conversation.ConversationType mConversationType;
    private String mTargetId;
    private View mRealTimeBar;
    private TextView mRealTimeText;
    private List<String> mLocationShareParticipants;

    private LocationManager.IRealTimeLocationChangedCallback mLocationChangeCallBack = new LocationManager.IRealTimeLocationChangedCallback() {
        @Override
        public void onParticipantChanged(List<String> userIdList) {
            if (mFragment == null || mFragment.getActivity() == null || mFragment.isDetached()) {
                return;
            }
            if (mRealTimeBar == null) {
                mRealTimeBar = LayoutInflater.from(mFragment.getActivity()).inflate(R.layout.rc_notification_realtime_location, mFragment.getNotificationContainer(), false);
                mRealTimeText = mRealTimeBar.findViewById(R.id.real_time_location_text);
                mRealTimeBar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};

                        if (!PermissionCheckUtil.checkPermissions(mFragment.getContext(), permissions)) {
                            PermissionCheckUtil.requestPermissions(mFragment.getActivity(), permissions, REQUEST_CODE_ASK_PERMISSIONS);
                            return;
                        }

                        RealTimeLocationConstant.RealTimeLocationStatus status = RongIMClient.getInstance().getRealTimeLocationCurrentState(mConversationType, mTargetId);
                        if (status == RealTimeLocationConstant.RealTimeLocationStatus.RC_REAL_TIME_LOCATION_STATUS_INCOMING) {
                            PromptPopupDialog dialog = PromptPopupDialog.newInstance(mFragment.getActivity(), "", mFragment.getActivity().getResources().getString(R.string.rc_real_time_join_notification));
                            dialog.setPromptButtonClickedListener(new PromptPopupDialog.OnPromptButtonClickedListener() {
                                @Override
                                public void onPositiveButtonClicked() {
                                    int result;
                                    if (mFragment.getActivity().getResources().getBoolean(R.bool.rc_location_2D)) {
                                        result = LocationDelegate2D.getInstance().joinLocationSharing();
                                    } else {
                                        result = LocationDelegate3D.getInstance().joinLocationSharing();
                                    }
                                    if (result == 0) {
                                        Intent intent;
                                        if (mFragment.getActivity().getResources().getBoolean(R.bool.rc_location_2D)) {
                                            intent = new Intent(mFragment.getActivity(), AMapRealTimeActivity2D.class);
                                        } else {
                                            intent = new Intent(mFragment.getActivity(), AMapRealTimeActivity.class);
                                        }
                                        if (mLocationShareParticipants != null) {
                                            intent.putStringArrayListExtra("participants", (ArrayList<String>) mLocationShareParticipants);
                                        }
                                        mFragment.getActivity().startActivity(intent);
                                    } else if (result == 1) {
                                        Toast.makeText(mFragment.getActivity(), R.string.rc_network_exception, Toast.LENGTH_SHORT).show();
                                    } else if ((result == 2)) {
                                        Toast.makeText(mFragment.getActivity(), R.string.rc_location_sharing_exceed_max, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            dialog.show();
                        } else {
                            Intent intent;
                            if (mFragment == null || mFragment.getContext() == null || mFragment.getActivity() == null) {
                                return;
                            }
                            if (mFragment.getContext().getResources().getBoolean(R.bool.rc_location_2D)) {
                                intent = new Intent(mFragment.getActivity(), AMapRealTimeActivity2D.class);
                            } else {
                                intent = new Intent(mFragment.getActivity(), AMapRealTimeActivity.class);
                            }
                            if (mLocationShareParticipants != null) {
                                intent.putStringArrayListExtra("participants", (ArrayList<String>) mLocationShareParticipants);
                            }
                            mFragment.getActivity().startActivity(intent);
                        }
                    }
                });
            }
            mLocationShareParticipants = userIdList;
            if (userIdList != null) {
                if (userIdList.size() == 0) {
                    mFragment.hideNotificationView(mRealTimeBar);
                } else {
                    if (userIdList.size() == 1 && userIdList.contains(RongIMClient.getInstance().getCurrentUserId())) {
                        mRealTimeText.setText(mFragment.getActivity().getResources().getString(R.string.rc_location_you_are_sharing));
                    } else if (userIdList.size() == 1 && !userIdList.contains(RongIMClient.getInstance().getCurrentUserId())) {
                        UserInfo info = RongUserInfoManager.getInstance().getUserInfo(userIdList.get(0));
                        String name = info == null ? userIdList.get(0) : info.getName();
                        mRealTimeText.setText(name + mFragment.getActivity().getResources().getString(R.string.rc_location_other_is_sharing));
                    } else {
                        mRealTimeText.setText(userIdList.size() + mFragment.getActivity().getResources().getString(R.string.rc_location_others_are_sharing));
                    }
                    mFragment.showNotificationView(mRealTimeBar);
                }
            } else {
                mFragment.hideNotificationView(mRealTimeBar);
            }
        }
    };

    @Override
    public void init(ConversationFragment fragment, RongExtension extension, Conversation.ConversationType conversationType, String targetId) {
        mFragment = fragment;
        mConversationType = conversationType;
        mTargetId = targetId;
        LocationManager.getInstance().setOnRTLocationChangedCallback(mLocationChangeCallBack);
    }

    @Override
    public boolean handlePageEvent(PageEvent event) {
        return false;
    }

    @Override
    public boolean onBackPressed() {
        if (mFragment == null || mFragment.getActivity() == null || mFragment.isDetached()) {
            return false;
        }
        boolean isLocationSharing;
        if (mFragment.getActivity() != null && mFragment.getActivity().getResources().getBoolean(R.bool.rc_location_2D)) {
            isLocationSharing = LocationDelegate2D.getInstance().isSharing();
        } else {
            isLocationSharing = LocationDelegate3D.getInstance().isSharing();
        }

        if (isLocationSharing) {
            PromptPopupDialog.newInstance(mFragment.getContext(), mFragment.getString(R.string.rc_location_warning),
                    mFragment.getString(R.string.rc_location_real_time_exit_notification), mFragment.getString(R.string.rc_dialog_ok))
                    .setPromptButtonClickedListener(new PromptPopupDialog.OnPromptButtonClickedListener() {
                        @Override
                        public void onPositiveButtonClicked() {
                            mFragment.hideNotificationView(mRealTimeBar);
                            mFragment.getActivity().finish();
                        }
                    }).show();
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        if (mFragment != null && mFragment.getActivity() != null && mFragment.getActivity().getResources().getBoolean(R.bool.rc_location_2D)) {
            LocationDelegate2D.getInstance().quitLocationSharing();
            LocationDelegate2D.getInstance().setParticipantChangedListener(null);
            LocationDelegate2D.getInstance().unBindConversation();
        } else {
            LocationDelegate3D.getInstance().quitLocationSharing();
            LocationDelegate3D.getInstance().setParticipantChangedListener(null);
            LocationDelegate3D.getInstance().unBindConversation();
        }
        mFragment = null;
        mRealTimeBar = null;
        mRealTimeText = null;
        LocationManager.getInstance().setOnRTLocationChangedCallback(null);
    }

}
