package org.jenkinsci.plugins.fstrigger.core;

import hudson.model.AbstractProject;

import java.io.File;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerFilesAction extends FSTriggerAction {

    public FSTriggerFilesAction(AbstractProject<?, ?> job, File logFile, String actionTitle) {
        super(job, logFile, actionTitle);
    }

    @Override
    public String getDisplayName() {
        return "FSTrigger Files Log";
    }

    @Override
    public String getUrlName() {
        return "triggerPollLogFiles";
    }

}
