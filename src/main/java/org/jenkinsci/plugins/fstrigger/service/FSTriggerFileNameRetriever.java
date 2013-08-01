package org.jenkinsci.plugins.fstrigger.service;

import hudson.Util;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.triggers.FileNameTrigger;
import org.jenkinsci.plugins.fstrigger.triggers.FileNameTriggerInfo;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;


/**
 * @author Gregory Boissinot
 */
public class FSTriggerFileNameRetriever implements Serializable {

    private XTriggerLog log;

    private FileNameTriggerInfo fileInfo;

    private Map<String, String> envVars;

    public FSTriggerFileNameRetriever(FileNameTriggerInfo fileInfo, XTriggerLog log, Map<String, String> envVars) {

        if (log == null) {
            throw new NullPointerException("The log object must be set.");
        }

        if (fileInfo == null) {
            throw new NullPointerException("The file info object must be set.");
        }

        this.log = log;
        this.fileInfo = fileInfo;
        this.envVars = envVars;
    }


    public File getFile() throws XTriggerException {

        if (fileInfo.getFilePathPattern() == null) {
            log.info("A file pattern must be set.");
            return null;
        }

        FileNameExtractInfo extractInfo = extract(fileInfo.getFilePathPattern());
        String folder = extractInfo.getRootDir();
        String fileName = extractInfo.getFileNamePattern();

        //Tests the existing of the folder
        File folderPathFile = new File(folder);
        if (!folderPathFile.exists()) {
            String msg = String.format("The folder path '%s' doesn't exist.", folder);
            log.info(msg);
            return null;
        }

        //Computes all the files
        FileSet fileSet = Util.createFileSet(folderPathFile, fileName);
        if (fileSet.size() == 0) {
            log.info(String.format("There is no matching files in the folder '%s' for the fileName '%s'.", folder, fileName));
            return null;
        }

        if (fileSet.size() == 1) {
            File file = ((FileResource) fileSet.iterator().next()).getFile();
            log.info(String.format("Checking one file: '%s'.", file));
            return file;
        }

        if (fileSet.size() > 1) {

            log.info(String.format("There is more than one file for the file pattern '%s'.", fileInfo.getFilePathPattern()));
            if (FileNameTrigger.STRATEGY_IGNORE.equals(fileInfo.getStrategy())) {
                log.info("According to the checked strategy, the schedule has been ignored.");
                return null;
            }

            if (FileNameTrigger.STRATEGY_LATEST.equals(fileInfo.getStrategy())) {
                log.info("According to the checked strategy, the latest modified file has been selected for the polling.");
                File lastModifiedFile = null;
                for (Iterator it = fileSet.iterator(); it.hasNext(); ) {
                    FileResource fileResource = (FileResource) it.next();
                    File curFile = fileResource.getFile();
                    if ((lastModifiedFile == null)
                            || ((curFile != null) && curFile.lastModified() > lastModifiedFile.lastModified())) {
                        lastModifiedFile = curFile;
                    }
                }

                if (lastModifiedFile != null) {
                    log.info(String.format("The selected file to poll is '%s'.", lastModifiedFile.getPath()));
                    return lastModifiedFile;
                }

                return null;
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

    private FileNameExtractInfo extract(String filePattern) throws XTriggerException {

        String fileToMonitor = filter(filePattern);
        if (fileToMonitor == null) {
            throw new XTriggerException("There is not files to monitor.");
        }

        if (fileToMonitor.length() < 2) {
            throw new XTriggerException("The given pattern for the file to monitor must have a directory.");
        }

        if (fileToMonitor.lastIndexOf(File.separator) == -1) {
            throw new XTriggerException("The given pattern for the file to monitor must have a directory.");
        }

        return new FileNameExtractInfo(
                fileToMonitor.substring(0, fileToMonitor.lastIndexOf(File.separator)),
                fileToMonitor.substring(fileToMonitor.lastIndexOf(File.separator) + 1));
    }


    private String filter(String filePattern) {

        if (filePattern == null) {
            return null;
        }

        filePattern = filePattern.replaceAll("[\t\r\n]+", " ");
        filePattern = filePattern.replaceAll("\\\\", Matcher.quoteReplacement(File.separator));
        filePattern = filePattern.trim();

        if (envVars != null) {
            filePattern = Util.replaceMacro(filePattern, envVars);
        }

        return filePattern;
    }
}
