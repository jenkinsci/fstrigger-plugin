package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import java.io.File;
import java.net.URISyntaxException;

/**
 * @author Gregory Boissinot
 */
public class JarManifestFileContentTest extends PropertiesFileContentTest {

    @Override
    protected File getInitFile() throws URISyntaxException {
        return new File(this.getClass().getResource("JARManifestFileContent/initFile.jar").toURI());
    }

    @Override
    protected File getNewFile() throws URISyntaxException {
        return new File(this.getClass().getResource("JARManifestFileContent/newFile.jar").toURI());
    }

    @Override
    protected File getNotGoodTypeFile() throws URISyntaxException {
        return new File(this.getClass().getResource("JARManifestFileContent/jarWithoutManifest.jar").toURI());
    }

    @Override
    protected Class<? extends PropertiesFileContent> getType() {
        return JARManifestFileContent.class;
    }

}
