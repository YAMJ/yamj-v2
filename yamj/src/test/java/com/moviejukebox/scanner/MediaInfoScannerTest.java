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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.moviejukebox.model.Codec;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.enumerations.CodecType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaInfoScannerTest {

    private static final Logger LOG = LoggerFactory.getLogger(MediaInfoScannerTest.class);
    private static final MediaInfoScanner MI_TEST = new MediaInfoScanner();
    private static final String TEST_DIR = "src/test/java/TestFiles/MediaInfo/";
    Map<String, String> infosGeneral = new HashMap<>();
    List<Map<String, String>> infosVideo = new ArrayList<>();
    List<Map<String, String>> infosAudio = new ArrayList<>();
    List<Map<String, String>> infosText = new ArrayList<>();

    @Ignore
    public void testMediaInfoScan() {
        getMediaInfoTestFile("test.mkv", false);
        printTextInfos("Video", this.infosVideo);
        printTextInfos("Audio", this.infosAudio);
    }

    @Test
    public void testStaticFile() {
        getMediaInfoTestFile("mediainfo-ts-2d.txt", true);

        Movie movie = new Movie();
        MI_TEST.updateMovieInfo(movie, infosGeneral, infosVideo, infosAudio, infosText, infosGeneral);

        LOG.info("Runtime: {}", movie.getRuntime());
        assertEquals("Wrong runtime", "1h 54m", movie.getRuntime());
    }

    @Test
    public void testAudioStreamFile() {
        getMediaInfoTestFile("mediainfo-2.txt", true);
        assertEquals(7, infosAudio.size());
        assertEquals(1, infosVideo.size());
    }

    @Test
    public void testAvi() {
        getMediaInfoTestFile("AVI_DTS_ES_6.1.AVI.txt", true);

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

        getMediaInfoTestFile("AVI_DTS_MA_7.1.AVI.txt", true);

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
        getMediaInfoTestFile("mediainfo-channels.txt", true);

        Codec codec;
        int counter = 1;
        for (Map<String, String> codecInfo : infosAudio) {
            codec = MI_TEST.getCodecInfo(CodecType.AUDIO, codecInfo);
            LOG.debug("{} = {}", counter++, codec.toString());
            assertTrue("No channels found!", codec.getCodecChannels() > 0);
        }
    }

    @Test
    public void testMultipleAudioCodes() {
        getMediaInfoTestFile("DTS_AC3_DTS_AC3.txt", true);
        Movie movie = new Movie();
        Codec codec;
        int counter = 1;

        for (Map<String, String> codecInfo : infosAudio) {
            codec = MI_TEST.getCodecInfo(CodecType.AUDIO, codecInfo);
            movie.addCodec(codec);
            LOG.debug("{} = {}", counter++, codec.toString());
            assertTrue("No channels found!", codec.getCodecChannels() > 0);
        }

        for (Codec audio : movie.getCodecs()) {
            LOG.debug("AudioCodec: {}", audio.toString());
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
                LOG.info("{} -> {}", entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Load a test file
     *
     * @param filename
     */
    private void getMediaInfoTestFile(String filename, boolean isText) {
        File file = FileUtils.getFile(TEST_DIR, filename);
        LOG.info("File: {} Length: {} Exists: {}", file.getAbsolutePath(), file.length(), file.exists());

        try (MediaInfoStream stream = (isText ? new MediaInfoStream(new FileInputStream(file)) : MI_TEST.createStream(file.getAbsolutePath()))) {
            infosGeneral.clear();
            infosVideo.clear();
            infosAudio.clear();
            infosText.clear();

            MI_TEST.parseMediaInfo(stream, infosGeneral, infosVideo, infosAudio, infosText);
        } catch (FileNotFoundException error) {
            LOG.warn("File not found.", error);
            assertFalse("No exception expected : " + error.getMessage(), true);
        } catch (Exception error) {
            LOG.warn("IOException.", error);
            assertFalse("No exception expected : " + error.getMessage(), true);
        }
    }
}
