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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gregory Boissinot
 */
public class TextFileContentTest extends FileContentAbstractTest {

    FSTriggerContentFileType type;

    @Mock
    XTriggerLog log;

    @Override
    public FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        List<TextFileContentEntry> expressions = new ArrayList<TextFileContentEntry>();
        expressions.add(new TextFileContentEntry("\\w*ERROR\\s*\\w*"));
        type = new TextFileContent(expressions);
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
        File initFile = new File(this.getClass().getResource("LogTextFileContent/initLog.txt").toURI());
        initType(initFile);
        type.isTriggeringBuild(new File("noExist"), log);
    }


    @Test(expected = NullPointerException.class)
    public void testPollingNewFileNullReferenceAsInput() throws XTriggerException, URISyntaxException {
        File initFile = new File(this.getClass().getResource("LogTextFileContent/initLog.txt").toURI());
        initType(initFile);
        type.isTriggeringBuild(null, log);
    }

    @Test
    public void testPolling() throws URISyntaxException, XTriggerException {
        File initFile = new File(this.getClass().getResource("LogTextFileContent/initLog.txt").toURI());
        initType(initFile);
        Assert.assertFalse(type.isTriggeringBuild(initFile, log));
        File newFile = new File(this.getClass().getResource("LogTextFileContent/newLog.txt").toURI());
        Assert.assertTrue(type.isTriggeringBuild(newFile, log));
    }
}
