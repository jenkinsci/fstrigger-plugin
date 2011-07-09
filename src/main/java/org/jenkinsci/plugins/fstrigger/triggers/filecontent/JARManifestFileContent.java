package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Extension;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author Gregory Boissinot
 */
public class JARManifestFileContent extends ManifestFileContent {

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public JARManifestFileContent(String keys2Inspect, boolean allKeys) {
        super(keys2Inspect, allKeys);
    }

    @Override
    protected Manifest getManifest(File file) {
        try {
            JarFile jarFile = new JarFile(file);
            return jarFile.getManifest();
        } catch (IOException ioe) {
            return null;
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static class JARManifestFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<JARManifestFileContent> {

        @Override
        public Class<? extends FSTriggerContentFileType> getType() {
            return JARManifestFileContent.class;
        }

        @Override
        public String getDisplayName() {
            return "Monitor a MANIFEST file (contained in a Jar file)";
        }

        public String getLabel() {
            return "JAR MANIFEST File";
        }

    }

}
