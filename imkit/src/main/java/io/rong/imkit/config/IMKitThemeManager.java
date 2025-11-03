package io.rong.imkit.config;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.rong.common.rlog.RLog;
import io.rong.imkit.R;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 融云主题管理器
 *
 * <p>用于管理融云 IM SDK 的主题切换和资源适配，支持多主题动态切换。
 *
 * <h3>核心功能</h3>
 *
 * <ul>
 *   <li><b>内置主题：</b>提供传统主题和欢快主题，欢快主题自动跟随系统深浅色模式
 *   <li><b>自定义主题：</b>支持开发者扩展和注册自定义主题，支持深浅色模式和主题叠加
 *   <li><b>系统跟随：</b>自动跟随系统深浅色模式切换（欢快主题及自定义主题）
 *   <li><b>动态资源：</b>提供动态资源获取方法，根据当前主题和深浅色模式自动选择合适的资源
 *   <li><b>自动应用：</b>通过 Activity 生命周期回调自动应用主题到所有页面
 *   <li><b>主题监听：</b>支持监听主题切换事件，方便进行自定义处理
 * </ul>
 *
 * <h3>重要说明</h3>
 *
 * <p><b>关于系统深浅色模式切换：</b>
 *
 * <p>在部分机型上，系统设置切换深浅色模式后，返回 APP 可能无法及时切换到对应的模式。 这是由于系统原因导致，当前 APP 通过系统方法获取的深浅色状态仍然是旧值，需要重启 APP
 * 才能生效。
 *
 * <p><b>解决方案（推荐）：</b>
 *
 * <p>如果您的应用使用了 AppCompat 库，建议在 Application 中通过 {@code
 * AppCompatDelegate.setDefaultNightMode(nightMode)} 来设置应用的深浅色模式， 这样可以保证深浅色模式的及时切换。
 *
 * <pre>
 * // 在 Application 的 onCreate 中设置
 * // 跟随系统
 * AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
 *
 * // 或强制使用浅色模式
 * AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
 *
 * // 或强制使用深色模式
 * AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
 * </pre>
 *
 * <h3>快速开始</h3>
 *
 * <pre>
 * // 1. 切换到欢快主题（自动跟随系统深浅色）
 * IMKitThemeManager.changeInnerTheme(context, IMKitThemeManager.LIVELY_THEME);
 *
 * // 2. 切换到传统主题
 * IMKitThemeManager.changeInnerTheme(context, IMKitThemeManager.TRADITION_THEME);
 *
 * // 3. 添加自定义主题（支持深浅色模式）
 * IMKitThemeManager.addTheme("CUSTOM_THEME",
 *     R.style.MyCustomLightTheme,  // 浅色模式样式
 *     R.style.MyCustomDarkTheme    // 深色模式样式
 * );
 *
 * // 4. 切换到自定义主题（基于欢快主题）
 * IMKitThemeManager.changeCustomTheme(context, "CUSTOM_THEME", IMKitThemeManager.LIVELY_THEME);
 *
 * // 5. 在代码中动态获取主题资源
 * int bgResId = IMKitThemeManager.dynamicResource(
 *     context,
 *     R.attr.rc_conversation_bg,  // 主题属性
 *     R.drawable.rc_old_bg        // 默认资源
 * );
 * view.setBackgroundResource(bgResId);
 *
 * // 6. 获取当前主题
 * String currentTheme = IMKitThemeManager.getCurrentThemeName();
 *
 * // 7. 判断当前主题类型
 * if (IMKitThemeManager.TRADITION_THEME.equals(currentTheme)) {
 *     // 传统主题
 * }
 * </pre>
 */
public class IMKitThemeManager {

    // ========== 主题变化监听器接口 ==========

