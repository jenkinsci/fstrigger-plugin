package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import org.hamcrest.core.IsNull;
import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gregory Boissinot
 */
@ExtendWith(MockitoExtension.class)
class PropertiesFileContentTest extends FileContentAbstractTest {

    protected PropertiesFileContent type;

    @Mock
    protected XTriggerLog log;

    @Override
    protected FSTriggerContentFileType getTypeInstance() {
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
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException ne) {
            throw new RuntimeException(ne);
        }
    }

    protected boolean isTriggered(FSTriggerContentFileType type, File initFile, File newFile) throws XTriggerException {
        initType(initFile);
        return type.isTriggeringBuild(newFile, log);
    }

    @Test
    void test1InitParameters() {
        type = getType(null, true);
        assertThat(type.isAllKeys(), equalTo(true));
        assertThat(type.getKeys2Inspect(), IsNull.nullValue());
    }

    @Test
    void test2InitParameters() {
        type = getType("", true);
        assertThat(type.isAllKeys(), equalTo(true));
        assertThat(type.getKeys2Inspect(), IsNull.nullValue());
    }

    @Test
    void test3InitParameters() {
        String keys = "key1, key2";
        type = getType(keys, true);
        assertThat(type.isAllKeys(), equalTo(true));
        assertThat(type.getKeys2Inspect(), equalTo(keys));
    }

    @Test
    void test4InitParameters() {
        type = getType(null, false);
        assertThat(type.isAllKeys(), equalTo(false));
        assertThat(type.getKeys2Inspect(), IsNull.nullValue());
    }

    @Test
    void test5InitParameters() {
        type = getType("", false);
        assertThat(type.isAllKeys(), equalTo(false));
        assertThat(type.getKeys2Inspect(), IsNull.nullValue());
    }

    @Test
    void test6InitParameters() {
        String keys = "key1, key2";
        type = new PropertiesFileContent(keys, false);
        assertThat(type.isAllKeys(), equalTo(false));
        assertThat(type.getKeys2Inspect(), equalTo(keys));
    }

    @Test
    void test1NullFileAsInputInit() {
        type = getType(null, true);
        assertThrows(NullPointerException.class, () ->
            initType(null));
    }

    @Test
    void test2NullFileAsInputInit() {
        type = getType(null, false);
        assertThrows(NullPointerException.class, () ->
            initType(null));
    }

    @Test
    void test3NullFileAsInputInit() {
        type = getType("", true);
        assertThrows(NullPointerException.class, () ->
            initType(null));
    }

    @Test
    void test4NullRefFileAsInputInit() {
        type = getType(null, false);
        assertThrows(NullPointerException.class, () ->
            initType(null));
    }

    @Test
    void testFileNotExistAsInputInit() {
        type = getType(null, false);
        assertThrows(XTriggerException.class, () ->
            initType(getNoExistFile()));
    }

    @Test
    void testFileNotGoodFileTypeAsInputInit() {
        type = getType(null, false);
        assertThrows(XTriggerException.class, () ->
            initType(getNotGoodTypeFile()));
    }

    @Test
    void testPollingAllKeys() throws URISyntaxException, XTriggerException {
        type = getType(null, true);
        assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        assertTrue(isTriggered(type, getInitFile(), getNewFile()));
    }

    @Test
    void testPollingNoKeys() throws URISyntaxException, XTriggerException {
        type = getType(null, false);
        assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        assertFalse(isTriggered(type, getInitFile(), getNewFile()));
    }

    @Test
    void testPollingSomeModifiedKeys() throws URISyntaxException, XTriggerException {
        String keys = "key1, key2, key4";
        type = getType(keys, false);
        assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        assertTrue(isTriggered(type, getInitFile(), getNewFile()));
    }

    @Test
    void testPollingSomeNoModifiedKeys() throws URISyntaxException, XTriggerException {
        String keys = "key1,key4";
        type = getType(keys, false);
        assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        assertFalse(isTriggered(type, getInitFile(), getNewFile()));
    }

    @Test
    void testPollingSomeNoExistingKeys() throws URISyntaxException, XTriggerException {
        String keys = "key1NotExist,key4NotExist";
        type = getType(keys, false);
        assertFalse(isTriggered(type, getInitFile(), getInitFile()));
        assertFalse(isTriggered(type, getInitFile(), getNewFile()));
    }


}
