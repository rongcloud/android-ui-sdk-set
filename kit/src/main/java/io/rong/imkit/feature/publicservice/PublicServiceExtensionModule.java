package io.rong.imkit.feature.publicservice;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.IExtensionModule;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.emoticon.IEmoticonTab;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.utils.RouteUtils;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.publicservice.message.PublicServiceCommandMessage;
import io.rong.imlib.publicservice.model.PublicServiceMenu;
import io.rong.imlib.publicservice.model.PublicServiceMenuItem;
import io.rong.imlib.publicservice.model.PublicServiceProfile;

import static android.view.View.VISIBLE;

public class PublicServiceExtensionModule implements IExtensionModule {
    private Fragment mFragment;
    private RongExtension mRongExtension;
    private RelativeLayout mContentContainer;
    private List<RelativeLayout> mMenuItemList;
    private PublicServiceProfile mPublicServiceProfile;
    private ImageView mInputToggleBtn;
    private boolean isMenuMode = true;
    private LinearLayout mMenuContainer;

    @Override
    public void onInit(Context context, String appKey) {

    }

    @Override
    public void onAttachedToExtension(Fragment fragment, RongExtension extension) {
        mFragment = fragment;
        mRongExtension = extension;
        mMenuItemList = new ArrayList<>();
    }

    void updateMenu(PublicServiceProfile publicServiceProfile) {
        if (publicServiceProfile == null || mFragment == null || mRongExtension == null) {
            return;
        }
        mPublicServiceProfile = publicServiceProfile;
        List<InputMenu> inputMenuList = new ArrayList<>();
        PublicServiceMenu menu = publicServiceProfile.getMenu();
        List<PublicServiceMenuItem> items = menu != null ? menu.getMenuItems() : null;
        if (items != null && items.size() > 0 && mRongExtension != null) {
            for (PublicServiceMenuItem item : items) {
                InputMenu inputMenu = new InputMenu();
                inputMenu.title = item.getName();
                inputMenu.subMenuList = new ArrayList<>();
                for (PublicServiceMenuItem i : item.getSubMenuItems()) {
                    inputMenu.subMenuList.add(i.getName());
                }
                inputMenuList.add(inputMenu);
            }

            RelativeLayout extensionContainer = mRongExtension.getContainer(RongExtension.ContainerType.INPUT);
            LinearLayout menuBar = (LinearLayout) LayoutInflater.from(mFragment.getContext()).inflate(R.layout.rc_ext_public_service_menu, null);
            mContentContainer = menuBar.findViewById(R.id.rc_menu_container);
            mInputToggleBtn = menuBar.findViewById(R.id.rc_switch_button);
            mInputToggleBtn.setOnClickListener(mInputToggleClickListener);

            mMenuItemList.clear();

            if (inputMenuList.size() > 0) {
                getRealMenuContainer();
            }

            mMenuContainer.removeAllViews();
            for (int i = 0; i < inputMenuList.size(); i++) {
                final InputMenu inputMenu = inputMenuList.get(i);
                RelativeLayout menuItem = (RelativeLayout) LayoutInflater.from(mFragment.getContext()).inflate(R.layout.rc_ext_menu_item, null);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
                menuItem.setLayoutParams(lp);
                TextView title = menuItem.findViewById(R.id.rc_menu_title);
                title.setText(inputMenu.title);
                ImageView iv = menuItem.findViewById(R.id.rc_menu_icon);
                if (inputMenu.subMenuList != null && inputMenu.subMenuList.size() > 0) {
                    iv.setVisibility(VISIBLE);
                    iv.setImageResource(R.drawable.rc_ext_menu_trangle);
                }

                mMenuItemList.add(menuItem);
                mMenuContainer.addView(menuItem);
                final int rootIndex = i;
                menuItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        List<String> subMenuList = inputMenu.subMenuList;
                        if (subMenuList != null && subMenuList.size() > 0) {
                            InputSubMenu subMenu = new InputSubMenu(v.getContext(), subMenuList);
                            subMenu.setOnItemClickListener(new ISubMenuItemClickListener() {
                                @Override
                                public void onClick(int index) {
                                    onMenuClick(rootIndex, index);
                                }
                            });
                            subMenu.showAtLocation(v);
                        } else {
                            onMenuClick(rootIndex, -1);
                        }
                    }
                });

