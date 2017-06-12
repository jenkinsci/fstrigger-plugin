package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Extension;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Manifest;

/**
 * @author Gregory Boissinot
 */
public class SourceManifestFileContent extends ManifestFileContent {

    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public SourceManifestFileContent(String keys2Inspect, boolean allKeys) {
        super(keys2Inspect, allKeys);
    }

    @Override
    protected Manifest getManifest(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            Manifest manifest = new Manifest(fis);
            fis.close();
            return manifest;
        } catch (IOException ioe) {
            return null;
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static class SourceManifestFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<SourceManifestFileContent> {

        @Override
        public Class<? extends FSTriggerContentFileType> getType() {
            return SourceManifestFileContent.class;
        }

        @Override
        public String getDisplayName() {
            return "Monitor a raw MANIFEST file";
        }

        @Override
        public String getLabel() {
            return "Source MANIFEST File";
        }

    }
}
