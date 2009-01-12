package com.moviejukebox.scanner;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.logging.Logger;

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

    public class BDPlaylistInfo {

        public String streamList[];
        public int duration;

        public BDPlaylistInfo() {

            this.streamList = null;
            this.duration = 0;
        }
    }

    public class BDFilePropertiesMovie {

        public File fileList[];
        public int duration;

        public BDFilePropertiesMovie() {

            this.fileList = null;
            this.duration = 0;
        }
    }

    public BDFilePropertiesMovie executeGetBDInfo(File mediaRep) {
        try {

            /* Gets the BDMV path... */
            File selectedFile = mediaRep;

            if (selectedFile.getName().equalsIgnoreCase("BDMV")) {
                selectedFile = selectedFile.getParentFile();
            }

            File[] list = selectedFile.listFiles();

            String bdmv = "";

            for (int i = 0; i < list.length && !bdmv.equalsIgnoreCase("BDMV"); i++) {
                bdmv = list[i].getName();
            }

            if (!bdmv.equalsIgnoreCase("BDMV")) {
                return null;
            }

            selectedFile = new File(selectedFile.getAbsolutePath(), bdmv);


            /* Gets the PLAYLIST path... */
            list = selectedFile.listFiles();

            String playlist = "";

            for (int i = 0; i < list.length && !playlist.equalsIgnoreCase("PLAYLIST"); i++) {
                playlist = list[i].getName();
            }

            if (!playlist.equalsIgnoreCase("PLAYLIST")) {
                return null;
            }

            selectedFile = new File(selectedFile.getAbsolutePath(), playlist);


            /* Get the mpls files */
            list = selectedFile.listFiles();


            BDPlaylistInfo playlistInfo;

            int longestDuration = 0;
            String longestFiles[] = null;

            for (int i = 0; i < list.length; i++) {

                if (list[i].getName().regionMatches(true,
                        list[i].getName().lastIndexOf("."), ".mpls", 0, 4)) {

                    playlistInfo = getBDPlaylistInfo(list[i].getAbsolutePath());

                    if (playlistInfo.duration > longestDuration) {
                        longestFiles = playlistInfo.streamList;
                        longestDuration = playlistInfo.duration;
                    }
                }
            }

            selectedFile = mediaRep;

            selectedFile = new File(selectedFile.getAbsolutePath(), bdmv);

            /* Gets the STREAM path... */
            list = selectedFile.listFiles();

            String stream = "";

            for (int i = 0; i < list.length && !stream.equalsIgnoreCase("STREAM"); i++) {
                stream = list[i].getName();
            }

            if (!stream.equalsIgnoreCase("STREAM")) {
                return null;
            }

            selectedFile = new File(selectedFile.getAbsolutePath(), stream);


            /* Get the m2ts files */
            list = selectedFile.listFiles();

            BDFilePropertiesMovie ret = new BDFilePropertiesMovie();
            ret.fileList = new File[longestFiles.length];

            for (int i = 0; i < list.length; i++) {

                // Go over the playlist file names
                for (int j = 0; j < longestFiles.length; j++) {

                    if (list[i].getName().equalsIgnoreCase(longestFiles[j])) {
                        ret.fileList[j] = list[i];
                    }
                }
            }

            ret.duration = longestDuration;

            return ret;

        } catch (Exception err) {
            err.printStackTrace();
            return null;
        }
    }

    public BDPlaylistInfo getBDPlaylistInfo(String filePath) throws Exception {

        BDPlaylistInfo ret = new BDPlaylistInfo();
        ret.duration = 0;

        /* The input stream... */
        RandomAccessFile fileReader = new RandomAccessFile(filePath, "r");

        /* Some ported code from the bdinfo free project */
        byte[] data = new byte[(int) fileReader.length()];
        int dataLength = fileReader.read(data, 0, data.length);

        fileReader.close();


        byte[] fileType = new byte[8];
        System.arraycopy(data, 0, fileType, 0, fileType.length);

        String FileType = new String(fileType);
        if ((FileType.equals("MPLS0100") && FileType.equals("MPLS0200")) /*|| data[45] != 1*/) {
            log.info("Invalid playlist file " + FileType);
            return ret;
        }

        int playlistIndex =
                (((int) data[8] & 0xFF) << 24) +
                (((int) data[9] & 0xFF) << 16) +
                (((int) data[10] & 0xFF) << 8) +
                ((int) data[11]);

        int playlistLength = data.length - playlistIndex - 4;
        int playlistLengthCorrect =
                (((int) data[playlistIndex] & 0xFF) << 24) +
                (((int) data[playlistIndex + 1] & 0xFF) << 16) +
                (((int) data[playlistIndex + 2] & 0xFF) << 8) +
                (((int) data[playlistIndex + 3] & 0xFF));

        byte[] playlistData = new byte[playlistLength];
        System.arraycopy(data, playlistIndex + 4,
                playlistData, 0, playlistData.length);

        int streamFileCount =
                ((((int) playlistData[2] & 0xFF) << 8) + ((int) playlistData[3] & 0xFF));


        ret.streamList = new String[streamFileCount];

        int streamFileOffset = 6;
        for (int streamFileIndex = 0;
                streamFileIndex < streamFileCount;
                streamFileIndex++) {
            byte[] streamFileNameData = new byte[5];
            System.arraycopy(playlistData, streamFileOffset + 2,
                    streamFileNameData, 0, streamFileNameData.length);

            String streamFile = new String(streamFileNameData) + ".M2TS";

            long timeIn =
                    (((long) playlistData[streamFileOffset + 14] & 0xFF) << 24) +
                    (((long) playlistData[streamFileOffset + 15] & 0xFF) << 16) +
                    (((long) playlistData[streamFileOffset + 16] & 0xFF) << 8) +
                    ((long) playlistData[streamFileOffset + 17] & 0xFF);

            long timeOut =
                    (((long) playlistData[streamFileOffset + 18] & 0xFF) << 24) +
                    (((long) playlistData[streamFileOffset + 19] & 0xFF) << 16) +
                    (((long) playlistData[streamFileOffset + 20] & 0xFF) << 8) +
                    ((long) playlistData[streamFileOffset + 21] & 0xFF);

            long length = (timeOut - timeIn) / 45000;


            // Process this movie stream
            if (streamFileIndex == 0 || !ret.streamList[streamFileIndex - 1].equals(streamFile)) {
                ret.duration += (int) length;
            }
            ret.streamList[streamFileIndex] = streamFile;

            streamFileOffset += 2 +
                    (((int) playlistData[streamFileOffset] & 0xFF) << 8) +
                    (((int) playlistData[streamFileOffset + 1] & 0xFF));
        }

        return ret;
    }
}
