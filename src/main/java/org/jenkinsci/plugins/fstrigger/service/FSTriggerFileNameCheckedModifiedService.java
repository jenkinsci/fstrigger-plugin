package org.jenkinsci.plugins.fstrigger.service;

import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.triggers.FileNameTriggerInfo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerFileNameCheckedModifiedService {

    private final XTriggerLog log;

    private final FileNameTriggerInfo fileInfo;

    private final File resolvedFile;

    private final long lastModifiedDateTime;

    private final File newResolvedFile;

    public FSTriggerFileNameCheckedModifiedService(XTriggerLog log, FileNameTriggerInfo fileInfo, String resolvedFilePath, Long resolvedFileLastModified, File newResolvedFile) {

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
        this.lastModifiedDateTime = resolvedFileLastModified;
        this.newResolvedFile = newResolvedFile;
    }

    public Boolean checkFileName() throws XTriggerException {

        if (newResolvedFile == null) {
            log.info("The computed file doesn't exist.");
            return false;
        }

        if (resolvedFile == null) {
            log.info("The file didn't exist for the previous polling and now it exists.");
            return true;
        }

        if (!resolvedFile.equals(newResolvedFile)) {
            log.info("The current polling file has changed.");
            return true;
        }

        if (!fileInfo.isDoNotCheckLastModificationDate() && (newResolvedFile.lastModified() != lastModifiedDateTime)) {
            log.info("The last modification date of the file '" + newResolvedFile + "' has changed.\n");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
            Date lastModifiedDate = new Date(lastModifiedDateTime);
            Date newResolvedFileDate = new Date(newResolvedFile.lastModified());
            log.info("The last date/time was   " + simpleDateFormat.format(lastModifiedDate));
            log.info("The current date/time is " + simpleDateFormat.format(newResolvedFileDate));
            return true;
        }

        return false;
    }


    public Boolean checkContentType(FSTriggerContentFileType type) throws XTriggerException {
        return type.isTriggeringBuild(newResolvedFile, log);
    }


}
