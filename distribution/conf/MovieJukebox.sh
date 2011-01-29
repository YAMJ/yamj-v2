#!/bin/sh
# Intel Mac users : needs java 1.6, set fullpath to java 1.6 instead of default java
# This path is usually : /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Commands/java

java -Xms256m -Xmx1024m -classpath .:./resources:./lib/* com.moviejukebox.MovieJukebox "$@"