    /**
     * 主题变化监听器接口
     *
     * <p>用于监听主题切换事件，当主题发生变化时会收到回调通知。
     *
     * <p><b>使用示例：</b>
     *
     * <pre>
     * // 创建监听器
     * OnThemeListener listener = new OnThemeListener() {
     *     {@literal @}Override
     *     public void onThemeChanged(Context context, String oldTheme, String newTheme) {
     *         Log.d("Theme", "主题从 " + oldTheme + " 切换到 " + newTheme);
     *         // 可以直接使用传入的 context，无需自己持有引用
     *         if (context instanceof Activity) {
     *             ((Activity) context).recreate(); // 重建 Activity 应用新主题
     *         }
     *     }
     * };
     *
     * // 添加监听器
     * IMKitThemeManager.addThemeListener(listener);
     *
     * // 不需要时移除监听器（避免内存泄漏）
     * IMKitThemeManager.removeThemeListener(listener);
     * </pre>
     */
    public interface OnThemeListener {
        /**
         * 主题变化回调
         *
         * @param context 上下文（通常是调用主题切换方法时传入的 Context）
         * @param oldTheme 旧主题标识（如 TRADITION_THEME、LIVELY_THEME 或自定义主题名称）
         * @param newTheme 新主题标识（如 TRADITION_THEME、LIVELY_THEME 或自定义主题名称）
         */
        void onThemeChanged(Context context, String oldTheme, String newTheme);
    }

    // ========== 主题类型定义 ==========

    /** 传统主题 - 经典的界面设计风格 */
    public static final String TRADITION_THEME = "TRADITION_THEME";

    /** 欢快主题 - 现代化界面，自动跟随系统深浅色模式 */
    public static final String LIVELY_THEME = "LIVELY_THEME";

    // ========== 常量 ==========

    private static final String TAG = "ThemeManager";

    // ========== 静态变量 ==========

    /** 初始化标志，确保主题系统只初始化一次 */
    private static volatile boolean isInit = false;

    /** 主题配置映射表，key 为主题类型，value 为对应的主题配置列表（支持主题叠加） */
    private static final Map<String, List<ThemeConfig>> themeConfigMap = new HashMap<>();

    /** 当前激活的主题类型，默认为传统主题 */
    private static String currentTheme = TRADITION_THEME;

    /** 自定义主题的基础主题（用于主题叠加），如果不是自定义主题则为 null */
    private static String currentBaseTheme = null;

    /** 主题变化监听器列表，使用线程安全的 CopyOnWriteArrayList */
    private static final CopyOnWriteArrayList<OnThemeListener> themeListeners =
            new CopyOnWriteArrayList<>();

    // ========== 内部类：主题配置 ==========

    /** 主题配置类，存储浅色和深色模式的样式资源 ID */
    private static class ThemeConfig {
        final int lightStyleResId; // 浅色模式样式
        final int darkStyleResId; // 深色模式样式

        ThemeConfig(int lightStyleResId, int darkStyleResId) {
            this.lightStyleResId = lightStyleResId;
            this.darkStyleResId = darkStyleResId;
        }
    }

    // ========== 构造函数 ==========

    /** 私有构造函数，防止实例化 */
    private IMKitThemeManager() {
        throw new UnsupportedOperationException("IMKitThemeManager is a utility class");
    }

    // ========== 初始化方法 ==========

    /**
     * 初始化主题系统（包内可见，线程安全，只初始化一次）
     *
     * @param context 上下文
     */
    static void initThemes(Context context) {
        if (context == null) {
            RLog.w(TAG, "initThemes: context is null");
            return;
        }

        if (!isInit) {
            isInit = true;
            try {
                // 添加内置主题（传统主题：浅色和深色使用相同样式）
                addTheme(
                        TRADITION_THEME,
                        R.style.RCTraditionLightTheme,
                        R.style.RCTraditionLightTheme);

                // 添加现代主题（自动跟随系统深浅色）
                addTheme(LIVELY_THEME, R.style.RCLivelyLightTheme, R.style.RCLivelyDarkTheme);

                // 初始化主题管理器
                initializeThemeManager(context);

                RLog.i(TAG, "Theme initialization completed");
            } catch (Exception e) {
                RLog.e(TAG, "Failed to initialize themes", e);
            }
        }
    }

    // ========== 主题管理方法 ==========

