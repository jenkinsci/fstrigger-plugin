package org.jenkinsci.plugins.fstrigger;

import org.jenkinsci.plugins.xtriggerapi.XTriggerCause;

/**
 * @author Gregory Boissinot
 */
@Deprecated
public class FSTriggerCause extends XTriggerCause {

    public FSTriggerCause(String causeFrom) {
        super("FSTrigger", causeFrom, false);
    }
}
