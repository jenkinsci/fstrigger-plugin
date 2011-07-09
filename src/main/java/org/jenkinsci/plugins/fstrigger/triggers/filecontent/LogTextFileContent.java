package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Extension;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerLog;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gregory Boissinot
 */
public class LogTextFileContent extends FSTriggerContentFileType {

    private List<LogTextFileContentEntry> regexElements = new ArrayList<LogTextFileContentEntry>();

    @DataBoundConstructor
    public LogTextFileContent(List<LogTextFileContentEntry> element) {
        this.regexElements = element;
    }

    @Override
    public Object getMemoryInfo() {
        return regexElements;
    }

    @Override
    public void setMemoryInfo(Object memoryInfo) {
        if (!(memoryInfo instanceof List)) {
            throw new IllegalArgumentException(String.format("The memory info %s object is not a List object.", memoryInfo));
        }
        this.regexElements = (List) memoryInfo;
    }

    @SuppressWarnings("unused")
    public List<LogTextFileContentEntry> getRegexElements() {
        return regexElements;
    }

    @Override
    protected void initForContent(File file) throws FSTriggerException {
    }

    @Override
    protected boolean isTriggeringBuildForContent(File file, FSTriggerLog log) throws FSTriggerException {

        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            //Check line by line if a pattern matches
            while ((line = bufferedReader.readLine()) != null) {
                for (LogTextFileContentEntry regexEntry : regexElements) {
                    Pattern pattern = Pattern.compile(regexEntry.getRegex());
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        log.info(String.format("The line '%s' matches the pattern '%s'", line, pattern));
                        return true;
                    }
                }
            }
            bufferedReader.close();
            fileReader.close();
        } catch (FileNotFoundException fne) {
            throw new FSTriggerException(fne);
        } catch (IOException ioe) {
            throw new FSTriggerException(ioe);
        }

        return false;
    }


    @Extension
    @SuppressWarnings("unused")
    public static class LogFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<LogTextFileContent> {

        @Override
        public Class<? extends FSTriggerContentFileType> getType() {
            return LogTextFileContent.class;
        }

        @Override
        public String getDisplayName() {
            return "Poll the contents of a text file (e.g. log file)";
        }

        @Override
        public String getLabel() {
            return "Text File";
        }
    }

}