    /**
     * 添加主题（支持浅色和深色样式）
     *
     * <p>此方法用于添加自定义主题，支持为主题指定浅色和深色两种样式。系统会根据当前的深浅色模式自动应用对应的样式。
     *
     * <p><b>使用场景：</b>
     *
     * <pre>
     * // 添加自定义主题（支持深浅色模式）
     * IMKitThemeManager.addTheme(
     *     "CUSTOM_THEME",
     *     R.style.MyCustomLightTheme,  // 浅色模式样式
     *     R.style.MyCustomDarkTheme    // 深色模式样式
     * );
     *
     * // 添加单一样式主题（浅色和深色使用相同样式）
     * IMKitThemeManager.addTheme(
     *     "SIMPLE_THEME",
     *     R.style.MyTheme,  // 浅色模式样式
     *     R.style.MyTheme   // 深色模式样式（相同）
     * );
     *
     * // 为已有主题追加额外样式（样式叠加）
     * IMKitThemeManager.addTheme(
     *     IMKitThemeManager.LIVELY_THEME,
     *     R.style.MyCustomOverrideLight,  // 浅色模式扩展样式
     *     R.style.MyCustomOverrideDark    // 深色模式扩展样式
     * );
     *
     * // 切换到自定义主题（基于欢快主题）
     * IMKitThemeManager.changeCustomTheme(context, "CUSTOM_THEME", IMKitThemeManager.LIVELY_THEME);
     * </pre>
     *
     * <p><b>注意事项：</b>
     *
     * <ul>
     *   <li>多次调用此方法添加同一主题时，样式会按顺序叠加应用
     *   <li>后添加的样式属性会覆盖先前的同名属性
     *   <li>lightStyleResId 和 darkStyleResId 不能为 0
     *   <li>主题会自动跟随系统深浅色模式切换
     * </ul>
     *
     * @param themeType 主题类型标识（建议使用全大写下划线格式，如 "CUSTOM_BLUE_THEME"）
     * @param lightStyleResId 浅色模式的主题样式资源 ID（如 R.style.YourLightTheme）
     * @param darkStyleResId 深色模式的主题样式资源 ID（如 R.style.YourDarkTheme）
     */
    public static void addTheme(String themeType, int lightStyleResId, int darkStyleResId) {
        if (themeType == null || themeType.isEmpty()) {
            RLog.e(TAG, "addTheme failed: themeType is null or empty");
            return;
        }

        if (lightStyleResId == 0 || darkStyleResId == 0) {
            RLog.e(TAG, "addTheme failed: lightStyleResId or darkStyleResId is zero");
            return;
        }

        ThemeConfig config = new ThemeConfig(lightStyleResId, darkStyleResId);

        // 获取或创建主题配置列表
        List<ThemeConfig> configList = themeConfigMap.get(themeType);
        if (configList == null) {
            configList = new ArrayList<>();
            themeConfigMap.put(themeType, configList);
        }

        // 追加新的配置（支持叠加）
        configList.add(config);

        RLog.i(
                TAG,
                "Added theme: themeType="
                        + themeType
                        + ", lightStyleResId="
                        + lightStyleResId
                        + ", darkStyleResId="
                        + darkStyleResId
                        + ", total configs="
                        + configList.size());
    }

    /**
     * 切换应用内置主题
     *
     * <p>切换到指定的内置主题并立即应用到当前上下文。该方法会自动处理以下操作：
     *
     * <ul>
     *   <li>验证并设置新的主题类型
     *   <li>根据系统深浅色模式自动选择对应的样式
     *   <li>应用主题到 Application Context（全局生效）
     *   <li>应用主题到当前 Context（立即生效）
     *   <li>通过 ActivityLifecycleCallbacks 自动应用到后续创建的所有 Activity
     * </ul>
     *
     * <p><b>使用示例：</b>
     *
     * <pre>
     * // 切换到欢快主题（自动跟随系统深浅色）
     * IMKitThemeManager.changeInnerTheme(context, IMKitThemeManager.LIVELY_THEME);
     *
     * // 切换到传统主题
     * IMKitThemeManager.changeInnerTheme(context, IMKitThemeManager.TRADITION_THEME);
     * </pre>
     *
     * <p><b>注意：</b>主题切换是全局性的，会影响所有融云 SDK 的 UI 组件。如果当前已经是目标主题，方法会自动跳过，避免不必要的重复应用。
     *
     * @param context 上下文（建议传入 Activity 或 Application Context）
     * @param themeType 主题类型标识（使用 TRADITION_THEME 或 LIVELY_THEME）
     */
    public static void changeInnerTheme(Context context, String themeType) {
        if (context == null) {
            RLog.w(TAG, "changeTheme: context is null");
            return;
        }
        if (!isInit) {
            initThemes(context);
        }

        if (themeType == null || themeType.isEmpty()) {
            RLog.w(TAG, "changeTheme: themeType is null or empty");
            return;
        }

        if (!themeConfigMap.containsKey(themeType)) {
            RLog.e(TAG, "changeTheme: theme not found: " + themeType);
            return;
        }

        if (themeType.equals(currentTheme)) {
            RLog.d(TAG, "Theme already set to: " + themeType);
            return;
        }

        RLog.i(TAG, "Changing theme from " + currentTheme + " to " + themeType);
        String oldTheme = currentTheme;
        currentTheme = themeType;
        currentBaseTheme = null; // 内置主题没有基础主题

        // 应用新主题
        applyTheme(context.getApplicationContext());
        applyTheme(context);

        // 通知监听器
        notifyThemeChanged(context, oldTheme, themeType);
    }

