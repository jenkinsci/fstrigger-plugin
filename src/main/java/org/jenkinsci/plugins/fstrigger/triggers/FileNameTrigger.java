package org.jenkinsci.plugins.fstrigger.triggers;

import antlr.ANTLRException;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.util.FormValidation;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerAction;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerFileNameCheckedModifiedService;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerFileNameGetFileService;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerLog;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Gregory Boissinot
 */
public class FileNameTrigger extends AbstractTrigger {

    public static final String STRATEGY_IGNORE = "IGNORE";
    public static final String STRATEGY_LATEST = "LATEST";

    private static Logger LOGGER = Logger.getLogger(FileNameTrigger.class.getName());
    private static final String CAUSE = "Triggered by a change to a file";

    private FileNameTriggerInfo[] fileInfo = new FileNameTriggerInfo[0];

//    private FileFoundInfo buildInfoObject(FileNameTriggerInfo info) {
//        return new FileFoundInfo(
//                info.getFileName(), info.getStrategy(), info.isDoNotCheckLastModificationDate()
//        );
//    }

    public FileNameTrigger(String cronTabSpec, FileNameTriggerInfo[] fileInfo) throws ANTLRException {
        super(cronTabSpec);
        this.fileInfo = fileInfo;
    }

    @SuppressWarnings("unused")
    public FileNameTriggerInfo[] getFileInfo() {
        return fileInfo;
    }

