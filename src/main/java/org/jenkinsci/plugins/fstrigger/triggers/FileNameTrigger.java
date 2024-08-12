package org.jenkinsci.plugins.fstrigger.triggers;

import antlr.ANTLRException;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.io.output.NullPrintStream;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.plugins.xtriggerapi.AbstractTrigger;
import org.jenkinsci.plugins.xtriggerapi.XTriggerDescriptor;
import org.jenkinsci.plugins.xtriggerapi.XTriggerException;
import org.jenkinsci.plugins.xtriggerapi.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerAction;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerComputeFileService;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerFileNameCheckedModifiedService;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Gregory Boissinot
 */
public class FileNameTrigger extends AbstractTrigger {

    public static final String STRATEGY_IGNORE = "IGNORE";
    public static final String STRATEGY_LATEST = "LATEST";

    private static final Logger LOGGER = Logger.getLogger(FileNameTrigger.class.getName());
    private static final String CAUSE = "Triggered by a change to a file";

    private FileNameTriggerInfo[] fileInfo;

    public FileNameTrigger(String cronTabSpec, FileNameTriggerInfo[] fileInfo) throws ANTLRException {
        super(cronTabSpec);
        this.fileInfo = Arrays.copyOf(fileInfo, fileInfo.length);
    }

    @SuppressWarnings("unused")
    public FileNameTriggerInfo[] getFileInfo() {
        return Arrays.copyOf(fileInfo, fileInfo.length);
    }

    @Override
    protected File getLogFile() {
        if (job == null) return null;
        return new File(job.getRootDir(), "trigger-polling-files.log");
    }

    @Override
    protected Action[] getScheduledActions(Node node, XTriggerLog log) {
        return new Action[0];
    }

    @Override
    protected boolean requiresWorkspaceForPolling() {
        return false;
    }

