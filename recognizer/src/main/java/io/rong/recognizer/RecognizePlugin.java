package io.rong.recognizer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import io.rong.common.rlog.RLog;
import io.rong.imkit.conversation.extension.InputMode;
import io.rong.imkit.conversation.extension.RongExtension;
import io.rong.imkit.conversation.extension.RongExtensionViewModel;
import io.rong.imkit.conversation.extension.component.plugin.IPluginModule;
import io.rong.imkit.conversation.extension.component.plugin.IPluginRequestPermissionResultCallback;
import io.rong.imkit.manager.AudioPlayManager;
import io.rong.imkit.utils.PermissionCheckUtil;
import io.rong.imkit.utils.RongOperationPermissionUtils;
import io.rong.imkit.utils.ToastUtils;

public class RecognizePlugin implements IPluginModule, IPluginRequestPermissionResultCallback {
    private static final String TAG = "RecognizePlugin";

    @Override
    public Drawable obtainDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_recognizer_voice_selector);
    }

    @Override
    public String obtainTitle(Context context) {
        return context.getString(R.string.rc_plugin_recognize);
    }

    @Override
    public void onClick(Fragment currentFragment, final RongExtension extension, int index) {
        if (extension == null) {
            RLog.e(TAG, "onClick extension null");
            return;
        }
        if (currentFragment.getContext() == null || currentFragment.getActivity() == null) {
            RLog.e(TAG, "onClick getContext null");
            return;
        }

        // 判断正在视频通话和语音通话中不能进行语音消息发送
        if (RongOperationPermissionUtils.isOnRequestHardwareResource()) {
            String text = currentFragment.getString(R.string.rc_voip_occupying);
            ToastUtils.show(currentFragment.getActivity(), text, Toast.LENGTH_SHORT);
            return;
        }
        String[] permissions = {Manifest.permission.RECORD_AUDIO};
        if (PermissionCheckUtil.checkPermissions(currentFragment.getActivity(), permissions)) {
            startRecognize(currentFragment, extension);
        } else {
            extension.requestPermissionForPluginResult(
                    permissions,
                    IPluginRequestPermissionResultCallback.REQUEST_CODE_PERMISSION_PLUGIN,
                    this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // default implementation ignored
    }

    private void startRecognize(Fragment fragment, final RongExtension extension) {
        final FragmentActivity activity = fragment.getActivity();
        if (activity == null || activity.isDestroyed() || activity.isFinishing()) {
            RLog.e(TAG, "startRecognize activity null");
            return;
        }
        if (AudioPlayManager.getInstance().isPlaying()) {
            AudioPlayManager.getInstance().stopPlay();
        }
        RongExtensionViewModel mExtensionViewModel =
                new ViewModelProvider(fragment).get(RongExtensionViewModel.class);
        mExtensionViewModel.getInputModeLiveData().postValue(InputMode.RecognizeMode);

        Recognizer recognizerView = new Recognizer(extension.getContext());
        recognizerView.setResultCallBack(
                new IRecognizedResult() {
                    @Override
                    public void onResult(String data) {
                        EditText inputEditText = extension.getInputEditText();
                        if (inputEditText != null && inputEditText.getText() != null) {
                            String str = inputEditText.getText().toString() + data;
                            inputEditText.setText(str);
                            inputEditText.setSelection(str.length());
                        }
                    }

                    @Override
                    public void onClearClick() {
                        EditText inputEditText = extension.getInputEditText();
                        if (inputEditText != null) {
                            inputEditText.setText("");
                        }
                    }
                });
        extension.addPluginPager(recognizerView);
        recognizerView.startRecognize();
    }

    @Override
    public boolean onRequestPermissionResult(
            Fragment fragment,
            RongExtension extension,
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (PermissionCheckUtil.checkPermissions(fragment.getActivity(), permissions)) {
            startRecognize(fragment, extension);
        } else {
            PermissionCheckUtil.showRequestPermissionFailedAlter(
                    fragment.getContext(), permissions, grantResults);
        }
        return true;
    }
}
