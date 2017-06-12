package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class TextFileContentEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String regex;

    @DataBoundConstructor
    public TextFileContentEntry(String regex) {
        this.regex = Util.fixEmpty(regex);
    }

    @SuppressWarnings("unused")
    public String getRegex() {
        return regex;
    }
}