    @Override
    public void start(Node pollingNode, BuildableItem project, boolean newInstance, XTriggerLog log) {

        try {
            FSTriggerComputeFileService service = new FSTriggerComputeFileService();
            for (FileNameTriggerInfo info : fileInfo) {
                StreamTaskListener nullTaskListener = new StreamTaskListener(NullPrintStream.INSTANCE, UTF_8);
                FilePath resolvedFile = service.computedFile(pollingNode, (Job) project, info, new XTriggerLog(nullTaskListener));
                if (resolvedFile != null) {
                    info.setResolvedFile(resolvedFile);
                    info.setLastModifications(resolvedFile.lastModified());
                    // Initialize the memory information if whe introspect the content
                    initContentElementsIfNeed(info);
                }
            }
        } catch (XTriggerException fse) {
            LOGGER.log(Level.SEVERE, "Error on trigger startup " + fse.getMessage());
            fse.printStackTrace();
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Severe error on trigger startup " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void initContentElementsIfNeed(FileNameTriggerInfo info) throws XTriggerException {
        if (job == null) return;
        FilePath resolvedFile = info.getResolvedFile();
        if (resolvedFile != null) {
            boolean inspectingContentFile = info.isInspectingContentFile();
            if (inspectingContentFile) {
                FSTriggerContentFileType[] contentFileTypes = info.getContentFileTypes();
                if (contentFileTypes != null) {
                    for (final FSTriggerContentFileType type : contentFileTypes) {
                        if (type != null) {
                            final String jobName = job.getName();
                            try {
                                Object memoryInfo = resolvedFile.act(new MemoryInfo(jobName, type));
                                type.setMemoryInfo(memoryInfo);
                            } catch (Throwable ie) {
                                throw new XTriggerException(ie);
                            }
                        }
                    }
                }
            }
        }
    }

    private static class MemoryInfo extends MasterToSlaveFileCallable<Object> {
        private final String jobName;
        private final FSTriggerContentFileType type;

        public MemoryInfo(String jobName, FSTriggerContentFileType type) {
            this.jobName = jobName;
            this.type = type;
        }
        @Override
        public Object invoke(File file, VirtualChannel virtualChannel) {
            try {
                type.initMemoryFields(jobName, file);
            } catch (XTriggerException fse) {
                throw new RuntimeException(fse);
            }
            return type.getMemoryInfo();
        }
    }

    private void refreshMemoryInfo(FileNameTriggerInfo info, FilePath newComputedFile) throws XTriggerException {
        try {
            if (newComputedFile != null && newComputedFile.exists()) {
                info.setResolvedFile(newComputedFile);
                info.setLastModifications(newComputedFile.lastModified());
                initContentElementsIfNeed(info);
            } else {
                info.setResolvedFile(null);
                info.setLastModifications(0L);
            }
        } catch (IOException | InterruptedException ioe) {
            throw new XTriggerException(ioe);
        }
    }

    @Override
    protected boolean checkIfModified(Node pollingNode, XTriggerLog log) throws XTriggerException {

        //1-- Compute new resolved files
        FilePath[] resolvedFiles = getNewResolvedFiles(pollingNode, log);

        //2-- Check if there are at least one change
        boolean changeResult = checkIfThereAreAtLeastOneChange(resolvedFiles, log);

        //3-- Refresh new resolved files
        refreshNewResolvedFiles(resolvedFiles);

        //4-- Return change status
        return changeResult;
    }


    private FilePath[] getNewResolvedFiles(Node pollingNode, XTriggerLog log) throws XTriggerException {
        FilePath[] resolvedFiles = new FilePath[fileInfo.length];
        for (int i = 0; i < fileInfo.length; i++) {
            FileNameTriggerInfo info = fileInfo[i];
            FilePath resolvedFile = new FSTriggerComputeFileService().computedFile(pollingNode, (AbstractProject) job, info, log);
            resolvedFiles[i] = resolvedFile;
        }
        return resolvedFiles;
    }

    private boolean checkIfThereAreAtLeastOneChange(FilePath[] resolvedFiles, XTriggerLog log) throws XTriggerException {
        for (int i = 0; i < resolvedFiles.length; i++) {
            if (offlineSlaveOnStartup) {
                log.info("No nodes were available at startup or at previous poll.");
                offlineSlaveOnStartup = false;
                return false;
            }

            FileNameTriggerInfo info = fileInfo[i];
            FilePath resolvedFile = resolvedFiles[i];
            boolean changed = checkIfModifiedFile(resolvedFile, info, log);
            if (changed) {
                return true;
            }
        }
        return false;
    }

    private void refreshNewResolvedFiles(FilePath[] resolvedFiles) throws XTriggerException {
        for (int i = 0; i < resolvedFiles.length; i++) {
            refreshMemoryInfo(fileInfo[i], resolvedFiles[i]);
        }
    }

    private boolean checkIfModifiedFile(FilePath newResolvedFile, final FileNameTriggerInfo info, final XTriggerLog log) throws XTriggerException {

        // Do not trigger a build if the new computed file doesn't exist.
        if (newResolvedFile == null) {
            log.info("The computed file doesn't exist.");
            return false;
        }

        try {
            FilePath resolvedFile = info.getResolvedFile();
            final Long lastModification = info.getLastModifications();
            final String resolvedFilePath = (resolvedFile != null) ? resolvedFile.getRemote() : null;
            boolean changedFileName = newResolvedFile.act(new MasterToSlaveFileCallable<Boolean>() {
                public Boolean invoke(File newResolvedFile, VirtualChannel channel) {
                    try {
                        FSTriggerFileNameCheckedModifiedService service = new FSTriggerFileNameCheckedModifiedService(log, info, resolvedFilePath, lastModification, newResolvedFile);
                        return service.checkFileName();
                    } catch (XTriggerException fse) {
                        throw new RuntimeException(fse);
                    }
                }
            });

            if (changedFileName) {
                return true;
            }

            boolean inspectingContentFile = info.isInspectingContentFile();
            FSTriggerContentFileType[] contentFileTypes = info.getContentFileTypes();
            if (inspectingContentFile) {
                log.info("Inspecting the contents of '" + newResolvedFile + "'");
                for (final FSTriggerContentFileType type : contentFileTypes) {
                    final Object memoryObject = type.getMemoryInfo();
                    if (memoryObject == null) {
                        log.info("No modifications according the given criteria.");
                        return false;
                    }
                    boolean isTriggered = newResolvedFile.act(new MasterToSlaveFileCallable<Boolean>() {
                        public Boolean invoke(File newResolvedFile, VirtualChannel channel) {
                            boolean isTriggered;
                            try {
                                FSTriggerFileNameCheckedModifiedService service = new FSTriggerFileNameCheckedModifiedService(log, info, resolvedFilePath, lastModification, newResolvedFile);
                                type.setMemoryInfo(memoryObject);
                                isTriggered = service.checkContentType(type);
                            } catch (XTriggerException fse) {
                                throw new RuntimeException(fse);
                            }
                            return isTriggered;
                        }
                    });
                    if (isTriggered) {
                        return true;
                    }
                }

                return false;
            }


        } catch (Throwable ie) {
            throw new XTriggerException(ie);
        }
        return false;
    }

    @Override
    protected String getName() {
        return "FSTrigger";
    }

    @Override
    public String getCause() {
        return CAUSE;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new FSTriggerFilesAction(this.getDescriptor().getDisplayName()));
    }

