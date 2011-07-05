package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.net.URISyntaxException;

/**
 * @author Gregory Boissinot
 */
public class JARFileContentTest extends ZIPFileContentTest {

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        type = new JARFileContent();
    }

    @Override
    public FSTriggerContentFileType getTypeInstance() {
        return type;
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

    @Override
    protected File getNotGoodTypeFile() throws URISyntaxException {
        return new File(this.getClass().getResource("JARFileContent/noJARFile.jar").toURI());
    }

}
