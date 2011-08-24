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

    public FSTriggerFileNameGetFileService(FileNameTriggerInfo fileInfo, FSTriggerLog log) {

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

        if (fileInfo.getFileName() == null) {
            return null;
        }

        log.info("\n" + String.format("Trying to monitor the  file '%s'", fileInfo.getFileName()));
        FileNameExtractInfo extractInfo = extract(fileInfo.getFileName());
        String folder = extractInfo.getRootDir();
        String fileName = extractInfo.getFileNamePattern();


        //Tests the ¬existing of the folder
        File folderPathFile = new File(folder);
        if (!folderPathFile.exists()) {
            String msg = String.format("The folder path '%s' doesn't exist.", folder);
            log.info(msg);
            return null;
        }

        //Computes all the files
        FileSet fileSet = Util.createFileSet(folderPathFile, fileName);
        if (fileSet.size() == 0) {
            log.info(String.format("There is no matching files in the folder '%s' for the fileName '%s'", folder, fileName));
            return null;
        }

        if (fileSet.size() == 1) {
            File file = ((FileResource) fileSet.iterator().next()).getFile();
            log.info(String.format("Inspecting the  file '%s'", file));
            return file;
        }

        if (fileSet.size() > 1) {

            log.info(String.format("There is more than one file for the file pattern '%s'.", fileInfo.getFileName()));
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

    class FileNameExtractInfo {

        private String rootDir;

        private String fileNamePattern;

        FileNameExtractInfo(String rootDir, String fileNamePattern) {
            this.rootDir = rootDir;
            this.fileNamePattern = fileNamePattern;
        }

        public String getRootDir() {
            return rootDir;
        }

        public String getFileNamePattern() {
            return fileNamePattern;
        }
    }

    private FileNameExtractInfo extract(String guiFileName) {

        String fileName = filter(guiFileName);

        if (fileName.length() < 3) {
            //TODO
        }

        if (fileName.lastIndexOf(File.separator) == -1) {
            //TODO
        }

        return new FileNameExtractInfo(
                fileName.substring(0, fileName.lastIndexOf(File.separator)),
                fileName.substring(fileName.lastIndexOf(File.separator) + 1));
    }


    private String filter(String fileName) {

        if (fileName == null) {
            return null;
        }

        fileName = fileName.replaceAll("[\t\r\n]+", " ");
        fileName = fileName.replaceAll("\\\\", File.separator);
        fileName = fileName.trim();

        return fileName;
    }
}
