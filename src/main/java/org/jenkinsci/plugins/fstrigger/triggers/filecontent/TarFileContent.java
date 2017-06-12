package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Gregory Boissinot
 */
public class TarFileContent extends FSTriggerContentFileType {

    private static final long serialVersionUID = 1L;

    @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
    protected transient List<TarArchiveEntry> tarEntries = new ArrayList<>();

    private transient StringBuilder tarContent;

    @DataBoundConstructor
    @SuppressWarnings("unused")
    public TarFileContent() {
    }

    @Override
    public Object getMemoryInfo() {
        return tarEntries;
    }

    @Override
    public void setMemoryInfo(Object memoryInfo) {
        if (!(memoryInfo instanceof List)) {
            throw new IllegalArgumentException(String.format("The memory info %s object is not a List object.", memoryInfo));
        }
        this.tarEntries = (List) memoryInfo;
    }

    @Override
    protected void initForContent(File file) throws XTriggerException {
        try {
            tarEntries = getTarEntries(file);
            if (tarEntries.isEmpty()) {
                throw new XTriggerException("The tar file is empty.");
            }
            tarContent = new StringBuilder();
            fillTarContent(tarEntries, tarContent);

        } catch (IOException ioe) {
            throw new XTriggerException(ioe);
        }
    }


    private List<TarArchiveEntry> getTarEntries(File file) throws IOException {
        List<TarArchiveEntry> result = new ArrayList<>();
        try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(new FileInputStream(file))) {
            TarArchiveEntry tarEntry;
            while ((tarEntry = tarInputStream.getNextTarEntry()) != null) {
                result.add(tarEntry);
            }
        }
        return result;
    }

    @Override
    protected boolean isTriggeringBuildForContent(File file, XTriggerLog log) throws XTriggerException {

        List<TarArchiveEntry> newTarEntries;
        try {
            newTarEntries = getTarEntries(file);

            if (tarEntries.size() != newTarEntries.size()) {
                log.info("The size of the new tar file has changed.");
                log.info(displayTarEntries(newTarEntries));
                return true;
            }

            //Initiated to true for detecting when the two zip files has not the same number of elements
            boolean changed = true;
            Iterator<TarArchiveEntry> tarEntryIterator = tarEntries.iterator();
            Iterator<TarArchiveEntry> newTarEntryIterator = newTarEntries.iterator();
            while (tarEntryIterator.hasNext() && newTarEntryIterator.hasNext()) {

                TarArchiveEntry initTarEntry = tarEntryIterator.next();
                TarArchiveEntry newTarEntry = newTarEntryIterator.next();

                if (initTarEntry == null) {
                    return true;
                }

                if (newTarEntry == null) {
                    return true;
                }

                if ((initTarEntry.getName() != null) && !((initTarEntry.getName()).equals(newTarEntry.getName()))) {
                    log.info(String.format("The name of the '%s' entry has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
                    return true;
                }

                if (initTarEntry.getSize() != newTarEntry.getSize()) {
                    log.info(String.format("The size of the entry '%s' has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
                    return true;
                }

                if (initTarEntry.getModTime().getTime() != newTarEntry.getModTime().getTime()) {
                    log.info(String.format("The time of the '%s' entry has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
                    return true;
                }

                boolean bothIsDirectory = initTarEntry.isDirectory() ? newTarEntry.isDirectory() : false;
                if (bothIsDirectory) {
                    log.info(String.format("The type (file or directory) of the '%s' entry has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
                    return true;
                }

                if (initTarEntry.getMode() != newTarEntry.getMode()) {
                    log.info(String.format("The mode of the '%s' entry has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
                    return true;
                }

                if (initTarEntry.getGroupId() != newTarEntry.getGroupId()) {
                    log.info(String.format("The group id of the '%s' entry has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
                    return true;
                }

                if ((initTarEntry.getGroupName() != null) && !((initTarEntry.getGroupName()).equals(newTarEntry.getGroupName()))) {
                    log.info(String.format("The group name of the '%s' entry has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
                    return true;
                }

                if ((initTarEntry.getLinkName() != null) && !((initTarEntry.getLinkName()).equalsIgnoreCase(newTarEntry.getLinkName()))) {
                    log.info(String.format("The link name of the '%s' entry has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
                    return true;
                }

                if (initTarEntry.getUserId() != newTarEntry.getUserId()) {
                    log.info(String.format("The user id of the '%s' entry has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
                    return true;
                }

                if ((initTarEntry.getUserName() != null) && !((initTarEntry.getUserName()).equals(newTarEntry.getUserName()))) {
                    log.info(String.format("The user name of the '%s' entry has changed.", initTarEntry.getName()));
                    log.info(displayTarEntries(newTarEntries));
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

    private String displayTarEntries(List<TarArchiveEntry> newTarEntries) {

        StringBuilder sb = new StringBuilder();
        sb.append("The content of the tar file has changed.\n");
        sb.append("The old content is:\n");
        sb.append(tarContent);
        sb.append("The new content is:\n");
        fillTarContent(newTarEntries, sb);
        return sb.toString();
    }

    private void fillTarContent(List<TarArchiveEntry> newTarEntries, StringBuilder sb) {

        for (TarArchiveEntry tarEntry : newTarEntries) {
            Object[] elements = new Object[]{
                    "Name:" + tarEntry.getName(),
                    "Size:" + tarEntry.getSize(),
                    "Tme:" + tarEntry.getModTime(),
                    "isDirectory:" + tarEntry.isDirectory(),
                    "Mode:" + tarEntry.getMode(),
                    "UserId:" + tarEntry.getUserId(),
                    "UserName:" + tarEntry.getUserName(),
                    "GroupId:" + tarEntry.getGroupId(),
                    "GroupName:" + tarEntry.getGroupName(),
                    "LinkName:" + tarEntry.getLinkName()
            };
            sb.append(Arrays.toString(elements));
            sb.append("\n");
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static class TarFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<TarFileContent> {

        @Override
        public Class<? extends FSTriggerContentFileType> getType() {
            return TarFileContent.class;
        }

        @Override
        public String getDisplayName() {
            return "Monitor the contents of a Tar file";
        }

        @Override
        public String getLabel() {
            return "Tar File";
        }

    }

}
