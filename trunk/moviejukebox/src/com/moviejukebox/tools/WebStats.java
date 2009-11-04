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
    protected volatile int totalBytes;
    private long start = System.currentTimeMillis();
    
    public int seconds() {
        int result = (int) ((System.currentTimeMillis() - start) / 1000);
        return result == 0 ? 1 : result; // avoid div by zero
    }
    
    public void bytes(int length) {
        totalBytes += length;
    }
    public void print() {
        System.out.print("\r");
        System.out.printf(statusString());
    }

    public String statusString() {
        int kbpersecond = (int) (totalBytes / seconds() / 1024);
        return String.format("%10d KB%5s%%  (%d KB/s)", totalBytes/1024,
            calculatePercentageComplete(), kbpersecond);
    }

    public abstract String calculatePercentageComplete();

    //actual classes
    private static class WebStatsBasic extends WebStats {
        public String calculatePercentageComplete(){
            return "???";
        }
    }
    public static class WebStatsProgress extends WebStats {
        private long contentLength;
        public WebStatsProgress(long contentLength) {
            this.contentLength = contentLength;
        }
        public String calculatePercentageComplete() {
            return Long.toString((totalBytes * 100L / contentLength));
        }
    }

    public static WebStats make(URL url) throws IOException {
        URLConnection con = url.openConnection();
        con.setRequestProperty("User-Agent", "QuickTime/7.6.2");
        int size = con.getContentLength();
        return size == -1 ? new WebStatsBasic() : new WebStatsProgress(size);
    }
}

