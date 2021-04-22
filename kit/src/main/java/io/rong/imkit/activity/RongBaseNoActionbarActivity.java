package io.rong.imkit.activity;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;

import io.rong.imkit.utils.language.LangUtils;
import io.rong.imkit.utils.language.RongConfigurationManager;

public class RongBaseNoActionbarActivity extends FragmentActivity {


    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = RongConfigurationManager.getInstance().getConfigurationContext(newBase);
        super.attachBaseContext(context);
    }
}
