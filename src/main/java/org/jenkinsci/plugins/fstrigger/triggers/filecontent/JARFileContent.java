package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Extension;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Gregory Boissinot
 */
public class JARFileContent extends ZIPFileContent {

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public JARFileContent() {
    }

    @Extension
    @SuppressWarnings("unused")
    public static class JARFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<JARFileContent> {

        @Override
        public String getDisplayName() {
            return "Monitor the contents of a JAR file";
        }

        @Override
        public String getLabel() {
            return "JAR File";
        }

    }

}
