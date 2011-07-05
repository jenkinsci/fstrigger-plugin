package org.jenkinsci.plugins.fstrigger.core;

import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
import org.apache.commons.jelly.XMLOutput;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerAction implements Action {

    private transient AbstractProject<?, ?> job;

    private transient File logFile;

    private transient String actionTitle;

    private transient String[] subActionTitle;

    public FSTriggerAction(AbstractProject<?, ?> job, File logFile, String actionTitle, String[] subActionTitle) {
        this.job = job;
        this.logFile = logFile;
        this.actionTitle = actionTitle;
        this.subActionTitle = subActionTitle;
    }

    public AbstractProject<?, ?> getOwner() {
        return job;
    }

    @SuppressWarnings("unused")
    public String getActionTitle() {
        return actionTitle;
    }

    @SuppressWarnings("unused")
    public String[] getSubActionTitle() {
        return subActionTitle;
    }

    public String getIconFileName() {
        return "clipboard.gif";
    }

    public String getDisplayName() {
        return "FSTrigger Log";
    }

    public String getUrlName() {
        return "triggerPollLog";
    }

    @SuppressWarnings("unused")
    public String getLog() throws IOException {
        return Util.loadFile(getLogFile());
    }

    public File getLogFile() {
        return logFile;
    }

    @SuppressWarnings("unused")
    public void writeLogTo(XMLOutput out) throws IOException {
        new AnnotatedLargeText<FSTriggerAction>(getLogFile(), Charset.defaultCharset(), true, this).writeHtmlTo(0, out.asWriter());
    }
}
