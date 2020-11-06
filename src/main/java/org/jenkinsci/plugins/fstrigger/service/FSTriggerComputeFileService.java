package org.jenkinsci.plugins.fstrigger.service;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
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

    /**
     * Gets the file to poll
     *
     * @param fileInfo
     * @return a FilePath object to the file, null if the object can't be determined or doesn't exist
     * @throws XTriggerException
     */
    public FilePath computedFile(Node node, AbstractProject project, final FileNameTriggerInfo fileInfo, final XTriggerLog log) throws XTriggerException {

        if (node == null || node.getRootPath() == null) {
            throw new XTriggerException("A valid node must be set.");
        }

        FilePath rootPath = node.getRootPath();
        if (rootPath == null) {
            throw new XTriggerException("An online node must be set.");
        }

        EnvVarsResolver varsRetriever = new EnvVarsResolver();
        try {
            final Map<String, String> envVars = varsRetriever.getPollingEnvVars(project, node);
            return rootPath.act(new MasterToSlaveFileCallable<FilePath>() {
                @Override
                public FilePath invoke(File file, VirtualChannel virtualChannel) throws IOException {
                    File f;
                    try {
                        f = new FSTriggerFileNameRetriever(fileInfo, log, envVars).getFile();
                    } catch (XTriggerException e) {
                        throw new IOException(e);
                    }
                    if (f == null) {
                        return null;
                    }
                    return new FilePath(f);
                }
            });
        } catch (EnvInjectException | InterruptedException | IOException e) {
            throw new XTriggerException(e);
        }
    }
}
