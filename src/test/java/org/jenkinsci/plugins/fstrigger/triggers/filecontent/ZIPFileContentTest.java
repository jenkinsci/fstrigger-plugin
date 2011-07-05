package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerLog;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URISyntaxException;

/**
 * @author Gregory Boissinot
 */
public class ZIPFileContentTest extends FileContentAbstractTest {

    protected FSTriggerContentFileType type;

    @Mock
    FSTriggerLog log;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        type = new ZIPFileContent();
    }

    public FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    protected File getInitFile() throws URISyntaxException {
        return new File(this.getClass().getResource("ZIPFileContent/initFile.zip").toURI());
    }

    protected File getNewFileAddedFile() throws URISyntaxException {
        return new File(this.getClass().getResource("ZIPFileContent/newFileAddedFile.zip").toURI());
    }

    protected File getNewFileChangedContentOneFile() throws URISyntaxException {
        return new File(this.getClass().getResource("ZIPFileContent/newFileChangedContentOneFile.zip").toURI());
    }

    protected File getNoExistFile() {
        return new File("noExist");
    }

    protected File getNotGoodTypeFile() throws URISyntaxException {
        return new File(this.getClass().getResource("ZIPFileContent/noZIPFile.zip").toURI());
    }

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
