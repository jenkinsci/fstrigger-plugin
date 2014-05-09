package org.jenkinsci.plugins.fstrigger.triggers.filecontent;

import hudson.Extension;
import hudson.util.FormValidation;
import org.jenkinsci.lib.xtrigger.XTriggerException;
import org.jenkinsci.lib.xtrigger.XTriggerLog;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileType;
import org.jenkinsci.plugins.fstrigger.core.FSTriggerContentFileTypeDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class XMLFileContent extends FSTriggerContentFileType {

    private transient Map<String, Object> results;

    private transient Document xmlDocument;

    private List<XMLFileContentEntry> expressions = new ArrayList<XMLFileContentEntry>();

    @DataBoundConstructor
    public XMLFileContent(List<XMLFileContentEntry> element) {
        if (element != null) {
            this.expressions = element;
        }
    }

    @Override
    public Object getMemoryInfo() {
        return results;
    }

    @Override
    public void setMemoryInfo(Object memoryInfo) {
        if ((memoryInfo != null) && !(memoryInfo instanceof Map)) {
            throw new IllegalArgumentException(String.format("The memory info %s object is not a Map object.", memoryInfo));
        }
        this.results = (Map) memoryInfo;
    }


    @SuppressWarnings("unused")
    public List<XMLFileContentEntry> getExpressions() {
        return expressions;
    }

    @Override
    protected void initForContent(File file) throws XTriggerException {
        xmlDocument = initXMLFile(file);
        results = readXMLPath(xmlDocument);
    }

    private Document initXMLFile(File file) throws XTriggerException {
        Document xmlDocument;
        try {
            xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        } catch (SAXException e) {
            throw new XTriggerException(e);
        } catch (IOException e) {
            throw new XTriggerException(e);
        } catch (ParserConfigurationException e) {
            throw new XTriggerException(e);
        }
        return xmlDocument;
    }

    private Map<String, Object> readXMLPath(Document document) throws XTriggerException {
        Map<String, Object> results = new HashMap<String, Object>(expressions.size());
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        try {
            for (XMLFileContentEntry expressionEntry : expressions) {
                String expression = expressionEntry.getExpression();
                XPathExpression xPathExpression = xPath.compile(expression);
                Object result = xPathExpression.evaluate(document);
                results.put(expression, result);
            }
        } catch (XPathExpressionException xpe) {
            throw new XTriggerException(xpe);
        }
        return results;
    }

    @Override
    protected boolean isTriggeringBuildForContent(File file, XTriggerLog log) throws XTriggerException {

        Document newDocument = initXMLFile(file);
        Map<String, Object> newResults = readXMLPath(newDocument);

        if (results == null) {
            throw new NullPointerException("Initial result object must not be a null reference.");
        }
        if (newResults == null) {
            throw new NullPointerException("New computed results object must not be a null reference.");
        }

        if (results.size() != newResults.size()) {
            throw new XTriggerException("Regarding the trigger life cycle, the size between old results and new results have to be the same.");
        }

        //The results object have to be the same keys
        if (!results.keySet().containsAll(newResults.keySet())) {
            throw new XTriggerException("Regarding the set up of the result objects, the keys for the old results and the new results have to the same.");
        }


        for (Map.Entry<String, Object> entry : results.entrySet()) {

            String expression = entry.getKey();
            Object initValue = entry.getValue();
            Object newValue = newResults.get(expression);

            if (initValue == null && newValue == null) {
                log.info(String.format("There is no matching for the expression '%s'.", expression));
                continue;
            }

            if (initValue == null && newValue != null) {
                log.info(String.format("There was no value and there is a new value for the expression '%s'.", expression));
                return true;
            }

            if (initValue != null && newValue == null) {
                log.info(String.format("There was a value and now the there is no value for the expression '%s'.", expression));
                return true;
            }

            if (!initValue.equals(newValue)) {
                log.info(String.format("The value for the expression '%s' has changed.", expression));
                return true;
            }

        }

        return false;
    }

    @Extension
    @SuppressWarnings("unused")
    public static class XMLFileContentDescriptor extends FSTriggerContentFileTypeDescriptor<XMLFileContent> {

        @Override
        public Class<? extends FSTriggerContentFileType> getType() {
            return XMLFileContent.class;
        }

        @Override
        public String getDisplayName() {
            return "Monitor the contents of an XML file";
        }

        @Override
        public String getLabel() {
            return "XML File";
        }

        /**
         * Performs presence check.
         *
         * @param value the xpath
         * @return the form validation object
         */
        public FormValidation doCheckXpath(@QueryParameter String value) {

            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("You must provide an XPath.");
            }

            return FormValidation.ok();
        }

    }
}
