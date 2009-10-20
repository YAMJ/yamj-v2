/**
 * @(#)FileProperties.java 1.0 26.01.06 (dd.mm.yy)
 *
 * Copyright (2003) Mediterranean
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Boston, MA 02111.
 * 
 * Contact: mediterranean@users.sourceforge.net
 * -----------------------------------------------------
 * gaelead modifications :
 * - org.apache.log4j.Logger switched to java.util.logging.Logger
 * - removed all unused code
 **/
package net.sf.xmm.moviemanager.fileproperties;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;
import java.util.logging.Logger;

abstract class FileProperties {

    static Logger log = Logger.getLogger("moviejukebox");
    protected boolean supported = false;
    protected boolean errorOccured = false;
    /**
     * The video duration (seconds).
     */
    private int _duration = -1;
    protected String filePath = "";

    /**
     * Returns the duration.
     */
    protected int getDuration() {
        return _duration;
    }

    /**
     * Sets the duration.
     */
    protected void setDuration(int duration) {
        _duration = duration;
    }

    protected boolean isSupported() {
        return supported;
    }

    /**
     * Processes a file from the given DataInputStream.
     */
    protected void process(RandomAccessFile dataStream) throws Exception {
    }

    /**
     * Reads an unsigned 8-bit integer.
     */
    protected int readUnsignedByte(byte[] b, int offset) throws Exception {
        return b[offset];
    }

    /**
     * Reads an unsigned 16-bit integer.
     */
    protected int readUnsignedInt16(byte[] b, int offset) throws Exception {
        return (b[offset] | (b[offset + 1] << 8));
    }

    /**
     * Reads an unsigned 32-bit integer.
     */
    protected int readUnsignedInt32(byte[] b, int offset) throws Exception {
        return (readUnsignedInt16(b, offset) | (readUnsignedInt16(b, offset + 2) << 16));
    }

    /**
     * Returns a 16-bit integer.
     */
    protected int getUnsignedInt16(int byte1, int byte2) throws Exception {
        return (byte2 | (byte1 << 8));
    }

    /**
     * Returns a 16-bit integer.
     */
    protected int getUnsignedInt16(byte byte1, byte byte2) throws Exception {
        return (new Byte(byte2).intValue() | new Byte(byte1).intValue() << 8);
    }

    /**
     * Returns an unsigned 32-bit integer.
     */
    protected int getUnsignedInt32(byte byte1, byte byte2) throws Exception {
        return (new Byte(byte2).intValue() | new Byte(byte1).intValue() << 16);
    }

    /**
     * Returns an unsigned 32-bit integer.
     */
    protected int getUnsignedInt32(int byte1, int byte2) throws Exception {
        return (byte1 | byte2 << 16);
    }

    /**
     * Reads an unsigned byte and returns its int representation.
     */
    protected int readUnsignedByte(RandomAccessFile dataStream)
            throws Exception {
        int data = dataStream.readUnsignedByte();
        if (data == -1) {
            throw new Exception("Unexpected end of stream.");
        }
        return data;
    }

    /**
     * Reads n unsigned bytes and returns it in an int[n].
     */
    protected int[] readUnsignedBytes(RandomAccessFile dataStream, int n)
            throws Exception {
        int[] data = new int[n];
        for (int i = 0; i < data.length; i++) {
            data[i] = readUnsignedByte(dataStream);
        }
        return data;
    }

    /**
     * Reads an unsigned 16-bit integer.
     */
    protected int readUnsignedInt16(RandomAccessFile dataStream)
            throws Exception {
        return (readUnsignedByte(dataStream) | (readUnsignedByte(dataStream) << 8));
    }

    /**
     * Reads an unsigned 32-bit integer.
     */
    protected int readUnsignedInt32(RandomAccessFile dataStream)
            throws Exception {
        return (readUnsignedInt16(dataStream) | (readUnsignedInt16(dataStream) << 16));
    }

    /**
     * Discards n bytes.
     */
    protected boolean skipBytes(RandomAccessFile dataStream, int n)
            throws Exception {

        int len = (int) (dataStream.length() - dataStream.getFilePointer());

        if (n > 0 && n < 10000 && len > n) {
            readUnsignedBytes(dataStream, n);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Reverses the byte order
     */
    int changeEndianness(int num) {
        return (num >>> 24) | (num << 24) | ((num << 8) & 0x00FF0000 | ((num >> 8) & 0x0000FF00));
    }

    /**
     * Returns the ascii value of id
     */
    String fromByteToAscii(int id, int numberOfBytes) throws Exception {
        /* Transforms the id in a string... */
        StringBuffer buffer = new StringBuffer(4);

        for (int i = 0; i < numberOfBytes; i++) {
            int c = id & 0xff;
            buffer.append((char) c);
            id >>= 8;
        }
        return new String(buffer);
    }

    /**
     * Returns the decimal value of a specified number of bytes from a specific
     * part of a byte.
     */
    int getDecimalValue(int[] bits, int start, int stop, boolean printBits) {

        String dec = "";

        for (int i = start; i >= stop; i--) {
            dec += bits[i];
        }

        if (printBits) {
            log.finest("dec:" + dec);
        }

        return Integer.parseInt(dec, 2);
    }

    /**
     * Returns an array containing the bits from the value.
     */
    int[] getBits(int value, int numberOfBytes) {

        int[] bits = new int[numberOfBytes * 8];

        for (int i = bits.length - 1; i >= 0; i--) {
            bits[i] = (value >>> i) & 1;
        }
        return bits;
    }

    /**
     * Debugging.
     */
    void printBits(int[] bits) {

        for (int i = bits.length - 1; i >= 0; i--) {
            System.out.print(bits[i]);

            if ((i) % 8 == 0) {
                System.out.print(" ");
            }
        }

        System.out.print(" ");
    }

    /**
     * Searches in the inputStream stream the name following the string id
     * (Separated by a \t).
     */
    protected String findName(InputStream stream, String id) throws Exception {

        if (stream == null || id == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream));
        String line = null;

        while ((line = reader.readLine()) != null) {
            if (line.length() > 0) {
                StringTokenizer tokenizer = new StringTokenizer(line, "\t");
                if (tokenizer.countTokens() > 0 && id.compareToIgnoreCase(tokenizer.nextToken()) == 0) {
                    return tokenizer.nextToken();
                }
            }
        }
        return "";
    }
}
