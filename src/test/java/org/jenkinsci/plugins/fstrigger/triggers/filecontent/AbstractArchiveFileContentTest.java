package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerLog;
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
    FSTriggerLog log;


    public FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    protected abstract File getInitFile() throws URISyntaxException;

    protected abstract File getNewFileAddedFile() throws URISyntaxException;

    protected abstract File getNewFileChangedContentOneFile() throws URISyntaxException;

    protected abstract File getNoExistFile();

    protected abstract File getNotGoodTypeFile() throws URISyntaxException;

    @Test(expected = NullPointerException.class)
    public void testInitNullFileRefAsInput() throws URISyntaxException, FSTriggerException {
        initType(null);
    }

    @Test(expected = FSTriggerException.class)
    public void testInitNoExistFileAsInput() throws URISyntaxException, FSTriggerException {
        initType(getNoExistFile());
    }

    @Test(expected = FSTriggerException.class)
    public void testInitNoGoodFileAsInput() throws URISyntaxException, FSTriggerException {
        initType(getNotGoodTypeFile());
    }

    @Test(expected = FSTriggerException.class)
    public void testPollingNewFileNoExistAsInput() throws FSTriggerException, URISyntaxException {
        initType(getInitFile());
        type.isTriggeringBuild(getNoExistFile(), log);
    }

    @Test(expected = NullPointerException.class)
    public void testPollingNewFileNullReferenceAsInput() throws FSTriggerException, URISyntaxException {
        initType(getInitFile());
        type.isTriggeringBuild(null, log);
    }

    @Test
    public void testPollingSameFile() throws URISyntaxException, FSTriggerException {
        initType(getInitFile());
        Assert.assertFalse(type.isTriggeringBuild(getInitFile(), log));
    }

    @Test
    public void testPollingNewContentOneLeastFile() throws URISyntaxException, FSTriggerException {
        initType(getInitFile());
        Assert.assertTrue(type.isTriggeringBuild(getNewFileChangedContentOneFile(), log));
    }

    @Test
    public void testPollingNewContentAddedFile() throws URISyntaxException, FSTriggerException {
        initType(getInitFile());
        Assert.assertTrue(type.isTriggeringBuild(getNewFileAddedFile(), log));
    }
}
