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
package com.moviejukebox.tools.downloader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * This is the RBCWrapper class
 *
 * It will get the progress as a percentage, if known, otherwise it will return
 * -1.0 to indicate indeterminate progress.
 *
 * Taken from http://stackoverflow.com/a/11068356/443283
 *
 * @author stuart.boston
 */
public final class RBCWrapper implements ReadableByteChannel {

    private RBCWrapperDelegate delegate;
    private long expectedSize;
    private ReadableByteChannel rbc;
    private long readSoFar;

    RBCWrapper(ReadableByteChannel rbc, long expectedSize, RBCWrapperDelegate delegate) {
        this.delegate = delegate;
        this.expectedSize = expectedSize;
        this.rbc = rbc;
    }

    @Override
    public void close() throws IOException {
        rbc.close();
    }

    public long getBytesReadSoFar() {
        return readSoFar;
    }

    public long getKbReadSoFar() {
        return readSoFar / 1024;
    }

    @Override
    public boolean isOpen() {
        return rbc.isOpen();
    }

    @Override
    public int read(ByteBuffer bb) throws IOException {
        int n;
        double progress;

        if ((n = rbc.read(bb)) > 0) {
            readSoFar += n;
            progress = expectedSize > 0 ? (double) readSoFar / (double) expectedSize * 100.0 : -1.0;
            delegate.rbcProgressCallback(this, progress);
        }

        return n;
    }
}
