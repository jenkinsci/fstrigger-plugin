package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Extension;
import hudson.Util;
import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Gregory Boissinot
 */
public class ZIPFileContent extends FSTriggerContentFileType {

    protected transient List<ZipEntry> zipEntries = new ArrayList<>();

    private transient StringBuilder zipContent;

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public ZIPFileContent() {
    }

    @Override
    public Object getMemoryInfo() {
        return zipEntries;
    }

    @Override
    public void setMemoryInfo(Object memoryInfo) {
        if ((memoryInfo != null) && !(memoryInfo instanceof List)) {
            throw new IllegalArgumentException(String.format("The memory info %s object is not a List object.", memoryInfo));
        }
        if (memoryInfo != null) {
            this.zipEntries = (List) memoryInfo;
        }
    }

    private List<ZipEntry> getListZipEntries(Enumeration<? extends ZipEntry> entriesEnumeration) {
        List<ZipEntry> zipEntries = new ArrayList<>();
        while (entriesEnumeration.hasMoreElements()) {
            zipEntries.add(entriesEnumeration.nextElement());
        }
        return zipEntries;
    }

    @Override
    protected void initForContent(File file) throws XTriggerException {
        try (ZipFile zipFile = new ZipFile(file)) {
            zipContent = new StringBuilder();
            fillZipContent(zipFile.entries(), zipContent);
            zipEntries = getListZipEntries(zipFile.entries());
        } catch (IOException ioe) {
            throw new XTriggerException(ioe);
        }
    }

    @Override
    protected boolean isTriggeringBuildForContent(File file, XTriggerLog log) throws XTriggerException {

        List<ZipEntry> newZipEntries;
        try (ZipFile zipFile = new ZipFile(file)) {
            newZipEntries = getListZipEntries(zipFile.entries());

            //Initiated to true for detecting when the two zip files has not the same number of elements
            boolean changed = true;
            Iterator<ZipEntry> zipEntryIterator = zipEntries.iterator();
            Iterator<ZipEntry> newZipEntryIterator = newZipEntries.iterator();
            while (zipEntryIterator.hasNext() && newZipEntryIterator.hasNext()) {

                ZipEntry initZipEntry = zipEntryIterator.next();
                ZipEntry newZipEntry = newZipEntryIterator.next();

                if (initZipEntry == null) {
                    return true;
                }

                if (newZipEntry == null) {
                    return true;
                }

                if (!initZipEntry.getName().equals(newZipEntry.getName())) {
                    log.info(String.format("The name of the '%s' entry has changed.", initZipEntry.getName()));
                    log.info(displayZipEntries(zipFile.entries()));
                    return true;
                }

                if (initZipEntry.getSize() != newZipEntry.getSize()) {
                    log.info(String.format("The size of the entry '%s' has changed.", initZipEntry.getName()));
                    log.info(displayZipEntries(zipFile.entries()));
                    return true;
                }

                if (initZipEntry.getTime() != newZipEntry.getTime()) {
                    log.info(String.format("The time of the '%s' entry has changed.", initZipEntry.getName()));
                    log.info(displayZipEntries(zipFile.entries()));
                    return true;
                }

                boolean bothIsDirectory = initZipEntry.isDirectory() && newZipEntry.isDirectory();
                if (bothIsDirectory) {
                    log.info(String.format("The type (file or directory) of the '%s' entry has changed.", initZipEntry.getName()));
                    log.info(displayZipEntries(zipFile.entries()));
                    return true;
                }

                if (initZipEntry.getCrc() != newZipEntry.getCrc()) {
                    log.info(String.format("The crc of the '%s' entry has changed.", initZipEntry.getName()));
                    log.info(displayZipEntries(zipFile.entries()));
                    return true;
                }

                byte[] initBytes = initZipEntry.getExtra();
                byte[] newBytes = newZipEntry.getExtra();
                boolean changedMd5;
                if (initBytes == null && newBytes == null) {
                    changedMd5 = false;
                } else if (initBytes == null || newBytes == null) {
                    changedMd5 = true;
                } else {
                    String initMd5 = Util.getDigestOf(new String(initZipEntry.getExtra(), StandardCharsets.UTF_8));
                    String newMd5 = Util.getDigestOf(new String(newZipEntry.getExtra(), StandardCharsets.UTF_8));
                    changedMd5 = !initMd5.equals(newMd5);
                }
                if (changedMd5) {
                    log.info(String.format("The content the '%s' entry has changed.", initZipEntry.getName()));
                    log.info(displayZipEntries(zipFile.entries()));
                    return true;
                }

                changed = false;
            }

            //Returns true if a logical expression has changed
            return changed;

        } catch (IOException ioe) {
            throw new XTriggerException(ioe);
        }
    }


    private String displayZipEntries(Enumeration<? extends ZipEntry> newZipEntries) {

        StringBuilder sb = new StringBuilder();
        sb.append("The content of the zip file has changed.\n");
        sb.append("The old content is:\n");
        sb.append(zipContent);
        sb.append("The new content is:\n");
        fillZipContent(newZipEntries, sb);
        return sb.toString();
    }

    private void fillZipContent(Enumeration<? extends ZipEntry> entries, StringBuilder sb) {

        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            Object[] elements = new Object[]{
                    "Name:" + zipEntry.getName(),
                    "Size:" + zipEntry.getSize(),
                    "Tme:" + zipEntry.getTime(),
                    "isDirectory:" + zipEntry.isDirectory(),
                    "Crc:" + zipEntry.getCrc()};
            sb.append(Arrays.toString(elements));
            sb.append("\n");
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static class ZIPFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<ZIPFileContent> {

        @Override
        public Class<? extends FSTriggerContentFileType> getType() {
            return ZIPFileContent.class;
        }

        @Override
        public String getDisplayName() {
            return "Monitor the contents of a ZIP file";
        }

        @Override
        public String getLabel() {
            return "ZIP File";
        }
    }
    protected Object readResolve() {
        this.zipEntries = new ArrayList<>();
        return this;
    }
    private static final long serialVersionUID = 1L;
}
