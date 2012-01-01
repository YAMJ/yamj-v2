/*
 *      Copyright (c) 2004-2012 YAMJ Members
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

import com.moviejukebox.model.Codec;
import com.moviejukebox.model.Codec.CodecType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

public class MediaInfoScannerTest {

    private static MediaInfoScanner toTest = new MediaInfoScanner();
    private static final String testDir = "src/test/java/TestFiles/MediaInfo/";
    HashMap<String, String> infosGeneral = new HashMap<String, String>();
    ArrayList<HashMap<String, String>> infosVideo = new ArrayList<HashMap<String, String>>();
    ArrayList<HashMap<String, String>> infosAudio = new ArrayList<HashMap<String, String>>();
    ArrayList<HashMap<String, String>> infosText = new ArrayList<HashMap<String, String>>();

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
        for (HashMap<String, String> codecInfo : infosVideo) {
            codec = toTest.getCodecInfo(CodecType.VIDEO, codecInfo);
            System.out.println(counter++ + " = " + codec.toString());
        }

        counter = 1;
        for (HashMap<String, String> codecInfo : infosAudio) {
            codec = toTest.getCodecInfo(CodecType.AUDIO, codecInfo);
            System.out.println(counter++ + " = " + codec.toString());
        }
        
        getMediaInfoTestFile("AVI_DTS_MA_7.1.AVI.txt");
        
        counter = 1;
        for (HashMap<String, String> codecInfo : infosVideo) {
            codec = toTest.getCodecInfo(CodecType.VIDEO, codecInfo);
            System.out.println(counter++ + " = " + codec.toString());
        }

        counter = 1;
        for (HashMap<String, String> codecInfo : infosAudio) {
            codec = toTest.getCodecInfo(CodecType.AUDIO, codecInfo);
            System.out.println(counter++ + " = " + codec.toString());
        }
    }

    /**
     * Load a test file
     * @param filename 
     */
    private void getMediaInfoTestFile(String filename) {
        File file = new File(testDir + filename);
        System.out.print("File:" + file.getAbsolutePath());
        System.out.print(" Length:" + file.length());
        System.out.println(" Exists: " + file.exists());

        try {
            FileInputStream fis = new FileInputStream(file);

            infosGeneral.clear();
            infosVideo.clear();
            infosAudio.clear();
            infosText.clear();

            toTest.parseMediaInfo(fis, infosGeneral, infosVideo, infosAudio, infosText);
        } catch (FileNotFoundException error) {
            Logger.getLogger(MediaInfoScannerTest.class.getName()).log(Level.SEVERE, null, error);
            assertFalse("No exception expected : " + error.getMessage(), true);
        } catch (IOException error) {
            Logger.getLogger(MediaInfoScannerTest.class.getName()).log(Level.SEVERE, null, error);
            assertFalse("No exception expected : " + error.getMessage(), true);
        }
    }
}
