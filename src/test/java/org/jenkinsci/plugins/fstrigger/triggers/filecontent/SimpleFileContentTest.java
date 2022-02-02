package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
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
public class SimpleFileContentTest extends FileContentAbstractTest {

    protected FSTriggerContentFileType type;

    @Mock
    XTriggerLog log;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        type = new SimpleFileContent();
    }

    public FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    @Test(expected = NullPointerException.class)
    public void testInitNullFileRefAsInput() throws URISyntaxException, XTriggerException {
        initType(null);
    }

    @Test(expected = XTriggerException.class)
    public void testInitNoExistFileAsInput() throws URISyntaxException, XTriggerException {
        File initFile = new File("noExist.txt");
        initType(initFile);
    }

    @Test(expected = XTriggerException.class)
    public void testPollingNewFileNoExistAsInput() throws XTriggerException, URISyntaxException {
        File initFile = new File(this.getClass().getResource("SimpleFileContent/initFile.txt").toURI());
        initType(initFile);
        type.isTriggeringBuild(new File("noExist"), log);
    }


    @Test(expected = NullPointerException.class)
    public void testPollingNewFileNullReferenceAsInput() throws XTriggerException, URISyntaxException {
        File initFile = new File(this.getClass().getResource("SimpleFileContent/initFile.txt").toURI());
        initType(initFile);
        type.isTriggeringBuild(null, log);
    }

    @Test
    public void testPolling() throws URISyntaxException, XTriggerException {
        File initFile = new File(this.getClass().getResource("SimpleFileContent/initFile.txt").toURI());
        initType(initFile);
        Assert.assertFalse(type.isTriggeringBuild(initFile, log));
        File newFile = new File(this.getClass().getResource("SimpleFileContent/newFile.txt").toURI());
        Assert.assertTrue(type.isTriggeringBuild(newFile, log));
    }

}
