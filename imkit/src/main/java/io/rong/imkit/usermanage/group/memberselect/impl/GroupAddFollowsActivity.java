package io.rong.imkit.usermanage.group.memberselect.impl;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import io.rong.imkit.IMCenter;
import io.rong.imkit.R;
import io.rong.imkit.base.BaseActivity;
import io.rong.imkit.utils.KitConstants;
import io.rong.imlib.model.ConversationIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 创建增加群关注人页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupAddFollowsActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ConversationIdentifier conversationIdentifier,
            List<String> disableUserIds) {
        return newIntent(context, conversationIdentifier, disableUserIds, 30);
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ConversationIdentifier conversationIdentifier,
            List<String> disableUserIds,
            int maxCount) {
        Intent intent = new Intent(context, GroupAddFollowsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER, conversationIdentifier);
        int validatedMaxCountDisplay = Math.max(1, Math.min(100, maxCount));
        bundle.putInt(KitConstants.KEY_MAX_SELECT_COUNT, validatedMaxCountDisplay);
        bundle.putStringArrayList(
                KitConstants.KEY_DISABLE_USER_IDS, new ArrayList<>(disableUserIds));
        intent.putExtras(bundle);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_activity);

        fragment = createFragment();
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack();
        manager.beginTransaction().replace(R.id.fl_fragment_container, fragment).commit();
    }

    @NonNull
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle();
        return IMCenter.getKitFragmentFactory().newGroupAddFollowsFragment(bundle);
    }
}
