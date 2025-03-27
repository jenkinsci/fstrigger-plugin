package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gregory Boissinot
 */
@ExtendWith(MockitoExtension.class)
class XMLFileContentTest extends FileContentAbstractTest {

    protected FSTriggerContentFileType type;

    protected FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    @BeforeEach
    void setUp() {
        List<XMLFileContentEntry> expressions = new ArrayList<>();
        expressions.add(new XMLFileContentEntry("/employees/employee[1]/@id"));
        expressions.add(new XMLFileContentEntry("/employees/employee[2]/age"));
        type = new XMLFileContent(expressions);
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
    void testPollingNewFileNoExistAsInput() throws Exception{
        File initFile = new File(this.getClass().getResource("XMLFileContent/initFile.xml").toURI());
        initType(initFile);
        assertThrows(XTriggerException.class, () ->
            type.isTriggeringBuild(new File("noExist"), log));
    }

    @Test
    void testFileNotGoodFileTypeAsInputInit() throws Exception {
        File initFile = new File(this.getClass().getResource("XMLFileContent/noXMLFile.xml").toURI());
        assertThrows(XTriggerException.class, () ->
                initType(initFile));
    }

    @Test
    void testPollingNewFileNullReferenceAsInput() throws Exception {
        File initFile = new File(this.getClass().getResource("XMLFileContent/initFile.xml").toURI());
        initType(initFile);
        assertThrows(NullPointerException.class, () ->
            type.isTriggeringBuild(null, log));
    }

    @Test
    void testPolling() throws URISyntaxException, XTriggerException {
        File initFile = new File(this.getClass().getResource("XMLFileContent/initFile.xml").toURI());
        initType(initFile);
        assertFalse(type.isTriggeringBuild(initFile, log));
        File newFile = new File(this.getClass().getResource("XMLFileContent/newFile.xml").toURI());
        assertTrue(type.isTriggeringBuild(newFile, log));
    }
}
