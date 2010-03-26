/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;
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

    private PrintWriter printWriter;
    private XMLStreamWriter writer;

    private static final Logger logger = Logger.getLogger("moviejukebox");

    public XMLWriter(File xmlFile) {
        try {
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            FileOutputStream outputStream = new FileOutputStream(xmlFile);
            printWriter = new PrintWriter(outputStream);
            writer = outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
        } catch (Exception ex) {
            logger.severe("Error creating XML file: " + xmlFile);
        }
    }

    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        writer.writeStartDocument(encoding, version);
        writer.flush();
    }

    public void writeEndDocument() throws XMLStreamException {
        writer.writeEndDocument();
        writer.flush();
    }

    public void flush() throws XMLStreamException {
        writer.flush();
    }

    public void close() {
        try {
            writer.close();
            printWriter.close();
        } catch (Exception ignore) {}
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        if (wasPrevElement) {
            writer.writeCharacters("");
            printTab();
        }

        writer.writeStartElement(localName);
        indentCount++;
        writer.flush();
        wasPrevElement = true;
    }

    public void writeEndElement() throws XMLStreamException {
        indentCount--;
        if (wasPrevElement) {
            printTab();
        }
        writer.writeEndElement();
        writer.flush();
        wasPrevElement = true;
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        writer.writeAttribute(localName, value);
        writer.flush();
    }

    public void writeCharacters(String text) throws XMLStreamException {
        writer.writeCharacters(text);
        writer.flush();
        wasPrevElement = false;
    }

    private void printTab() {
        printWriter.print("\n");
        for (int i = 0; i < indentCount; i++) {
            printWriter.print(indentString);
        }
        printWriter.flush();
    }

    public int getIndent() {
        return indent;
    }

    public void setIndent(int indent) {
        this.indent  = indent;
        indentString = "";
        for (int i = 0; i < indent; i++) {
            indentString += " ";
        }
    }
}
