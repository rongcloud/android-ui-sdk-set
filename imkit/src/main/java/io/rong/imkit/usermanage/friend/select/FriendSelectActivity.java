package io.rong.imkit.usermanage.friend.select;

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
 * 选择好友页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class FriendSelectActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return newIntent(context, 30);
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context, int maxCount) {
        Intent intent = new Intent(context, FriendSelectActivity.class);
        Bundle bundle = new Bundle();
        int validatedMaxMemberCountDisplay = Math.max(1, Math.min(100, maxCount));
        bundle.putInt(KitConstants.KEY_MAX_FRIEND_SELECT_COUNT, validatedMaxMemberCountDisplay);
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
        return IMCenter.getKitFragmentFactory().newFriendSelectFragment(bundle);
    }
}
