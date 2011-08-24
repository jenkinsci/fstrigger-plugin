package org.jenkinsci.plugins.fstrigger.service;

import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.triggers.FileNameTriggerInfo;

import java.io.File;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerFileNameCheckedModifiedService {

    private FSTriggerLog log;

    private FileNameTriggerInfo fileInfo;

    private File resolvedFile;

    private long lastModifiedDate;

    private File newResolvedFile;

    public FSTriggerFileNameCheckedModifiedService(FSTriggerLog log, FileNameTriggerInfo fileInfo, String resolvedFilePath, Long resolvedFileLastModified, File newResolvedFile) {

        if (log == null) {
            throw new NullPointerException("The log object must be set.");
        }

        if (fileInfo == null) {
            throw new NullPointerException("The file info object must be set.");
        }

        this.log = log;
        this.fileInfo = fileInfo;
        if (resolvedFilePath == null) {
            this.resolvedFile = null;
        } else {
            this.resolvedFile = new File(resolvedFilePath);
        }
        this.lastModifiedDate = resolvedFileLastModified;
        this.newResolvedFile = newResolvedFile;
    }

    public Boolean checkFileName() throws FSTriggerException {

        if (newResolvedFile == null) {
            log.info("The computed file doesn't exist.");
            return false;
        }

        if (resolvedFile == null && newResolvedFile != null) {
            log.info("The file didn't exist for the previous polling and now it exists.");
            return true;
        }

        assert (resolvedFile != null);

        if (!resolvedFile.equals(newResolvedFile)) {
            log.info("The current polling file has changed.");
            return true;
        }

        if (!fileInfo.isDoNotCheckLastModificationDate() && (newResolvedFile.lastModified() != lastModifiedDate)) {
            log.info("The last modified date of the file '" + newResolvedFile + "' has changed.");
            return true;
        }

        return false;
    }


    public Boolean checkContentType(FSTriggerContentFileType type) throws FSTriggerException {
        return type.isTriggeringBuild(newResolvedFile, log);
    }


}
