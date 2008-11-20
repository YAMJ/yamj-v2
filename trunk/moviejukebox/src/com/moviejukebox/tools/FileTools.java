package com.moviejukebox.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class FileTools {

    private static Logger logger = Logger.getLogger("moviejukebox");
    final static int BUFF_SIZE = 100000;
    final static byte[] buffer = new byte[BUFF_SIZE];

    private static final String BOM1 = "" + (char)239 + (char)187 + (char)191;
    private static final String BOM2 = "" + (char)254 + (char)255;
    private static final String BOM3 = "" + (char)255 + (char)254;
    private static final String BOM4 = "" + (char)00 + (char)00 + (char)254 + (char)255;
    private static final String BOM5 = "" + (char)254 + (char)255 + (char)00 + (char)00;

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
    
    public static String stripBOM(String input) {
        String output = input;
        if (input != null) {
            if (output.startsWith(BOM1)) {
                output = output.substring(BOM1.length());
            } else if (output.startsWith(BOM2)) {
                output = output.substring(BOM2.length());
            } else if (output.startsWith(BOM3)) {
                output = output.substring(BOM3.length());
            } else if (output.startsWith(BOM4)) {
                output = output.substring(BOM4.length());
            } else if (output.startsWith(BOM5)) {
                output = output.substring(BOM5.length());
            }
        }
        return output;
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
        return stripBOM(out.toString());
    }
    
}