    @Override
    public FileNameTriggerDescriptor getDescriptor() {
        return (FileNameTriggerDescriptor) Jenkins.get().getDescriptorOrDie(getClass());
    }

    public final class FSTriggerFilesAction extends FSTriggerAction {

        private final transient String actionTitle;

        public FSTriggerFilesAction(String actionTitle) {
            this.actionTitle = actionTitle;
        }

        @Override
        public String getDisplayName() {
            return "FSTrigger Files Log";
        }

        @Override
        public String getUrlName() {
            return "triggerPollLogFiles";
        }

        @Override
        public String getIconFileName() {
            return "clipboard.gif";
        }

        @SuppressWarnings("unused")
        public String getActionTitle() {
            return actionTitle;
        }

        @SuppressWarnings("unused")
        public String getLog() throws IOException {
            return Util.loadFile(getLogFile());
        }

        @SuppressWarnings("unused")
        public void writeLogTo(XMLOutput out) throws IOException {
            long pos = new AnnotatedLargeText<>(getLogFile(), Charset.defaultCharset(), true, this).writeHtmlTo(0, out.asWriter());
            if (pos == 0) {
                LOGGER.warning("Failed to write log for FileNameTrigger");
            }
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static class FileNameTriggerDescriptor extends XTriggerDescriptor {

        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Executors.newSingleThreadExecutor());

        public ExecutorService getExecutor() {
            return queue.getExecutors();
        }

        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return org.jenkinsci.plugins.fstrigger.Messages.fstrigger_fileNameContent_displayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/fstrigger/help-monitorFile.html";
        }

        @SuppressWarnings("unchecked")
        public DescriptorExtensionList getListFSTriggerFileNameDescriptors() {
            return DescriptorExtensionList.createDescriptorList(Jenkins.get(), FSTriggerContentFileType.class);
        }

        /**
         * Performs syntax check.
         *
         * @param value the file pattern
         * @return the form validation object
         */
        public FormValidation doCheckFile(@QueryParameter String value) {

            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("You must provide a file to monitor");
            }

            value = value.replaceAll("[\t\r\n]+", " ");
            value = value.trim();
            if (value.length() < 2) {
                return FormValidation.error("You must provide a folder.");
            }

            return FormValidation.ok();
        }


        public FormValidation doCheckContentNature() {
            return FormValidation.ok();
        }

        @Override
        public FileNameTrigger newInstance(StaplerRequest req, JSONObject formData) throws FormException {

            FileNameTrigger fileNameTrigger;
            String cronTab = formData.getString("cronTabSpec");
            Object entryObject = formData.get("fileElement");

            List<FileNameTriggerInfo> entries = new ArrayList<FileNameTriggerInfo>();
            if (entryObject instanceof JSONObject) {
                entries.add(fillAndGetEntry(req, (JSONObject) entryObject));
            } else {
                JSONArray jsonArray = (JSONArray) entryObject;
                if (jsonArray != null) {
                    for (Object aJsonArray : jsonArray) {
                        entries.add(fillAndGetEntry(req, (JSONObject) aJsonArray));
                    }
                }
            }

            try {
                return new FileNameTrigger(cronTab, entries.toArray(new FileNameTriggerInfo[entries.size()]));

            } catch (ANTLRException ae) {
                throw new FormException(ae, "cronTabSpec");
            }

        }

        private FileNameTriggerInfo fillAndGetEntry(StaplerRequest req, JSONObject entryObject) {
            FileNameTriggerInfo info = new FileNameTriggerInfo();
            info.setFilePathPattern(Util.fixEmpty(entryObject.getString("filePathPattern")));
            info.setStrategy(entryObject.getString("strategy"));
            setInfoContentType(req, entryObject, info);
            return info;

        }

