package io.rong.imkit.feature.publicservice;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.Locale;

import io.rong.common.RLog;
import io.rong.imkit.R;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imkit.widget.SettingItemView;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.publicservice.model.PublicServiceProfile;

public class PublicServiceProfileFragment extends DispatchResultFragment {
    public static final String AGS_PUBLIC_ACCOUNT_INFO = "arg_public_account_info";
    PublicServiceProfile mPublicAccountInfo;

    private ImageView mPortraitIV;
    private TextView mNameTV;
    private TextView mAccountTV;
    private SettingItemView mNotificationView;
    private TextView mDescriptionTV;
    private Button mEnterBtn;
    private Button mFollowBtn;
    private Button mUnfollowBtn;

    private String mTargetId;
    private Conversation.ConversationType mConversationType;
    private String name;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_public_service_inf, container, false);

        mPortraitIV = view.findViewById(R.id.portrait);
        mNameTV = view.findViewById(R.id.name);
        mAccountTV = view.findViewById(R.id.account);
        mNotificationView = view.findViewById(R.id.notification);
        mDescriptionTV = view.findViewById(R.id.description);
        mEnterBtn = view.findViewById(R.id.enter);
        mFollowBtn = view.findViewById(R.id.follow);
        mUnfollowBtn = view.findViewById(R.id.unfollow);

        return view;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initFragment();
        if (mPublicAccountInfo != null) {
            initData(mPublicAccountInfo);
        } else {
            if (!TextUtils.isEmpty(mTargetId)) {
                Conversation.PublicServiceType publicServiceType = null;
                if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                    publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                else if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)
                    publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                else
                    System.err.print("the public service type is error!!");

                PublicServiceManager.getInstance().getPublicServiceProfile(publicServiceType, mTargetId, new RongIMClient.ResultCallback<PublicServiceProfile>() {
                    @Override
                    public void onSuccess(PublicServiceProfile info) {
                        if (info != null) {
                            initData(info);
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        RLog.e("PublicServiceProfileFragment", "Failure to get data!!!");
                    }
                });
            }
        }
    }

    private void initFragment() {
        Uri uri = null;
        if (getActivity() != null && getActivity().getIntent() != null) {
            uri = getActivity().getIntent().getData();
            mPublicAccountInfo = getActivity().getIntent().getParcelableExtra(AGS_PUBLIC_ACCOUNT_INFO);
        }

        if (uri != null) {
            if (mPublicAccountInfo == null) {
                String pathSeg = uri.getLastPathSegment();
                String typeStr = !TextUtils.isEmpty(pathSeg) ? pathSeg.toUpperCase(Locale.US) : "";
                mConversationType = Conversation.ConversationType.valueOf(typeStr);
                mTargetId = uri.getQueryParameter("targetId");
                name = uri.getQueryParameter("name");
            } else {
                mConversationType = mPublicAccountInfo.getConversationType();
                mTargetId = mPublicAccountInfo.getTargetId();
                name = mPublicAccountInfo.getName();
            }
        }

    }

    private void initData(final PublicServiceProfile info) {
        if (info != null) {
            Glide.with(this)
                    .load(info.getPortraitUri())
                    .placeholder(R.drawable.rc_default_portrait)
                    .into(mPortraitIV);
            mNameTV.setText(info.getName());
            mAccountTV.setText(String.format(getResources().getString(R.string.rc_pub_service_info_account), info.getTargetId()));
            mDescriptionTV.setText(info.getIntroduction());

            boolean isFollow = info.isFollow();
            boolean isGlobal = info.isGlobal();

            if (isGlobal) {
                mNotificationView.setVisibility(View.VISIBLE);
                mFollowBtn.setVisibility(View.GONE);
                mEnterBtn.setVisibility(View.VISIBLE);
                mUnfollowBtn.setVisibility(View.GONE);
            } else {
                if (isFollow) {
                    mNotificationView.setVisibility(View.VISIBLE);
                    mFollowBtn.setVisibility(View.GONE);
                    mEnterBtn.setVisibility(View.VISIBLE);
                    mUnfollowBtn.setVisibility(View.VISIBLE);
                } else {
                    mNotificationView.setVisibility(View.GONE);
                    mFollowBtn.setVisibility(View.VISIBLE);
                    mEnterBtn.setVisibility(View.GONE);
                    mUnfollowBtn.setVisibility(View.GONE);
                }
            }


            mEnterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (PublicServiceManager.getInstance().getPubBehaviorListener() != null
                            && PublicServiceManager.getInstance().getPubBehaviorListener().onEnterConversationClick(v.getContext(), info)) {
                        return;
                    }
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                    Bundle bundle = new Bundle();
                    bundle.putString(RouteUtils.TITLE, info.getName());
                    RouteUtils.routeToConversationActivity(getActivity(), info.getConversationType(), info.getTargetId(), bundle);
                }
            });

            mFollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    Conversation.PublicServiceType publicServiceType = null;
                    if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                    else if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                    else
                        System.err.print("the public service type is error!!");

                    RongIMClient.getInstance().subscribePublicService(publicServiceType, info.getTargetId(), new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            mNotificationView.setVisibility(View.VISIBLE);
                            mFollowBtn.setVisibility(View.GONE);
                            mEnterBtn.setVisibility(View.VISIBLE);
                            mUnfollowBtn.setVisibility(View.VISIBLE);

                            //RongContext.getInstance().getEventBus().post(Event.PublicServiceFollowableEvent.obtain(info.getTargetId(), info.getConversationType(), true));
                            PublicServiceManager.PublicServiceBehaviorListener listener = PublicServiceManager.getInstance().getPubBehaviorListener();
                            if (listener != null && listener.onFollowClick(v.getContext(), info)) {
                                return;
                            }
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                            Bundle bundle = new Bundle();
                            bundle.putString(RouteUtils.TITLE, info.getName());
                            RouteUtils.routeToConversationActivity(getActivity(), info.getConversationType(), info.getTargetId(), bundle);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode coreErrorCode) {

                        }
                    });
                }
            });

            mUnfollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {

                    Conversation.PublicServiceType publicServiceType = null;
                    if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                    else if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                    else
                        System.err.print("the public service type is error!!");

                    RongIMClient.getInstance().unsubscribePublicService(publicServiceType, info.getTargetId(), new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            mFollowBtn.setVisibility(View.VISIBLE);
                            mEnterBtn.setVisibility(View.GONE);
                            mUnfollowBtn.setVisibility(View.GONE);
                            mNotificationView.setVisibility(View.GONE);
                            //RongContext.getInstance().getEventBus().post(Event.PublicServiceFollowableEvent.obtain(info.getTargetId(), info.getConversationType(), false));
                            PublicServiceManager.PublicServiceBehaviorListener listener = PublicServiceManager.getInstance().getPubBehaviorListener();
                            if (listener != null && listener.onUnFollowClick(v.getContext(), info)) {
                                return;
                            }
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode coreErrorCode) {

                        }
                    });
                }
            });
        }
    }

}
