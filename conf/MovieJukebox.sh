#!/bin/sh
# Intel Mac users : needs java 1.6, set fullpath to java 1.6 instead of default java
# This path is usually : /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Commands/java

java -Xms256m -Xmx512m -classpath .:./resources:./lib/moviejukebox.jar:./lib/commons-logging-1.1.1.jar:./lib/commons-lang-2.4.jar:./lib/commons-configuration-1.5.jar:./lib/commons-collections-3.2.1.jar:./lib/filters.jar:./lib/mucommanderlight.jar:./lib/thetvdbapi.jar com.moviejukebox.MovieJukebox "$@"
