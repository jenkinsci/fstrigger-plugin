package org.jenkinsci.plugins.fstrigger.triggers;

import java.io.Serializable;

/**
 * @author Gregory Boissinot
 */
public class FileNameTriggerInfo implements Serializable {

    private String folderPath;
    private String fileName;
    private String strategy;
    private boolean doNotCheckLastModificationDate;


    public FileNameTriggerInfo(String folderPath, String fileName, String strategy, boolean doNotCheckLastModificationDate) {
        this.folderPath = folderPath;
        this.fileName = fileName;
        this.strategy = strategy;
        this.doNotCheckLastModificationDate = doNotCheckLastModificationDate;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStrategy() {
        return strategy;
    }

    public boolean isDoNotCheckLastModificationDate() {
        return doNotCheckLastModificationDate;
    }

}
