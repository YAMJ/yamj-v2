@echo off
rem ***
rem *** Yet Another Movie Jukebox command script
rem ***
rem *** DO NOT CHANGE ANYTHING IN THIS SCRIPT
rem *** UNLESS YOU KNOW WHAT YOU ARE DOING
rem ***

java -Xms256m -Xmx512m -classpath .;resources;lib/* com.moviejukebox.MovieJukebox %*
