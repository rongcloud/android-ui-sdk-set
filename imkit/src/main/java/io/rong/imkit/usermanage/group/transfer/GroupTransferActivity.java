package io.rong.imkit.usermanage.group.transfer;

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
import io.rong.imlib.model.GroupMemberRole;

/**
 * 群转让页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupTransferActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull ConversationIdentifier conversationIdentifier,
            GroupMemberRole role) {
        Intent intent = new Intent(context, GroupTransferActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER, conversationIdentifier);
        bundle.putSerializable(KitConstants.KEY_GROUP_MEMBER_ROLE, role);
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
        return IMCenter.getKitFragmentFactory().newGroupTransferFragment(bundle);
    }
}
