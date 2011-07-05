package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class LogTextFileContentEntry implements Serializable {

    private String regex;

    @DataBoundConstructor
    public LogTextFileContentEntry(String regex) {
        this.regex = regex;
    }

    @SuppressWarnings("unused")
    public String getRegex() {
        return regex;
    }
}
