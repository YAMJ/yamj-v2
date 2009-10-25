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

package com.moviejukebox.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

// import com.moviejukebox.tools.HTMLTools;

public class FileTools {

    private static Logger logger = Logger.getLogger("moviejukebox");
    final static int BUFF_SIZE = 100000;
    final static byte[] buffer = new byte[BUFF_SIZE];
    static Map<CharSequence, CharSequence> unsafeChars = new HashMap<CharSequence, CharSequence>();
    static Character encodeEscapeChar = null;
    
    static {
        // What to do if the user specifies a blank encodeEscapeChar? I guess disable encoding.
        String encodeEscapeCharString = PropertiesUtil.getProperty("mjb.charset.filenameEncodingEscapeChar", "$");
        if (encodeEscapeCharString.length() > 0) {
            // What to do if the user specifies a >1 character long string? I guess just use the first char.
            encodeEscapeChar = encodeEscapeCharString.charAt(0);
        
            String repChars = PropertiesUtil.getProperty("mjb.charset.unsafeFilenameChars", "<>:\"/\\|?*");
            for (String repChar : repChars.split("")) {
                if (repChar.length() > 0) {
                    char ch = repChar.charAt(0);
                    // Don't encode characters that are hex digits
                    // Also, don't encode the escape char -- it is safe by definition!
                    if (!Character.isDigit(ch) && -1 == "AaBbCcDdEeFf".indexOf(ch) && !encodeEscapeChar.equals(ch)) {
                        String hex = Integer.toHexString(ch).toUpperCase();
                        unsafeChars.put(repChar, encodeEscapeChar + hex);
                    }
                }
            }
        }
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        try {
            while (true) {
                synchronized (buffer) {
                    int amountRead = is.read(buffer);
                    if (amountRead == -1) {
                        break;
                    }
                    os.write(buffer, 0, amountRead);
                }
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException error) {
                // ignore
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException error) {
                // ignore
            }
        }
    }

    public static void copyFile(File src, File dst) {
        try {
            if (!src.exists()) {
                logger.severe("The specified " + src + " file does not exist!");
                return;
            }

            if (dst.isDirectory()) {
                dst.mkdirs();
                copy(new FileInputStream(src), new FileOutputStream(dst + File.separator + src.getName()));
            } else {
                copy(new FileInputStream(src), new FileOutputStream(dst));
            }

        } catch (IOException error) {
            logger.severe("Failed copying file " + src + " to " + dst);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    public static void copyDir(String srcDir, String dstDir) {
        try {
            File src = new File(srcDir);
            if (!src.exists()) {
                logger.severe("The specified " + srcDir + " file or directory does not exist!");
                return;
            }

            File dst = new File(dstDir);
            dst.mkdirs();

            if (!dst.exists()) {
                logger.severe("The specified " + dstDir + " output directory does not exist!");
                return;
            }

            if (src.isFile()) {
                copy(new FileInputStream(src), new FileOutputStream(dstDir));
            } else {
                File[] contentList = src.listFiles();
                if (contentList != null) {
                    List<File> files = Arrays.asList(contentList);
                    Collections.sort(files);

                    for (File file : files) {
                        if (!file.getName().equals(".svn")) {
                            if (file.isDirectory()) {
                                copyDir(file.getAbsolutePath(), dstDir + File.separator + file.getName());
                            } else {
                                copyFile(file, dst);
                            }
                        }
                    }
                }
            }
        } catch (IOException error) {
            logger.severe("Failed copying file " + srcDir + " to " + dstDir);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    public static String readFileToString(File file) {
        StringBuffer out = new StringBuffer();
        
        if (file != null) {
            try {
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new FileReader(file));
                    String line = in.readLine();
                    while (line != null) {
                        out.append(line);
                        line = in.readLine();
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (IOException error) {
                logger.severe("Failed reading file " + file.getName());
            }
        }
        
        return out.toString();
    }

    /***
     *  @author Stuart Boston
     *  @param  file1 - first file to compare
     *  @param  file2 - second file to compare
     *  @return true if the files exist and file2 is older, false otherwise.
     *
     * Note that file1 will be checked to see if it's newer than file2
     */
    public static boolean isNewer(File file1, File file2) {
        // If file1 exists and file2 doesn't then return true
        if (file1.exists()) {
            // If file2 doesn't exist then file1 is newer 
            if (!file2.exists()) {
                return true;
            }
        } else {
            // File1 doesn't exist so return false
            return false;
        }

        // Compare the file dates. This is only true if the first file is newer than the second, as the second file is the file2 file
        if (file1.lastModified() <= file2.lastModified()) {
            // file1 is older than the file2.
            return false;
        } else {
            // file1 is newer than file2
            return true;
        }
    }

    public static String createCategoryKey(String key2) {
        return key2;
    }

    public static String createPrefix(String category, String key) {
        return category + '_' + key + '_';
    }
    
    public static String makeSafeFilename(String filename) {
        String oldfilename = filename;
        for (Map.Entry<CharSequence, CharSequence> rep : unsafeChars.entrySet()) {
            filename = filename.replace(rep.getKey(), rep.getValue());
        }
        if (!filename.equals(oldfilename)) {
            logger.finest("Encoded filename string " + oldfilename + " to " + filename);
        }
            
        return filename;
    }
    
    /**
     * Download the image for the specified URL into the specified file. 
     * Utilises the WebBrowser downloadImage function to allow for proxy connections.
     * 
     * @param imageFile
     * @param imageURL
     * @throws IOException
     */
    public static void downloadImage(File imageFile, String imageURL) throws IOException {
        WebBrowser webBrowser = new WebBrowser();
        webBrowser.downloadImage(imageFile, imageURL);
    }

    /**
     * Find the parent directory of the movie file.
     * @param movie
     * @return Parent folder
     * @author Stuart Boston
     */
    public static String getParentFolder(File movieFile) {
    	String parentFolder = null;
    	
        if (movieFile.isDirectory()) { // for VIDEO_TS
        	parentFolder = movieFile.getPath();
        } else {
        	parentFolder = movieFile.getParent();
        }
        
        // Issue 1070, /BDMV/STREAM is being appended to the parent path
        if (parentFolder.substring(parentFolder.length()-12).equalsIgnoreCase(File.separator + "BDMV" + File.separator + "STREAM"))
        	parentFolder = parentFolder.substring(0, parentFolder.length() - 12);

    	return parentFolder;
    }
}
