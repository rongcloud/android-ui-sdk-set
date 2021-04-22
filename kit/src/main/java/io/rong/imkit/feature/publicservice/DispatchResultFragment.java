package io.rong.imkit.feature.publicservice;

import android.content.Intent;

import androidx.fragment.app.Fragment;

/**
 * Created by DragonJ on 15/3/19.
 */
public abstract class DispatchResultFragment extends Fragment {

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        int index = requestCode >> 12;
        if (index != 0) {
            index--;

            Fragment fragment = getOffsetFragment(index, this);

            if (fragment != null) {
                fragment.onActivityResult(requestCode & 0xfff, resultCode, data);
            }

            return;
        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    public void startActivityForResult(Fragment fragment, Intent intent, int requestCode) {
        int index = getFragmentOffset(0, fragment, this);

        if (index > 0xf) {
            throw new RuntimeException("DispatchFragment only support 16 fragments。");
        }

        if (requestCode == -1) {
            startActivityForResult(intent, -1);
            return;
        }

        if ((requestCode & 0xfffff000) != 0) {
            throw new IllegalArgumentException("Can only use lower 12 bits for requestCode");
        }

        startActivityForResult(intent, ((index + 1) << 12) + (requestCode & 0xfff));
    }


    private int getFragmentOffset(int offset, Fragment targetFragment, Fragment parentFragment) {
        if (parentFragment == null || parentFragment.getChildFragmentManager().getFragments() == null)
            return 0;

        for (Fragment item : parentFragment.getChildFragmentManager().getFragments()) {
            offset++;
            if (targetFragment == item) {
                return offset;
            } else {
                return getFragmentOffset(offset, targetFragment, item);
            }
        }

        return 0;
    }

    private Fragment getOffsetFragment(int offset, Fragment fragment) {

        if (offset == 0)
            return fragment;

        for (Fragment item : getChildFragmentManager().getFragments()) {
            if (--offset == 0)
                return item;

            if (item.getChildFragmentManager().getFragments() != null && item.getChildFragmentManager().getFragments().size() > 0) {
                return getOffsetFragment(offset, item);
            }
        }

        return null;
    }


}
