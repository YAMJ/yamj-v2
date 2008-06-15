package com.moviejukebox.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
	
	public static void copy(InputStream is, OutputStream os) throws IOException {
		try {
			while (true) {
				synchronized (buffer) {
					int amountRead = is.read(buffer);
					if (amountRead == -1)
						break;
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

                if (src.isFile())
                        copy(new FileInputStream(src), new FileOutputStream(dstDir));
                else {
                        File[] contentList = src.listFiles();
                        if (contentList!=null) {
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


}
