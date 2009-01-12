package com.moviejukebox.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author altman.matthew
 */
public class XMLHelper {

    public static XMLEventReader getEventReader(String url) throws IOException, XMLStreamException {
        InputStream in = (new URL(url)).openStream();
        return XMLInputFactory.newInstance().createXMLEventReader(in);
    }

    public static void closeEventReader(XMLEventReader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (XMLStreamException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static String getCData(XMLEventReader r) throws XMLStreamException {
        StringBuffer sb = new StringBuffer();
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
            f = Float.parseFloat(val);
        }
        return f;
    }

    public static List<String> parseList(String input, String delim) {
        List<String> result = new ArrayList<String>();

        StringTokenizer st = new StringTokenizer(input, delim);
        while (st.hasMoreTokens()) {
            String token = st.nextToken().trim();
            if (token.length() > 0) {
                result.add(token);
            }
        }

        return result;
    }
}
