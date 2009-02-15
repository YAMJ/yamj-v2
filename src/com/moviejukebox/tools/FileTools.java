package com.moviejukebox.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class FileTools {

    private static Logger logger = Logger.getLogger("moviejukebox");
    final static int BUFF_SIZE = 100000;
    final static byte[] buffer = new byte[BUFF_SIZE];

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
            } catch (IOException e) {
                // ignore
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
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

        } catch (IOException e) {
            logger.severe("Failed copying file " + src + " to " + dst);
            e.printStackTrace();
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
        } catch (IOException e) {
            logger.severe("Failed copying file " + srcDir + " to " + dstDir);
            e.printStackTrace();
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
            } catch (IOException e) {
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
        try {
            return URLEncoder.encode(key2, "UTF-8").replace('%', '$');
        } catch (UnsupportedEncodingException e) {
            System.err.println("Failed generating HTML library index.");
            System.err.println(key2);
        e.printStackTrace();
            return "FAILED";
        }
    }

    public static String createPrefix(String category, String key) {
        return category + '_' + key + '_';
    }
}
