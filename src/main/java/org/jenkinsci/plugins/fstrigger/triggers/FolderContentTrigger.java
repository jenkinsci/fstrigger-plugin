package org.jenkinsci.plugins.fstrigger.triggers;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerAction;
import org.jenkinsci.plugins.fstrigger.service.FSTriggerLog;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Gregory Boissinot
 */
public class FolderContentTrigger extends AbstractTrigger implements Serializable {

    private static Logger LOGGER = Logger.getLogger(FolderContentTrigger.class.getName());

    private static final String CAUSE = "Triggered by a change to a folder";

    /**
     * GUI fields
     */
    private final String path;
    private final String includes;
    private final String excludes;


    /**
     * Memory fields
     */
    private transient String jobName;
    private transient Map<String, FileInfo> md5Map = new HashMap<String, FileInfo>();
    private transient FilePath currentSlave;

    @DataBoundConstructor
    public FolderContentTrigger(String cronTabSpec, String path, String includes, String excludes) throws ANTLRException {
        super(cronTabSpec);
        this.path = path;
        this.includes = includes;
        this.excludes = excludes;
    }

    @SuppressWarnings("unused")
    public String getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public String getIncludes() {
        return includes;
    }

    @SuppressWarnings("unused")
    public String getExcludes() {
        return excludes;
    }

    private void initInfo(String jobName) {
        this.jobName = jobName;
    }

    class FileInfo implements Serializable {

        private final String md5;

        private final long lastModified;

        public FileInfo(String md5, long lastModified) {
            this.md5 = md5;
            this.lastModified = lastModified;
        }

        public String getMd5() {
            return md5;
        }

        public long getLastModified() {
            return lastModified;
        }

    }

    private void refreshMemoryInfo(boolean startStage) throws FSTriggerException {
        md5Map = getMd5Map(startStage);
    }

    private void refreshMemoryInfo(Map<String, FileInfo> newMd5Map) throws FSTriggerException {
        md5Map = newMd5Map;
    }

    /**
     * Computes and gets the file information of the folder
     *
     * @param startingStage true if the caller is the starting stage
     * @return the file of the folder information
     * @throws FSTriggerException
     */
    private Map<String, FileInfo> getMd5Map(boolean startingStage) throws FSTriggerException {
        Map<String, FileInfo> result = null;
        AbstractProject p = (AbstractProject) job;
        Label label = p.getAssignedLabel();
        if (label == null) {
            result = getFileInfo(path, includes, excludes);
            //When there is no slave, disable offline information
            disableOffLineInfo(startingStage);

        } else {

            //Enables offline info
            enableOffLineInfo(startingStage);

            Set<Node> nodes = label.getNodes();
            Node node;
            for (Iterator<Node> it = nodes.iterator(); it.hasNext();) {
                node = it.next();
                FilePath nodePath = node.getRootPath();
                if (nodePath != null) {
                    currentSlave = nodePath;
                    try {
                        result = nodePath.act(new FilePath.FileCallable<Map>() {
                            public Map invoke(File node, VirtualChannel channel) throws IOException, InterruptedException {
                                try {
                                    return getFileInfo(path, includes, excludes);
                                } catch (FSTriggerException fse) {
                                    throw new RuntimeException(fse);
                                }
                            }
                        });
                    } catch (Throwable t) {
                        throw new FSTriggerException(t);
                    }

                    //We stop at first slave with file information
                    if (result != null) {
                        disableOffLineInfo(startingStage);
                        break;
                    }
                }
            }
        }

        return result;
    }


    private Map<String, FileInfo> getFileInfo(String path, String includes, String excludes) throws FSTriggerException {

        File folder = new File(path);
        if (!folder.exists()) {
            return null;
        }

        if (!folder.isDirectory()) {
            return null;
        }


        Map<String, FileInfo> result = new HashMap<String, FileInfo>();
        FileSet fileSet = Util.createFileSet(new File(path), includes, excludes);
        for (Iterator it = fileSet.iterator(); it.hasNext();) {
            FileResource fileResource = (FileResource) it.next();
            String currentMd5;
            try {
                FileInputStream fis = new FileInputStream(fileResource.getFile());
                currentMd5 = Util.getDigestOf(fis);
                fis.close();
            } catch (FileNotFoundException e) {
                throw new FSTriggerException(e);
            } catch (IOException e) {
                throw new FSTriggerException(e);
            }
            FileInfo fileInfo = new FileInfo(currentMd5, fileResource.getLastModified());
            result.put(fileResource.getFile().getAbsolutePath(), fileInfo);

        }
        return result;
    }

    @Override
    public String getCause() {
        return CAUSE;
    }

