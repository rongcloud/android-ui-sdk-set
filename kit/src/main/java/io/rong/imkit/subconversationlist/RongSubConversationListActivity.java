package io.rong.imkit.subconversationlist;

import android.os.Bundle;

import io.rong.imkit.R;
import io.rong.imkit.activity.RongBaseActivity;
import io.rong.imkit.utils.RouteUtils;

public class RongSubConversationListActivity extends RongBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            String title = getIntent().getStringExtra(RouteUtils.TITLE);
            mTitleBar.setTitle(title);
        }
        mTitleBar.setRightVisible(false);
        setContentView(R.layout.rc_subconversationlist_activity);
    }
}
