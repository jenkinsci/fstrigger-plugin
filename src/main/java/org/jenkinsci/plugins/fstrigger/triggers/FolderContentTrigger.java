package org.jenkinsci.plugins.fstrigger.triggers;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.*;
import hudson.remoting.VirtualChannel;
import hudson.util.SequentialExecutionQueue;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.Jenkins;
import org.apache.commons.jelly.XMLOutput;
import org.apache.tools.ant.types.DirSet;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.resources.FileResource;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.service.EnvVarsResolver;
import org.jenkinsci.lib.xtrigger.AbstractTrigger;
import org.jenkinsci.lib.xtrigger.XTriggerDescriptor;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerAction;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Gregory Boissinot
 */
public class FolderContentTrigger extends AbstractTrigger {

    private static Logger LOGGER = Logger.getLogger(FolderContentTrigger.class.getName());

    private static final String CAUSE = "Triggered by a change to a folder";

    /**
     * GUI fields
     */
    private final String path;
    private final String includes;
    private final String excludes;
    private boolean excludeCheckLastModificationDate;
    private boolean excludeCheckContent;
    private boolean excludeCheckFewerOrMoreFiles;

    /**
     * Memory fields
     */
    private transient Map<String, FileInfo> md5Map = new HashMap<String, FileInfo>();


