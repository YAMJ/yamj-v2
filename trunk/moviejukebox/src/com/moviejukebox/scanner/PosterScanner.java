/**
 * 
 */
package com.moviejukebox.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.FileTools;

/**
 * Scanner for poster files in local directory
 *
 * @author groll.troll
 * @version 1.0, 7 oct. 2008
 */
public class PosterScanner {

	protected static Logger logger = Logger.getLogger("moviejukebox");
	
	protected String[] coverArtExtensions;
	protected String searchForExistingCoverArt;
	protected String fixedCoverArtName;
	protected String coverArtDirectory;
	
	public PosterScanner(Properties props) {
		// We get covert art scanner behaviour
		searchForExistingCoverArt = props.getProperty("poster.scanner.searchForExistingCoverArt", "moviename");
		// We get the fixed name property 
		fixedCoverArtName = props.getProperty("poster.scanner.fixedCoverArtName", "folder");

		// We get valid extensions
		StringTokenizer st = new StringTokenizer(props.getProperty("poster.scanner.coverArtExtensions", ""), ",;| ");
		Collection<String> extensions = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			extensions.add(st.nextToken());
		}
		coverArtExtensions = extensions.toArray(new String[] {});
		
		// We get coverart Directory if needed
		coverArtDirectory = props.getProperty("poster.scanner.coverArtDirectory", "");		
	}
	
	public void scan(String jukeboxDetailsRoot, String tempJukeboxDetailsRoot, Movie movie) {
		
		if (searchForExistingCoverArt.equalsIgnoreCase("no")) {
			 // nothing to do we return
			return;
		}
		
		String localPosterBaseFilename = Movie.UNKNOWN;
		String fullPosterFilename = null;
		File localPosterFile = null;


		if (searchForExistingCoverArt.equalsIgnoreCase("moviename")) {
			localPosterBaseFilename = movie.getBaseName();
		}else if (searchForExistingCoverArt.equalsIgnoreCase("fixedcoverartname")){
			localPosterBaseFilename = fixedCoverArtName;
		}else {
			logger.fine("Wrong value for poster.scanner.searchForExistingCoverArt properties !");
			return;
		}
		
		boolean foundLocalCoverArt= false;

		for (String extension : coverArtExtensions) {
			fullPosterFilename = movie.getFile().getParent();
			if (! coverArtDirectory.equals("")) {
				fullPosterFilename += File.separator + coverArtDirectory;
			}
			fullPosterFilename += File.separator + localPosterBaseFilename + "." + extension;
//			logger.finest("Check if "+ fullPosterFilename + " exists");
			localPosterFile = new File(fullPosterFilename);
			if (localPosterFile.exists()){
//				logger.finest("The file "+ fullPosterFilename + " exists");
				foundLocalCoverArt= true;
				break;
			}			
		}
		
		if (foundLocalCoverArt) {
			String finalDestinationFileName = jukeboxDetailsRoot + File.separator + movie.getPosterFilename();
			String destFileName = tempJukeboxDetailsRoot + File.separator + movie.getPosterFilename();

			File finalDestinationFile = new File(finalDestinationFileName);
			File destFile = new File(destFileName);
			
			if( !finalDestinationFile.exists()  || finalDestinationFile.length() != localPosterFile.length()) {
				FileTools.copyFile(localPosterFile, destFile);
				logger.finer("PosterScanner : " + fullPosterFilename + " has been copied to " + destFileName);				
			}else {
				logger.finer("PosterScanner : " + finalDestinationFileName + " already exists and has same size as " + fullPosterFilename);								
			}
		}else {
			logger.finer("PosterScanner : No local covertArt found for " + movie.getBaseName());								
		}
	}	
}
