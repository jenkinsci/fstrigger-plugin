package org.jenkinsci.plugins.fstrigger.service;

import hudson.EnvVars;
import hudson.Util;
import hudson.model.*;
import org.jenkinsci.plugins.envinject.EnvInjectAction;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;

import java.io.IOException;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerEnvVarsResolver {

    public Map<String, String> getEnvVars(AbstractProject project, FSTriggerLog log) throws FSTriggerException {
        try {
            Run lastBuild = project.getLastBuild();
            if (lastBuild != null) {
                EnvInjectAction envInjectAction = lastBuild.getAction(EnvInjectAction.class);
                if (envInjectAction != null) {
                    return envInjectAction.getEnvMap();
                }
                return lastBuild.getEnvironment(log.getListener());
            } else {
                return getDefaultEnvVars(project, log.getListener());
            }
        } catch (IOException ioe) {
            throw new FSTriggerException(ioe);
        } catch (InterruptedException ie) {
            throw new FSTriggerException(ie);
        }
    }

    private Map<String, String> getDefaultEnvVars(AbstractProject project, TaskListener listener) throws IOException, InterruptedException {

        EnvVars env = new EnvVars();
        env.put("JENKINS_SERVER_COOKIE", Util.getDigestOf("ServerID:" + Hudson.getInstance().getSecretKey()));
        env.put("HUDSON_SERVER_COOKIE", Util.getDigestOf("ServerID:" + Hudson.getInstance().getSecretKey())); // Legacy compatibility
        env.put("JOB_NAME", project.getFullName());
        Computer c = Computer.currentComputer();
        if (c != null)
            env = c.getEnvironment().overrideAll(env);
        String rootUrl = Hudson.getInstance().getRootUrl();
        if (rootUrl != null) {
            env.put("JENKINS_URL", rootUrl);
            env.put("HUDSON_URL", rootUrl);
            env.put("JOB_URL", rootUrl + project.getUrl());
        }

        env.put("JENKINS_HOME", Hudson.getInstance().getRootDir().getPath());
        env.put("HUDSON_HOME", Hudson.getInstance().getRootDir().getPath());   // legacy compatibility

        Thread t = Thread.currentThread();
        if (t instanceof Executor) {
            Executor e = (Executor) t;
            env.put("EXECUTOR_NUMBER", String.valueOf(e.getNumber()));
            env.put("NODE_NAME", e.getOwner().getName());
            Node n = e.getOwner().getNode();
            if (n != null)
                env.put("NODE_LABELS", Util.join(n.getAssignedLabels(), " "));
        }

        return env;
    }

}


