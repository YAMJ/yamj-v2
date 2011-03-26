/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import org.apache.log4j.Logger;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author matthew.altman
 */
public class XMLWriter {

    private int indent = 4;
    private String indentString = "    ";
    private int indentCount = 0;

    private boolean wasPrevElement = true;

    private ByteArrayOutputStream outputStream; 
    private FileOutputStream fileStream;
    private PrintWriter printWriter;
    private XMLStreamWriter writer;

    private static final Logger logger = Logger.getLogger("moviejukebox");
    private static XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

    public XMLWriter(File xmlFile) {
        try {
            // output stream is a byte array, so flushing has no effect 
            outputStream = new ByteArrayOutputStream(128*1024);
            printWriter = new PrintWriter(outputStream);
            synchronized(outputFactory){
                writer = outputFactory.createXMLStreamWriter(outputStream, "UTF-8");                
            }
            // the content is saved only at the end, but created here for early exception detection
            // no need for buffered write, because there is only 1 step
            fileStream = new FileOutputStream(xmlFile);
        } catch (Exception ex) {
            logger.error("Error creating XML file: " + xmlFile);
        }
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        writer.writeStartDocument(encoding, version);
    }

    public void writeEndDocument() throws XMLStreamException {
        writer.writeEndDocument();
    }

    public void close() {
        try {
            writer.flush();
            writer.close();
            printWriter.close();
            outputStream.close();
            //ByteArrayOutputStream can save after closing 
            outputStream.writeTo(fileStream);
            fileStream.close();
        } catch (Exception ignore) {}
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        if (wasPrevElement) {
            writer.writeCharacters("");
            printTab();
        }

        writer.writeStartElement(localName);
        indentCount++;
        wasPrevElement = true;
    }
    
    public void writeComment(String comment) throws XMLStreamException {
        writer.writeComment(comment);
    }

    public void writeEndElement() throws XMLStreamException {
        indentCount--;
        if (wasPrevElement) {
            printTab();
        }
        writer.writeEndElement();
        wasPrevElement = true;
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        writer.writeAttribute(localName, value);
    }

    public void writeCharacters(String text) throws XMLStreamException {
        writer.writeCharacters(text);
        wasPrevElement = false;
    }

    private void printTab() throws XMLStreamException {
        writer.flush();
        printWriter.print("\n");
        for (int i = 0; i < indentCount; i++) {
            printWriter.print(indentString);
        }
        printWriter.flush();
    }

    @SuppressWarnings("unused")
    private int getIndent() {
        return indent;
    }

    @SuppressWarnings("unused")
    private void setIndent(int indent) {
        this.indent  = indent;
        indentString = "";
        for (int i = 0; i < indent; i++) {
            indentString += " ";
        }
    }
}
