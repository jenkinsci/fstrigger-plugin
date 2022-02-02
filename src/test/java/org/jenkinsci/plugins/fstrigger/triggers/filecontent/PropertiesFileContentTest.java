package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.hamcrest.core.IsNull;
import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Gregory Boissinot
 */
public class PropertiesFileContentTest extends FileContentAbstractTest {

    PropertiesFileContent type;

    @Mock
    protected XTriggerLog log;

    @Override
    public FSTriggerContentFileType getTypeInstance() {
        return type;
    }

    protected File getInitFile() throws URISyntaxException {
        return new File(this.getClass().getResource("PropertiesFileContent/initFile.properties").toURI());
    }

    protected File getNewFile() throws URISyntaxException {
        return new File(this.getClass().getResource("PropertiesFileContent/newFile.properties").toURI());
    }

    protected File getNoExistFile() {
        return new File("noExist");
    }

    protected File getNotGoodTypeFile() throws URISyntaxException {
        return new File(this.getClass().getResource("PropertiesFileContent/noPropertiesFile.properties").toURI());
    }

    protected Class<? extends PropertiesFileContent> getType() {
        return PropertiesFileContent.class;
    }

    private PropertiesFileContent getType(String keys2Inspect, boolean allKeys) {
        return getType(getType(), keys2Inspect, allKeys);
    }

    private <T extends PropertiesFileContent> T getType(Class<T> typeClass, String keys2Inspect, boolean allKeys) {
        try {
            Constructor<? extends PropertiesFileContent> constructor = typeClass.getConstructor(String.class, boolean.class);
            return (T) constructor.newInstance(keys2Inspect, allKeys);
        } catch (NoSuchMethodException ne) {
            throw new RuntimeException(ne);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean isTriggered(FSTriggerContentFileType type, File initFile, File newFile) throws XTriggerException {
        initType(initFile);
        return type.isTriggeringBuild(newFile, log);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test1InitParameters() throws XTriggerException {
        type = getType(null, true);
        assertThat(type.isAllKeys(), equalTo(true));
        assertThat(type.getKeys2Inspect(), IsNull.<Object>nullValue());
    }

    @Test
    public void test2InitParameters() throws XTriggerException {
        type = getType("", true);
        assertThat(type.isAllKeys(), equalTo(true));
        assertThat(type.getKeys2Inspect(), IsNull.<Object>nullValue());
    }

    @Test
    public void test3InitParameters() throws XTriggerException {
        String keys = "key1, key2";
        type = getType(keys, true);
        assertThat(type.isAllKeys(), equalTo(true));
        assertThat(type.getKeys2Inspect(), equalTo(keys));
    }

    @Test
    public void test4InitParameters() throws XTriggerException {
        type = getType(null, false);
        assertThat(type.isAllKeys(), equalTo(false));
        assertThat(type.getKeys2Inspect(), IsNull.<Object>nullValue());
    }

    @Test
    public void test5InitParameters() throws XTriggerException {
        type = getType("", false);
        assertThat(type.isAllKeys(), equalTo(false));
        assertThat(type.getKeys2Inspect(), IsNull.<Object>nullValue());
    }

    @Test
    public void test6InitParameters() throws XTriggerException {
        String keys = "key1, key2";
        type = new PropertiesFileContent(keys, false);
        assertThat(type.isAllKeys(), equalTo(false));
        assertThat(type.getKeys2Inspect(), equalTo(keys));
    }

    @Test(expected = NullPointerException.class)
    public void test1NullFileAsInputInit() throws XTriggerException {
        type = getType(null, true);
        initType(null);
    }

    @Test(expected = NullPointerException.class)
    public void test2NullFileAsInputInit() throws XTriggerException {
        type = getType(null, false);
        initType(null);
    }

    @Test(expected = NullPointerException.class)
    public void test3NullFileAsInputInit() throws XTriggerException {
        type = getType("", true);
        initType(null);
    }

    @Test(expected = NullPointerException.class)
    public void test4NullRefFileAsInputInit() throws XTriggerException {
        type = getType(null, false);
        initType(null);
    }

    @Test(expected = XTriggerException.class)
    public void testFileNotExistAsInputInit() throws XTriggerException {
        type = getType(null, false);
        initType(getNoExistFile());
    }

    @Test(expected = XTriggerException.class)
    public void testFileNotGoodFileTypeAsInputInit() throws XTriggerException, URISyntaxException {
        type = getType(null, false);
        initType(getNotGoodTypeFile());
    }

    @Test
    public void testPollingAllKeys() throws URISyntaxException, XTriggerException {
        type = getType(null, true);
        Assert.assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        Assert.assertTrue(isTriggered(type, getInitFile(), getNewFile()));
    }

    @Test
    public void testPollingNoKeys() throws URISyntaxException, XTriggerException {
        type = getType(null, false);
        Assert.assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        Assert.assertFalse(isTriggered(type, getInitFile(), getNewFile()));
    }

    @Test
    public void testPollingSomeModifiedKeys() throws URISyntaxException, XTriggerException {
        String keys = "key1, key2, key4";
        type = getType(keys, false);
        Assert.assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        Assert.assertTrue(isTriggered(type, getInitFile(), getNewFile()));
    }

    @Test
    public void testPollingSomeNoModifiedKeys() throws URISyntaxException, XTriggerException {
        String keys = "key1,key4";
        type = getType(keys, false);
        Assert.assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        Assert.assertFalse(isTriggered(type, getInitFile(), getNewFile()));
    }

    @Test
    public void testPollingSomeNoExistingKeys() throws URISyntaxException, XTriggerException {
        String keys = "key1NotExist,key4NotExist";
        type = getType(keys, false);
        Assert.assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        Assert.assertFalse(isTriggered(type, getInitFile(), getNewFile()));
    }


}
