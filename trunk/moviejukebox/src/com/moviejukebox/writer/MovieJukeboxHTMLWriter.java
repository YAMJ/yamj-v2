package com.moviejukebox.writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
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
	private int nbMoviesPerPage;
	private String skinHome;

	public MovieJukeboxHTMLWriter(Properties props) {
		forceHTMLOverwrite = Boolean.parseBoolean(props.getProperty("mjb.forceHTMLOverwrite", "false"));
		nbMoviesPerPage = Integer.parseInt(props.getProperty("mjb.nbThumbnailsPerPage", "10"));
		skinHome = props.getProperty("mjb.skin.dir", "./skins/default");
	}

	public void generateMovieDetailsHTML(String rootPath, Movie movie) {
		try {
			String filename = rootPath + File.separator + movie.getBaseName();
			File xmlFile = new File(filename + ".xml");
			File htmlFile = new File(filename + ".html");
			
			if (!htmlFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
			
				htmlFile.getParentFile().mkdirs();
				
				TransformerFactory tranformerFactory = TransformerFactory.newInstance();

				Source xslSource = new StreamSource(new File(skinHome + File.separator + "detail.xsl"));
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
			int nbPages;
			if (library.getMoviesByIndexKey(key).size() % nbMoviesPerPage != 0) {
				nbPages = 1 + library.getMoviesByIndexKey(key).size() / nbMoviesPerPage;
			} else {
				nbPages = library.getMoviesByIndexKey(key).size() / nbMoviesPerPage;
			}
			for (int page=1 ; page<=nbPages ; page++) {
				writeSingleIndexPage(rootPath, detailsDirName, key, page);
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
				writer.writeAttribute("content", "0; url=" + detailsDirName+"/index_" + keys.get(0) + "_1.html");
				writer.writeEndElement();

				writer.writeEndElement();					
				writer.writeEndElement();					
			} catch (Exception e) {
				System.err.println("Failed generating HTML library index.");
				e.printStackTrace();
			}
		}
	}

	private void writeSingleIndexPage(String rootPath, String detailsDirName, String key, int page) throws TransformerFactoryConfigurationError {
		try {
			String filename = rootPath + File.separator +  detailsDirName + File.separator + "index_" + key + "_" + page;
			File xmlFile = new File(filename + ".xml");
			File htmlFile = new File(filename + ".html");
			
			htmlFile.getParentFile().mkdirs();
			
			TransformerFactory tranformerFactory = TransformerFactory.newInstance();
			
			Source xslSource = new StreamSource(new File(skinHome + File.separator + "index.xsl"));
			Transformer transformer = tranformerFactory.newTransformer(xslSource);
		 
			Source xmlSource = new StreamSource(new FileInputStream(xmlFile));
			Result xmlResult = new StreamResult(new FileOutputStream(htmlFile));
		 
			transformer.transform(xmlSource, xmlResult);
		} catch (Exception e) {
			System.err.println("Failed generating HTML library index.");
			e.printStackTrace();
		}
	}
}