    /**
     * 切换自定义主题（基于内置主题扩展）
     *
     * <p>此方法用于切换到自定义主题，并指定一个基础主题。会先应用基础主题的配置，然后再应用自定义主题的配置，从而实现主题的叠加和扩展。
     *
     * <p><b>应用顺序：</b>
     *
     * <ol>
     *   <li>先应用 baseOnTheme 对应的所有主题配置
     *   <li>再应用 customThemeType 对应的所有主题配置
     *   <li>后应用的配置会覆盖先前的同名属性
     * </ol>
     *
     * <p><b>使用示例：</b>
     *
     * <pre>
     * // 先添加自定义主题
     * IMKitThemeManager.addTheme(
     *     "MY_BLUE_THEME",
     *     R.style.MyBlueLightTheme,
     *     R.style.MyBlueDarkTheme
     * );
     *
     * // 基于欢快主题，应用自定义主题（先应用欢快主题，再应用自定义主题）
     * IMKitThemeManager.changeCustomTheme(context, "MY_BLUE_THEME", IMKitThemeManager.LIVELY_THEME);
     *
     * // 基于传统主题，应用自定义主题
     * IMKitThemeManager.changeCustomTheme(context, "MY_BLUE_THEME", IMKitThemeManager.TRADITION_THEME);
     * </pre>
     *
     * <p><b>注意事项：</b>
     *
     * <ul>
     *   <li>baseOnTheme 只能传递 TRADITION_THEME 或 LIVELY_THEME
     *   <li>customThemeType 必须是已通过 addTheme 方法添加的主题
     *   <li>主题切换是全局性的，会影响所有融云 SDK 的 UI 组件
     * </ul>
     *
     * @param context 上下文（建议传入 Activity 或 Application Context）
     * @param customThemeType 自定义主题类型标识（必须已通过 addTheme 添加）
     * @param baseOnTheme 基础主题类型（只能是 TRADITION_THEME 或 LIVELY_THEME）
     */
    public static void changeCustomTheme(
            Context context, String customThemeType, String baseOnTheme) {
        if (context == null) {
            RLog.w(TAG, "changeCustomTheme: context is null");
            return;
        }
        if (!isInit) {
            initThemes(context);
        }

        if (customThemeType == null || customThemeType.isEmpty()) {
            RLog.w(TAG, "changeCustomTheme: customThemeType is null or empty");
            return;
        }

        // 验证 baseOnTheme 只能是内置主题
        if (!TRADITION_THEME.equals(baseOnTheme) && !LIVELY_THEME.equals(baseOnTheme)) {
            RLog.e(
                    TAG,
                    "changeCustomTheme: baseOnTheme must be TRADITION_THEME or LIVELY_THEME, but got: "
                            + baseOnTheme);
            return;
        }

        // 验证自定义主题是否存在
        if (!themeConfigMap.containsKey(customThemeType)) {
            RLog.e(TAG, "changeCustomTheme: custom theme not found: " + customThemeType);
            return;
        }

        // 验证基础主题是否存在
        if (!themeConfigMap.containsKey(baseOnTheme)) {
            RLog.e(TAG, "changeCustomTheme: base theme not found: " + baseOnTheme);
            return;
        }

        if (customThemeType.equals(currentTheme)) {
            RLog.d(TAG, "Theme already set to: " + customThemeType);
            return;
        }

        RLog.i(
                TAG,
                "Changing custom theme from "
                        + currentTheme
                        + " to "
                        + customThemeType
                        + " (based on "
                        + baseOnTheme
                        + ")");
        String oldTheme = currentTheme;
        currentTheme = customThemeType;
        currentBaseTheme = baseOnTheme; // 保存基础主题，用于后续 Activity 创建时应用

        // 应用新主题（applyTheme 会自动先应用基础主题，再应用自定义主题）
        applyTheme(context.getApplicationContext());
        applyTheme(context);

        // 通知监听器
        notifyThemeChanged(context, oldTheme, customThemeType);
    }

