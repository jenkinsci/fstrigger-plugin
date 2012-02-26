package org.jenkinsci.plugins.fstrigger;

import org.jenkinsci.lib.xtrigger.XTriggerCause;

/**
 * @author Gregory Boissinot
 */
@Deprecated
public class FSTriggerCause extends XTriggerCause {

    public FSTriggerCause(String causeFrom) {
        super("FSTrigger", causeFrom, false);
    }
}
