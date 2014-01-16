/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
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
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.tools;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Decodes and encodes codec strings for XML
 */
public class UrlCodecAdapter
        extends XmlAdapter<String, String> {

    @Override
    public String unmarshal(String s) {
        return s == null ? null : HTMLTools.decodeUrl(s);
    }

    @Override
    public String marshal(String c) {
        return c == null ? null : HTMLTools.encodeUrl(c);
    }
}
