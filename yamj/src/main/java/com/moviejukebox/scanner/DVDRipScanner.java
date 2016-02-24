/*
 *      Copyright (c) 2004-2016 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sf.xmm.moviemanager.fileproperties.FilePropertiesMovie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.moviejukebox.tools.SystemTools;

/**
 * @author Grael by using GPL Source from Mediterranean :
 * @(#)DialogMovieInfo.java 1.0 26.09.06 (dd.mm.yy)
 *
 * Copyright (2003) Mediterranean
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Boston, MA 02111.
 *
 * Contact: mediterranean@users.sourceforge.net
 */
public class DVDRipScanner {

    private static final Logger LOG = LoggerFactory.getLogger(DVDRipScanner.class);

    public DVDRipScanner() {
    }

    public FilePropertiesMovie executeGetDVDInfo(File mediaRep) {
        try {

            // Gets the path...
            File selectedFile = mediaRep;

            if (selectedFile.getName().equalsIgnoreCase("AUDIO_TS") || selectedFile.getName().equalsIgnoreCase("VIDEO_TS")) { //$NON-NLS-1$
                selectedFile = selectedFile.getParentFile();
            }

            File[] list = selectedFile.listFiles();

            String videoTS = "";

            for (int i = 0; i < list.length && !videoTS.equalsIgnoreCase("VIDEO_TS"); i++) {
                videoTS = list[i].getName();
            }

            if (!videoTS.equalsIgnoreCase("VIDEO_TS")) {
                return null;
            }

            selectedFile = new File(selectedFile.getAbsolutePath(), videoTS);

            /* Get the ifo files */
            list = selectedFile.listFiles();

            List<File> ifoList = new ArrayList<>(4);
            for (File list1 : list) {
                if (list1.getName().regionMatches(true, list1.getName().lastIndexOf("."), ".ifo", 0, 4) && !"VIDEO_TS.IFO".equalsIgnoreCase(list1.getName())) {
                    //$NON-NLS-1$ //$NON-NLS-2$
                    ifoList.add(list1);
                }
            }

            File[] ifo = ifoList.toArray(new File[ifoList.size()]);

            if (ifo == null || ifo.length == 0) {
                LOG.info("No Ifo Found with disk format.");
            } else {

                int longestDuration = 0;
                int longestDurationIndex = -1;

                FilePropertiesMovie[] fileProperties = new FilePropertiesMovie[ifo.length];

                for (int i = 0; i < ifo.length; i++) {
                    try {
                        fileProperties[i] = new FilePropertiesMovie(ifo[i].getAbsolutePath());

                        if (longestDuration == 0 || fileProperties[i].getDuration() > longestDuration) {
                            longestDuration = fileProperties[i].getDuration();
                            longestDurationIndex = i;
                        }

                    } catch (Exception error) {
                        LOG.debug("Error when parsing file: {}", ifo[i]);
                    }
                }

                if (longestDurationIndex == -1) {
                    LOG.info("Error retrieving file durations for IFO file, processing skipped.");
                    return null;
                }
                return fileProperties[longestDurationIndex];
            }
        } catch (Exception error) {
            LOG.error(SystemTools.getStackTrace(error));
            return null;
        }
        return null;
    }
}
