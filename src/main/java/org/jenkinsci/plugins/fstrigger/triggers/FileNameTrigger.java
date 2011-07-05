package org.jenkinsci.plugins.fstrigger.triggers;

import antlr.ANTLRException;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerAction;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerFileNameCheckedModifiedService;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerFileNameGetFileService;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerLog;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
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

    /**
     * GUI fields
     */
    private String folderPath;
    private String fileName;
    private String strategy;

    private boolean inspectingContentFile;
    private boolean doNotCheckLastModificationDate;
    private FSTriggerContentFileType[] contentFileTypes;

    /**
     * Memory field for detection
     */
    private transient FilePath resolvedFile;

    private transient long resolvedFileLastModified;

    /**
     * Builds a FileNameTrigger object
     * The instance is build by the JSON binding with the newInstance in {@link FileNameTriggerDescriptor}
     */
    public FileNameTrigger(String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
    }

    /**
     * Getters and setters
     */
    @SuppressWarnings("unused")
    public String getFolderPath() {
        return folderPath;
    }

    @SuppressWarnings("unused")
    public String getFileName() {
        return fileName;
    }

    @SuppressWarnings("unused")
    public String getStrategy() {
        return strategy;
    }

    @SuppressWarnings("unused")
    public boolean isInspectingContentFile() {
        return inspectingContentFile;
    }

    @SuppressWarnings("unused")
    public FSTriggerContentFileType[] getContentFileTypes() {
        return contentFileTypes;
    }

    @SuppressWarnings("unused")
    public boolean isDoNotCheckLastModificationDate() {
        return doNotCheckLastModificationDate;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setInspectingContentFile(boolean inspectingContentFile) {
        this.inspectingContentFile = inspectingContentFile;
    }

    public void setContentFileTypes(FSTriggerContentFileType[] contentFileTypes) {
        this.contentFileTypes = contentFileTypes;
    }

    public void setDoNotCheckLastModificationDate(boolean doNotCheckLastModificationDate) {
        this.doNotCheckLastModificationDate = doNotCheckLastModificationDate;
    }

    private FileNameTriggerInfo buildInfoObject() {
        //Pre process GUI Fields
        String folderPathProceed = folderPath;
        if (folderPathProceed != null) {
            folderPathProceed = folderPathProceed.replaceAll("[\t\r\n]+", " ");
        }
        String fileNameProceed = fileName;
        if (fileNameProceed != null) {
            fileNameProceed = fileNameProceed.replaceAll("[\t\r\n]+", " ");
        }

        return new FileNameTriggerInfo(
                folderPathProceed,
                fileNameProceed,
                strategy,
                doNotCheckLastModificationDate
        );
    }


    /**
     * Computes and gets the file to poll
     *
     * @param startStage true if called in the starting stage
     * @return a FilePath object to the file, null if the object can't be determined or doesn't exist
     * @throws FSTriggerException
     */
    private FilePath computedFile(final FSTriggerLog log, boolean startStage) throws FSTriggerException {

        FilePath computedFile;

        AbstractProject p = (AbstractProject) job;
        Label label = p.getAssignedLabel();

        //Compute the file to the master
        if (label == null) {
            File file = new FSTriggerFileNameGetFileService(log, buildInfoObject()).call();
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
                                    return new FSTriggerFileNameGetFileService(log, buildInfoObject()).call();
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
            //Compute the file
            resolvedFile = computedFile(new FSTriggerLog(), true);
            resolvedFileLastModified = resolvedFile.lastModified();

            // Initialize the memory information if whe introspect the content
            initContentElementsIfNeed();

        } catch (FSTriggerException fse) {
            LOGGER.log(Level.SEVERE, "Error on trigger startup " + fse.getMessage());
            fse.printStackTrace();
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE, "Severe error on trigger startup " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void initContentElementsIfNeed() throws FSTriggerException {
        if (resolvedFile != null) {
            if (inspectingContentFile) {
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

    private void refreshMemoryInfo(FilePath newComputedFile) throws FSTriggerException {
        resolvedFile = newComputedFile;
        try {
            resolvedFileLastModified = resolvedFile.lastModified();
        } catch (IOException ioe) {
            throw new FSTriggerException(ioe);
        } catch (InterruptedException ie) {
            throw new FSTriggerException(ie);
        }
        initContentElementsIfNeed();
    }


    @Override
    protected boolean checkIfModified(final FSTriggerLog log) throws FSTriggerException {

        //Get the new resolved file
        FilePath newResolvedFile = computedFile(log, false);

        // Checks if slaves were offline at startup
        if (offlineSlavesForStartingStage) {
            //Refresh the memory field and reset offline info only if the new computed file was on active slaves (or no slaves)
            if (!offlineSlavesForCheckingStage) {
                log.info("The job is attached to a slave but the slave was started after the master and was offline during the check. Waiting for the next trigger schedule.");
                //Set the memory file to the new computed file
                refreshMemoryInfo(newResolvedFile);
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

        boolean changed = checkIfModifiedFile(log, newResolvedFile);
        refreshMemoryInfo(newResolvedFile);
        return changed;
    }

    private boolean checkIfModifiedFile(final FSTriggerLog log, FilePath newResolvedFile) throws FSTriggerException {

        // Do not trigger a build if the new computed file doesn't exist.
        if (newResolvedFile == null) {
            log.info("The computed file doesn't exist.");
            return false;
        }

        try {
            final String resolvedFilePath = (resolvedFile != null) ? resolvedFile.getRemote() : null;
            boolean changedFileName = newResolvedFile.act(new FilePath.FileCallable<Boolean>() {
                public Boolean invoke(File newResolvedFile, VirtualChannel channel) throws IOException, InterruptedException {
                    try {
                        FSTriggerFileNameCheckedModifiedService service = new FSTriggerFileNameCheckedModifiedService(log, buildInfoObject(), resolvedFilePath, new Long(resolvedFileLastModified), newResolvedFile);
                        return service.checkFileName();
                    } catch (FSTriggerException fse) {
                        throw new RuntimeException(fse);
                    }
                }
            });

            if (changedFileName) {
                return true;
            }

            if (inspectingContentFile) {
                log.info("Inspecting the contents of '" + newResolvedFile + "'");
                for (final FSTriggerContentFileType type : contentFileTypes) {
                    final Object memoryObject = type.getMemoryInfo();
                    boolean isTriggered = newResolvedFile.act(new FilePath.FileCallable<Boolean>() {
                        public Boolean invoke(File newResolvedFile, VirtualChannel channel) throws IOException, InterruptedException {
                            boolean isTriggered;
                            try {
                                FSTriggerFileNameCheckedModifiedService service = new FSTriggerFileNameCheckedModifiedService(log, buildInfoObject(), resolvedFilePath, new Long(resolvedFileLastModified), newResolvedFile);
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
        String[] subActionTitles = null;
        if (contentFileTypes != null) {
            subActionTitles = new String[contentFileTypes.length];
            for (int i = 0; i < contentFileTypes.length; i++) {
                Descriptor<FSTriggerContentFileType> descriptor = contentFileTypes[i].getDescriptor();
                if (descriptor instanceof FSTriggerContentFileTypeDescriptor) {
                    subActionTitles[i] = ((FSTriggerContentFileTypeDescriptor) descriptor).getLabel();
                }
            }
        }
        return Collections.singleton(new FSTriggerAction((AbstractProject) job, getLogFile(), this.getDescriptor().getLabel(), subActionTitles));
    }

    @Override
    public FileNameTriggerDescriptor getDescriptor() {
        return (FileNameTriggerDescriptor) Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    @SuppressWarnings("unused")
    public static class FileNameTriggerDescriptor extends TriggerDescriptor {

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
            return "[FSTrigger] - Monitor file";
        }

        public String getLabel() {
            return "Monitor file";
        }


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

        @Override
        public FileNameTrigger newInstance(StaplerRequest req, JSONObject formData) throws FormException {

            FileNameTrigger fileNameTrigger;
            try {
                fileNameTrigger = new FileNameTrigger(formData.getString("cronTabSpec"));
            } catch (ANTLRException ae) {
                throw new FormException(ae, "cronTabSpec");
            }

            fileNameTrigger.setFolderPath(formData.getString("folderPath"));
            fileNameTrigger.setFileName(formData.getString("fileName"));
            fileNameTrigger.setStrategy(formData.getString("strategy"));

            //InspectingContent info extracting
            Object inspectingFileContentObject = formData.get("inspectingContentFile");
            if (inspectingFileContentObject == null) {
                fileNameTrigger.setInspectingContentFile(false);
                //If we don't inspect the content, we inspect the last modification date
                fileNameTrigger.setDoNotCheckLastModificationDate(false);
                fileNameTrigger.setContentFileTypes(new FSTriggerContentFileType[0]);
            } else {
                JSONObject inspectingFileContentJSONObject = formData.getJSONObject("inspectingContentFile");
                fileNameTrigger.setInspectingContentFile(true);
                //Get the no checked last modified date
                fileNameTrigger.setDoNotCheckLastModificationDate(inspectingFileContentJSONObject.getBoolean("doNotCheckLastModificationDate"));
                //Content Types
                JSON contentFileTypesJsonElt;
                if (inspectingFileContentJSONObject.isArray()) {
                    contentFileTypesJsonElt = inspectingFileContentJSONObject.getJSONArray("contentFileTypes");
                } else {
                    contentFileTypesJsonElt = inspectingFileContentJSONObject.getJSONObject("contentFileTypes");
                }
                List<FSTriggerContentFileType> types = req.bindJSONToList(FSTriggerContentFileType.class, contentFileTypesJsonElt);
                fileNameTrigger.setContentFileTypes(types.toArray(new FSTriggerContentFileType[types.size()]));
            }

            return fileNameTrigger;
        }
    }

}