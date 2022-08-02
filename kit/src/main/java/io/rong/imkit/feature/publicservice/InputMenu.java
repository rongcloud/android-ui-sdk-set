package io.rong.imkit.feature.publicservice;

import java.util.List;


public class InputMenu {
    public String title;
    public List<String> subMenuList;

    public static InputMenu obtain() {
        return new InputMenu();
    }
}
