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
public class SimpleFileContentTest extends FileContentAbstractTest {

    protected FSTriggerContentFileType type;

    @Mock
    FSTriggerLog log;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        type = new SimpleFileContent();
    }

    public FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    @Test(expected = NullPointerException.class)
    public void testInitNullFileRefAsInput() throws URISyntaxException, FSTriggerException {
        initType(null);
    }

    @Test(expected = FSTriggerException.class)
    public void testInitNoExistFileAsInput() throws URISyntaxException, FSTriggerException {
        File initFile = new File("noExist.txt");
        initType(initFile);
    }

    @Test(expected = FSTriggerException.class)
    public void testPollingNewFileNoExistAsInput() throws FSTriggerException, URISyntaxException {
        File initFile = new File(this.getClass().getResource("SimpleFileContent/initFile.txt").toURI());
        initType(initFile);
        type.isTriggeringBuild(new File("noExist"), log);
    }


    @Test(expected = NullPointerException.class)
    public void testPollingNewFileNullReferenceAsInput() throws FSTriggerException, URISyntaxException {
        File initFile = new File(this.getClass().getResource("SimpleFileContent/initFile.txt").toURI());
        initType(initFile);
        type.isTriggeringBuild(null, log);
    }

    @Test
    public void testPolling() throws URISyntaxException, FSTriggerException {
        File initFile = new File(this.getClass().getResource("SimpleFileContent/initFile.txt").toURI());
        initType(initFile);
        Assert.assertFalse(type.isTriggeringBuild(initFile, log));
        File newFile = new File(this.getClass().getResource("SimpleFileContent/newFile.txt").toURI());
        Assert.assertTrue(type.isTriggeringBuild(newFile, log));
    }

}
