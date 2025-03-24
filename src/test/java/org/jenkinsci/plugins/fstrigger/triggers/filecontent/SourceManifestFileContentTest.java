package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.net.URISyntaxException;

/**
 * @author Gregory Boissinot
 */
@ExtendWith(MockitoExtension.class)
class SourceManifestFileContentTest extends PropertiesFileContentTest {

    @Override
    protected File getInitFile() throws URISyntaxException {
        return new File(this.getClass().getResource("SourceManifestFileContent/initFileMANIFEST.txt").toURI());
    }

    @Override
    protected File getNewFile() throws URISyntaxException {
        return new File(this.getClass().getResource("SourceManifestFileContent/newFileMANIFEST.txt").toURI());
    }

    @Override
    protected File getNotGoodTypeFile() throws URISyntaxException {
        return new File(this.getClass().getResource("SourceManifestFileContent/noManifest.txt").toURI());
    }

    @Override
    protected Class<? extends PropertiesFileContent> getType() {
        return SourceManifestFileContent.class;
    }

}
