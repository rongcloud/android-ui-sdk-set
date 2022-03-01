package io.rong.imkit.feature.customservice.event;

import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imlib.cs.model.CSGroupItem;
import java.util.List;

public class CSSelectGroupEvent implements PageEvent {
    public List<CSGroupItem> mGroupList;

    public CSSelectGroupEvent(List<CSGroupItem> mGroupList) {
        this.mGroupList = mGroupList;
    }
}
