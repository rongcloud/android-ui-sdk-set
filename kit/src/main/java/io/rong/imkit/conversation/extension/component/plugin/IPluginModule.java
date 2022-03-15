package io.rong.imkit.conversation.extension.component.plugin;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import androidx.fragment.app.Fragment;
import io.rong.imkit.conversation.extension.RongExtension;

public interface IPluginModule {

    /**
     * /~chinese 获取 plugin 图标
     *
     * @param context 上下文
     * @return 图片的 Drawable
     */

    /**
     * /~english Get the plugin icon
     *
     * @param context Context
     * @return Image Drawable
     */
    Drawable obtainDrawable(Context context);

    /**
     * /~chinese 获取 plugin 标题
     *
     * @param context 上下文
     * @return 标题的字符串
     */

    /**
     * /~english Get the plugin title
     *
     * @param context Context
     * @return The string of the title
     */
    String obtainTitle(Context context);

    /**
     * /~chinese plugin 被点击时调用。 1. 如果需要 Extension 中的的数据，可以调用 Extension 相应的方法获取。 2. 如果在点击后需要开启新的
     * activity，可以使用 {@link Activity#startActivityForResult(Intent, int)} 或者 {@link
     * RongExtension#startActivityForPluginResult(Intent, int, IPluginModule)} 方式。
     *
     * <p>注意：不要长期持有 fragment 或者 extension 对象，否则会有内存泄露。
     *
     * @param currentFragment plugin 所关联的 fragment。
     * @param extension RongExtension 对象
     * @param index plugin 在 plugin 面板中的序号。
     */

    /**
     * /~english Called when plugin is clicked. 1. If you need the data in Extension, you can call
     * the corresponding method of Extension to get it.. 2. If you shall open a new activity, after
     * clicking, you can use Activity.startActivityForResult(Intent, int) or
     * RongExtension.startActivityForPluginResult(Intent, int, IPluginModule) . Note: Do not hold
     * fragment or extension objects for a long time, or there will be a memory leak.
     *
     * @param currentFragment The fragment associated with plugin.
     * @param extension RongExtension object
     * @param index The serial number of plugin in the plugin panel.
     */
    void onClick(Fragment currentFragment, RongExtension extension, int index);

    /**
     * /~chinese activity 结束时返回数据结果。
     *
     * <p>在 {@link #onClick(Fragment, RongExtension, int)} 中，你可能会开启新的 activity，你有两种开启方式：
     *
     * <p>1. 使用系统中 {@link Activity#startActivityForResult(Intent, int)} 开启方法 这就需要自己在对应的 Activity
     * 中接收处理 {@link Activity#onActivityResult(int, int, Intent)} 返回的结果。
     *
     * <p>2. 如果调用了 {@link RongExtension#startActivityForPluginResult(Intent, int, IPluginModule)}
     * 开启方法 则在 ConversationFragment 中接收到 {@link Activity#onActivityResult(int, int, Intent)} 后， 必须调用
     * {@link RongExtension#onActivityPluginResult(int, int, Intent)} 方法，RongExtension 才会将数据结果 通过
     * IPluginModule 中 onActivityResult 方法返回。
     *
     * <p>
     *
     * @param requestCode 开启 activity 时请求码，不会超过 255.
     * @param resultCode activity 结束时返回的数据结果.
     * @param data 返回的数据.
     */

    /**
     * /~english The data result is returned at the end of the activity. In onClick(Fragment,
     * RongExtension, int) , you may open a new activity,. You can turn it on in two ways: 1. Using
     * the Activity.startActivityForResult(Intent, int) opening method in the system, you shall
     * receive the results returned by the processing Activity.onActivityResult(int, int, Intent) in
     * the corresponding Activity. 2. If the RongExtension.startActivityForPluginResult(Intent, int,
     * IPluginModule) open method is called, after receiving the Activity.onActivityResult(int, int,
     * Intent) in ConversationFragment, the method RongExtension.onActivityPluginResult(int, int,
     * Intent) must be called and then RongExtension will return the data result through the
     * onActivityResult method in IPluginModule.
     *
     * @param requestCode The request code for enabling activity will not exceed 255.
     * @param resultCode The result of the data returned at the end of the activity.
     * @param data The data returned.
     */
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
