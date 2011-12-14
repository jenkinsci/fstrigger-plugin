package org.jenkinsci.plugins.fstrigger.service;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.lib.xtrigger.service.XTriggerEnvVarsResolver;
import org.jenkinsci.plugins.fstrigger.triggers.FileNameTriggerInfo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerComputeFileService implements Serializable {

    private static FSTriggerComputeFileService instance;

    private FSTriggerComputeFileService() {
    }

    public static synchronized FSTriggerComputeFileService getInstance() {
        if (instance == null) {
            instance = new FSTriggerComputeFileService();
        }
        return instance;
    }

    /**
     * Computes and gets the file to poll
     *
     * @param fileInfo
     * @return a FilePath object to the file, null if the object can't be determined or doesn't exist
     * @throws XTriggerException
     */
    @SuppressWarnings({"JavaDoc"})
    public FilePath computedFile(AbstractProject project, FileNameTriggerInfo fileInfo, XTriggerLog log) throws XTriggerException {

        Label label = project.getAssignedLabel();
        if (label == null) {
            return computeFileOnMaster(fileInfo, project, log);
        }

        return computeFileLabel(label, fileInfo, project, log);
    }


    private FilePath computeFileOnMaster(FileNameTriggerInfo fileInfo, AbstractProject project, XTriggerLog log) throws XTriggerException {
        Map<String, String> envVars = new XTriggerEnvVarsResolver().getEnvVars(project, Hudson.getInstance(), log);
        log.info("Polling on the master");
        File file = new FSTriggerFileNameGetFileService(fileInfo, log, envVars).call();
        if (file != null) {
            log.info(String.format("\nMonitoring the file pattern '%s'", file.getPath()));
            return new FilePath(Hudson.MasterComputer.localChannel, file.getPath());
        } else {
            return null;
        }
    }

    private FilePath computeFileLabel(Label label, FileNameTriggerInfo fileInfo, AbstractProject project, XTriggerLog log) throws XTriggerException {

        log.info(String.format("Polling on all nodes for the label '%s' attached to the job.", label));

        if (label.isOffline()) {
            log.info("All slaves are offline.");
            return null;
        }

        for (Node node : label.getNodes()) {
            File file = computeFileNode(node, fileInfo, project, log);
            //We stop when the file exists on the slave
            if (file != null) {
                log.info(String.format("\nMonitoring the file pattern '%s'", file.getPath()));
                return new FilePath(node.getChannel(), file.getPath());
            }
        }

        log.info(String.format("The file doesn't exist for the slaves with the label '%s'", label));
        return null;
    }

    private File computeFileNode(Node node, final FileNameTriggerInfo fileInfo, final AbstractProject project, final XTriggerLog log) throws XTriggerException {
        try {
            FilePath nodePath = node.getRootPath();
            log.info(String.format("Polling on the node '%s'", node.getNodeName()));
            // Is null if the slave is offline
            if (nodePath != null) {
                Map<String, String> envVars = new XTriggerEnvVarsResolver().getEnvVars(project, node, log);
                return nodePath.act(new FSTriggerFileNameGetFileService(fileInfo, log, envVars));
            } else {
                log.info(String.format("The node '%s' is offline", node.getNodeName()));
            }
        } catch (IOException e) {
            throw new XTriggerException(e);
        } catch (InterruptedException e) {
            throw new XTriggerException(e);
        }

        return null;
    }

}
