package io.rong.imkit.usermanage.friend.my.nikename;

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
import io.rong.imlib.model.FriendInfo;
import io.rong.imlib.model.UserProfile;

public class UpdateNickNameActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull FriendInfo friendInfo) {
        Intent intent = new Intent(context, UpdateNickNameActivity.class);
        intent.putExtra(KitConstants.KEY_FRIEND_INFO, friendInfo);
        return intent;
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull UserProfile userProfile) {
        Intent intent = new Intent(context, UpdateNickNameActivity.class);
        intent.putExtra(KitConstants.KEY_USER_PROFILER, userProfile);
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
        return IMCenter.getKitFragmentFactory().newUpdateNikeNameFragment(bundle);
    }
}