    /**
     * 获取当前激活的主题类型
     *
     * <p><b>使用示例：</b>
     *
     * <pre>
     * String theme = IMKitThemeManager.getCurrentThemeName();
     * if (IMKitThemeManager.LIVELY_THEME.equals(theme)) {
     *     // 当前是欢快主题
     * }
     * </pre>
     *
     * @return 当前主题类型标识字符串（如 TRADITION_THEME、LIVELY_THEME 或自定义主题名称）
     */
    public static String getCurrentThemeName() {
        return currentTheme;
    }

    /**
     * 添加主题变化监听器
     *
     * <p>注册一个监听器以接收主题切换事件通知。当调用 {@link #changeInnerTheme(Context, String)} 或 {@link
     * #changeCustomTheme(Context, String, String)} 方法且主题实际发生变化时，所有已注册的监听器都会收到 {@link
     * OnThemeListener#onThemeChanged(Context, String, String)} 回调。
     *
     * <p><b>使用场景：</b>
     *
     * <ul>
     *   <li>需要在主题切换时更新自定义 UI 组件
     *   <li>需要在主题变化时重新加载资源或刷新界面
     *   <li>需要同步主题状态到其他模块
     * </ul>
     *
     * <p><b>Context 参数的优势：</b>
     *
     * <ul>
     *   <li>监听器无需自己持有 Context 引用，避免潜在的内存泄漏
     *   <li>可以直接使用传入的 Context 进行 UI 操作（如 Activity.recreate()）
     *   <li>通过 Context 获取资源或调用其他需要 Context 的方法
     * </ul>
     *
     * <p><b>使用示例：</b>
     *
     * <pre>
     * public class MyActivity extends AppCompatActivity {
     *     private OnThemeListener themeListener;
     *
     *     {@literal @}Override
     *     protected void onCreate(Bundle savedInstanceState) {
     *         super.onCreate(savedInstanceState);
     *
     *         // 创建监听器
     *         themeListener = new OnThemeListener() {
     *             {@literal @}Override
     *             public void onThemeChanged(Context context, String oldTheme, String newTheme) {
     *                 // 使用传入的 context 重新创建 Activity
     *                 if (context instanceof Activity) {
     *                     ((Activity) context).recreate();
     *                 }
     *             }
     *         };
     *
     *         // 添加监听器
     *         IMKitThemeManager.addThemeListener(themeListener);
     *     }
     *
     *     {@literal @}Override
     *     protected void onDestroy() {
     *         super.onDestroy();
     *         // 移除监听器，避免内存泄漏
     *         IMKitThemeManager.removeThemeListener(themeListener);
     *     }
     * }
     * </pre>
     *
     * <p><b>注意事项：</b>
     *
     * <ul>
     *   <li>同一个监听器对象不会被重复添加
     *   <li>请在合适的时机（如 Activity/Fragment 的 onDestroy）移除监听器，避免内存泄漏
     *   <li>监听器回调在主题切换所在的线程执行，通常是主线程
     * </ul>
     *
     * @param listener 主题变化监听器，不能为 null
     * @see #removeThemeListener(OnThemeListener)
     * @see #changeInnerTheme(Context, String)
     * @see #changeCustomTheme(Context, String, String)
     */
    public static void addThemeListener(OnThemeListener listener) {
        if (listener == null) {
            RLog.w(TAG, "addThemeChangeListener: listener is null");
            return;
        }

        if (!themeListeners.contains(listener)) {
            themeListeners.add(listener);
            RLog.d(TAG, "Theme change listener added, total listeners: " + themeListeners.size());
        } else {
            RLog.w(TAG, "Theme change listener already exists");
        }
    }

    /**
     * 移除主题变化监听器
     *
     * <p>移除之前通过 {@link #addThemeListener(OnThemeListener)} 添加的监听器。 建议在 Activity/Fragment 的生命周期结束时（如
     * onDestroy）调用此方法，避免内存泄漏。
     *
     * <p><b>使用示例：</b>
     *
     * <pre>
     * {@literal @}Override
     * protected void onDestroy() {
     *     super.onDestroy();
     *     IMKitThemeManager.removeThemeListener(themeListener);
     * }
     * </pre>
     *
     * @param listener 要移除的监听器，不能为 null
     * @see #addThemeListener(OnThemeListener)
     */
    public static void removeThemeListener(OnThemeListener listener) {
        if (listener == null) {
            RLog.w(TAG, "removeThemeChangeListener: listener is null");
            return;
        }

        boolean removed = themeListeners.remove(listener);
        if (removed) {
            RLog.d(
                    TAG,
                    "Theme change listener removed, remaining listeners: " + themeListeners.size());
        } else {
            RLog.w(TAG, "Theme change listener not found");
        }
    }

