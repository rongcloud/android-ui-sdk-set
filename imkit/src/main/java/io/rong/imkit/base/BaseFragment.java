package io.rong.imkit.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Fragment 的基类
 *
 * @author rongcloud
 * @since 5.10.4
 */
public abstract class BaseFragment extends Fragment {

    private final BroadcastReceiver receiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (BaseFragment.this.getClass().getName().equals(intent.getAction())) {
                        finishActivity();
                    }
                }
            };

    /**
     * 发送关闭指定页面的广播
     *
     * @param action 页面的 Class
     */
    protected void sendFinishActivityBroadcast(Class<? extends BaseViewModelFragment> action) {
        Intent intent = new Intent(action.getName());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter filter = new IntentFilter(getClass().getName());
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
    }

    /**
     * 判断当前 Fragment 是否处于活动状态。
     *
     * @return 如果当前处于活动状态则返回true
     */
    protected boolean isFragmentAlive() {
        boolean isDeactivated = !isAdded() || isRemoving() || isDetached() || getContext() == null;
        return !isDeactivated;
    }

    /** 关闭当前页面 */
    protected void finishActivity() {
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    /**
     * 返回上一页
     *
     * @return false:拦截返回
     */
    public boolean onBackPressed() {
        return false;
    }
}
