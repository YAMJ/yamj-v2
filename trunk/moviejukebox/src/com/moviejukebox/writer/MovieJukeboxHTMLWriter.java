package com.moviejukebox.writer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
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
import com.moviejukebox.model.MovieFile;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Generate HTML pages from XML movies and indexes
 * @author Julien
 */
public class MovieJukeboxHTMLWriter {

	private boolean forceHTMLOverwrite;
	private int nbMoviesPerPage;
	private String skinHome;
	private String homePage;

	public MovieJukeboxHTMLWriter(Properties props) {
		forceHTMLOverwrite = Boolean.parseBoolean(props.getProperty("mjb.forceHTMLOverwrite", "false"));
		nbMoviesPerPage = Integer.parseInt(props.getProperty("mjb.nbThumbnailsPerPage", "10"));
		skinHome = props.getProperty("mjb.skin.dir", "./skins/default");
		homePage = props.getProperty("mjb.homePage", "Other_All_1") + ".html";
	}

	public void generateMovieDetailsHTML(String rootPath, String tempRootPath, Movie movie) {
		try {
			String tempFilename = tempRootPath + File.separator + movie.getBaseName();
			File tempXmlFile = new File(tempFilename + ".xml");
			File finalHtmlFile = new File(rootPath + File.separator + movie.getBaseName() + ".html");
			File tempHtmlFile = new File(tempFilename + ".html");

			if (!finalHtmlFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
			
				tempHtmlFile.getParentFile().mkdirs();
				
				TransformerFactory tranformerFactory = TransformerFactory.newInstance();

				Source xslSource = new StreamSource(new File(skinHome + File.separator + "detail.xsl"));
				Transformer transformer = tranformerFactory.newTransformer(xslSource);
			 
				Source xmlSource = new StreamSource(new FileInputStream(tempXmlFile));
				Result xmlResult = new StreamResult(new FileOutputStream(tempHtmlFile));
			 
				transformer.transform(xmlSource, xmlResult);
			}
		} catch(Exception e) {
			System.err.println("Failed generating HTML for movie " + movie);
			e.printStackTrace();
		}
	}

        public void generatePlaylist(String rootPath, String tempRootPath, Movie movie) {
            try {
                if (movie.getFiles().size() > 1) {
                    String tempFilename = tempRootPath + File.separator + movie.getBaseName();
                    File finalPlaylistFile = new File(rootPath + File.separator + movie.getBaseName() + ".playlist.jsp");
                    File tempPlaylistFile = new File(tempFilename + ".playlist.jsp");

                    if (!finalPlaylistFile.exists() || forceHTMLOverwrite || movie.isDirty()) {
                        tempPlaylistFile.getParentFile().mkdirs();

                        PrintWriter writer = new PrintWriter(tempPlaylistFile);

                        for (MovieFile part : movie.getFiles()) {
                            // write one line each in the format "name|0|0|path" replacing an | that may exist in the title
                            writer.println(movie.getTitle().replace('|', ' ') + " " + part.getPart() + "|0|0|" + part.getFilename() + "|");
                        }
                        writer.flush();
                        writer.close();
                    }
                }
            } catch(Exception e) {
                System.err.println("Failed generating playlist for movie " + movie);
                e.printStackTrace();
            }
        }
    
	public void generateMoviesCategoryHTML( String rootPath, String detailsDirName, Library library )
	{
		try
		{
			File detailsFolder = new File( rootPath, detailsDirName );
			String filename = "Categories";
			File xmlFile = new File( detailsFolder, filename + ".xml");
			File htmlFile = new File( detailsFolder, filename + ".html");

			htmlFile.getParentFile().mkdirs();

			TransformerFactory tranformerFactory = TransformerFactory.newInstance();

			Source xslSource = new StreamSource(new File( skinHome, "categories.xsl"));
			Transformer transformer = tranformerFactory.newTransformer(xslSource);

			Source xmlSource = new StreamSource(new FileInputStream(xmlFile));
			Result xmlResult = new StreamResult(new FileOutputStream(htmlFile));

			transformer.transform(xmlSource, xmlResult);
		}
		catch ( Exception e )
		{
			System.err.println("Failed generating HTML library category index.");
			e.printStackTrace();
		}
	}
	
	public void generateMoviesIndexHTML(String rootPath, String detailsDirName, Library library) {
		for ( Map.Entry<String, Map<String,List<Movie>>> category : library.getIndexes().entrySet())
		{
			String categoryName = category.getKey();
			Map< String, List<Movie>> index = category.getValue();
			
			for (Map.Entry<String, List<Movie>> indexEntry : index.entrySet()) {
				String key = indexEntry.getKey();
				List<Movie> movies = indexEntry.getValue();
				int nbPages;
				int movieCount = movies.size();
				if (movieCount % nbMoviesPerPage != 0) {
					nbPages = 1 + movieCount / nbMoviesPerPage;
				} else {
					nbPages = movieCount / nbMoviesPerPage;
				}
				for (int page=1 ; page<=nbPages ; page++) {
					writeSingleIndexPage(rootPath, detailsDirName, categoryName, key, page);
				}
			}
		}
		try {
			File htmlFile = new File(rootPath, "index.htm");
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
			writer.writeAttribute("content", "0; url=" + detailsDirName+ '/' + homePage );
			writer.writeEndElement();

			writer.writeEndElement();					
			writer.writeEndElement();					
		} catch (Exception e) {
			System.err.println("Failed generating HTML library index.");
			e.printStackTrace();
		}
	}

	private void writeSingleIndexPage(String rootPath, String detailsDirName, String categoryName, String key, int page) throws TransformerFactoryConfigurationError {
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
