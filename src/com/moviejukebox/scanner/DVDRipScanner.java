package com.moviejukebox.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
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

	private static Logger log = Logger.getLogger("moviejukebox");

	public DVDRipScanner(Properties props) {
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

			for (int i = 0; i < list.length
					&& !video_ts.equalsIgnoreCase("VIDEO_TS"); i++)
				video_ts = list[i].getName();

			if (!video_ts.equalsIgnoreCase("VIDEO_TS"))
				throw new Exception(
						"DVD drive not found:" + mediaRep);

			selectedFile = new File(selectedFile.getAbsolutePath(), video_ts);

			/* Get the ifo files */
			list = selectedFile.listFiles();

			ArrayList<File> ifoList = new ArrayList<File>(4);

			for (int i = 0; i < list.length; i++) {

				if (list[i].getName().regionMatches(true,
						list[i].getName().lastIndexOf("."), ".ifo", 0, 4)
						&& !"VIDEO_TS.IFO".equalsIgnoreCase(list[i].getName())) {//$NON-NLS-1$ //$NON-NLS-2$
					ifoList.add(list[i]);
				}
			}

			File[] ifo = (File[]) ifoList.toArray(new File[ifoList.size()]);

			if (ifo == null || ifo.length == 0) {
				log.info("No Ifo Found");
			} else {

				int longestDuration = 0;
				int longestDurationIndex = -1;

				FilePropertiesMovie[] fileProperties = new FilePropertiesMovie[ifo.length];

				for (int i = 0; i < ifo.length; i++) {
					try {
						fileProperties[i] = new FilePropertiesMovie(ifo[i]
								.getAbsolutePath());

						if (fileProperties[i].getDuration() > longestDuration) {
							longestDuration = fileProperties[i].getDuration();
							longestDurationIndex = i;
						}

					} catch (Exception e) {
						log.finer("Error when parsing file:" + ifo[i]); 
					}
				}

				return fileProperties[longestDurationIndex];
			}
		} catch (Exception err) {
			err.printStackTrace();
			return null;
		}
		return null;
	}

	public String formatDuration(FilePropertiesMovie fileProperties) {
		int duration = fileProperties.getDuration();
		StringBuffer returnString = new StringBuffer("");

		int nbHours = duration / 3600;
		if (nbHours != 0) {
			returnString.append(nbHours).append("h");
			duration = duration - nbHours * 3600;
		}

		int nbMinutes = duration / 60;
		if (nbMinutes != 0) {
			if (nbHours != 0)
				returnString.append(" ");
			returnString.append(nbMinutes).append("mn");
		}

		return returnString.toString();
	}

}
