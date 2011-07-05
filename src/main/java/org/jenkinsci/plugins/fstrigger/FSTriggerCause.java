package org.jenkinsci.plugins.fstrigger;

import hudson.model.Cause;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerCause extends Cause {

    private final String causeFrom;

    public FSTriggerCause(String causeFrom) {
        this.causeFrom = causeFrom;
    }

    @Override
    public String getShortDescription() {
        if (causeFrom == null) {
            return "[FSTrigger]";
        } else {
            return String.format("[FSTrigger] '%s'", causeFrom);
        }
    }


}
