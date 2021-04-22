package io.rong.imkit.feature.customservice;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import androidx.fragment.app.Fragment;

import io.rong.imkit.R;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;


public class EvaluatePlugin implements IPluginModule, CSEvaluateDialog.EvaluateClickListener {
    private CSEvaluateDialog mEvaluateDialog;
    private boolean mResolvedButton;

    public EvaluatePlugin(boolean mResolvedButton) {
        this.mResolvedButton = mResolvedButton;
    }

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_cs_evaluate_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_cs_evaluate);
    }

    @Override
    public void onClick(Fragment currentFragment, RongExtension extension, int index) {
        mEvaluateDialog = new CSEvaluateDialog(currentFragment.getActivity(), extension.getTargetId());
        mEvaluateDialog.showStarMessage(mResolvedButton);
        mEvaluateDialog.setClickListener(this);
        extension.collapseExtension();
    }

    @Override
    public void onEvaluateSubmit() {
        mEvaluateDialog.destroy();
        mEvaluateDialog = null;
    }

    @Override
    public void onEvaluateCanceled() {
        mEvaluateDialog.destroy();
        mEvaluateDialog = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }
}
