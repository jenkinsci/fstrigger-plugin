package org.jenkinsci.plugins.fstrigger;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerException extends Exception {

    public FSTriggerException() {
    }

    public FSTriggerException(String s) {
        super(s);
    }

    public FSTriggerException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public FSTriggerException(Throwable throwable) {
        super(throwable);
    }
}
