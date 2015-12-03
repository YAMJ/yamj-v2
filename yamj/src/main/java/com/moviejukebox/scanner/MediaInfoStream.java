/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.scanner;

import java.io.InputStream;

public class MediaInfoStream implements AutoCloseable {

    private final Process process;
    private final InputStream inputStream;

    public MediaInfoStream(Process process) {
        this.process = process;
        this.inputStream = process.getInputStream();
    }

    public MediaInfoStream(InputStream inputStream) {
        this.process = null;
        this.inputStream = inputStream;
    }
    
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void close() {
        if (process != null) {
            try {
                process.waitFor();
            } catch (Exception ignore) { /* ignore */ }
        }
            
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignore) { /* ignore */ }
        }
        
        if (process != null) {
            try {
                if (process.getErrorStream() != null) {
                    process.getErrorStream().close();  
                }
            } catch (Exception ignore) { /* ignore */ }
            try {
                if (process.getOutputStream() != null) {
                    process.getOutputStream().close();
                }
            } catch (Exception ignore) { /* ignore */ }
            try {
                process.destroy();
            } catch (Exception ignore) { /* ignore */ }
        }
    }
}