    // ========== 主题判断方法 ==========

    /**
     * 判断当前是否为传统主题（内部使用，不推荐外部调用）
     *
     * <p><b>⚠️ 注意：</b>此方法仅供 SDK 内部使用，外部不推荐直接调用。
     *
     * <p><b>推荐做法：</b>外部应用请使用 {@link #getCurrentThemeName()} 获取当前主题名称，然后自行判断：
     *
     * <pre>
     * String theme = IMKitThemeManager.getCurrentThemeName();
     * if (IMKitThemeManager.TRADITION_THEME.equals(theme)) {
     *     // 传统主题逻辑
     * } else if (IMKitThemeManager.LIVELY_THEME.equals(theme)) {
     *     // 欢快主题逻辑
     * } else {
     *     // 自定义主题逻辑
     * }
     * </pre>
     *
     * @return true 表示当前是传统主题；false 表示当前是欢快主题或自定义主题
     * @see #getCurrentThemeName()
     */
    public static boolean isTraditionTheme() {
        return TRADITION_THEME.equals(currentTheme);
    }

    // ========== 资源获取方法 ==========

    /**
     * 动态获取主题资源（基于 Context 解析）
     *
     * <p>此方法根据当前激活的主题自动选择对应的资源：
     *
     * <ul>
     *   <li><b>现代化主题：</b>从主题属性（Theme Attribute）中动态解析资源 ID，支持多主题切换
     *   <li><b>传统主题：</b>直接返回指定的固定资源 ID，保持向后兼容
     * </ul>
     *
     * <p><b>适用场景：</b>
     *
     * <ul>
     *   <li>在 Java/Kotlin 代码中动态获取需要主题适配的资源
     *   <li>需要在运行时根据当前主题选择不同资源的场景
     *   <li>自定义 View 或 Adapter 中需要加载主题相关的图片、颜色等资源
     * </ul>
     *
     * <p><b>使用示例：</b>
     *
     * <pre>
     * // 获取背景资源：现代化主题使用主题属性，传统主题使用固定资源
     * int bgResId = IMKitThemeManager.dynamicResource(
     *     context,
     *     R.attr.rc_conversation_bg,      // 主题属性
     *     R.drawable.rc_old_bg            // 固定资源（用于传统主题）
     * );
     * view.setBackgroundResource(bgResId);
     *
     * // 获取颜色资源
     * int colorResId = IMKitThemeManager.dynamicResource(
     *     context,
     *     R.attr.rc_text_primary,         // 主题属性
     *     R.color.rc_old_text_color       // 固定颜色（用于传统主题）
     * );
     * textView.setTextColor(getResources().getColor(colorResId));
     * </pre>
     *
     * @param context 上下文（用于解析主题属性，不能为 null）
     * @param newVersionAttrId 主题属性 ID（如 R.attr.rc_xxx）
     * @param oldVersionResId 固定资源 ID（如 R.drawable.xxx 或 R.color.xxx，用于传统主题）
     * @return 解析后的资源 ID
     */
    public static int dynamicResource(Context context, int newVersionAttrId, int oldVersionResId) {
        if (context == null) {
            RLog.w(TAG, "getResource: context is null, returning oldVersionResId");
            return oldVersionResId;
        }

        if (isTraditionTheme()) {
            // 传统主题：返回固定资源
            return oldVersionResId;
        }
        // 现代化主题：从主题属性中获取资源
        return getAttrResId(context, newVersionAttrId);
    }

