package org.jenkinsci.plugins.fstrigger.triggers.filecontent;


import hudson.Extension;
import hudson.Util;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Gregory Boissinot
 */
public class SimpleFileContent extends FSTriggerContentFileType {

    private static final long serialVersionUID = 1L;

    /**
     * Memory field for detection
     */
    private transient String md5;

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public SimpleFileContent() {
    }

    @Override
    public Object getMemoryInfo() {
        return md5;
    }

    @Override
    public void setMemoryInfo(Object memoryInfo) {
        if (!(memoryInfo instanceof String)) {
            throw new IllegalArgumentException(String.format("The memory info %s object is not a String object.", memoryInfo));
        }
        this.md5 = (String) memoryInfo;
    }

    @Override
    protected void initForContent(File file) throws XTriggerException {
        try {
            FileInputStream fis = new FileInputStream(file);
            md5 = Util.getDigestOf(fis);
            fis.close();
        } catch (FileNotFoundException fne) {
            throw new XTriggerException(fne);
        } catch (IOException ioe) {
            throw new XTriggerException(ioe);
        }
    }

    @Override
    protected boolean isTriggeringBuildForContent(File file, XTriggerLog log) throws XTriggerException {
        String newComputedMd5;
        try {
            FileInputStream fis = new FileInputStream(file);
            newComputedMd5 = Util.getDigestOf(fis);
            fis.close();
        } catch (FileNotFoundException fne) {
            throw new XTriggerException(fne);
        } catch (IOException ioe) {
            throw new XTriggerException(ioe);
        }

        assert md5 != null;

        if (!newComputedMd5.equals(md5)) {
            String msg = "The content of the file '%s' has changed.";
            log.info(String.format(msg, file.getPath()));
            return true;
        }

        return false;
    }

    @Extension
    @SuppressWarnings("unused")
    public static class SimpleFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<SimpleFileContent> {

        @Override
        public Class<? extends FSTriggerContentFileType> getType() {
            return SimpleFileContent.class;
        }

        @Override
        public String getDisplayName() {
            return "Monitor a change of the content";
        }

        @Override
        public String getLabel() {
            return getDisplayName();
        }
    }

}
