package io.rong.imkit.feature.editmessage;

import androidx.fragment.app.Fragment;
import io.rong.imkit.conversation.extension.RongExtension;
import java.lang.ref.WeakReference;

public class EditMessageState {
    public WeakReference<Fragment> mFragment;
    public WeakReference<RongExtension> mRongExtension;
    public EditMessageConfig config;
}
