package org.jenkinsci.plugins.fstrigger.core;

import hudson.model.AbstractProject;

import java.io.File;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerFolderAction extends FSTriggerAction {

    public FSTriggerFolderAction(AbstractProject<?, ?> job, File logFile, String actionTitle) {
        super(job, logFile, actionTitle);
    }

    @Override
    public String getDisplayName() {
        return "FSTrigger Folder Log";
    }

    @Override
    public String getUrlName() {
        return "triggerPollLogFolder";
    }
}