    @Override
    protected synchronized boolean checkIfModified(final FSTriggerLog log) throws FSTriggerException {

        //Get the current information
        Map<String, FileInfo> newMd5Map = getMd5Map(false);

        // Checks if slaves were offline at startup
        if (offlineSlavesForStartingStage) {
            //Refresh the memory field and reset offline info only if the new computed file was on active slaves (or no slaves)
            if (!offlineSlavesForCheckingStage) {
                log.info("The job is attached to a slave but the slave was started after the master and was offline during the check. Waiting for the next trigger schedule.");
                //Set the origin file to the new computed map
                refreshMemoryInfo(newMd5Map);
                //Disable slave information for starting stage and checking stage
                disableOffLineInfo(true);
            } else {
                log.info("The job is attached to a slave but the slave is offline. Waiting for the next trigger schedule.");
            }

            return false;
        }

        if (!offlineSlavesForStartingStage && offlineSlavesForCheckingStage) {
            log.info("The job is attached to a slave but the slave is offline. Waiting for the next trigger schedule.");
            //Reset offline slave information for compute stage
            disableOffLineInfo(false);
            return false;
        }

        boolean changed = checkIfModified(log, newMd5Map);
        refreshMemoryInfo(newMd5Map);
        return changed;
    }


    private boolean checkIfModified(final FSTriggerLog log, final Map<String, FileInfo> newMd5Map) throws FSTriggerException {

        //The folder doesn't exist anymore (or others), do not trigger the build
        if (newMd5Map == null) {
            log.info("The directory '" + new File(path) + "' doesn't exist.");
            return false;
        }

        //No new files matching criterion, we don't trigger the build
        if (newMd5Map.isEmpty()) {
            log.info("The folder '" + new File(path) + "' does not contain any files matching the criteria.");
            return false;
        }

        //There was no any files for criterion and now there are some files
        if (this.md5Map == null) {
            log.info("The folder '" + new File(path) + "' contains new files matching the criteria.");
            return true;
        }

        //There are more or fewer files
        if (this.md5Map.size() != newMd5Map.size()) {
            log.info("The folder '" + new File(path) + "' contents have changed.");
            return true;
        }

        //Check each file
        if (currentSlave == null) {
            return computeEachFile(log, this.md5Map, newMd5Map);
        } else {
            boolean isTriggering;
            try {
                final Map<String, FileInfo> originMd5Map = md5Map;
                isTriggering = currentSlave.act(new FilePath.FileCallable<Boolean>() {
                    public Boolean invoke(File slavePath, VirtualChannel channel) throws IOException, InterruptedException {
                        return computeEachFile(log, originMd5Map, newMd5Map);
                    }
                });
            } catch (IOException ioe) {
                throw new FSTriggerException(ioe);
            } catch (InterruptedException ie) {
                throw new FSTriggerException(ie);
            }
            return isTriggering;
        }
    }

    private boolean computeEachFile(FSTriggerLog log, Map<String, FileInfo> originMd5Map, Map<String, FileInfo> newMd5Map) {

        assert log != null;
        assert originMd5Map != null;
        assert newMd5Map != null;

        //Comparing each file
        for (Map.Entry<String, FileInfo> entry : originMd5Map.entrySet()) {

            String originFilePath = entry.getKey();
            FileInfo originFileInfo = entry.getValue();
            assert originFileInfo != null;

            //Checks if the newMd5Map contains the originFilePath
            FileInfo newFileInfo = newMd5Map.get(originFilePath);
            if (newFileInfo == null) {
                log.info("The path '" + originFilePath + "' doesn't exist anymore.");
                return true;
            }

            //Checks it the content file from the new compute has been modified
            if (!originFileInfo.getMd5().equals(newFileInfo.getMd5())) {
                log.info("The contents of '" + originFilePath + "' have changed.");
                return true;
            }

            //Checks if the file from the new compute has been modified
            if (originFileInfo.getLastModified() != newFileInfo.getLastModified()) {
                log.info("The modification date of '" + originFilePath + "' has changed.");
                return true;
            }
        }

        return false;

    }

    @Override
    public void start(BuildableItem project, boolean newInstance) {
        super.start(project, newInstance);
        /**
         * Records a md5 for each file of the folder that matches includes and excludes pattern
         */
        try {
            initInfo(project.getName());
            refreshMemoryInfo(true);
        } catch (FSTriggerException fse) {
            //Log the exception
            LOGGER.log(Level.SEVERE, "Error on trigger startup " + fse.getMessage());
            fse.printStackTrace();
        }

    }


    @Override
    public void run() {
        FolderContentTriggerDescriptor descriptor = getDescriptor();
        ExecutorService executorService = descriptor.getExecutor();
        StreamTaskListener listener;
        try {
            listener = new StreamTaskListener(getLogFile());
            FSTriggerLog log = new FSTriggerLog(listener);
            Runner runner = new Runner(log);
            executorService.execute(runner);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during the trigger execution " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new FSTriggerAction((AbstractProject) job, getLogFile(), this.getDescriptor().getLabel(), null));
    }

    @Override
    public FolderContentTriggerDescriptor getDescriptor() {
        return (FolderContentTriggerDescriptor) Hudson.getInstance().getDescriptorOrDie(getClass());
    }


    @Extension
    @SuppressWarnings("unused")
    public static class FolderContentTriggerDescriptor extends TriggerDescriptor {

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
            return org.jenkinsci.plugins.fstrigger.Messages.fstrigger_folderContent_displayName();
        }

        public String getLabel() {
            return org.jenkinsci.plugins.fstrigger.Messages.fstrigger_folderContent_label();
        }

        public String getHelpFile() {
            return "/plugin/fstrigger/help-monitorFolder.html";
        }

    }
}
