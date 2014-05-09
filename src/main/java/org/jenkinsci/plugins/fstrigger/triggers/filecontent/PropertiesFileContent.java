package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;

/**
 * @author Gregory Boissinot
 */
public class PropertiesFileContent extends FSTriggerContentFileType {

    /**
     * GUI fields
     */
    protected static final String KEY_SEPARATOR = ",";

    protected String keys2Inspect;
    protected boolean allKeys;

    /**
     * Memory fields for detection
     */
    private transient Properties properties = new Properties();

    @DataBoundConstructor
    public PropertiesFileContent(String keys2Inspect, boolean allKeys) {
        if (keys2Inspect != null && !keys2Inspect.trim().isEmpty()) {
            this.keys2Inspect = Util.fixEmpty(keys2Inspect);
        }
        this.allKeys = allKeys;
    }

    @Override
    public Object getMemoryInfo() {
        return properties;
    }

    @Override
    public void setMemoryInfo(Object memoryInfo) {
        if ((memoryInfo != null) && !(memoryInfo instanceof Properties)) {
            throw new IllegalArgumentException(String.format("The memory info %s object is not a Properties object.", memoryInfo));
        }
        this.properties = (Properties) memoryInfo;
    }


    @SuppressWarnings("unused")
    public String getKeys2Inspect() {
        return keys2Inspect;
    }

    @SuppressWarnings("unused")
    public boolean isAllKeys() {
        return allKeys;
    }


    private boolean isPropertiesFile(Properties prop) {

        //We don't accept any file without properties
        //Note: a non properties file can have no properties
        if (prop.isEmpty()) {
            return false;
        }

        boolean ok = true;
        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                return false;
            }

            if (value instanceof String) {
                String valueStr = String.valueOf(value);
                if (valueStr.trim().isEmpty()) {
                    ok = false;
                    continue;
                }
            }

            ok = true;
        }

        return ok;
    }

    private Properties computePropertiesObject(File file) throws XTriggerException {

        Properties propsReader = new Properties();
        try {
            Reader reader = new FileReader(file);
            propsReader.load(reader);
            reader.close();
        } catch (IOException ioe) {
            throw new XTriggerException(ioe);
        }

        if (!isPropertiesFile(propsReader)) {
            throw new XTriggerException(String.format("The '%s' has no properties", file));
        }

        if (allKeys) {
            return propsReader;
        }

        // Iterates from the UI keys
        Properties propsWriter = new Properties();
        if (keys2Inspect != null) {
            String[] keys = keys2Inspect.split(KEY_SEPARATOR);
            for (String key : keys) {
                key = key.trim();
                if (propsReader.containsKey(key)) {
                    propsWriter.put(key, propsReader.getProperty(key));
                }
            }
        }
        return propsWriter;

    }

    @Override
    protected void initForContent(File file) throws XTriggerException {
        this.properties = computePropertiesObject(file);
    }

    @Override
    protected boolean isTriggeringBuildForContent(File file, XTriggerLog log) throws XTriggerException {

        Properties newProperties = computePropertiesObject(file);
        assert newProperties != null;

        //Compare the properties objects
        if (properties.size() != newProperties.size()) {
            String msg = String.format("The new content file contains %d properties whereas the previous content contains %d properties", properties.size(), properties.size());
            log.info(msg);
            return true;
        }

        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            Object newValue = newProperties.get(key);
            if (newValue == null) {
                String msg = String.format("The property '%s' is not longer available.", key);
                log.info(msg);
                return true;
            }

            if (!newValue.equals(value)) {
                String msg = String.format("The previous value for the property '%s' was '%s' but it's now '%s'.", key, value, newValue);
                log.info(msg);
                return true;
            }
        }

        return false;
    }


    @Extension
    @SuppressWarnings("unused")
    public static class PropertiesFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<PropertiesFileContent> {

        @Override
        public Class<? extends FSTriggerContentFileType> getType() {
            return PropertiesFileContent.class;
        }

        @Override
        public String getDisplayName() {
            return "Monitor the contents of a properties file";
        }

        @Override
        public String getLabel() {
            return "Properties File";
        }

        /**
         * Performs presence check.
         *
         * @param value the keys
         * @return the form validation object
         */
        public FormValidation doCheckKeys2Inspect(@QueryParameter String value) {

            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("You must provide keys.");
            }

            return FormValidation.ok();
        }

    }

}
