Javascript Console Admin Console component for Alfresco 7.x
===============================================================
[![Build Status](https://travis-ci.org/share-extras/js-console.svg?branch=master)](https://travis-ci.org/share-extras/js-console)

Author: Florian Maul (fme AG)  

This project contains a Javascript Console component for the Alfresco Share's
Administration Console, that enables the execution of arbitrary javascript code
in the repository.


Usage
--------
Add the dependencies to the Alfresco repository and share POM files of your WAR projects.

For the Repository

    <dependencies>
      ...
      <dependency>
        <groupId>de.fmaul</groupId>
        <artifactId>javascript-console-repo</artifactId>
        <version>0.6</version>
        <type>amp</type>
      </dependency>
      ...
    </dependencies>

    <overlays>
      ...
      <overlay>
        <groupId>de.fmaul</groupId>
        <artifactId>javascript-console-repo</artifactId>
        <type>amp</type>
      </overlay>
      ...
    </overlays>

For Share

    <dependencies>
      ...
      <dependency>
        <groupId>de.fmaul</groupId>
        <artifactId>javascript-console-share</artifactId>
        <version>0.6</version>
        <type>amp</type>
      </dependency>
      ...
    </dependencies>

    <overlays>
      ...
      <overlay>
        <groupId>de.fmaul</groupId>
        <artifactId>javascript-console-share</artifactId>
        <type>amp</type>
      </overlay>
      ...
    </overlays>

Watch the video
--------

https://www.youtube.com/watch?v=c3JIeVY8Nnk

New Features
--------

+ autocomplete with tern.js

+ new json output view for validation and formatting json from the freemarker template processing
![JSONView](https://raw.github.com/jgoldhammer/js-console/master/javascript-console-share/screenshots/json_output_view.png)

+ JSHint integration in javascript editor- it validates input on the fly and integrates the alfresco root objects like search, node...
![JSHINT](https://raw.github.com/jgoldhammer/js-console/master/javascript-console-share/screenshots/jshint_live_integration.png)

+ performance statistics - displays overall time, time for javascript and freemarker processing, time for network and time for rest of the server side processing
![PERFORMANCE](https://raw.github.com/jgoldhammer/js-console/master/javascript-console-share/screenshots/performance_stats.png)

+ Editor Theming - allows you to theme the javascript and freemarker codemirror editor
![Editor themes](https://raw.github.com/jgoldhammer/js-console/master/javascript-console-share/screenshots/editor_themes.png)

+ better error marking of server runtime errors in the editors
![Error](https://raw.github.com/jgoldhammer/js-console/master/javascript-console-share/screenshots/js_error_detection_and_marking.png)

+ update codemirror to version 3 and enabling many codemirror addons(activeline, hightlight selection, new autocomplete module)


Installation
------------

The component has been developed to install on top of an existing Alfresco
7.x installation. The `javascript-console-repo-<version>.amp` or
`javascript-console-share-<version>.amp` needs to be installed into the Alfresco
Repository / Share webapp using the Alfresco Module Management Tool:

    java -jar alfresco-mmt.jar install javascript-console-repo-<version>.amp /path/to/alfresco.war
    java -jar alfresco-mmt.jar install javascript-console-share-<version>.amp /path/to/share.war

You can also use the Alfresco Maven SDK to install or overlay the AMP during the build of a
Repository / Share WAR project. See https://artifacts.alfresco.com/nexus/content/repositories/alfresco-docs/alfresco-lifecycle-aggregator/latest/plugins/alfresco-maven-plugin/advanced-usage.html
for details.


Building
--------

To build the module and its AMP / JAR files, run the following command from the base
project directory:

    mvn clean install

Using the component
-------------------

- Log in to Alfresco Share and navigate to an Administration page such as Users
  or Groups
- In the left-hand-side navigation, click *Javascript Console*
- Enter Alfresco repository javascript code in the textarea at the top. Press
  the execute button to run the script.
- You can use the special `print(..)` javascript command to output messages to
  the output window.
- use Ctrl+Space for code completion. Note that only global objects and specific
  variables (document, space, variables ending in ...node) are completed.
