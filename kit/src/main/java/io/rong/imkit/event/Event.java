package io.rong.imkit.event;

import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imkit.widget.refresh.constant.RefreshState;

public class Event {
    public static class RefreshEvent implements PageEvent {
        public RefreshState state;

        public RefreshEvent(RefreshState state) {
            this.state = state;
        }
    }

}
