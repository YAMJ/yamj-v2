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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import org.apache.log4j.Logger;

/**
 *
 * @author altman.matthew
 */
public class XMLHelper {

    protected static Logger logger = Logger.getLogger(XMLHelper.class);

    public static XMLEventReader getEventReader(String url) throws IOException, XMLStreamException {
        WebBrowser wb = new WebBrowser();
        InputStream in = wb.openProxiedConnection(new URL(url)).getInputStream();
        return XMLInputFactory.newInstance().createXMLEventReader(in);
    }

    public static void closeEventReader(XMLEventReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException error) {
                logger.error("Failed closing the event reader.");
                logger.error(SystemTools.getStackTrace(error));
            }
        }
    }

    public static String getCData(XMLEventReader r) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        while (r.peek().isCharacters()) {
            sb.append(r.nextEvent().asCharacters().getData());
        }
        return sb.toString().trim();
    }

    public static int parseInt(XMLEventReader r) throws XMLStreamException {
        int i = 0;
        String val = getCData(r);
        if (val != null && !val.isEmpty()) {
            i = Integer.parseInt(val);
        }
        return i;
    }

    public static float parseFloat(XMLEventReader r) throws XMLStreamException {
        float f = 0.0f;
        String val = getCData(r);
        if (val != null && !val.isEmpty()) {
            try {
                f = NumberFormat.getInstance(Locale.getDefault()).parse(val).floatValue();
            } catch (ParseException error) {
                f = 0.0f;
            }
        }
        return f;
    }

    public static String parseCData(XMLEventReader r) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        XMLEvent e;
        while ((e = r.nextEvent()) instanceof Characters) {
            sb.append(e.toString());
        }
        return HTMLTools.decodeHtml(sb.toString());
    }
}
