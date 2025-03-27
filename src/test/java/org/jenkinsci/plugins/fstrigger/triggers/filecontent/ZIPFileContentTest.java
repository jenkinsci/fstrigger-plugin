package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.net.URISyntaxException;

/**
 * @author Gregory Boissinot
 */
@ExtendWith(MockitoExtension.class)
class ZIPFileContentTest extends AbstractArchiveFileContentTest {

    @BeforeEach
    void setUp() {
        type = new ZIPFileContent();
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
}
