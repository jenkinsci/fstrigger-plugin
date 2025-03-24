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
class JARFileContentTest extends ZIPFileContentTest {

    @BeforeEach
    void setUp() {
        type = new JARFileContent();
    }

    @Override
    protected File getInitFile() throws URISyntaxException {
        return new File(this.getClass().getResource("JARFileContent/initFile.jar").toURI());
    }

    @Override
    protected File getNewFileAddedFile() throws URISyntaxException {
        return new File(this.getClass().getResource("JARFileContent/newFileAddedFile.jar").toURI());
    }

    @Override
    protected File getNewFileChangedContentOneFile() throws URISyntaxException {
        return new File(this.getClass().getResource("JARFileContent/newFileChangedContentOneFile.jar").toURI());
    }
}
