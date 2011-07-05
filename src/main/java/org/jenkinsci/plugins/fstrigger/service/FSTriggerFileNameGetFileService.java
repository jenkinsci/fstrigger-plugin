package org.jenkinsci.plugins.fstrigger.service;

import hudson.Util;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.triggers.FileNameTrigger;
import org.jenkinsci.plugins.fstrigger.triggers.FileNameTriggerInfo;

import java.io.File;
import java.util.Iterator;


/**
 * @author Gregory Boissinot
 */
public class FSTriggerFileNameGetFileService {

    //The current logger
    private FSTriggerLog log;

    // FileNameTrigger information
    private FileNameTriggerInfo fileInfo;

    public FSTriggerFileNameGetFileService(FSTriggerLog log, FileNameTriggerInfo fileInfo) {

        if (log == null) {
            throw new NullPointerException("The log object must be set.");
        }

        if (fileInfo == null) {
            throw new NullPointerException("The file info object must be set.");
        }

        this.log = log;
        this.fileInfo = fileInfo;
    }

    public File call() throws FSTriggerException {

        if (fileInfo.getFolderPath() == null) {
            return null;
        }

        if (fileInfo.getFileName() == null) {
            return null;
        }

        //Tests the ¬existing of the folder
        File folderPathFile = new File(fileInfo.getFolderPath());
        if (!folderPathFile.exists()) {
            String msg = String.format("The folder path '%s' doesn't exist.", fileInfo.getFolderPath());
            log.info(msg);
            return null;
        }

        //Computes all the files
        FileSet fileSet = Util.createFileSet(folderPathFile, fileInfo.getFileName());
        if (fileSet.size() == 0) {
            log.info(String.format("There is no matching files in the folder '%s' for the fileName '%s'", fileInfo.getFolderPath(), fileInfo.getFileName()));
            return null;
        }

        if (fileSet.size() == 1) {
            File file = ((FileResource) fileSet.iterator().next()).getFile();
            log.info(String.format("Monitoring the  file '%s'", file));
            return file;
        }

        if (fileSet.size() > 1) {

            log.info(String.format("There is more than one file for the pattern '%s' in the folder '%s'", fileInfo.getFileName(), fileInfo.getFolderPath()));
            if (FileNameTrigger.STRATEGY_IGNORE.equals(fileInfo.getStrategy())) {
                log.info("Regarding the checked strategy, the schedule has been ignored.");
                return null;
            }

            if (FileNameTrigger.STRATEGY_LATEST.equals(fileInfo.getStrategy())) {
                log.info("Regarding the checked strategy, the latest modified file has been selected for the polling.");
                File lastModifiedFile = null;
                for (Iterator it = fileSet.iterator(); it.hasNext();) {
                    FileResource fileResource = (FileResource) it.next();
                    File curFile = fileResource.getFile();
                    if ((lastModifiedFile == null) || curFile.lastModified() > lastModifiedFile.lastModified()) {
                        lastModifiedFile = curFile;
                    }
                }
                log.info("The selected file for polling is '" + lastModifiedFile.getPath() + "'");
                return lastModifiedFile;
            }

            throw new RuntimeException("The strategy '" + fileInfo.getStrategy() + "' is not supported.");

        }

        return null;
    }
}
