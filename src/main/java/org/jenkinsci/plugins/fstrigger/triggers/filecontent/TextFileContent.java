package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

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
    public TextFileContent(List<TextFileContentEntry> element) throws Descriptor.FormException {
        this.regexElements = element;
    }

    @Override
    public Object getMemoryInfo() {
        return regexElements;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setMemoryInfo(Object memoryInfo) {
        if ((memoryInfo != null) && !(memoryInfo instanceof List)) {
            throw new IllegalArgumentException(String.format("The memory info %s object is not a List object.", memoryInfo));
        }
        this.regexElements = (List<TextFileContentEntry>) memoryInfo;
    }

    @SuppressWarnings("unused")
    public List<TextFileContentEntry> getRegexElements() {
        return regexElements;
    }

    @Override
    protected void initForContent(File file) throws XTriggerException {
    }

    @Override
    protected boolean isTriggeringBuildForContent(File file, XTriggerLog log) throws XTriggerException {

        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        try {
            fileReader = new FileReader(file);
            bufferedReader = new BufferedReader(fileReader);
            String line;
            //Check line by line if a pattern matches
            while ((line = bufferedReader.readLine()) != null) {
                for (TextFileContentEntry regexEntry : regexElements) {
                    String regex = regexEntry.getRegex();
                    if (regex == null) {
                        log.info("You have to provide a pattern for each entry");
                        return false;
                    }
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        log.info(String.format("The line '%s' matches the pattern '%s'", line, pattern));
                        return true;
                    }
                }
            }
        } catch (FileNotFoundException fne) {
            throw new XTriggerException(fne);
        } catch (IOException ioe) {
            throw new XTriggerException(ioe);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ioe) {
                    throw new XTriggerException(ioe);
                }
            }
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException ioe) {
                    throw new XTriggerException(ioe);
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


        /**
         * Performs presence check.
         *
         * @param value the regular expression
         * @return the form validation object
         */
        public FormValidation doCheckRegex(@QueryParameter String value) {

            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("You must provide a regular expression.");
            }

            return FormValidation.ok();
        }

    }

}