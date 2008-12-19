@echo off

java -Xms256m -Xmx512m -classpath .;resources;lib/* com.moviejukebox.MovieJukebox %*

