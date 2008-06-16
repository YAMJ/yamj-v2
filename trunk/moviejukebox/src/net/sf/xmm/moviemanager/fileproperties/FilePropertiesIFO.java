/**
 * @(#)FilePropertiesIFO.java 1.0 06.06.05 (dd.mm.yy)
 *
 * Copyright (2003) Bro3
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License aloSng with 
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Boston, MA 02111.
 * 
 * Contact: bro3@users.sourceforge.net
 * -----------------------------------------------------
 * gaelead modifications :
 * - org.apache.log4j.Logger switched to java.util.logging.Logger
 * - Use only getRuntime (Video/Audio/Subs infos will be scanned by mediainfo)
 * - removed all unused code
 * - fixed the way duration is computed (minutes and secondes intepretation was
 *   faulty)
 **/

package net.sf.xmm.moviemanager.fileproperties;

import java.io.RandomAccessFile;

class FilePropertiesIFO extends FileProperties {

	private final int DVDVIDEO_VTS = 0x535456; /* 'VTS' */

	private final int SIZE = 100000;

	/**
	 * Processes a file from the given DataInputStream.
	 */
	protected void process(RandomAccessFile dataStream) throws Exception {
		log.finest("Start processing IFO file.");

		byte[] ifoFile = new byte[SIZE + 10];

		/*
		 * 4 bytes has already been read in FilePropertiesMovie, therefore the
		 * first byte read is stored in index 4
		 */
		dataStream.read(ifoFile, 4, SIZE);

		/* Gets the stream type... (4 bytes) */
		int streamType = readUnsignedInt32(ifoFile, 9);

		if (streamType == DVDVIDEO_VTS) {

			supported = true;
			processIfoFile(ifoFile);

			log.finest("Processing IFO file done.");
		} else
			log.finest("IFO type not supported.");
	}

	void processIfoFile(byte[] ifoFile) throws Exception {

		getRuntime(ifoFile);
	}

	void getRuntime(byte[] ifoFile) throws Exception {

		int[] runtime;

		/* sector pointer to VTS_PGCI (Title Program Chain table) Offset 204 */
		int sectorPointerVTS_PGCI = changeEndianness(readUnsignedInt32(ifoFile,
				204));

		/*
		 * Offset value of the VTS_PGCITI (Video title set program chain
		 * information table)
		 */
		int offsetVTS_PGCI = sectorPointerVTS_PGCI * 2048;

		int numberOfTitles = readUnsignedInt16(ifoFile, offsetVTS_PGCI + 1);

		runtime = new int[numberOfTitles];

		// int startByteVTS_PGCI = offsetVTS_PGCI;

		int pointer = offsetVTS_PGCI;
		int startcode = changeEndianness(readUnsignedInt32(ifoFile,
				offsetVTS_PGCI + 12));

		offsetVTS_PGCI += 12;

		for (int i = 0; i < numberOfTitles; i++) {

			runtime[i] = 0;

			runtime[i] += encode(ifoFile[pointer + startcode + 4]) * 60 * 60; /* Hours */
			runtime[i] += encode(ifoFile[pointer + startcode + 5]) * 60; /* Minutes */
			runtime[i] += encode(ifoFile[pointer + startcode + 6]); /* Seconds */

			/* Encrease by 8 to get to the next startcode */
			offsetVTS_PGCI += 8;
			startcode = changeEndianness(readUnsignedInt32(ifoFile,
					offsetVTS_PGCI));
        }

		setDuration(runtime[0]);
	}

	public static int encode(byte dataByte) {
		StringBuilder builder = new StringBuilder();

		// A byte stand for 2 hex characters
		builder.append(Integer.toHexString((dataByte & 0xF0) >> 4));
		builder.append(Integer.toHexString(dataByte & 0x0F));

		return Integer.parseInt(builder.toString());
	}

}
