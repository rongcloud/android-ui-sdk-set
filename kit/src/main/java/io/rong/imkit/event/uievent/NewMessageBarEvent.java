package io.rong.imkit.event.uievent;

public class NewMessageBarEvent implements PageEvent {
    private int count;

    public NewMessageBarEvent(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
