package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * @author Gregory Boissinot
 */
public abstract class ManifestFileContent extends PropertiesFileContent {

    protected transient Attributes attributes;

    public ManifestFileContent(String keys2Inspect, boolean allKeys) {
        super(keys2Inspect, allKeys);
    }

    @Override
    public Object getMemoryInfo() {
        return attributes;
    }

    @Override
    public void setMemoryInfo(Object memoryInfo) {
        if ((memoryInfo != null) && !(memoryInfo instanceof Attributes)) {
            throw new IllegalArgumentException(String.format("The memory info %s object is not an Attributes object.", memoryInfo));
        }
        this.attributes = (Attributes) memoryInfo;
    }

    /**
     * Gets the MANIFEST object from a File object
     *
     * @param file the input java.io.File object
     * @return the java.util.jar.Manifest object if exists, otherwise null
     */
    protected abstract Manifest getManifest(File file);


    private Attributes computeAttributesObject(File file) throws XTriggerException {
        Manifest manifest = getManifest(file);
        if (manifest == null) {
            throw new XTriggerException(String.format("The file '%s' doesn't contain any MANIFEST file", file));
        }

        Attributes ats = manifest.getMainAttributes();
        if (ats.isEmpty()) {
            //We don't accept a MANIFEST without attributes
            //Note: If the file is not a MANIFEST file, it has no attributes.
            throw new XTriggerException(String.format("The MANIFEST file '%s' doesn't contain any attributes", file));
        }

        if (allKeys) {
            return ats;
        }

        Attributes attributes2Writer = new Attributes();
        if (keys2Inspect != null) {
            String[] inputKeys = keys2Inspect.split(KEY_SEPARATOR);

            //Trims the User Inputs
            for (int i = 0; i < inputKeys.length; i++) {
                inputKeys[i] = inputKeys[i].trim();
            }


            //Process only UI keys
            List<String> inputKeyList = Arrays.asList(inputKeys);
            for (Map.Entry<Object, Object> entry : ats.entrySet()) {

                Object fileKeyObject = entry.getKey();
                Attributes.Name fileKey = null;
                if (!(fileKeyObject instanceof Attributes.Name)) {
                    throw new XTriggerException("Internal Error of conversion");
                } else {
                    fileKey = (Attributes.Name) fileKeyObject;
                    if (inputKeyList.contains(fileKey.toString())) {
                        attributes2Writer.put(fileKeyObject, entry.getValue());
                    }
                }
            }
        }
        return attributes2Writer;
    }


    @Override
    protected void initForContent(File file) throws XTriggerException {
        this.attributes = computeAttributesObject(file);
    }

    @Override
    protected boolean isTriggeringBuildForContent(File file, XTriggerLog log) throws XTriggerException {

        if (attributes == null) {
            return false;
        }

        Attributes newAttributes = computeAttributesObject(file);
        if (newAttributes == null) {
            return false;
        }

        if (attributes.size() != newAttributes.size()) {
            String msg = String.format("The new content file contains %d attribute(s) whereas the previous content contains %d attribute(s)", newAttributes.size(), attributes.size());
            log.info(msg);
            return true;
        }

        for (Map.Entry<?, ?> entry : attributes.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();

            Object newValue = newAttributes.get(key);
            if (newValue == null) {
                String msg = String.format("The '%s' attribute is not longer available.", key);
                log.info(msg);
                return true;
            }

            if (!newValue.equals(value)) {
                String msg = String.format("The previous value for the attribute '%s' was '%s' but it's now '%s'.", key, value, newValue);
                log.info(msg);
                return true;
            }

        }

        return false;
    }
}
