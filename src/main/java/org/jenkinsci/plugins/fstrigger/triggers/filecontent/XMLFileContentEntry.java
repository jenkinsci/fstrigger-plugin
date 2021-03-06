package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class XMLFileContentEntry implements Serializable {

    private final String expression;

    @DataBoundConstructor
    public XMLFileContentEntry(String expression) {
        this.expression = Util.fixEmpty(expression);
    }

    @SuppressWarnings("unused")
    public String getExpression() {
        return expression;
    }
}
