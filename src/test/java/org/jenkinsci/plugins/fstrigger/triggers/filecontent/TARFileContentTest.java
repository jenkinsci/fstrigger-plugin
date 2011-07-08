package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URISyntaxException;

/**
 * @author Gregory Boissinot
 */
public class TARFileContentTest extends AbstractArchiveFileContentTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        type = new TarFileContent();
    }

    public FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    protected File getInitFile() throws URISyntaxException {
        return new File(this.getClass().getResource("TARFileContent/initFile.tar").toURI());
    }

    protected File getNewFileAddedFile() throws URISyntaxException {
        return new File(this.getClass().getResource("TARFileContent/newFileAddedFile.tar").toURI());
    }

    protected File getNewFileChangedContentOneFile() throws URISyntaxException {
        return new File(this.getClass().getResource("TARFileContent/newFileChangedContentOneFile.tar").toURI());
    }

    protected File getNoExistFile() {
        return new File("noExist");
    }

    protected File getNotGoodTypeFile() throws URISyntaxException {
        return new File(this.getClass().getResource("TARFileContent/noTarFile.tar").toURI());
    }
}
