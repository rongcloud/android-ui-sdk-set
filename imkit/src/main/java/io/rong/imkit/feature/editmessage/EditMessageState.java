package io.rong.imkit.feature.editmessage;

import androidx.fragment.app.Fragment;
import io.rong.imkit.conversation.extension.RongExtension;
import java.lang.ref.WeakReference;

public class EditMessageState {
    WeakReference<Fragment> mFragment;
    WeakReference<RongExtension> mRongExtension;
    EditMessageConfig config;
}