    /**
     * 动态获取主题资源（无需 Context 的快速版本）
     *
     * <p>此方法是 {@link #dynamicResource(Context, int, int)} 的轻量级版本，适用于 XML 布局文件或无需立即解析的场景。
     *
     * <ul>
     *   <li><b>现代化主题：</b>直接返回主题属性 ID，由系统在渲染时自动解析
     *   <li><b>传统主题：</b>返回固定资源 ID
     * </ul>
     *
     * <p><b>适用场景：</b>
     *
     * <ul>
     *   <li>XML 布局文件中需要根据主题类型选择资源的场景（如 android:background）
     *   <li>延迟解析场景，资源 ID 会在后续使用时由 Android 系统自动解析
     *   <li>无法获取 Context 但需要资源 ID 的场景
     * </ul>
     *
     * <p><b>使用示例：</b>
     *
     * <pre>
     * // 在代码中使用（系统会自动解析属性）
     * view.setBackgroundResource(
     *     IMKitThemeManager.dynamicResource(R.attr.rc_bg, R.drawable.rc_old_bg)
     * );
     *
     * // 注意：返回的可能是属性 ID，需要系统自动解析
     * // 如果需要立即获取解析后的资源，请使用带 Context 参数的版本
     * </pre>
     *
     * <p><b>注意：</b>此方法返回的是属性 ID（现代化主题）或资源 ID（传统主题），如果需要立即使用解析后的实际资源 ID，请使用 {@link
     * #dynamicResource(Context, int, int)} 方法。
     *
     * @param newVersionResId 主题资源 ID（如 R.drawable.rc_xxx 或 R.color.rc_xxx）
     * @param oldVersionResId 固定资源 ID（如 R.drawable.xxx 或 R.color.xxx，用于传统主题）
     * @return 现代化主题返回资源 ID；传统主题返回资源 ID
     */
    public static int dynamicResource(int newVersionResId, int oldVersionResId) {
        if (isTraditionTheme()) {
            // 传统主题：返回固定资源
            return oldVersionResId;
        }
        // 现代化主题：返回主题资源 ID
        return newVersionResId;
    }

