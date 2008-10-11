package com.moviejukebox.writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * Generate HTML pages from XML movies and indexes
 * @author Julien
 */
public class MovieFlowWriter {

	private boolean forceHTMLOverwrite;
	private String skinHome;
	private String homePage;

	public MovieFlowWriter() {
            forceHTMLOverwrite = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.forceHTMLOverwrite", "false"));
            skinHome = PropertiesUtil.getProperty("mjb.skin.dir", "./skins/default");
            homePage = PropertiesUtil.getProperty("mjb.skin.homePage", "movieflow") + ".html";
	}

	/**
	 * Write the set of index JS files for the library
	 * @throws IOException 
	 */
	public void writeIndexJS(String rootPath, String detailsDirName, Library library) {
		try  {
			for ( Map.Entry<String, Map<String,List<Movie>>> category : library.getIndexes().entrySet())
			{
				String categoryName = category.getKey();
				Map< String, List<Movie>> index = category.getValue();
				
				for ( Map.Entry< String, List<Movie>> group : index.entrySet()) {
					String key = group.getKey(); 
					List<Movie> movies = group.getValue();
		
					writeJSIndexPage(library, movies, rootPath, categoryName, key);
				}
			}
		} catch(Exception e) {
			System.err.println("Failed generating MovieFlow javascript files.");
			e.printStackTrace();
		}
	}

	public void writeJSIndexPage(Library library, Collection<Movie> movies, String rootPath, String categoryName, String key) throws XMLStreamException, IOException {
		File jsFile = new File(rootPath, "sel" + key.toLowerCase() + ".js");
		jsFile.getParentFile().mkdirs();
	
		PrintWriter writer = new PrintWriter(jsFile);
		
		writer.println("function sel" + key.toLowerCase() + "()");
		writer.println("{");
		writer.println("//---- set the number of movies do not forget to count Zero and make sure you start at zero ---------");
		writer.println("total = " + movies.size() + ";");
		writer.println();
		writer.println("//----- load the movie titles");
	
		int i=0;
		for (Movie movie : movies) {
			writer.println();
			
			StringBuffer sb = new StringBuffer(movie.getTitleSort());
			if (movie.getYear() != null && !movie.getYear().isEmpty() && !movie.getYear().equalsIgnoreCase("Unknown")) {
				sb.append(" [").append(movie.getYear()).append("]");
			}
				
			writer.println("title[" + i + "]=\"" + sb.toString() + "\";"); 
			writer.println("images[" + i + "]=\"" + movie.getThumbnailFilename() + "\";"); 
			writer.println("url[" + i + "]=\"" + movie.getBaseName() + ".html\";"); 
			i++;
		}
	
		writer.write("}");
		writer.close();
	}

	public void writeSingleIndexPage(String rootPath, String detailsDirName, String categoryName, String key, int page) throws TransformerFactoryConfigurationError {
		try {
			File detailsDir = new File( rootPath, detailsDirName );
			detailsDir.mkdirs();

			String prefix = categoryName + '_' + key + '_';
			String filename = prefix + page;

			File xmlFile = new File(detailsDir, filename + ".xml");
			File htmlFile = new File(detailsDir, filename + ".html");
			
			TransformerFactory tranformerFactory = TransformerFactory.newInstance();
			
			Source xslSource = new StreamSource(new File(skinHome, "index.xsl"));
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
