package org.jenkinsci.plugins.fstrigger.core;

import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerLog;

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
     * @throws FSTriggerException
     */
    public void initMemoryFields(String jobName, File file) throws FSTriggerException {
        this.jobName = jobName;

        if (file == null) {
            throw new NullPointerException("The given file input reference is not set.");
        }
        if (!file.exists()) {
            throw new FSTriggerException(String.format("The given file '%s' doesn't exist.", file));
        }

        initForContent(file);
    }


    /**
     * Called by the caller trigger for checking if there is a change
     *
     * @param file the current file to check
     * @param log  the log object
     * @return true if we need to schedule a job, false otherwise
     * @throws FSTriggerException
     */
    public boolean isTriggeringBuild(File file, FSTriggerLog log) throws FSTriggerException {

        if (file == null) {
            throw new NullPointerException("The given file input reference is not set.");
        }
        if (!file.exists()) {
            throw new FSTriggerException(String.format("The given file '%s' doesn't exist.", file));
        }

        return isTriggeringBuildForContent(file, log);
    }

    public Descriptor<FSTriggerContentFileType> getDescriptor() {
        return (FSTriggerContentFileTypeDescriptor) Hudson.getInstance().getDescriptor(getClass());
    }

    /**
     * Cycle of the trigger
     * These methods have to be overridden in each trigger implementation
     */
    protected abstract void initForContent(File file) throws FSTriggerException;

    protected abstract boolean isTriggeringBuildForContent(File file, FSTriggerLog log) throws FSTriggerException;


    /**
     * Used by caller trigger for transferring objects between objects
     * between master and slave
     */
    public abstract Object getMemoryInfo();

    public abstract void setMemoryInfo(Object memoryInfo);

}
