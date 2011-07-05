package org.jenkinsci.plugins.fstrigger.service;

import hudson.model.TaskListener;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerLog implements Serializable {

    private TaskListener listener;

    public FSTriggerLog() {
    }

    public FSTriggerLog(TaskListener listener) {
        this.listener = listener;
    }

    public void info(String jobName, Class triggerClass, String message) {
        if (listener != null) {
            String msg = String.format("[FSTrigger] - [Job %s] - [%s] - %s", jobName, triggerClass.getSimpleName(), message);
            listener.getLogger().println(msg);
        }
    }

    public void info(String message) {
        if (listener != null) {
            listener.getLogger().println(message);
        }
    }

    public void error(String jobName, Class triggerClass, String message) {
        if (listener != null) {
            String msg = String.format("[FSTrigger] - [Job %s] - [%s] - %s", jobName, triggerClass.getSimpleName(), message);
            listener.getLogger().println("[ERROR] - " + msg);
        }
    }

    public void error(String message) {
        if (listener != null) {
            listener.getLogger().println("[ERROR] - " + message);
        }
    }


}
