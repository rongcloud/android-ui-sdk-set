package io.rong.imkit.event.uievent;

public class ScrollEvent implements PageEvent {
    private int position;

    public ScrollEvent(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
