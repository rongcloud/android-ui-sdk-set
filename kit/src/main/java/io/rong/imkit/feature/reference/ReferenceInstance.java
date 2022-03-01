package io.rong.imkit.feature.reference;

import androidx.fragment.app.Fragment;
import io.rong.imkit.conversation.extension.RongExtension;
import java.lang.ref.WeakReference;

public class ReferenceInstance {
    WeakReference<Fragment> mFragment;
    WeakReference<RongExtension> mRongExtension;
}
