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

import com.moviejukebox.scanner.MovieNFOScanner;
import com.moviejukebox.writer.MovieNFOReader;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Generic set of routines to process the DOM model data Used for read XML files.
 *
 * @author Stuart.Boston
 *
 */
public class DOMHelper {

    private static final Logger logger = Logger.getLogger(DOMHelper.class);
    private static final String DEFAULT_RETURN = "";
    private static final String YES = "yes";
    private static final String TYPE_ROOT = "xml";

    /**
     * Add a child element to a parent element
     *
     * @param doc
     * @param parentElement
     * @param elementName
     * @param elementValue
     */
    public static void appendChild(Document doc, Element parentElement, String elementName, String elementValue) {
        appendChild(doc, parentElement, elementName, elementValue, null);
    }

    /**
     * Add a child element to a parent element with a set of attributes
     *
     * @param doc
     * @param parentElement
     * @param elementName
     * @param elementValue
     * @param childAttributes
     */
    public static void appendChild(Document doc, Element parentElement, String elementName, String elementValue, Map<String, String> childAttributes) {
        Element child = doc.createElement(elementName);
        Text text = doc.createTextNode(elementValue);
        child.appendChild(text);

        if (childAttributes != null && !childAttributes.isEmpty()) {
            for (Map.Entry<String, String> attrib : childAttributes.entrySet()) {
                child.setAttribute(attrib.getKey(), attrib.getValue());
            }
        }

        parentElement.appendChild(child);
    }

    /**
     * Append a child element to a parent element with a single attribute/value pair
     *
     * @param doc
     * @param parentElement
     * @param elementName
     * @param elementValue
     * @param attribName
     * @param attribValue
     */
    public static void appendChild(Document doc, Element parentElement, String elementName, String elementValue, String attribName, String attribValue) {
        Element child = doc.createElement(elementName);
        Text text = doc.createTextNode(elementValue);
        child.appendChild(text);
        child.setAttribute(attribName, attribValue);
        parentElement.appendChild(child);
    }

    /**
     * Convert a DOM document to a string
     *
     * @param doc
     * @return
     * @throws TransformerException
     */
    public static String convertDocToString(Document doc) throws TransformerException {
        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, YES);
        trans.setOutputProperty(OutputKeys.INDENT, YES);

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
        return sw.toString();
    }

    /**
     * Create a blank Document
     *
     * @return a Document
     * @throws ParserConfigurationException
     */
    public static Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        return doc;
    }

    /**
     * Get a DOM document from the supplied file
     *
     * @param xmlFile
     * @return
     * @throws MalformedURLException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public static Document getEventDocFromUrl(File xmlFile) throws MalformedURLException, IOException, ParserConfigurationException, SAXException {
        URL url = xmlFile.toURI().toURL();
        InputStream in = url.openStream();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc;

        // Custom error handler
        db.setErrorHandler(new SaxErrorHandler());

        try {
            doc = db.parse(in);
        } catch (SAXParseException ex) {
            doc = null;
        } finally {
            // close the stream
            in.close();
        }

        if (doc == null) {
            // try wrapping the file in a root
            String wrappedFile = wrapInXml(FileTools.readFileToString(xmlFile));
            doc = db.parse(new InputSource(new StringReader(wrappedFile)));
        }

        doc.getDocumentElement().normalize();
        return doc;
    }

    /**
     * Gets the string value of the tag element name passed
     *
     * @param element
     * @param tagName
     * @return
     */
    public static String getValueFromElement(Element element, String tagName) {
        String returnValue = DEFAULT_RETURN;

        try {
            NodeList nlElement = element.getElementsByTagName(tagName);
            Element tagElement = (Element) nlElement.item(0);
            NodeList tagNodeList = tagElement.getChildNodes();
            returnValue = ((Node) tagNodeList.item(0)).getNodeValue();
        } catch (Exception ignore) {
            return returnValue;
        }

        return returnValue;
    }

    /**
     * Get an element from a parent element node
     *
     * @param eParent
     * @param elementName
     * @return
     */
    public static Element getElementByName(Element eParent, String elementName) {
        NodeList nlParent = eParent.getElementsByTagName(elementName);
        for (int looper = 0; looper < nlParent.getLength(); looper++) {
            if (nlParent.item(looper).getNodeType() == Node.ELEMENT_NODE) {
                return (Element) nlParent.item(looper);
            }
        }
        return null;
    }

    /**
     * Write the Document out to a file using nice formatting
     *
     * @param doc The document to save
     * @param localFile The file to write to
     * @return
     */
    public static boolean writeDocumentToFile(Document doc, String localFilename) {
        return writeDocumentToFile(doc, new File(localFilename));
    }

    /**
     * Write the Document out to a file using nice formatting
     *
     * @param doc The document to save
     * @param localFile The file to write to
     * @return
     */
    public static boolean writeDocumentToFile(Document doc, File localFile) {
        try {
            Transformer trans = TransformerFactory.newInstance().newTransformer();

            // Define the output properties
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            trans.setOutputProperty(OutputKeys.INDENT, YES);
            trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            doc.setXmlStandalone(true);

            trans.transform(new DOMSource(doc), new StreamResult(localFile));
            return true;
        } catch (Exception error) {
            logger.error("Error writing the document to " + localFile);
            logger.error("Message: " + error.getMessage());
            return false;
        }
    }

    /**
     * Override the standard Sax ErrorHandler with this one, to minimise noise about failed parsing errors
     */
    public static class SaxErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) throws SAXException {
//            logger.warn(buildMessage(exception, "Warning"));
            throw new SAXParseException(null, null, exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
//            logger.warn(buildMessage(exception, "Error"));
            throw new SAXParseException(null, null, exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
//            logger.warn(buildMessage(exception, "Fatal Error"));
            throw new SAXParseException(null, null, exception);
        }

        public String buildMessage(SAXParseException exception) {
            return buildMessage(exception, null);
        }

        public String buildMessage(SAXParseException exception, String level) {
            StringBuilder message = new StringBuilder("Parsing ");
            if (StringUtils.isNotBlank(level)) {
                message.append(level);
            } else {
                message.append("Error");
            }
            message.append(" - ");
            message.append(" Line: ").append(exception.getLineNumber());
            message.append(" Column: ").append(exception.getColumnNumber());
            message.append(" Message: ").append(exception.getMessage());
            return message.toString();
        }
    }

    /**
     * Take a file and wrap it in a new root element
     *
     * @param fileString
     * @return
     */
    public static String wrapInXml(String fileString) {
        StringBuilder newOutput = new StringBuilder(fileString);

        int posMovie = fileString.indexOf("<" + MovieNFOReader.TYPE_MOVIE);
        int posTvShow = fileString.indexOf("<" + MovieNFOReader.TYPE_TVSHOW);
        int posEpisode = fileString.indexOf("<" + MovieNFOReader.TYPE_EPISODE);

        boolean posValid = Boolean.FALSE;

        if (posMovie == -1) {
            posMovie = fileString.length();
        } else {
            posValid = Boolean.TRUE;
        }

        if (posTvShow == -1) {
            posTvShow = fileString.length();
        } else {
            posValid = Boolean.TRUE;
        }

        if (posEpisode == -1) {
            posEpisode = fileString.length();
        } else {
            posValid = Boolean.TRUE;
        }

        if (posValid) {
            int pos = Math.min(posMovie, Math.min(posTvShow, posEpisode));
            newOutput.insert(pos, "<" + TYPE_ROOT + ">");
            newOutput.append("</").append(TYPE_ROOT).append(">");
        }

        return newOutput.toString();
    }
}
