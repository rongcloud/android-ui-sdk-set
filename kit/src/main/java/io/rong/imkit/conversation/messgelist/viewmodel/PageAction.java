package io.rong.imkit.conversation.messgelist.viewmodel;

public class PageAction {
    public static final int FINISH_REFRESH = 0;
    public static final int FINISH_LOAD_MORE = 1;
    public static final int TOAST = 2;
    public static final int CUSTOM_SERVICE_ACTION = 3;
    public static final int SHOW_LONG_CLICK_DIALOG = 4;
    public static final int SCROLL_TO_POSITION = 5;
    public static final int SMOOTH_SCROLL_TO_POSITION = 6;
    public static final int SHOW_UNREAD_BAR = 7;
    public static final PageAction ACTION_FINISH_REFRESH = new PageAction(FINISH_REFRESH);
    public static final PageAction ACTION_NO_MORE_DATA = new PageAction(FINISH_LOAD_MORE);
    private int action;
    private Object obj;

    public PageAction(int action) {
        this(action, null);
    }

    public PageAction(int action, Object obj) {
        this.action = action;
        this.obj = obj;
    }

    public int getAction() {
        return action;
    }

    public Object get() {
        return obj;
    }

    public <T> T get(Class<T> clazz) {
        if (obj == null)
            return null;
        if (obj.getClass().equals(clazz)) {
            return (T) obj;
        } else {
            return null;
        }
    }

}
