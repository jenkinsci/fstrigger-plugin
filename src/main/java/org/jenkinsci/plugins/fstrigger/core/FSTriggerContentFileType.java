package org.jenkinsci.plugins.fstrigger.core;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;

import java.io.File;
import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public abstract class FSTriggerContentFileType implements ExtensionPoint, Describable<FSTriggerContentFileType>, Serializable {

    /**
     * The current job name
     * Used for log
     */
    protected transient String jobName;

    /**
     * Called by the caller trigger for refreshing memory information
     *
     * @param jobName the current job name
     * @param file    the current file to inspect
     * @throws XTriggerException
     */
    public void initMemoryFields(String jobName, File file) throws XTriggerException {
        this.jobName = jobName;

        if (file == null) {
            throw new NullPointerException("The given file input reference is not set.");
        }
        if (!file.exists()) {
            throw new XTriggerException(String.format("The given file '%s' doesn't exist.", file));
        }

        initForContent(file);
    }


    /**
     * Called by the caller trigger for checking if there is a change
     *
     * @param file the current file to check
     * @param log  the log object
     * @return true if we need to schedule a job, false otherwise
     * @throws XTriggerException
     */
    public boolean isTriggeringBuild(File file, XTriggerLog log) throws XTriggerException {

        if (file == null) {
            throw new NullPointerException("The given file input reference is not set.");
        }
        if (!file.exists()) {
            throw new XTriggerException(String.format("The given file '%s' doesn't exist.", file));
        }

        return isTriggeringBuildForContent(file, log);
    }

    public Descriptor<FSTriggerContentFileType> getDescriptor() {
        return (FSTriggerContentFileTypeDescriptor) Jenkins.getActiveInstance().getDescriptor(getClass());
    }

    /**
     * Cycle of the trigger
     * These methods have to be overridden in each trigger implementation
     */
    protected abstract void initForContent(File file) throws XTriggerException;

    protected abstract boolean isTriggeringBuildForContent(File file, XTriggerLog log) throws XTriggerException;


    /**
     * Used by caller trigger for transferring objects between objects
     * between master and slave
     */
    public abstract Object getMemoryInfo();

    public abstract void setMemoryInfo(Object memoryInfo);

}
