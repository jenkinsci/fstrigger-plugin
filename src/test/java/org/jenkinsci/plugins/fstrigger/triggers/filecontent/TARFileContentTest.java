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
class TARFileContentTest extends AbstractArchiveFileContentTest {

    @BeforeEach
    void setUp() {
        type = new TarFileContent();
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
