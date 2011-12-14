package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.net.URISyntaxException;

/**
 * @author Gregory Boissinot
 */
public abstract class AbstractArchiveFileContentTest extends FileContentAbstractTest {

    protected FSTriggerContentFileType type;

    @Mock
    XTriggerLog log;


    public FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    protected abstract File getInitFile() throws URISyntaxException;

    protected abstract File getNewFileAddedFile() throws URISyntaxException;

    protected abstract File getNewFileChangedContentOneFile() throws URISyntaxException;

    protected abstract File getNoExistFile();

    protected abstract File getNotGoodTypeFile() throws URISyntaxException;

    @Test(expected = NullPointerException.class)
    public void testInitNullFileRefAsInput() throws URISyntaxException, XTriggerException {
        initType(null);
    }

    @Test(expected = XTriggerException.class)
    public void testInitNoExistFileAsInput() throws URISyntaxException, XTriggerException {
        initType(getNoExistFile());
    }

    @Test(expected = XTriggerException.class)
    public void testInitNoGoodFileAsInput() throws URISyntaxException, XTriggerException {
        initType(getNotGoodTypeFile());
    }

    @Test(expected = XTriggerException.class)
    public void testPollingNewFileNoExistAsInput() throws XTriggerException, URISyntaxException {
        initType(getInitFile());
        type.isTriggeringBuild(getNoExistFile(), log);
    }

    @Test(expected = NullPointerException.class)
    public void testPollingNewFileNullReferenceAsInput() throws XTriggerException, URISyntaxException {
        initType(getInitFile());
        type.isTriggeringBuild(null, log);
    }

    @Test
    public void testPollingSameFile() throws URISyntaxException, XTriggerException {
        initType(getInitFile());
        Assert.assertFalse(type.isTriggeringBuild(getInitFile(), log));
    }

    @Test
    public void testPollingNewContentOneLeastFile() throws URISyntaxException, XTriggerException {
        initType(getInitFile());
        Assert.assertTrue(type.isTriggeringBuild(getNewFileChangedContentOneFile(), log));
    }

    @Test
    public void testPollingNewContentAddedFile() throws URISyntaxException, XTriggerException {
        initType(getInitFile());
        Assert.assertTrue(type.isTriggeringBuild(getNewFileAddedFile(), log));
    }
}
