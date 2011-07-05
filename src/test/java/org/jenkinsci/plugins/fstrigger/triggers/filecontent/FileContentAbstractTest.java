package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerLog;
import org.mockito.Mock;

import java.io.File;

/**
 * @author Gregory Boissinot
 */
public abstract class FileContentAbstractTest {

    @Mock
    protected FSTriggerLog log;

    protected void initType(File file) throws FSTriggerException {
        getTypeInstance().initMemoryFields("jobTest", file);
    }

    public abstract FSTriggerContentFileType getTypeInstance();

}
