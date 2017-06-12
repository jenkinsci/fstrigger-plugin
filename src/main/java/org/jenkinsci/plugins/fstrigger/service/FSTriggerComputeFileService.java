package org.jenkinsci.plugins.fstrigger.service;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.remoting.Callable;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;
import org.jenkinsci.lib.envinject.service.EnvVarsResolver;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.triggers.FileNameTriggerInfo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;


/**
 * @author Gregory Boissinot
 */
public class FSTriggerComputeFileService implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Gets the file to poll
     *
     * @param node the node that the file is on
     * @param project the FSTrigger's project
     * @param fileInfo the FileNameTriggerInfo of the file
     * @param log the XTrigger logger
     * @return a FilePath object to the file, null if the object can't be determined or doesn't exist
     * @throws XTriggerException if the node is invalid or offline
     */
    public FilePath computedFile(Node node, AbstractProject project, final FileNameTriggerInfo fileInfo, final XTriggerLog log) throws XTriggerException {

        FilePath rootPath;
        if (node == null || (rootPath = node.getRootPath()) == null) {
            throw new XTriggerException("A valid node must be set.");
        }

        EnvVarsResolver varsRetriever = new EnvVarsResolver();
        try {
            final Map<String, String> envVars = varsRetriever.getPollingEnvVars(project, node);
                                // file to poll could be on slave
            return rootPath.act(new MasterToSlaveCallable<FilePath, XTriggerException>() {
                public FilePath call() throws XTriggerException {
                    File file = new FSTriggerFileNameRetriever(fileInfo, log, envVars).getFile();
                    if (file == null) {
                        return null;
                    }
                    return new FilePath(file);
                }
            });

        } catch (EnvInjectException | IOException | InterruptedException e) {
            throw new XTriggerException(e);
        }
    }

}
