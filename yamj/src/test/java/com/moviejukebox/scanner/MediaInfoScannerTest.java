/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package com.moviejukebox.scanner;

import com.moviejukebox.model.Codec;
import com.moviejukebox.model.CodecType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaInfoScannerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MediaInfoScannerTest.class);
    private static final MediaInfoScanner MI_TEST = new MediaInfoScanner();
    private static final String testDir = "src/test/java/TestFiles/MediaInfo/";
    Map<String, String> infosGeneral = new HashMap<String, String>();
    List<Map<String, String>> infosVideo = new ArrayList<Map<String, String>>();
    List<Map<String, String>> infosAudio = new ArrayList<Map<String, String>>();
    List<Map<String, String>> infosText = new ArrayList<Map<String, String>>();

    @Test
    public void testStaticFile() {
        getMediaInfoTestFile("mediainfo-1.txt");

//        System.out.println(infosGeneral.values());
        assertEquals(8, infosGeneral.size());

//        Codec codec;
//        int counter = 1;
//        for (HashMap<String, String> codecInfo : infosVideo) {
//            codec = toTest.getCodecInfo(CodecType.VIDEO, codecInfo);
//            System.out.println(counter++ + " = " + codec.toString());
//        }
//
//        counter = 1;
//        for (HashMap<String, String> codecInfo : infosAudio) {
//            codec = toTest.getCodecInfo(CodecType.AUDIO, codecInfo);
//            System.out.println(counter++ + " = " + codec.toString());
//        }
    }

    @Test
    public void testAudioStreamFile() {
        getMediaInfoTestFile("mediainfo-2.txt");
        assertEquals(7, infosAudio.size());
        assertEquals(1, infosVideo.size());
    }

//    @Test
    public void testAvi() {
        getMediaInfoTestFile("AVI_DTS_ES_6.1.AVI.txt");

        Codec codec;
        int counter = 1;
        for (Map<String, String> codecInfo : infosVideo) {
            codec = MI_TEST.getCodecInfo(CodecType.VIDEO, codecInfo);
            System.out.println(counter++ + " = " + codec.toString());
        }

        counter = 1;
        for (Map<String, String> codecInfo : infosAudio) {
            codec = MI_TEST.getCodecInfo(CodecType.AUDIO, codecInfo);
            System.out.println(counter++ + " = " + codec.toString());
        }

        getMediaInfoTestFile("AVI_DTS_MA_7.1.AVI.txt");

        counter = 1;
        for (Map<String, String> codecInfo : infosVideo) {
            codec = MI_TEST.getCodecInfo(CodecType.VIDEO, codecInfo);
            System.out.println(counter++ + " = " + codec.toString());
        }

        counter = 1;
        for (Map<String, String> codecInfo : infosAudio) {
            codec = MI_TEST.getCodecInfo(CodecType.AUDIO, codecInfo);
            System.out.println(counter++ + " = " + codec.toString());
        }
    }

    @Test
    public void testChannels() {
        getMediaInfoTestFile("mediainfo-channels.txt");

        Codec codec;
        int counter = 1;
        for (Map<String, String> codecInfo : infosAudio) {
            codec = MI_TEST.getCodecInfo(CodecType.AUDIO, codecInfo);
            LOG.debug("{} = {}", counter++, codec.toString());
            assertTrue("No channels found!", codec.getCodecChannels()>0);
        }
    }

    /**
     * Output the infos
     *
     * printTextInfos("General", Arrays.asList(infosGeneral));
     *
     * printTextInfos("Video", infosVideo);
     *
     * printTextInfos("Audio", infosAudio);
     *
     * printTextInfos("Text", infosText);
     *
     * @param title
     * @param infos
     */
    public void printTextInfos(String title, List<Map<String, String>> infos) {
        LOG.info("***** {}", title);
        for (Map<String, String> info : infos) {
            for (Map.Entry<String, String> entry : info.entrySet()) {
                LOG.info("{}-{}", entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Load a test file
     *
     * @param filename
     */
    private void getMediaInfoTestFile(String filename) {
        File file = FileUtils.getFile(testDir, filename);
        System.out.print("File:" + file.getAbsolutePath());
        System.out.print(" Length:" + file.length());
        System.out.println(" Exists: " + file.exists());

        try {
            FileInputStream fis = new FileInputStream(file);

            infosGeneral.clear();
            infosVideo.clear();
            infosAudio.clear();
            infosText.clear();

            MI_TEST.parseMediaInfo(fis, infosGeneral, infosVideo, infosAudio, infosText);
        } catch (FileNotFoundException error) {
            LOG.warn("File not found.", error);
            assertFalse("No exception expected : " + error.getMessage(), true);
        } catch (IOException error) {
            LOG.warn("IOException.", error);
            assertFalse("No exception expected : " + error.getMessage(), true);
        }
    }
}
