package org.jenkinsci.plugins.fstrigger.triggers;

import hudson.FilePath;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Gregory Boissinot
 */
public class FileNameTriggerInfo implements Serializable {

    private String filePathPattern;

    private String strategy;

    private boolean inspectingContentFile;

    private boolean doNotCheckLastModificationDate;

    private FSTriggerContentFileType[] contentFileTypes;

    private transient FilePath resolvedFile;
    private transient long lastModifications;

    /**
     * Getters and setters
     */
    @SuppressWarnings("unused")
    public String getFilePathPattern() {
        return filePathPattern;
    }

    @SuppressWarnings("unused")
    public String getStrategy() {
        return strategy;
    }

    @SuppressWarnings("unused")
    public boolean isInspectingContentFile() {
        return inspectingContentFile;
    }

    @SuppressWarnings("unused")
    public FSTriggerContentFileType[] getContentFileTypes() {
        return Arrays.copyOf(contentFileTypes, contentFileTypes.length);
    }

    @SuppressWarnings("unused")
    public boolean isDoNotCheckLastModificationDate() {
        return doNotCheckLastModificationDate;
    }

    public void setFilePathPattern(String filePathPattern) {
        this.filePathPattern = filePathPattern;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setInspectingContentFile(boolean inspectingContentFile) {
        this.inspectingContentFile = inspectingContentFile;
    }

    public void setContentFileTypes(FSTriggerContentFileType[] contentFileTypes) {
        this.contentFileTypes = Arrays.copyOf(contentFileTypes, contentFileTypes.length);
    }

    public void setDoNotCheckLastModificationDate(boolean doNotCheckLastModificationDate) {
        this.doNotCheckLastModificationDate = doNotCheckLastModificationDate;
    }

    public FilePath getResolvedFile() {
        return resolvedFile;
    }

    public void setResolvedFile(FilePath resolvedFile) {
        this.resolvedFile = resolvedFile;
    }

    public long getLastModifications() {
        return lastModifications;
    }

    public void setLastModifications(long lastModifications) {
        this.lastModifications = lastModifications;
    }

    private static final long serialVersionUID = 1L;
}