                if (i == inputMenuList.size() - 1) {
                    mContentContainer.removeAllViews();
                    ViewGroup parent = (ViewGroup) mMenuContainer.getParent();
                    if (parent != null) {
                        parent.removeView(mMenuContainer);
                    }
                    mContentContainer.addView(mMenuContainer);
                }
            }
            extensionContainer.removeAllViews();
            extensionContainer.addView(menuBar);
            extensionContainer.setVisibility(VISIBLE);
        }
    }

    private LinearLayout getRealMenuContainer() {
        if (mMenuContainer != null) {
            return mMenuContainer;
        }
        mMenuContainer = new LinearLayout(mFragment.getActivity());
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mMenuContainer.setLayoutParams(rlp);
        mMenuContainer.setOrientation(LinearLayout.HORIZONTAL);
        return mMenuContainer;
    }

    View.OnClickListener mInputToggleClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isMenuMode) {
                mContentContainer.removeAllViews();
                mContentContainer.addView(mRongExtension.getInputPanel().getRootView());
                mInputToggleBtn.setImageDrawable(v.getContext().getResources().getDrawable(R.drawable.rc_ext_public_service_menu_mode));
            } else {
                mContentContainer.removeAllViews();
                getRealMenuContainer().removeAllViews();
                for (int i = 0; i < mMenuItemList.size(); i++) {
                    getRealMenuContainer().addView(mMenuItemList.get(i));
                    if (i == mMenuItemList.size() - 1) {
                        mContentContainer.addView(mMenuContainer);
                    }
                }
                mInputToggleBtn.setImageDrawable(v.getContext().getResources().getDrawable(R.drawable.rc_ext_public_service_input_mode));
            }
            isMenuMode = !isMenuMode;
        }
    };

    private void onMenuClick(int root, int sub) {
        if (mPublicServiceProfile != null && mRongExtension != null) {
            Conversation.ConversationType conversationType = mRongExtension.getConversationType();
            String targetId = mRongExtension.getTargetId();
            PublicServiceMenuItem item = mPublicServiceProfile.getMenu().getMenuItems().get(root);
            if (sub >= 0) {
                item = item.getSubMenuItems().get(sub);
            }
            if (item.getType().equals(PublicServiceMenu.PublicServiceMenuItemType.View)) {
                IPublicServiceMenuClickListener menuClickListener = PublicServiceManager.getInstance().getPublicServiceMenuClickListener();
                if (menuClickListener == null || !menuClickListener.onClick(conversationType, targetId, item)) {
                    RouteUtils.routeToWebActivity(mRongExtension.getContext(), item.getUrl());
                }
            }

            PublicServiceCommandMessage msg = PublicServiceCommandMessage.obtain(item);
            RongIMClient.getInstance().sendMessage(conversationType, targetId, msg, null, null, null);
        }
    }

    @Override
    public void onDetachedFromExtension() {
        mFragment = null;
        mRongExtension = null;
        mMenuItemList = null;
        mPublicServiceProfile = null;
    }

    @Override
    public void onReceivedMessage(Message message) {

    }

    @Override
    public List<IPluginModule> getPluginModules(Conversation.ConversationType conversationType) {
        return null;
    }

    @Override
    public List<IEmoticonTab> getEmoticonTabs() {
        return null;
    }

    @Override
    public void onDisconnect() {
        mFragment = null;
        mRongExtension = null;
        mMenuItemList = null;
        mPublicServiceProfile = null;
    }
}
