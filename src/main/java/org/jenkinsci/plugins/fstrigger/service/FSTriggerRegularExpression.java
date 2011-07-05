package org.jenkinsci.plugins.fstrigger.service;


import com.google.code.regexp.NamedMatcher;
import com.google.code.regexp.NamedPattern;
import org.jenkinsci.plugins.fstrigger.FSTriggerException;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Gregory Boissinot
 */
public class FSTriggerRegularExpression implements Serializable {


    public boolean isRegexGroupNamed(String pattern) throws FSTriggerException {
        try {
            com.google.code.regexp.NamedPattern namedPattern = com.google.code.regexp.NamedPattern.compile(pattern);
            List<String> groupNames = namedPattern.groupNames();
            return groupNames != null && groupNames.size() != 0;
        } catch (PatternSyntaxException pse) {
            throw new FSTriggerException(pse);
        }

    }

    public List<File> getFiles(File folder, String patternStr) throws FSTriggerException {

        boolean isRegexGroupNamed = isRegexGroupNamed(patternStr);

        //Iterates through the file in the folder
        List<File> matchFiles = new LinkedList<File>();
        if (isRegexGroupNamed) {
            NamedPattern namedPattern = NamedPattern.compile(patternStr);
            List<String> groupNames = namedPattern.groupNames();
            //A stored map of groupName/groupValue for environment publication
            Map<String, String> regex = new HashMap<String, String>();

            for (File file : folder.listFiles()) {
                NamedMatcher namedMatcher = namedPattern.matcher(file.getName());
                if (namedMatcher.matches()) {
                    for (String groupName : groupNames) {
                        String groupValue = namedMatcher.group(groupName);
                        regex.put(groupName, groupValue);
                        matchFiles.add(file);
                    }
                }
            }
        } else {
            Pattern pattern = Pattern.compile(patternStr);
            //Iterates through the file in the folder
            for (File file : folder.listFiles()) {
                Matcher matcher = pattern.matcher(file.getName());
                if (matcher.matches()) {
                    matchFiles.add(file);
                }
            }
        }


        return matchFiles;
    }
}
