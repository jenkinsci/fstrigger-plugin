package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gregory Boissinot
 */
@ExtendWith(MockitoExtension.class)
class SimpleFileContentTest extends FileContentAbstractTest {

    protected FSTriggerContentFileType type;

    @Mock
    protected XTriggerLog log;

    @BeforeEach
    void setUp() {
        type = new SimpleFileContent();
    }

    protected FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    @Test
    void testInitNullFileRefAsInput() {
        assertThrows(NullPointerException.class, () ->
            initType(null));
    }

    @Test
    void testInitNoExistFileAsInput() {
        File initFile = new File("noExist.txt");
        assertThrows(XTriggerException.class, () ->
            initType(initFile));
    }

    @Test
    void testPollingNewFileNoExistAsInput() throws Exception {
        File initFile = new File(this.getClass().getResource("SimpleFileContent/initFile.txt").toURI());
        initType(initFile);
        assertThrows(XTriggerException.class, () ->
            type.isTriggeringBuild(new File("noExist"), log));
    }


    @Test
    void testPollingNewFileNullReferenceAsInput() throws Exception {
        File initFile = new File(this.getClass().getResource("SimpleFileContent/initFile.txt").toURI());
        initType(initFile);
        assertThrows(NullPointerException.class, () ->
            type.isTriggeringBuild(null, log));
    }

    @Test
    void testPolling() throws Exception {
        File initFile = new File(this.getClass().getResource("SimpleFileContent/initFile.txt").toURI());
        initType(initFile);
        assertFalse(type.isTriggeringBuild(initFile, log));
        File newFile = new File(this.getClass().getResource("SimpleFileContent/newFile.txt").toURI());
        assertTrue(type.isTriggeringBuild(newFile, log));
    }

}