    /**
     * Computes and gets the file to poll
     *
     * @param startStage true if called in the starting stage
     * @return a FilePath object to the file, null if the object can't be determined or doesn't exist
     * @throws FSTriggerException
     */
    private FilePath computedFile(final FileNameTriggerInfo fileInfo, boolean startStage, final FSTriggerLog log) throws FSTriggerException {

        FilePath computedFile;

        AbstractProject p = (AbstractProject) job;
        Label label = p.getAssignedLabel();

        //Compute the file to the master
        if (label == null) {
            File file = new FSTriggerFileNameGetFileService(fileInfo, log).call();
            if (file != null) {
                computedFile = new FilePath(Hudson.MasterComputer.localChannel, file.getPath());
            } else {
                computedFile = null;
            }

            //Disable offline information of slaves
            disableOffLineInfo(startStage);

        }
        // Monitor the fill from a job slave
        else {
            Set<Node> nodes = label.getNodes();
            File file = null;
            Node node = null;

            //Enable offline info (and disable after if needed)
            enableOffLineInfo(startStage);

            //Go through each slave
            for (Iterator<Node> it = nodes.iterator(); it.hasNext();) {
                node = it.next();
                try {
                    FilePath nodePath = node.getRootPath();
                    // Is null if the slave is offline
                    if (nodePath != null) {
                        file = nodePath.act(new FilePath.FileCallable<File>() {
                            public File invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                                try {
                                    return new FSTriggerFileNameGetFileService(fileInfo, log).call();
                                } catch (FSTriggerException fse) {
                                    throw new RuntimeException(fse);
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    throw new FSTriggerException(e);
                } catch (InterruptedException e) {
                    throw new FSTriggerException(e);
                }

                //We stop when the file exists on the slave
                if (file != null) {
                    disableOffLineInfo(startStage);
                    break;
                }
            }

            if (file != null) {
                computedFile = new FilePath(node.getChannel(), file.getPath());
            } else {
                computedFile = null;
            }
        }

        return computedFile;
    }

    @Override
    public void start(BuildableItem project, boolean newInstance) {

        super.start(project, newInstance);
        try {
            for (FileNameTriggerInfo info : fileInfo) {
                FilePath resolvedFile = computedFile(info, true, new FSTriggerLog());
                if (resolvedFile != null) {
                    info.setResolvedFile(resolvedFile);
                    info.setLastModifications(resolvedFile.lastModified());
                }

                // Initialize the memory information if whe introspect the content
                initContentElementsIfNeed(info);
            }

        } catch (FSTriggerException fse) {
            LOGGER.log(Level.SEVERE, "Error on trigger startup " + fse.getMessage());
            fse.printStackTrace();
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Severe error on trigger startup " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void initContentElementsIfNeed(FileNameTriggerInfo info) throws FSTriggerException {

        FilePath resolvedFile = info.getResolvedFile();
        if (resolvedFile != null) {
            boolean inspectingContentFile = info.isInspectingContentFile();
            if (inspectingContentFile) {
                FSTriggerContentFileType[] contentFileTypes = info.getContentFileTypes();
                if (contentFileTypes != null) {
                    for (int i = 0; i < contentFileTypes.length; i++) {
                        final FSTriggerContentFileType type = contentFileTypes[i];
                        final String jobName = job.getName();
                        if (type != null) {
                            try {
                                Object memoryInfo = resolvedFile.act(new FilePath.FileCallable<Object>() {
                                    public Object invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                                        try {
                                            type.initMemoryFields(jobName, f);
                                        } catch (FSTriggerException fse) {
                                            throw new RuntimeException(fse);
                                        }
                                        return type.getMemoryInfo();
                                    }
                                });
                                contentFileTypes[i].setMemoryInfo(memoryInfo);
                            } catch (IOException ioe) {
                                throw new FSTriggerException(ioe);
                            } catch (InterruptedException ie) {
                                throw new FSTriggerException(ie);
                            } catch (Throwable t) {
                                throw new FSTriggerException(t);
                            }
                        }
                    }
                }
            }
        }

    }

    private void refreshMemoryInfo(FileNameTriggerInfo info, FilePath newComputedFile) throws FSTriggerException {
        FilePath resolvedFile = newComputedFile;
        try {
            if (resolvedFile != null) {
                info.setLastModifications(resolvedFile.lastModified());
            } else {
                info.setLastModifications(0l);
            }
        } catch (IOException ioe) {
            throw new FSTriggerException(ioe);
        } catch (InterruptedException ie) {
            throw new FSTriggerException(ie);
        }
        initContentElementsIfNeed(info);
    }


    @Override
    protected boolean checkIfModified(final FSTriggerLog log) throws FSTriggerException {

        for (FileNameTriggerInfo info : fileInfo) {

            //Get the new resolved file
            FilePath newResolvedFile = computedFile(info, false, log);

            // Checks if slaves were offline at startup
            if (offlineSlavesForStartingStage) {
                //Refresh the memory field and reset offline info only if the new computed file was on active slaves (or no slaves)
                if (!offlineSlavesForCheckingStage) {
                    log.info("The job is attached to a slave but the slave was started after the master and was offline during the check. Waiting for the next trigger schedule.");
                    //Set the memory file to the new computed file
                    refreshMemoryInfo(info, newResolvedFile);
                    //Disable slave information for starting stage and checking stage
                    disableOffLineInfo(true);
                } else {
                    log.info("The job is attached to a slave but the slave is offline. Waiting for the next trigger schedule.");
                }

                return false;
            }

            // Check if the new file was computed with slaves on offline (no startup)
            if (!offlineSlavesForStartingStage && offlineSlavesForCheckingStage) {
                log.info("The job is attached to a slave but the slave is offline. Waiting for the next trigger schedule.");
                //Reset offline slave information for compute stage
                disableOffLineInfo(false);
                return false;
            }

            boolean changed = checkIfModifiedFile(newResolvedFile, info, log);
            refreshMemoryInfo(info, newResolvedFile);
            if (changed) {
                return true;
            }
        }
        return false;
    }

    private boolean checkIfModifiedFile(FilePath newResolvedFile, final FileNameTriggerInfo info, final FSTriggerLog log) throws FSTriggerException {

        // Do not trigger a build if the new computed file doesn't exist.
        if (newResolvedFile == null) {
            log.info("The computed file doesn't exist.");
            return false;
        }

        try {
            FilePath resolvedFile = info.getResolvedFile();
            final Long lastModification = info.getLastModifications();
            final String resolvedFilePath = (resolvedFile != null) ? resolvedFile.getRemote() : null;
            boolean changedFileName = newResolvedFile.act(new FilePath.FileCallable<Boolean>() {
                public Boolean invoke(File newResolvedFile, VirtualChannel channel) throws IOException, InterruptedException {
                    try {
                        FSTriggerFileNameCheckedModifiedService service = new FSTriggerFileNameCheckedModifiedService(log, info, resolvedFilePath, lastModification, newResolvedFile);
                        return service.checkFileName();
                    } catch (FSTriggerException fse) {
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
                    boolean isTriggered = newResolvedFile.act(new FilePath.FileCallable<Boolean>() {
                        public Boolean invoke(File newResolvedFile, VirtualChannel channel) throws IOException, InterruptedException {
                            boolean isTriggered;
                            try {
                                FSTriggerFileNameCheckedModifiedService service = new FSTriggerFileNameCheckedModifiedService(log, info, resolvedFilePath, lastModification, newResolvedFile);
                                type.setMemoryInfo(memoryObject);
                                isTriggered = service.checkContentType(type);
                            } catch (FSTriggerException fse) {
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


        } catch (IOException ioe) {
            throw new FSTriggerException(ioe);
        } catch (InterruptedException ie) {
            throw new FSTriggerException(ie);
        } catch (Throwable e) {
            throw new FSTriggerException(e);
        }
        return false;
    }

    @Override
    public void run() {

        FileNameTriggerDescriptor descriptor = getDescriptor();
        ExecutorService executorService = descriptor.getExecutor();
        StreamTaskListener listener;
        try {
            listener = new StreamTaskListener(getLogFile());
            FSTriggerLog log = new FSTriggerLog(listener);
            Runner runner = new Runner(log);
            executorService.execute(runner);

        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Severe Error during the trigger execution " + t.getMessage());
            t.printStackTrace();
        }
    }

    @Override
    public String getCause() {
        return CAUSE;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
//        String[] subActionTitles = null;
//        for (FileNameTriggerInfo info : fileInfo) {
//            FSTriggerContentFileType[] contentFileTypes = info.getContentFileTypes();
//            if (contentFileTypes != null) {
//                subActionTitles = new String[contentFileTypes.length];
//                for (int i = 0; i < contentFileTypes.length; i++) {
//                    FSTriggerContentFileType fsTriggerContentFileType = contentFileTypes[i];
//                    if (fsTriggerContentFileType != null) {
//                        Descriptor<FSTriggerContentFileType> descriptor = fsTriggerContentFileType.getDescriptor();
//                        if (descriptor instanceof FSTriggerContentFileTypeDescriptor) {
//                            subActionTitles[i] = ((FSTriggerContentFileTypeDescriptor) descriptor).getLabel();
//                        }
//                    }
//                }
//            }
//        }
        //TODO subActions
        return Collections.singleton(new FSTriggerAction((AbstractProject) job, getLogFile(), this.getDescriptor().getLabel()));
    }

    @Override
    public FileNameTriggerDescriptor getDescriptor() {
        return (FileNameTriggerDescriptor) Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    @SuppressWarnings("unused")
    public static class FileNameTriggerDescriptor extends FSTriggerDescriptor {

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
        public String getLabel() {
            return org.jenkinsci.plugins.fstrigger.Messages.fstrigger_fileNameContent_label();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/fstrigger/help-monitorFile.html";
        }

        @SuppressWarnings("unchecked")
        public DescriptorExtensionList getListFSTriggerFileNameDescriptors() {
            return DescriptorExtensionList.createDescriptorList(Hudson.getInstance(), FSTriggerContentFileType.class);
        }

        /**
         * Performs syntax check.
         *
         * @param folderPath the given user folder field
         * @return the form validation object
         */
        public FormValidation doCheckFolderPath(@QueryParameter String folderPath) {
            File folderFile = new File(folderPath);
            if (!folderFile.exists()) {
                String msg = String.format("The %s folder must exist.", folderPath);
                return FormValidation.error(msg);
            }
            return FormValidation.ok();
        }

        private FileNameTriggerInfo fillAndGetEntry(StaplerRequest req, JSONObject entryObject) {

            FileNameTriggerInfo info = new FileNameTriggerInfo();
            info.setFileName(entryObject.getString("fileName"));
            info.setStrategy(entryObject.getString("strategy"));

            //InspectingContent info extracting
            Object inspectingFileContentObject = entryObject.get("inspectingContentFile");
            if (inspectingFileContentObject == null) {
                info.setInspectingContentFile(false);
                //If we don't inspect the content, we inspect the last modification date
                info.setDoNotCheckLastModificationDate(false);
                info.setContentFileTypes(new FSTriggerContentFileType[0]);
            } else {
                JSONObject inspectingFileContentJSONObject = entryObject.getJSONObject("inspectingContentFile");
                info.setInspectingContentFile(true);
                //Get the no checked last modified date
                info.setDoNotCheckLastModificationDate(inspectingFileContentJSONObject.getBoolean("doNotCheckLastModificationDate"));
                //Content Types
                JSON contentFileTypesJsonElt;
                try {
                    contentFileTypesJsonElt = inspectingFileContentJSONObject.getJSONArray("contentFileTypes");
                } catch (JSONException jsone) {
                    contentFileTypesJsonElt = inspectingFileContentJSONObject.getJSONObject("contentFileTypes");
                }
                List<FSTriggerContentFileType> types = req.bindJSONToList(FSTriggerContentFileType.class, contentFileTypesJsonElt);
                info.setContentFileTypes(types.toArray(new FSTriggerContentFileType[types.size()]));
            }

            return info;

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
                    Iterator it = jsonArray.iterator();
                    while (it.hasNext()) {
                        entries.add(fillAndGetEntry(req, (JSONObject) it.next()));
                    }
                }
            }

            try {
                return new FileNameTrigger(cronTab, entries.toArray(new FileNameTriggerInfo[entries.size()]));

            } catch (ANTLRException ae) {
                throw new FormException(ae, "cronTabSpec");
            }

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
        return contentFileTypes;
    }

    @SuppressWarnings({"unused", "deprecation"})
    protected Object readResolve() throws ObjectStreamException {
        super.readResolve();
        if (folderPath != null) {
            FileNameTriggerInfo info = new FileNameTriggerInfo();
            info.setFileName(folderPath + File.separatorChar + fileName);
            info.setStrategy(strategy);
            info.setInspectingContentFile(inspectingContentFile);
            info.setDoNotCheckLastModificationDate(doNotCheckLastModificationDate);
            info.setContentFileTypes(contentFileTypes);
            fileInfo = new FileNameTriggerInfo[]{info};
        }
        return this;
    }

}