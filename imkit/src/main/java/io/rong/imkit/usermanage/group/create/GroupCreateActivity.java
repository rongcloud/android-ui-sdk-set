package io.rong.imkit.usermanage.group.create;

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
import java.util.ArrayList;
import java.util.List;

/**
 * 创建群组页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class GroupCreateActivity extends BaseActivity {

    private Fragment fragment;

    public static Intent newIntent(@NonNull Context context, @NonNull List<String> inviteeUserIds) {
        return newIntent(context, "", inviteeUserIds);
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull String groupIds,
            @NonNull List<String> inviteeUserIds) {
        Intent intent = new Intent(context, GroupCreateActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(KitConstants.KEY_GROUP_ID, groupIds);
        bundle.putStringArrayList(
                KitConstants.KEY_INVITEE_USER_IDS, new ArrayList<>(inviteeUserIds));
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
        return IMCenter.getKitFragmentFactory().newGroupCreateFragment(bundle);
    }
}
