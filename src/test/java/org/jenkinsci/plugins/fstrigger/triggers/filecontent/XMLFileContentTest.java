package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gregory Boissinot
 */
public class XMLFileContentTest extends FileContentAbstractTest {

    protected FSTriggerContentFileType type;

    public FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        List<XMLFileContentEntry> expressions = new ArrayList<XMLFileContentEntry>();
        expressions.add(new XMLFileContentEntry("/employees/employee[1]/@id"));
        expressions.add(new XMLFileContentEntry("/employees/employee[2]/age"));
        type = new XMLFileContent(expressions);
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
        File initFile = new File(this.getClass().getResource("XMLFileContent/initFile.xml").toURI());
        initType(initFile);
        type.isTriggeringBuild(new File("noExist"), log);
    }

    @Test(expected = XTriggerException.class)
    public void testFileNotGoodFileTypeAsInputInit() throws XTriggerException, URISyntaxException {
        File initFile = new File(this.getClass().getResource("XMLFileContent/noXMLFile.xml").toURI());
        initType(initFile);
        type.isTriggeringBuild(new File("noExist"), log);
    }

    @Test(expected = NullPointerException.class)
    public void testPollingNewFileNullReferenceAsInput() throws XTriggerException, URISyntaxException {
        File initFile = new File(this.getClass().getResource("XMLFileContent/initFile.xml").toURI());
        initType(initFile);
        type.isTriggeringBuild(null, log);
    }

    @Test
    public void testPolling() throws URISyntaxException, XTriggerException {
        File initFile = new File(this.getClass().getResource("XMLFileContent/initFile.xml").toURI());
        initType(initFile);
        Assert.assertFalse(type.isTriggeringBuild(initFile, log));
        File newFile = new File(this.getClass().getResource("XMLFileContent/newFile.xml").toURI());
        Assert.assertTrue(type.isTriggeringBuild(newFile, log));
    }
}
