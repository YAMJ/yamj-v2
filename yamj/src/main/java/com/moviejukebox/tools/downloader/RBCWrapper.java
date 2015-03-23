/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
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

    private final RBCWrapperDelegate delegate;
    private final long expectedSize;
    private final ReadableByteChannel rbc;
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
