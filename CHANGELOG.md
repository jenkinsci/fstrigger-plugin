# Changelog

## Release 0.39

* Fix XPath computation  
* Fix Check Path field with latest Jenkins version in configuration  
* Upgrade to latest Jenkins LTS (1.554.1)

## Release 0.38

* Fix
[JENKINS-17591](https://issues.jenkins-ci.org/browse/JENKINS-17591) -
FSTrigger fails to poll when build node labels change

## Release 0.37

* Fix
[JENKINS-18658](https://issues.jenkins-ci.org/browse/JENKINS-18658) -
NPE in FSTrigger upon configuration save

## Release 0.36

* Fix
[JENKINS-17641](https://issues.jenkins-ci.org/browse/JENKINS-17641) -
Unknown field 'logEnabled' in org.jenkinsci.lib.xtrigger.XTriggerCause

## Release 0.35

* Fix
[JENKINS-16809](https://issues.jenkins-ci.org/browse/JENKINS-16809) -
Plugins FSTrigger/Envinject, NPE while loading jobs  
  * Update to xtrigger-lib 0.20  
  * Update to envinject-lib 1.17

## Release 0.34

* Fix NullPointerException on polling action  
* Upgrade to envinject-lib 1.11  
* Upgrade to xtrigger-lib 0.18

## Release 0.33

* Fix potential NullPointer exception at startup (envinject-lib 1.8/
xtrigger-lib 1.5)

## Release 0.32

* Upgrade to xtrigger-lib 0.14 (more logs)

## Release 0.31

* Fix
[JENKINS-12176](https://issues.jenkins-ci.org/browse/JENKINS-12176) -
Unable to delete a job that has a fstrigger  
* Upgrade to xtrigger-lib 0.13

## Release 0.30

* Fix reponed
[JENKINS-12924](https://issues.jenkins-ci.org/browse/JENKINS-12924) -
FSTrigger triggers builds on jenkins restart

## Release 0.29

* Fix
[JENKINS-12924](https://issues.jenkins-ci.org/browse/JENKINS-12924) -
FSTrigger triggers builds on jenkins restart

## Release 0.28

* Update to xtrigger-lib 0.8 (fix
[JENKINS-12888](https://issues.jenkins-ci.org/browse/JENKINS-12888))

## Release 0.27

* Fix
[JENKINS-12865](https://issues.jenkins-ci.org/browse/JENKINS-12865) -
[ERROR](https://wiki.jenkins.io/display/JENKINS/FSTrigger+Plugin#) -
SEVERE - Polling error Current Project is null from FSTrigger

## Release 0.26

* Update to xtrigger-lib 0.7

## Release 0.25

* Add the choice of check content, last modification date or a change
in the size of files for folder content type  
* Update to xtrigger-lib 0.6

## Release 0.24

* For 'Folder type', add check with new directories

## Release 0.23

* Fix
[JENKINS-12208](https://issues.jenkins-ci.org/browse/JENKINS-12208) -
More information in log file

## Release 0.22

* Fix
[JENKINS-12168](https://issues.jenkins-ci.org/browse/JENKINS-12168) -
Monitor files - Does not monitor a unix soft link

## Release 0.21

* Add check 'A job is not triggered when Jenkins is quieting down and
is not buildable'

## Release 0.20

* Fix reoponed
[JENKINS-12073](https://issues.jenkins-ci.org/browse/JENKINS-12073) -
fstrigger plugin download didn't pull in dependency envinject

## Release 0.19

* Fix
[JENKINS-12073](https://issues.jenkins-ci.org/browse/JENKINS-12073) -
fstrigger plugin download didn't pull in dependency envinject  
* For 'monitor folder' type, the last modification date is checked
before a content check

## Release 0.18

* Environment variables are taken into account

## Release 0.17

* Fix
[JENKINS-11569](https://issues.jenkins-ci.org/browse/JENKINS-11569) -
Enhanced help for includes

## Release 0.16

* Fix
[JENKINS-11567](https://issues.jenkins-ci.org/browse/JENKINS-11567) -
unhandled FileNotFountException

## Release 0.15

* Add check for configuration page  
* Built for 1.409 (compatible LTS)

## Release 0.14

* Fix bug on save when no content nature is selected for
FileNameTrigger

## Release 0.13

* Fix empty includes value for 'Folder trigger type'

## Release 0.12

* Fix path resolution for Windows - Merge pull request from vinaynaik

## Release 0.11

* Add the ability to monitor more than one file.

## Release 0.10.1

* Add an help message for the update center.

## Release 0.10

* Remove named regular expression (unusual)  
* Refactoring

## Release 0.9 (technical release)

* Internationalizing some messages

## Release 0.8

* Fix a bug for XML Content type  
* Added help messages for end users.

## Release 0.7

* Add Tar monitoring capabilities  
* Fix a regression on the last modification date check

## Release 0.6

* Fix check on last modification date

## Release 0.5

* Polling is done on slaves if configured

## Release 0.4

* Remove the usage of regular expression for the file name to poll

## Release 0.3

* Internal Refactoring  
* Add 'Poll the content of an XML File' regarding XPath expressions.

## Release 0.2

* Add a page for displaying polling log

## Release 0.1

* Initial release
