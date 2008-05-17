package com.moviejukebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collection;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;

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
			
			if (!htmlFile.exists() || forceHTMLOverwrite) {
			
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
	
	public void generateMoviesIndexHTML(String rootPath, Library library) {
		Collection<String> keys = library.getIndexes().keySet();

		for (String key : keys) {
			try {
				String filename = rootPath + File.separator + "index_" + key;
				File xmlFile = new File(filename + ".xml");
				File htmlFile = new File(filename + ".html");
				
				if (!htmlFile.exists() || forceHTMLOverwrite) {
					htmlFile.getParentFile().mkdirs();
					
					TransformerFactory tranformerFactory = TransformerFactory.newInstance();
					java.net.URL url = ClassLoader.getSystemResource("index.xsl");
				 
					Source xslSource = new StreamSource(url.openStream());
					Transformer transformer = tranformerFactory.newTransformer(xslSource);
				 
					Source xmlSource = new StreamSource(new FileInputStream(xmlFile));
					Result xmlResult = new StreamResult(new FileOutputStream(htmlFile));
				 
					transformer.transform(xmlSource, xmlResult);
				}
			} catch (Exception e) {
				System.err.println("Failed generating HTML library index.");
				e.printStackTrace();
			}
		}
	}
}
