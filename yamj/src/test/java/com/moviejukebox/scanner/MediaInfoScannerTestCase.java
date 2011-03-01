/*
 *      Copyright (c) 2004-2011 YAMJ Members
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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.TestCase;

public class MediaInfoScannerTestCase extends TestCase {

    public void test1() {
        File file = new File("src/test/java/mediainfo-1.txt");
        System.out.print("File:" + file.getAbsolutePath());
        System.out.print(" Length:" + file.length());
        System.out.println(" "+ file.exists());
        MediaInfoScanner toTest = new MediaInfoScanner();
        HashMap<String, String> infosGeneral = new HashMap<String, String>();
        ArrayList<HashMap<String, String>> infosVideo = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>> infosAudio = new ArrayList<HashMap<String, String>>();
        ArrayList<HashMap<String, String>> infosText  = new ArrayList<HashMap<String, String>>();

        try {
            toTest.parseMediaInfo(new FileInputStream(file), infosGeneral, infosVideo, infosAudio, infosText);
            System.out.println(infosGeneral.values());
            assertEquals(8, infosGeneral.size());

        } catch (Exception e) {
            assertFalse("No exception expected : " + e.getMessage(), true);
        }

    }
}