        private void setInfoContentType(StaplerRequest req, JSONObject entryObject, FileNameTriggerInfo info) {
            Object inspectingFileContentObject = entryObject.get("inspectingContentFile");
            if (inspectingFileContentObject == null) {
                unsetContentType(info);
            } else {
                JSONObject inspectingFileContentJSONObject = entryObject.getJSONObject("inspectingContentFile");
                if (isNoContentNatureSelected(inspectingFileContentJSONObject)) {
                    unsetContentType(info);
                } else {
                    info.setInspectingContentFile(true);
                    info.setDoNotCheckLastModificationDate(inspectingFileContentJSONObject.getBoolean("doNotCheckLastModificationDate"));
                    setContentNature(req, info, inspectingFileContentJSONObject);
                }
            }
        }

        private void unsetContentType(FileNameTriggerInfo info) {
            info.setInspectingContentFile(false);
            info.setDoNotCheckLastModificationDate(false);
            info.setContentFileTypes(new FSTriggerContentFileType[0]);
        }

        private boolean isNoContentNatureSelected(JSONObject inspectingFileContentJSONObject) {
            return inspectingFileContentJSONObject.size() == 1;
        }

        private void setContentNature(StaplerRequest req, FileNameTriggerInfo info, JSONObject inspectingFileContentJSONObject) {
            JSON contentFileTypesJsonElt;
            try {
                contentFileTypesJsonElt = inspectingFileContentJSONObject.getJSONArray("contentFileTypes");
            } catch (JSONException jsone) {
                contentFileTypesJsonElt = inspectingFileContentJSONObject.getJSONObject("contentFileTypes");
            }
            List<FSTriggerContentFileType> types = req.bindJSONToList(FSTriggerContentFileType.class, contentFileTypesJsonElt);
            info.setContentFileTypes(types.toArray(new FSTriggerContentFileType[types.size()]));
        }


    }

    /**
     * Backward compatibility
     */
    @SuppressWarnings("unused")
    @Deprecated
    private transient String folderPath;
    @SuppressWarnings("unused")
    @Deprecated
    private transient String fileName;
    @SuppressWarnings("unused")
    @Deprecated
    private transient String strategy;
    @SuppressWarnings("unused")
    @Deprecated
    private transient boolean inspectingContentFile;
    @SuppressWarnings("unused")
    @Deprecated
    private transient boolean doNotCheckLastModificationDate;
    @SuppressWarnings("unused")
    @Deprecated
    private transient FSTriggerContentFileType[] contentFileTypes;

    @SuppressWarnings({"unused", "deprecation"})
    @Deprecated
    public String getFolderPath() {
        return folderPath;
    }

    @SuppressWarnings({"unused", "deprecation"})
    @Deprecated
    public String getFileName() {
        return fileName;
    }

    @SuppressWarnings({"unused", "deprecation"})
    @Deprecated
    public String getStrategy() {
        return strategy;
    }

    @SuppressWarnings({"unused", "deprecation"})
    @Deprecated
    public boolean isInspectingContentFile() {
        return inspectingContentFile;
    }

    @SuppressWarnings({"unused", "deprecation"})
    @Deprecated
    public boolean isDoNotCheckLastModificationDate() {
        return doNotCheckLastModificationDate;
    }

    @SuppressWarnings({"unused", "deprecation"})
    @Deprecated
    public FSTriggerContentFileType[] getContentFileTypes() {
        return Arrays.copyOf(contentFileTypes, contentFileTypes.length);
    }

    @SuppressWarnings({"unused", "deprecation"})
    protected Object readResolve() throws ObjectStreamException {

        //Call Trigger readResolver to set tab field
        super.readResolve();

        //Previous version 0.11
        if (folderPath != null) {
            FileNameTriggerInfo info = new FileNameTriggerInfo();
            info.setFilePathPattern(folderPath + File.separatorChar + fileName);
            info.setStrategy(strategy);
            info.setInspectingContentFile(inspectingContentFile);
            info.setDoNotCheckLastModificationDate(doNotCheckLastModificationDate);
            info.setContentFileTypes(contentFileTypes);
            fileInfo = new FileNameTriggerInfo[]{info};
        }

        return this;
    }
    private static final long serialVersionUID = 1L;
}