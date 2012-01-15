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
