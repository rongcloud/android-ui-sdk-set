package io.rong.imkit.conversationlist;

import android.os.Bundle;
import android.text.TextUtils;
import io.rong.imkit.R;
import io.rong.imkit.activity.RongBaseActivity;
import io.rong.imkit.config.IMKitThemeManager;
import io.rong.imkit.utils.RouteUtils;

public class RongConversationListActivity extends RongBaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String title = "";
        if (getIntent() != null) {
            title = getIntent().getStringExtra(RouteUtils.TITLE);
        }
        if (TextUtils.isEmpty(title)) {
            title = getResources().getString(R.string.rc_conversation_list_title);
        }
        mTitleBar.setTitle(title);
        if (!IMKitThemeManager.isTraditionTheme()) {
            int bgColor =
                    IMKitThemeManager.getColorFromAttrId(this, R.attr.rc_view_background_color);
            if (bgColor != 0) {
                mTitleBar.setBackgroundColor(bgColor);
            }
        }
        setContentView(R.layout.rc_conversationlist_activity);
    }
}
