package org.jenkinsci.plugins.fstrigger.triggers;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.lib.xtrigger.service.XTriggerEnvVarsResolver;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerFolderAction;
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
public class FolderContentTrigger extends AbstractFSTrigger {

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

    @Override
    protected File getLogFile() {
        return new File(job.getRootDir(), "trigger-polling-folder.log");
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

    private void refreshMemoryInfo(boolean startStage, XTriggerLog log) throws XTriggerException {
        Map<String, String> envVars = new XTriggerEnvVarsResolver().getEnvVars((AbstractProject) job, Hudson.getInstance(), log);
        String pathResolved = Util.replaceMacro(path, envVars);
        String includesResolved = Util.replaceMacro(includes, envVars);
        String excludesResolved = Util.replaceMacro(excludes, envVars);
        md5Map = getMd5Map(pathResolved, includesResolved, excludesResolved, startStage, log);
    }

    private void refreshMemoryInfo(Map<String, FileInfo> newMd5Map) throws XTriggerException {
        md5Map = newMd5Map;
    }

    /**
     * Computes and gets the file information of the folder
     *
     * @param startingStage true if the caller is the starting stage
     * @param log
     * @return the file of the folder information
     * @throws XTriggerException
     */
    private Map<String, FileInfo> getMd5Map(String path, String includes, String excludes, boolean startingStage, XTriggerLog log) throws XTriggerException {

        if (path == null) {
            log.info("A folder path must be set.");
            return null;
        }
        Label label = job.getAssignedLabel();
        if (label == null) {
            log.info("Polling on the master");
            return getFileInfoMaster(path, includes, excludes, log);
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

    private Map<String, FileInfo> getFileInfoMaster(String path, String includes, String excludes, XTriggerLog log) throws XTriggerException {
        FilePath rootPath = Hudson.getInstance().getRootPath();
        if (rootPath == null) {
            return null;
        }

        return getFileInfo(path, includes, excludes, log);
    }


    private Map<String, FileInfo> getFileInfoLabel(Label label, final XTriggerLog log) throws XTriggerException {

        Set<Node> nodes = label.getNodes();
        Map<String, FileInfo> result;
        for (Node node : nodes) {
            FilePath nodePath = node.getRootPath();
            if (nodePath != null) {
                currentSlave = nodePath;
                try {
                    final Map<String, String> envVars = new XTriggerEnvVarsResolver().getEnvVars((AbstractProject) job, node, log);
                    result = nodePath.act(new FilePath.FileCallable<Map<String, FileInfo>>() {
                        public Map<String, FileInfo> invoke(File node, VirtualChannel channel) throws IOException, InterruptedException {
                            try {
                                String pathResolved = Util.replaceMacro(path, envVars);
                                String includesResolved = Util.replaceMacro(includes, envVars);
                                String excludesResolved = Util.replaceMacro(excludes, envVars);
                                return getFileInfo(pathResolved, includesResolved, excludesResolved, log);
                            } catch (XTriggerException fse) {
                                throw new RuntimeException(fse);
                            }
                        }
                    });
                } catch (Throwable t) {
                    throw new XTriggerException(t);
                }

                //We stop at first slave with file information
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private Map<String, FileInfo> getFileInfo(String path, String includes, String excludes, XTriggerLog log) throws XTriggerException {

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
            includes = "**/*.*, **/*";
        }
        FileSet fileSet = Util.createFileSet(new File(path), includes, excludes);
        for (Iterator it = fileSet.iterator(); it.hasNext();) {
            FileResource fileResource = (FileResource) it.next();
            processFileResource(log, result, fileResource);
        }
        return result;
    }

    private void processFileResource(XTriggerLog log, Map<String, FileInfo> result, FileResource fileResource) throws XTriggerException {
        if (!fileResource.isExists()) {
            log.info(String.format("\nThe file '%s' doesn't exist anymore ", fileResource.getFile().getPath()));
        } else {
            String currentMd5;
            try {
                FileInputStream fis = new FileInputStream(fileResource.getFile());
                currentMd5 = Util.getDigestOf(fis);
                fis.close();
            } catch (FileNotFoundException e) {
                throw new XTriggerException(e);
            } catch (IOException e) {
                throw new XTriggerException(e);
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
    protected synchronized boolean checkIfModified(final XTriggerLog log) throws XTriggerException {

        Map<String, String> envVars = new XTriggerEnvVarsResolver().getEnvVars((AbstractProject) job, Hudson.getInstance(), log);
        String pathResolved = Util.replaceMacro(path, envVars);
        String includesResolved = Util.replaceMacro(includes, envVars);
        String excludesResolved = Util.replaceMacro(excludes, envVars);

        //Get the current information
        Map<String, FileInfo> newMd5Map = getMd5Map(pathResolved, includesResolved, excludesResolved, false, log);

        if (offlineSlaveOnStartup) {
            refreshMemoryInfo(newMd5Map);
            log.info("Slave(s) were offline at startup. Waiting for next schedule to check if there are modifications.");
            offlineSlaveOnStartup = false;
            return false;
        }

        boolean changed = checkIfModified(pathResolved, log, newMd5Map);
        refreshMemoryInfo(newMd5Map);
        return changed;
    }


    private boolean checkIfModified(String path, final XTriggerLog log, final Map<String, FileInfo> newMd5Map) throws XTriggerException {

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
                throw new XTriggerException(ioe);
            } catch (InterruptedException ie) {
                throw new XTriggerException(ie);
            }
            return isTriggering;
        }
    }

    private boolean computeEachFile(XTriggerLog log, Map<String, FileInfo> originMd5Map, Map<String, FileInfo> newMd5Map) {

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
                log.info(String.format("The path '%s' doesn't exist anymore.", originFilePath));
                return true;
            }

            //Checks if the file from the new compute has been modified
            if (originFileInfo.getLastModified() != newFileInfo.getLastModified()) {
                log.info(String.format("The '%s' last modification date has changed.", originFilePath));
                return true;
            }

            //Checks it the content file from the new compute has been modified
            if (!originFileInfo.getMd5().equals(newFileInfo.getMd5())) {
                log.info(String.format("The '%s' content has changed.", originFilePath));
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
            refreshMemoryInfo(true, new XTriggerLog((StreamTaskListener) TaskListener.NULL));
        } catch (XTriggerException fse) {
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
            XTriggerLog log = new XTriggerLog(listener);
            if (!Hudson.getInstance().isQuietingDown() && ((AbstractProject) job).isBuildable()) {
                Runner runner = new Runner(log, "FolderTrigger");
                executorService.execute(runner);
            } else {
                log.info("Jenkins is quieting down or the job is not buildable.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during the trigger execution " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new FSTriggerFolderAction((AbstractProject) job, getLogFile(), this.getDescriptor().getLabel()));
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
