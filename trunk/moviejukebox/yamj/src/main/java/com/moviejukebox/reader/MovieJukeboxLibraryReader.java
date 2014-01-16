/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.reader;

import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

/**
 *
 * @author Stuart
 */
public class MovieJukeboxLibraryReader {

    private static final Logger LOG = Logger.getLogger(MovieJukeboxLibraryReader.class);
    private static final String LOG_MESSAGE = "LibraryReader: ";

    protected MovieJukeboxLibraryReader() {
        throw new UnsupportedOperationException("Cannot instantiate this class");
    }

    public static Collection<MediaLibraryPath> parse(File libraryFile) {
        Collection<MediaLibraryPath> mlp = new ArrayList<MediaLibraryPath>();

        if (!libraryFile.exists() || libraryFile.isDirectory()) {
            LOG.error(LOG_MESSAGE + "The moviejukebox library input file you specified is invalid: " + libraryFile.getName());
            return mlp;
        }

        try {
            XMLConfiguration c = new XMLConfiguration(libraryFile);

            List<HierarchicalConfiguration> fields = c.configurationsAt("library");
            for (HierarchicalConfiguration sub : fields) {
                // sub contains now all data about a single medialibrary node
                String path = sub.getString("path");
                String nmtpath = sub.getString("nmtpath"); // This should be depreciated
                String playerpath = sub.getString("playerpath");
                String description = sub.getString("description");
                boolean scrapeLibrary = true;

                String scrapeLibraryString = sub.getString("scrapeLibrary");
                if (StringTools.isValidString(scrapeLibraryString)) {
                    try {
                        scrapeLibrary = sub.getBoolean("scrapeLibrary");
                    } catch (Exception ignore) {
                    }
                }

                long prebuf = -1;
                String prebufString = sub.getString("prebuf");
                if (prebufString != null && !prebufString.isEmpty()) {
                    try {
                        prebuf = Long.parseLong(prebufString);
                    } catch (NumberFormatException ignore) {
                    }
                }

                // Note that the nmtpath should no longer be used in the library file and instead "playerpath" should be used.
                // Check that the nmtpath terminates with a "/" or "\"
                if (nmtpath != null) {
                    if (!(nmtpath.endsWith("/") || nmtpath.endsWith("\\"))) {
                        // This is the NMTPATH so add the unix path separator rather than File.separator
                        nmtpath = nmtpath + "/";
                    }
                }

                // Check that the playerpath terminates with a "/" or "\"
                if (playerpath != null) {
                    if (!(playerpath.endsWith("/") || playerpath.endsWith("\\"))) {
                        // This is the PlayerPath so add the Unix path separator rather than File.separator
                        playerpath = playerpath + "/";
                    }
                }

                List<Object> excludes = sub.getList("exclude[@name]");
                File medialibfile = new File(path);
                if (medialibfile.exists()) {
                    MediaLibraryPath medlib = new MediaLibraryPath();
                    medlib.setPath(medialibfile.getCanonicalPath());
                    if (playerpath == null || playerpath.equals("")) {
                        medlib.setPlayerRootPath(nmtpath);
                    } else {
                        medlib.setPlayerRootPath(playerpath);
                    }
                    medlib.setExcludes(excludes);
                    medlib.setDescription(description);
                    medlib.setScrapeLibrary(scrapeLibrary);
                    medlib.setPrebuf(prebuf);
                    mlp.add(medlib);

                    if (description != null && !description.isEmpty()) {
                        LOG.info(LOG_MESSAGE + "Found media library: " + description);
                    } else {
                        LOG.info(LOG_MESSAGE + "Found media library: " + path);
                    }
                    // Save the media library to the log file for reference.
                    LOG.debug(LOG_MESSAGE + "Media library: " + medlib);

                } else {
                    LOG.info(LOG_MESSAGE + "Skipped invalid media library: " + path);
                }
            }
        } catch (ConfigurationException error) {
            LOG.error(LOG_MESSAGE + "Failed parsing moviejukebox library input file: " + libraryFile.getName());
            LOG.error(SystemTools.getStackTrace(error));
        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed parsing moviejukebox library input file: " + libraryFile.getName());
            LOG.error(SystemTools.getStackTrace(error));
        }
        return mlp;
    }
}
