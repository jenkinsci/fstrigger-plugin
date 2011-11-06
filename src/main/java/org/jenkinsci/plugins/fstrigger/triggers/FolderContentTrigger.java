package org.jenkinsci.plugins.fstrigger.triggers;

import antlr.ANTLRException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
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
        this.path = Util.fixEmpty(path);
        this.includes = Util.fixEmpty(includes);
        this.excludes = Util.fixEmpty(excludes);
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

    private void refreshMemoryInfo(boolean startStage, FSTriggerLog log) throws FSTriggerException {
        md5Map = getMd5Map(startStage, log);
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
    private Map<String, FileInfo> getMd5Map(boolean startingStage, FSTriggerLog log) throws FSTriggerException {

        if (path == null) {
            log.info("A folder path must be set.");
            return null;
        }

        Label label = ((AbstractProject) job).getAssignedLabel();
        if (label == null) {
            log.info("Polling on the master");
            return getFileInfoMaster(log);
        }

        log.info(String.format("Polling on all nodes for the label '%s' attached to the job.", label));

        if (isOfflineNodes()) {
            log.info("All slaves are offline.");
            if (startingStage) {
                offlineSlaveOnStartup = true;
            }
            return null;
        }

        return getFileInfoLabel(label, log);
    }

    private Map<String, FileInfo> getFileInfoMaster(FSTriggerLog log) throws FSTriggerException {

        String pathResolved = Util.replaceMacro(path, EnvVars.masterEnvVars);
        String includesResolved = Util.replaceMacro(includes, EnvVars.masterEnvVars);
        String excludesResolved = Util.replaceMacro(excludes, EnvVars.masterEnvVars);

        return getFileInfo(pathResolved, includesResolved, excludesResolved, log);
    }

    private Map<String, FileInfo> getFileInfoLabel(Label label, final FSTriggerLog log) throws FSTriggerException {

        Set<Node> nodes = label.getNodes();
        Node node;

        Map<String, FileInfo> result;
        for (Iterator<Node> it = nodes.iterator(); it.hasNext();) {
            node = it.next();
            FilePath nodePath = node.getRootPath();
            if (nodePath != null) {
                currentSlave = nodePath;
                try {
                    result = nodePath.act(new FilePath.FileCallable<Map<String, FileInfo>>() {
                        public Map<String, FileInfo> invoke(File node, VirtualChannel channel) throws IOException, InterruptedException {
                            try {
                                String pathResolved = Util.replaceMacro(path, EnvVars.masterEnvVars);
                                String includesResolved = Util.replaceMacro(includes, EnvVars.masterEnvVars);
                                String excludesResolved = Util.replaceMacro(excludes, EnvVars.masterEnvVars);
                                return getFileInfo(pathResolved, includesResolved, excludesResolved, log);
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
                    return result;
                }
            }
        }

        return null;
    }

    private Map<String, FileInfo> getFileInfo(String path, String includes, String excludes, FSTriggerLog log) throws FSTriggerException {

        log.info(String.format("\nTrying to monitor the folder '%s'", path));

        File folder = new File(path);
        if (!folder.exists()) {
            return null;
        }

        if (!folder.isDirectory()) {
            return null;
        }

        Map<String, FileInfo> result = new HashMap<String, FileInfo>();
        if (includes == null) {
            includes = "**/*.*";
        }
        FileSet fileSet = Util.createFileSet(new File(path), includes, excludes);
        for (Iterator it = fileSet.iterator(); it.hasNext();) {
            FileResource fileResource = (FileResource) it.next();
            processFileResource(log, result, fileResource);
        }
        return result;
    }

    private void processFileResource(FSTriggerLog log, Map<String, FileInfo> result, FileResource fileResource) throws FSTriggerException {
        if (!fileResource.isExists()) {
            log.info(String.format("\nThe file '%s' doesn't exist anymore ", fileResource.getFile().getPath()));
        } else {
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
    }

    @Override
    public String getCause() {
        return CAUSE;
    }

    @Override
    protected synchronized boolean checkIfModified(final FSTriggerLog log) throws FSTriggerException {

        //Get the current information
        Map<String, FileInfo> newMd5Map = getMd5Map(false, log);

        if (offlineSlaveOnStartup) {
            refreshMemoryInfo(newMd5Map);
            log.info("Slave(s) were offline at startup. Waiting for next schedule to check if there are modifications.");
            offlineSlaveOnStartup = false;
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
            log.info("The folder '" + new File(path) + "' does not contain any files matching the includes/excludes information.");
            return false;
        }

        //There was no any files for criterion and now there are some files
        if (this.md5Map == null) {
            log.info("The folder '" + new File(path) + "' contains new files matching the includes/excludes information.");
            return true;
        }

        //There are more or fewer files
        if (this.md5Map.size() != newMd5Map.size()) {
            log.info("The folder '" + new File(path) + "' content has changed.");
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
            refreshMemoryInfo(true, new FSTriggerLog());
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
        return Collections.singleton(new FSTriggerAction((AbstractProject) job, getLogFile(), this.getDescriptor().getLabel()));
    }

    @Override
    public FolderContentTriggerDescriptor getDescriptor() {
        return (FolderContentTriggerDescriptor) Hudson.getInstance().getDescriptorOrDie(getClass());
    }


    @Extension
    @SuppressWarnings("unused")
    public static class FolderContentTriggerDescriptor extends FSTriggerDescriptor {

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

        @Override
        public String getLabel() {
            return org.jenkinsci.plugins.fstrigger.Messages.fstrigger_folderContent_label();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/fstrigger/help-monitorFolder.html";
        }

    }
}
