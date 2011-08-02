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
public class TextFileContent extends FSTriggerContentFileType {

    private List<TextFileContentEntry> regexElements = new ArrayList<TextFileContentEntry>();

    @DataBoundConstructor
    public TextFileContent(List<TextFileContentEntry> element) {
        this.regexElements = element;
    }

    @Override
    public Object getMemoryInfo() {
        return regexElements;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setMemoryInfo(Object memoryInfo) {
        if (!(memoryInfo instanceof List)) {
            throw new IllegalArgumentException(String.format("The memory info %s object is not a List object.", memoryInfo));
        }
        this.regexElements = (List<TextFileContentEntry>) memoryInfo;
    }

    @SuppressWarnings("unused")
    public List<TextFileContentEntry> getRegexElements() {
        return regexElements;
    }

    @Override
    protected void initForContent(File file) throws FSTriggerException {
    }

    @Override
    protected boolean isTriggeringBuildForContent(File file, FSTriggerLog log) throws FSTriggerException {

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            String line;
            //Check line by line if a pattern matches
            while ((line = bufferedReader.readLine()) != null) {
                for (TextFileContentEntry regexEntry : regexElements) {
                    Pattern pattern = Pattern.compile(regexEntry.getRegex());
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        log.info(String.format("The line '%s' matches the pattern '%s'", line, pattern));
                        return true;
                    }
                }
            }
        } catch (FileNotFoundException fne) {
            throw new FSTriggerException(fne);
        } catch (IOException ioe) {
            throw new FSTriggerException(ioe);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ioe) {
                    throw new FSTriggerException(ioe);
                }
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException ioe) {
                    throw new FSTriggerException(ioe);
                }
            }
        }


        return false;
    }


    @Extension
    @SuppressWarnings("unused")
    public static class LogFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<TextFileContent> {

        @Override
        public Class<? extends FSTriggerContentFileType> getType() {
            return TextFileContent.class;
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