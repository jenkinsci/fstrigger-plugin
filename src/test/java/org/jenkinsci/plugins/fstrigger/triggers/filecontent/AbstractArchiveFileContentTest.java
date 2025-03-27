package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gregory Boissinot
 */
@ExtendWith(MockitoExtension.class)
abstract class AbstractArchiveFileContentTest extends FileContentAbstractTest {

    protected FSTriggerContentFileType type;

    @Mock
    protected XTriggerLog log;

    protected FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    protected abstract File getInitFile() throws URISyntaxException;

    protected abstract File getNewFileAddedFile() throws URISyntaxException;

    protected abstract File getNewFileChangedContentOneFile() throws URISyntaxException;

    protected abstract File getNoExistFile();

    protected abstract File getNotGoodTypeFile() throws URISyntaxException;

    @Test
    void testInitNullFileRefAsInput() {
        assertThrows(NullPointerException.class, () ->
            initType(null));
    }

    @Test
    void testInitNoExistFileAsInput() {
        assertThrows(XTriggerException.class, () ->
            initType(getNoExistFile()));
    }

    @Test
    void testInitNoGoodFileAsInput() {
        assertThrows(XTriggerException.class, () ->
            initType(getNotGoodTypeFile()));
    }

    @Test
    void testPollingNewFileNoExistAsInput() throws Exception {
        initType(getInitFile());
        assertThrows(XTriggerException.class, () ->
            type.isTriggeringBuild(getNoExistFile(), log));
    }

    @Test
    void testPollingNewFileNullReferenceAsInput() throws Exception {
        initType(getInitFile());
        assertThrows(NullPointerException.class, () ->
            type.isTriggeringBuild(null, log));
    }

    @Test
    void testPollingSameFile() throws Exception {
        initType(getInitFile());
        assertFalse(type.isTriggeringBuild(getInitFile(), log));
    }

    @Test
    void testPollingNewContentOneLeastFile() throws Exception {
        initType(getInitFile());
        assertTrue(type.isTriggeringBuild(getNewFileChangedContentOneFile(), log));
    }

    @Test
    void testPollingNewContentAddedFile() throws Exception {
        initType(getInitFile());
        assertTrue(type.isTriggeringBuild(getNewFileAddedFile(), log));
    }
}
