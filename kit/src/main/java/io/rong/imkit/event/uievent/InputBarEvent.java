package io.rong.imkit.event.uievent;

/**
 * 输入栏相关刷新事件。
 */
public class InputBarEvent implements PageEvent {
    public Type mType;
    public String mExtra;

    public InputBarEvent(Type type, String mExtra) {
        this.mType =type;
        this.mExtra = mExtra;
    }
    public enum Type {
        ReEdit,  //重新编辑
        ShowMoreMenu,
        HideMoreMenu,
        ActiveMoreMenu,
        InactiveMoreMenu
    }
}
