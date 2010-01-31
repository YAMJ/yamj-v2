/*
 *      Copyright (c) 2004-2009 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list 
 *  
 *      Web: http://code.google.com/p/moviejukebox/
 *  
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *  
 *      For any reuse or distribution, you must make clear to others the 
 *      license terms of this work.  
 */

package com.moviejukebox.scanner;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.xmm.moviemanager.fileproperties.FilePropertiesMovie;

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

    private static Logger logger = Logger.getLogger("moviejukebox");

    public DVDRipScanner() {
    }

    public FilePropertiesMovie executeGetDVDInfo(File mediaRep) {
        try {

            /* Gets the path... */
            File selectedFile = mediaRep;

            if (selectedFile.getName().equalsIgnoreCase("AUDIO_TS") || selectedFile.getName().equalsIgnoreCase("VIDEO_TS")) { //$NON-NLS-1$
                selectedFile = selectedFile.getParentFile();
            }

            File[] list = selectedFile.listFiles();

            String video_ts = "";

            for (int i = 0; i < list.length && !video_ts.equalsIgnoreCase("VIDEO_TS"); i++) {
                video_ts = list[i].getName();
            }

            if (!video_ts.equalsIgnoreCase("VIDEO_TS")) {
                return null;
            }

            selectedFile = new File(selectedFile.getAbsolutePath(), video_ts);

            /* Get the ifo files */
            list = selectedFile.listFiles();

            ArrayList<File> ifoList = new ArrayList<File>(4);

            for (int i = 0; i < list.length; i++) {

                if (list[i].getName().regionMatches(true,
                        list[i].getName().lastIndexOf("."), ".ifo", 0, 4) && !"VIDEO_TS.IFO".equalsIgnoreCase(list[i].getName())) {//$NON-NLS-1$ //$NON-NLS-2$
                    ifoList.add(list[i]);
                }
            }

            File[] ifo = (File[]) ifoList.toArray(new File[ifoList.size()]);

            if (ifo == null || ifo.length == 0) {
                logger.info("DVDRipScanner: No Ifo Found with disk format.");
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
                        logger.finer("DVDRipScanner: Error when parsing file:" + ifo[i]);
                    }
                }

                if (longestDurationIndex == -1) {
                    logger.info("DVDRipScanner: Error retrieving file durations for IFO file, processing skipped.");
                    return null;
                } else {
                    return fileProperties[longestDurationIndex];
                }
            }
        } catch (Exception error) {
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return null;
        }
        return null;
    }
}
