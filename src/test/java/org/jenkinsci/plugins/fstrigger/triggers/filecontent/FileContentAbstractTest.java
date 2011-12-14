package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.mockito.Mock;

import java.io.File;

/**
 * @author Gregory Boissinot
 */
public abstract class FileContentAbstractTest {

    @Mock
    protected XTriggerLog log;

    protected void initType(File file) throws XTriggerException {
        getTypeInstance().initMemoryFields("jobTest", file);
    }

    public abstract FSTriggerContentFileType getTypeInstance();

}
