package com.moviejukebox.scanner;

import java.io.File;
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

public class BDRipScanner {

	private static Logger log = Logger.getLogger("moviejukebox");

	public BDRipScanner() {
	}

	public File executeGetBDInfo(File mediaRep) {
		try {

			/* Gets the BDMV path... */
			File selectedFile = mediaRep;

			if (selectedFile.getName().equalsIgnoreCase("BDMV")) { //$NON-NLS-1$
				selectedFile = selectedFile.getParentFile();
			}

			File[] list = selectedFile.listFiles();

			String bdmv = "";

			for (int i = 0; i < list.length
					&& !bdmv.equalsIgnoreCase("BDMV"); i++)
				bdmv = list[i].getName();

			if (!bdmv.equalsIgnoreCase("BDMV"))
				return null;

			selectedFile = new File(selectedFile.getAbsolutePath(), bdmv);


			/* Gets the STREAM path... */
			list = selectedFile.listFiles();

			String stream = "";

			for (int i = 0; i < list.length
					&& !stream.equalsIgnoreCase("STREAM"); i++)
				stream = list[i].getName();

			if (!stream.equalsIgnoreCase("STREAM"))
				return null;

			selectedFile = new File(selectedFile.getAbsolutePath(), stream);

			
			/* Get the m2ts files */
			list = selectedFile.listFiles();

			ArrayList<File> m2tsList = new ArrayList<File>(4);

			for (int i = 0; i < list.length; i++) {

				if (list[i].getName().regionMatches(true,
						list[i].getName().lastIndexOf("."), ".m2ts", 0, 4)) {
					m2tsList.add(list[i]);
				}
			}

			File[] m2ts = (File[]) m2tsList.toArray(new File[m2tsList.size()]);

			if (m2ts == null || m2ts.length == 0) {
				log.info("No m2ts Found");
			} else {

				long largestSize = 0;
				int largestSizeIndex = -1;

				for (int i = 0; i < m2ts.length; i++) {
					if (m2ts[i].length() > largestSize) {
						largestSize = m2ts[i].length();
						largestSizeIndex = i;
					}
				}

				return m2ts[largestSizeIndex];
			}
		} catch (Exception err) {
			err.printStackTrace();
			return null;
		}
		return null;
	}

}