    @DataBoundConstructor
    public FolderContentTrigger(String cronTabSpec, String path, String includes, String excludes, boolean excludeCheckLastModificationDate, boolean excludeCheckContent, boolean excludeCheckFewerOrMoreFiles) throws ANTLRException {
        super(cronTabSpec);
        this.path = Util.fixEmpty(path);
        this.includes = Util.fixEmpty(includes);
        this.excludes = Util.fixEmpty(excludes);
        this.excludeCheckLastModificationDate = excludeCheckLastModificationDate;
        this.excludeCheckContent = excludeCheckContent;
        this.excludeCheckFewerOrMoreFiles = excludeCheckFewerOrMoreFiles;
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

    @SuppressWarnings("unused")
    public boolean isExcludeCheckLastModificationDate() {
        return excludeCheckLastModificationDate;
    }

    @SuppressWarnings("unused")
    public boolean isExcludeCheckContent() {
        return excludeCheckContent;
    }

    @SuppressWarnings("unused")
    public boolean isExcludeCheckFewerOrMoreFiles() {
        return excludeCheckFewerOrMoreFiles;
    }

    @Override
    protected File getLogFile() {
        return new File(job.getRootDir(), "trigger-polling-folder.log");
    }

    @Override
    protected Action[] getScheduledActions(Node node, XTriggerLog log) {
        return new Action[0];
    }

    @Override
    protected boolean requiresWorkspaceForPolling() {
        return false;
    }

    static class FileInfo implements Serializable {

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

    @Override
    protected synchronized boolean checkIfModified(Node pollingNode, final XTriggerLog log) throws XTriggerException {

        EnvVarsResolver varsRetriever = new EnvVarsResolver();
        Map<String, String> envVars;
        try {
            envVars = varsRetriever.getPollingEnvVars((AbstractProject) job, pollingNode);
        } catch (EnvInjectException e) {
            throw new XTriggerException(e);
        }

        String pathResolved = Util.replaceMacro(path, envVars);
        String includesResolved = Util.replaceMacro(includes, envVars);
        String excludesResolved = Util.replaceMacro(excludes, envVars);

        //Get the current information
        Map<String, FileInfo> newMd5Map = getMd5Map(pollingNode, pathResolved, includesResolved, excludesResolved, log);

        if (offlineSlaveOnStartup) {
            refreshMemoryInfo(newMd5Map);
            log.info("Slave(s) were offline at startup. Waiting for next schedule to check if there are modifications.");
            offlineSlaveOnStartup = false;
            return false;
        }

        boolean changed = checkIfModified(pollingNode, pathResolved, log, newMd5Map);
        refreshMemoryInfo(newMd5Map);
        return changed;
    }

    private void refreshMemoryInfo(Map<String, FileInfo> newMd5Map) throws XTriggerException {
        md5Map = newMd5Map;
    }

    private Map<String, FileInfo> getMd5Map(Node launcherNode, final String path, final String includes, final String excludes, final XTriggerLog log) throws XTriggerException {

        if (path == null) {
            throw new XTriggerException("A folder path must be set.");
        }

        if (launcherNode == null) {
            throw new XTriggerException("A node must be set.");
        }

        if (launcherNode.getRootPath() == null) {
            log.info("The slave is now offline. Waiting next schedule");
            return null;
        }

        Map<String, FileInfo> result;
        try {
            result = launcherNode.getRootPath().act(new MasterToSlaveFileCallable<Map<String, FileInfo>>() {
                @Override
                public Map<String, FileInfo> invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
                    try {
                        return getFileInfo(path, includes, excludes, log);
                    } catch (XTriggerException fse) {
                        throw new RuntimeException(fse);
                    }
                }
            });
        } catch (IOException | InterruptedException e) {
            throw new XTriggerException(e);
        }

        return result;
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

        Map<String, FileInfo> result = new HashMap<>();
        if (includes == null) {
            includes = "**/*.*, **/*";
        }

        //Process Directories
        DirSet dirSet = new DirSet();
        dirSet.setProject(new org.apache.tools.ant.Project());
        dirSet.setDir(new File(path));
        dirSet.setIncludes("*");
        if (excludes != null) {
            dirSet.setExcludes(excludes);
        }
        for (Iterator it = dirSet.iterator(); it.hasNext(); ) {
            FileResource fileResource = (FileResource) it.next();
            processDirectoryResource(log, result, fileResource);
        }

        //Process files
        FileSet fileSet = Util.createFileSet(new File(path), includes, excludes);
        for (Iterator it = fileSet.iterator(); it.hasNext(); ) {
            FileResource fileResource = (FileResource) it.next();
            processFileResource(log, result, fileResource);
        }
        return result;
    }

    private void processDirectoryResource(XTriggerLog log, Map<String, FileInfo> result, FileResource folderResource) throws XTriggerException {
        if (!folderResource.isExists()) {
            log.info(String.format("\nThe folder '%s' doesn't exist anymore ", folderResource.getFile().getPath()));
        } else {
            FileInfo fileInfo = new FileInfo(null, folderResource.getLastModified());
            result.put(folderResource.getFile().getAbsolutePath(), fileInfo);
        }
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

    private boolean checkIfModified(Node launcherNode, String path, final XTriggerLog log, final Map<String, FileInfo> newMd5Map) throws XTriggerException {

        assert launcherNode != null;
        assert launcherNode.getRootPath() != null;

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
        if (!excludeCheckFewerOrMoreFiles && this.md5Map.size() != newMd5Map.size()) {
            log.info("The folder '" + new File(path) + "' content has changed.");
            return true;
        }

        //Check each file
        boolean isTriggering;
        try {
            final Map<String, FileInfo> originMd5Map = md5Map;
            isTriggering = launcherNode.getRootPath().act(new MasterToSlaveFileCallable<Boolean>() {
                @Override
                public Boolean invoke(File nodePath, VirtualChannel channel) throws IOException, InterruptedException {
                    return checkIfModifiedFile(log, originMd5Map, newMd5Map);
                }
            });
        } catch (IOException | InterruptedException ioe) {
            throw new XTriggerException(ioe);
        }

        return isTriggering;
    }

    private boolean checkIfModifiedFile(XTriggerLog log, Map<String, FileInfo> originMd5Map, Map<String, FileInfo> newMd5Map) {

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
            if (!excludeCheckLastModificationDate && (originFileInfo.getLastModified() != newFileInfo.getLastModified())) {
                log.info(String.format("The last modification date of '%s' has changed.", originFilePath));
                return true;
            }

            //Checks it the content file from the new compute has been modified
            if (!excludeCheckContent && (originFileInfo.getMd5() != null && !originFileInfo.getMd5().equals(newFileInfo.getMd5()))) {
                log.info(String.format("The content of '%s' has changed.", originFilePath));
                return true;
            }
        }

        return false;

    }

    @Override
    public void start(Node pollingNode, BuildableItem project, boolean newInstance, XTriggerLog log) {

        EnvVarsResolver varsRetriever = new EnvVarsResolver();
        Map<String, String> envVars = null;
        try {
            envVars = varsRetriever.getPollingEnvVars((AbstractProject) project, pollingNode);
        } catch (EnvInjectException e) {
            //Ignore the exception process, just log it
            LOGGER.log(Level.SEVERE, e.getMessage());
        }

        String pathResolved = Util.replaceMacro(path, envVars);
        String includesResolved = Util.replaceMacro(includes, envVars);
        String excludesResolved = Util.replaceMacro(excludes, envVars);

        /**
         * Records a md5 for each file of the folder that matches includes and excludes pattern
         */
        try {
            Map<String, FileInfo> md5Map = getMd5Map(pollingNode, pathResolved, includesResolved, excludesResolved, log);
            refreshMemoryInfo(md5Map);
        } catch (XTriggerException fse) {
            LOGGER.log(Level.SEVERE, "Error on trigger startup " + fse.getMessage());
            fse.printStackTrace();
        }
    }

    @Override
    protected String getName() {
        return "FSTrigger";
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new FSTriggerFolderAction(this.getDescriptor().getDisplayName()));
    }

    @Override
    public FolderContentTriggerDescriptor getDescriptor() {
        return (FolderContentTriggerDescriptor) Jenkins.get().getDescriptorOrDie(getClass());
    }

    public final class FSTriggerFolderAction extends FSTriggerAction {

        private final transient String actionTitle;

        public FSTriggerFolderAction(String actionTitle) {
            this.actionTitle = actionTitle;
        }

        @Override
        public String getDisplayName() {
            return "FSTrigger Folder Log";
        }

        @Override
        public String getUrlName() {
            return "triggerPollLogFolder";
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
            new AnnotatedLargeText<FSTriggerFolderAction>(getLogFile(), Charset.defaultCharset(), true, this).writeHtmlTo(0, out.asWriter());
        }
    }

    @Extension
    @SuppressWarnings("unused")
    public static class FolderContentTriggerDescriptor extends XTriggerDescriptor {

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
        public String getHelpFile() {
            return "/plugin/fstrigger/help-monitorFolder.html";
        }

    }
}
