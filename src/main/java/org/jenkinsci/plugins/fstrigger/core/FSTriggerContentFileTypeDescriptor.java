package org.jenkinsci.plugins.fstrigger.core;

import hudson.model.Descriptor;

/**
 * @author Gregory Boissinot
 */
public abstract class FSTriggerContentFileTypeDescriptor<T extends FSTriggerContentFileType> extends Descriptor<FSTriggerContentFileType> {

    /**
     * Gets the label for displaying in FSTrigger log page
     *
     * @return the trigger label
     */
    public abstract String getLabel();

    public abstract Class<? extends FSTriggerContentFileType> getType();

    @SuppressWarnings("unused")
    public  String getTypePackageName(){
        return  getType().getName();
    }

}
