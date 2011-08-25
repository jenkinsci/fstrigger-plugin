package org.jenkinsci.plugins.fstrigger.service;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Label;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;
import org.jenkinsci.plugins.fstrigger.triggers.FileNameTriggerInfo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

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
     * @throws FSTriggerException
     */
    @SuppressWarnings({"JavaDoc"})
    public FilePath computedFile(AbstractProject project, FileNameTriggerInfo fileInfo, FSTriggerLog log) throws FSTriggerException {

        log.info(String.format("\nTrying to monitor the file pattern '%s'", fileInfo.getFilePathPattern()));

        Label label = project.getAssignedLabel();
        if (label == null) {
            return computeFileOnMaster(fileInfo, log);
        }

        return computeFileLabel(label, fileInfo, log);
    }


    private FilePath computeFileOnMaster(FileNameTriggerInfo fileInfo, FSTriggerLog log) throws FSTriggerException {

        log.info("Polling on the master");

        File file = new FSTriggerFileNameGetFileService(fileInfo, log).call();
        if (file != null) {
            return new FilePath(Hudson.MasterComputer.localChannel, file.getPath());
        } else {
            return null;
        }
    }

    private FilePath computeFileLabel(Label label, FileNameTriggerInfo fileInfo, FSTriggerLog log) throws FSTriggerException {

        log.info(String.format("Polling on all nodes for the label '%s' attached to the job.", label));

        if (label.isOffline()) {
            log.info("All slaves are offline.");
            return null;
        }

        for (Node node : label.getNodes()) {
            File file = computeFileNode(node, fileInfo, log);
            //We stop when the file exists on the slave
            if (file != null) {
                log.info(String.format("The file was found on the slave '%s'", node.getNodeName()));
                return new FilePath(node.getChannel(), file.getPath());
            }
        }

        log.info(String.format("The file doesn't exist for the slaves with the label '%s'", label));
        return null;
    }

    private File computeFileNode(Node node, final FileNameTriggerInfo fileInfo, final FSTriggerLog log) throws FSTriggerException {
        try {
            FilePath nodePath = node.getRootPath();
            log.info(String.format("Polling on the node '%s'", node.getNodeName()));
            // Is null if the slave is offline
            if (nodePath != null) {
                return nodePath.act(new FilePath.FileCallable<File>() {
                    public File invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                        try {
                            return new FSTriggerFileNameGetFileService(fileInfo, log).call();
                        } catch (FSTriggerException fse) {
                            throw new RuntimeException(fse);
                        }
                    }
                });
            } else {
                log.info(String.format("The node '%s' is offline", node.getNodeName()));
            }
        } catch (IOException e) {
            throw new FSTriggerException(e);
        } catch (InterruptedException e) {
            throw new FSTriggerException(e);
        }

        return null;
    }

}
