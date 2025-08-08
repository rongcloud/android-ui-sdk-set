package io.rong.imkit.usermanage.friend.user.profile;

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

/**
 * 用户资料页面
 *
 * @author rongcloud
 * @since 5.12.0
 */
public class UserProfileActivity extends BaseActivity {

    private Fragment fragment;

    @NonNull
    public static Intent newIntent(@NonNull Context context, String userId) {
        return newIntent(context, userId, null);
    }

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            String userId,
            @Nullable ConversationIdentifier conversationIdentifier) {
        Intent intent = new Intent(context, UserProfileActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(KitConstants.KEY_USER_ID, userId);
        bundle.putParcelable(KitConstants.KEY_CONVERSATION_IDENTIFIER, conversationIdentifier);
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
        return IMCenter.getKitFragmentFactory().newUserProfileFragment(bundle);
    }
}
