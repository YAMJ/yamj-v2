package com.moviejukebox.writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;

/**
 * Generate HTML pages from XML movies and indexes
 * @author Julien
 */
public class MovieJukeboxHTMLWriter {

	private boolean forceHTMLOverwrite;

	public MovieJukeboxHTMLWriter(boolean forceHTMLOverwrite) {
		this.forceHTMLOverwrite = forceHTMLOverwrite;
	}
	
	public void generateMovieDetailsHTML(String rootPath, Movie movie) {
		try {
			String filename = rootPath + File.separator + movie.getBaseName();
			File xmlFile = new File(filename + ".xml");
			File htmlFile = new File(filename + ".html");
			
			if (!htmlFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
			
				htmlFile.getParentFile().mkdirs();
				
				TransformerFactory tranformerFactory = TransformerFactory.newInstance();
				java.net.URL url = ClassLoader.getSystemResource("detail.xsl");
			 
				Source xslSource = new StreamSource(url.openStream());
				Transformer transformer = tranformerFactory.newTransformer(xslSource);
			 
				Source xmlSource = new StreamSource(new FileInputStream(xmlFile));
				Result xmlResult = new StreamResult(new FileOutputStream(htmlFile));
			 
				transformer.transform(xmlSource, xmlResult);
			}
		} catch(Exception e) {
			System.err.println("Failed generating HTML for movie " + movie);
			e.printStackTrace();
		}
	}
	
	public void generateMoviesIndexHTML(String rootPath, String detailsDirName, Library library) {
		ArrayList<String> keys = new ArrayList<String>();
		for(HashMap<String, List<Movie>> index : library.getIndexes().values()) {
			keys.addAll(index.keySet());
		}

		for (String key : keys) {
			try {
				String filename = rootPath + File.separator +  detailsDirName + File.separator + "index_" + key;
				File xmlFile = new File(filename + ".xml");
				File htmlFile = new File(filename + ".html");
				
				htmlFile.getParentFile().mkdirs();
				
				TransformerFactory tranformerFactory = TransformerFactory.newInstance();
				java.net.URL url = ClassLoader.getSystemResource("index.xsl");
			 
				Source xslSource = new StreamSource(url.openStream());
				Transformer transformer = tranformerFactory.newTransformer(xslSource);
			 
				Source xmlSource = new StreamSource(new FileInputStream(xmlFile));
				Result xmlResult = new StreamResult(new FileOutputStream(htmlFile));
			 
				transformer.transform(xmlSource, xmlResult);
			} catch (Exception e) {
				System.err.println("Failed generating HTML library index.");
				e.printStackTrace();
			}
		}
		
		if (keys.size()>0) {
			try {
				File htmlFile = new File(rootPath + File.separator + "index.htm");
				htmlFile.getParentFile().mkdirs();
					
				XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
				XMLStreamWriter writer = outputFactory.createXMLStreamWriter(
						new FileOutputStream(htmlFile), "UTF-8");

				writer.writeStartDocument();
				writer.writeStartElement("html");
				writer.writeStartElement("head");
				
				writer.writeStartElement("meta");
				writer.writeAttribute("name", "Author");
				writer.writeAttribute("content", "MovieJukebox");
				writer.writeEndElement();
				
				writer.writeStartElement("meta");
				writer.writeAttribute("HTTP-EQUIV", "REFRESH");
				writer.writeAttribute("content", "0; url=" + detailsDirName+"/index_" + keys.get(0) + ".html");
				writer.writeEndElement();

				writer.writeEndElement();					
				writer.writeEndElement();					
			} catch (Exception e) {
				System.err.println("Failed generating HTML library index.");
				e.printStackTrace();
			}
		}
	}
}
