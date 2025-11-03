package io.rong.imkit.conversation.readreceipt;

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
import io.rong.imlib.model.Message;
import io.rong.imlib.model.ReadReceiptInfoV5;

/**
 * 群组消息阅读状态详情页面
 *
 * @author rongcloud
 * @since 5.30.0
 */
public class MessageReadDetailActivity extends BaseActivity {

    @NonNull
    public static Intent newIntent(
            @NonNull Context context,
            @NonNull Message message,
            @Nullable ReadReceiptInfoV5 readReceiptInfoV5) {
        Intent intent = new Intent(context, MessageReadDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(KitConstants.KEY_MESSAGE, message);
        bundle.putParcelable(KitConstants.KEY_READ_RECEIPT_INFO_V5, readReceiptInfoV5);
        intent.putExtras(bundle);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_activity);

        Fragment fragment = createFragment();
        FragmentManager manager = getSupportFragmentManager();
        manager.popBackStack();
        manager.beginTransaction().replace(R.id.fl_fragment_container, fragment).commit();
    }

    @NonNull
    protected Fragment createFragment() {
        Bundle bundle = getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle();
        return IMCenter.getKitFragmentFactory().newMessageReadDetailFragment(bundle);
    }
}
