package io.rong.imkit.feature.customservice.event;

import io.rong.imkit.event.uievent.PageEvent;
import io.rong.imkit.feature.customservice.CSEvaluateDialog;

public class CSEvaluateEvent implements PageEvent {
    public CSEvaluateDialog.EvaluateDialogType mDialogType;
    public boolean isResolved;

    public CSEvaluateEvent(CSEvaluateDialog.EvaluateDialogType type, boolean isResolved) {
        this.mDialogType = type;
        this.isResolved = isResolved;
    }
}
