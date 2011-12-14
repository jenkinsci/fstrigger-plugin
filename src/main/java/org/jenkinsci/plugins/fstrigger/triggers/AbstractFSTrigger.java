package org.jenkinsci.plugins.fstrigger.triggers;

import antlr.ANTLRException;
import hudson.model.Label;
import org.jenkinsci.lib.xtrigger.AbstractTrigger;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public abstract class AbstractFSTrigger extends AbstractTrigger implements Serializable {

    protected AbstractFSTrigger(String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
    }

    protected transient boolean offlineSlaveOnStartup;

    protected boolean isOfflineNodes() {
        Label label = job.getAssignedLabel();
        if (label == null) {
            return false;
        }
        return label.isOffline();
    }

}
