package io.rong.imkit.usermanage.group.application;

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

/**
 * 群申请列表页面
 *
 * @author rongcloud
 * @since 5.12.2
 */
public class GroupApplicationsActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return newIntent(context, 50);
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context, int maxCount) {
        Intent intent = new Intent(context, GroupApplicationsActivity.class);
        Bundle bundle = new Bundle();
        int validatedMaxMemberCountPaged = Math.max(1, Math.min(100, maxCount));
        bundle.putInt(KitConstants.KEY_MAX_COUNT_PAGED, validatedMaxMemberCountPaged);
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
        return IMCenter.getKitFragmentFactory().newGroupApplicationsFragment(bundle);
    }
}
