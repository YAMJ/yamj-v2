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

import java.net.*;
import java.io.IOException;

/*
 * Web stats class for downloading large files such as trailers
 * See here: http://www.javaspecialists.eu/archive/Issue122.html
 */
public abstract class WebStats {
    private volatile int totalBytes;
    private long start = System.currentTimeMillis();
    
    public int seconds() {
        int result = (int) ((System.currentTimeMillis() - start) / 1000);
        return result == 0 ? 1 : result; // avoid div by zero
    }
    
    public void bytes(int length) {
        totalBytes += length;
    }
    public void print() {
        int kbpersecond = (int) (totalBytes / seconds() / 1024);
        System.out.print("\r");
        System.out.printf("%10d KB%5s%%  (%d KB/s)", totalBytes/1024,
            calculatePercentageComplete(totalBytes), kbpersecond);
    }

    public abstract String calculatePercentageComplete(int bytes);

    public static WebStats make(URL url) throws IOException {
        URLConnection con = url.openConnection();
        int size = con.getContentLength();
        return size == -1 ? new WebStatsBasic() : new WebStatsProgress(size);
    }
}