    /**
     * 从主题属性中解析实际的资源 ID
     *
     * <p>此方法用于将主题属性（Theme Attribute）解析为实际的资源 ID。
     *
     * <p><b>使用场景：</b>需要从主题属性中获取实际资源 ID 时使用。
     *
     * <pre>
     * // 解析主题属性得到实际的资源 ID
     * int drawableResId = IMKitThemeManager.getAttrResId(context, R.attr.rc_conversation_bg);
     * // drawableResId 现在是实际的 R.drawable.xxx 值
     * </pre>
     *
     * @param context 上下文（不能为 null）
     * @param attrId 主题属性 ID（如 R.attr.rc_xxx）
     * @return 解析后的实际资源 ID，解析失败返回 0
     */
    public static int getAttrResId(Context context, int attrId) {
        if (context == null || attrId == 0) {
            return 0;
        }
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attrId, typedValue, true)) {
            return typedValue.resourceId;
        }
        if (context instanceof ContextThemeWrapper) {
            Context baseContext = ((ContextThemeWrapper) context).getBaseContext();
            if (baseContext != null) {
                if (!baseContext.getTheme().resolveAttribute(attrId, typedValue, true)) {
                    RLog.e(
                            TAG,
                            "getAttrResId error, context type "
                                    + context.getClass().getSimpleName());
                }
            }
        }
        return typedValue.resourceId;
    }

    /**
     * 从主题属性中直接获取颜色值
     *
     * <p>此方法是获取颜色的便捷方法，会自动完成"属性→资源ID→颜色值"的完整解析过程。
     *
     * <p><b>使用场景：</b>需要从主题属性中直接获取可用的颜色 int 值。
     *
     * <pre>
     * // 直接获取主题颜色值
     * int textColor = IMKitThemeManager.getColorFromAttrId(context, R.attr.rc_text_primary);
     * textView.setTextColor(textColor);
     *
     * // 等价于以下两步操作：
     * // int colorResId = IMKitThemeManager.getAttrResId(context, R.attr.rc_text_primary);
     * // int textColor = context.getResources().getColor(colorResId);
     * </pre>
     *
     * @param context 上下文（不能为 null）
     * @param attrId 颜色属性 ID（如 R.attr.rc_text_primary）
     * @return 解析后的颜色值（ARGB int），解析失败返回 0
     */
    public static int getColorFromAttrId(Context context, int attrId) {
        if (context == null || attrId == 0) {
            return 0;
        }
        return context.getResources().getColor(getAttrResId(context, attrId));
    }

    // ========== 私有方法 ==========

    /** 通知所有监听器主题已变化 */
    private static void notifyThemeChanged(Context context, String oldTheme, String newTheme) {
        if (themeListeners.isEmpty()) {
            return;
        }

        RLog.d(TAG, "Notifying " + themeListeners.size() + " theme change listeners");

        // CopyOnWriteArrayList 的迭代器本身就是快照，不会抛出 ConcurrentModificationException
        for (OnThemeListener listener : themeListeners) {
            if (listener != null) {
                try {
                    listener.onThemeChanged(context, oldTheme, newTheme);
                } catch (Exception e) {
                    RLog.e(TAG, "Error notifying theme change listener", e);
                }
            }
        }
    }

    /** 初始化主题管理器，注册 Activity 生命周期回调 */
    private static void initializeThemeManager(Context context) {
        if (context == null) {
            RLog.w(TAG, "initializeThemeManager: context is null");
            return;
        }

        Context appContext = context.getApplicationContext();

        RLog.i(TAG, "ThemeManager initialized");

        // 注册 Activity 生命周期回调
        if (appContext instanceof Application) {
            ((Application) appContext).registerActivityLifecycleCallbacks(new ThemeCallback());
        }

        // 应用主题到 Application Context
        applyTheme(appContext);
    }

    /** 判断系统是否处于深色模式 */
    private static boolean isSystemInDarkMode(Context context) {
        if (context == null) {
            return false;
        }
        int nightMode =
                context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** 应用当前主题到指定的 Context（自动处理自定义主题的基础主题叠加） */
    private static void applyTheme(Context context) {
        if (context == null) {
            RLog.w(TAG, "applyTheme: context is null");
            return;
        }

        Resources.Theme theme = context.getTheme();
        if (theme == null) {
            context.setTheme(android.R.style.Theme_Light);
            theme = context.getTheme();
        }

        // 根据系统深浅色模式选择对应的样式
        boolean isDarkMode = isSystemInDarkMode(context);

        // 如果有基础主题，先应用基础主题
        int totalAppliedCount = 0;
        if (currentBaseTheme != null) {
            List<ThemeConfig> baseConfigList = themeConfigMap.get(currentBaseTheme);
            if (baseConfigList != null && !baseConfigList.isEmpty()) {
                for (ThemeConfig config : baseConfigList) {
                    int styleResId = isDarkMode ? config.darkStyleResId : config.lightStyleResId;
                    try {
                        theme.applyStyle(styleResId, true);
                        totalAppliedCount++;
                        RLog.v(
                                TAG,
                                "Applied base style: "
                                        + styleResId
                                        + " for base theme: "
                                        + currentBaseTheme);
                    } catch (Exception e) {
                        RLog.e(TAG, "Failed to apply base theme style: " + styleResId, e);
                    }
                }
            }
        }

        // 应用当前主题
        List<ThemeConfig> configList = themeConfigMap.get(currentTheme);
        if (configList == null || configList.isEmpty()) {
            RLog.w(TAG, "No theme config found for: " + currentTheme);
            return;
        }

        int currentAppliedCount = 0;
        for (ThemeConfig config : configList) {
            int styleResId = isDarkMode ? config.darkStyleResId : config.lightStyleResId;
            try {
                theme.applyStyle(styleResId, true);
                currentAppliedCount++;
                RLog.v(TAG, "Applied style: " + styleResId + " for theme: " + currentTheme);
            } catch (Exception e) {
                RLog.e(TAG, "Failed to apply theme style: " + styleResId, e);
            }
        }

        totalAppliedCount += currentAppliedCount;

        if (currentBaseTheme != null) {
            RLog.d(
                    TAG,
                    "Applied custom theme: "
                            + currentTheme
                            + " (based on "
                            + currentBaseTheme
                            + "), isDarkMode="
                            + isDarkMode
                            + ", applied "
                            + totalAppliedCount
                            + " styles");
        } else {
            RLog.d(
                    TAG,
                    "Applied theme: "
                            + currentTheme
                            + ", isDarkMode="
                            + isDarkMode
                            + ", applied "
                            + totalAppliedCount
                            + " of "
                            + configList.size()
                            + " styles");
        }
    }

    // ========== 内部类 ==========

    /** Activity 生命周期回调，用于在 Activity 创建时自动应用主题 */
    private static class ThemeCallback implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityCreated(
                @NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            RLog.v(TAG, "onActivityCreated: " + activity.getClass().getSimpleName());
            // applyTheme 会自动处理内置主题和自定义主题（包括基础主题叠加）
            IMKitThemeManager.applyTheme(activity);
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            // 无需处理
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            // 无需处理
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
            // 无需处理
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
            // 无需处理
        }

        @Override
        public void onActivitySaveInstanceState(
                @NonNull Activity activity, @NonNull Bundle outState) {
            // 无需处理
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            // 无需处理
        }
    }
}
