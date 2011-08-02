package org.jenkinsci.plugins.fstrigger.triggers;

import hudson.triggers.TriggerDescriptor;

/**
 * @author Gregory Boissinot
 */
public abstract class FSTriggerDescriptor extends TriggerDescriptor {

    public abstract String getLabel();
}
