package com.moviejukebox.tools;

public class WebStatsProgress extends WebStats {
    private final long contentLength;
    public WebStatsProgress(long contentLength) {
        this.contentLength = contentLength;
    }
    public String calculatePercentageComplete(int totalBytes) {
        return Long.toString((totalBytes * 100L / contentLength));
    }
}