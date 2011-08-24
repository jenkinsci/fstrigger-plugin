package org.jenkinsci.plugins.fstrigger.triggers;

import antlr.ANTLRException;
import hudson.Util;
import hudson.model.BuildableItem;
import hudson.triggers.Trigger;
import org.jenkinsci.plugins.fstrigger.FSTriggerCause;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerLog;

import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;


/**
 * @author Gregory Boissinot
 */
public abstract class AbstractTrigger extends Trigger<BuildableItem> implements Serializable {

    /**
     * Set to true if the starting stage was called when
     * the slave job was offline.
     */
    protected transient boolean offlineSlavesForStartingStage;

    /**
     * Set to true if the check stage was called when
     * the slave job was offline.
     */
    protected transient boolean offlineSlavesForCheckingStage;


    /**
     * Builds a trigger object
     * Calls an implementation trigger
     *
     * @param cronTabSpec the scheduler value
     * @throws ANTLRException
     */
    public AbstractTrigger(String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
    }

    /**
     * Gets the triggering log file
     *
     * @return the trigger log
     */
    protected File getLogFile() {
        return new File(job.getRootDir(), "trigger-polling.log");
    }

    /**
     * Checks if the new folder content has been modified
     * The date time and the content file are used.
     *
     * @return true if the new folder content has been modified
     * @throws FSTriggerException
     */
    protected abstract boolean checkIfModified(FSTriggerLog log) throws FSTriggerException;


    /**
     * Gets the trigger cause
     *
     * @return the trigger cause
     */
    public abstract String getCause();


    private void setOffLineInfo(boolean startStage, boolean value) {
        if (startStage) {
            offlineSlavesForStartingStage = value;
        }
        offlineSlavesForCheckingStage = value;
    }

    /**
     * Resets the information about offline slave
     *
     * @param startStage
     */
    protected void disableOffLineInfo(boolean startStage) {
        setOffLineInfo(startStage, false);
    }

    /**
     * Enables the information about offline slave
     *
     * @param startStage
     */
    protected void enableOffLineInfo(boolean startStage) {
        setOffLineInfo(startStage, true);
    }

    /**
     * Asynchronous task
     */
    protected class Runner implements Runnable, Serializable {

        private FSTriggerLog log;

        Runner(FSTriggerLog log) {
            this.log = log;
        }

        public void run() {

            try {
                long start = System.currentTimeMillis();
                log.info("Polling started on " + DateFormat.getDateTimeInstance().format(new Date(start)));
                boolean changed = checkIfModified(log);
                log.info("\nPolling complete. Took " + Util.getTimeSpanString(System.currentTimeMillis() - start));
                if (changed) {
                    log.info("Changes found. Scheduling a build.");
                    job.scheduleBuild(new FSTriggerCause(getCause()));
                } else {
                    log.info("No changes.");
                }

            } catch (FSTriggerException e) {
                log.error("Polling error " + e.getMessage());
            } catch (Throwable e) {
                log.error("SEVERE - Polling error " + e.getMessage());
            }
        }
    }

}
