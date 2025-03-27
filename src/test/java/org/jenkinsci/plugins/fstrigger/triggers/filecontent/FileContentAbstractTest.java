package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

/**
 * @author Gregory Boissinot
 */
@ExtendWith(MockitoExtension.class)
abstract class FileContentAbstractTest {

    @Mock
    protected XTriggerLog log;

    protected void initType(File file) throws XTriggerException {
        getTypeInstance().initMemoryFields("jobTest", file);
    }

    protected abstract FSTriggerContentFileType getTypeInstance();

}
