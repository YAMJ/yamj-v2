package com.moviejukebox.scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.StringTokenizer;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;

/**
 * Simple movie filename scanner. 
 * Scans a movie filename for keywords commonly used in scene released video files.
 * 
 * Main pattern for file scanner is the following: 
 * 
 *                <MovieTitle>[Keyword*].<container>
 *         
 *    * The movie title is in the first position of the filename.
 *    * it is followed by zero or more keywords.
 *    * the file extension match the container name.
 * 
 * @author jjulien
 * @author quickfinga
 */
public class MovieFilenameScanner {
	
	protected String[] skipKeywords;
	protected int firstKeywordIndex = 0;

	public MovieFilenameScanner(Properties props) {
		StringTokenizer st = new StringTokenizer(props.getProperty("filename.scanner.skip.keywords", ""), ",;| ");
		Collection<String> keywords = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			keywords.add(st.nextToken());
		}
		skipKeywords = keywords.toArray(new String[] {});
	}
	
	public void scan(Movie movie) {
		File fileToScan = movie.getFile();
		String filename = fileToScan.getName();
		
		firstKeywordIndex = filename.indexOf("[");
		firstKeywordIndex = (firstKeywordIndex==-1)?filename.length():firstKeywordIndex;

		Collection<MovieFile> movieFiles = movie.getFiles();
		for (MovieFile movieFile : movieFiles) {
			movieFile.setPart(getPart(filename));	
		}

		if (fileToScan.isFile()) {
			movie.setAudioCodec(getAudioCodec(filename));
			movie.setContainer(getContainer(filename));
			movie.setFps(getFPS(filename));
			movie.setSubtitles(hasSubtitles(fileToScan));
			movie.setVideoCodec(getVideoCodec(filename));
			movie.setVideoOutput(getVideoOutput(filename));
			movie.setVideoSource(getVideoSource(filename));
			movie.setLanguage(getLanguage(filename));
		} else {
			// For DVD images
			movie.setAudioCodec(getAudioCodec(filename));
			movie.setContainer("DVD");
			movie.setFps(getFPS(filename));
			movie.setSubtitles(hasSubtitles(fileToScan));
			movie.setVideoCodec("MPEG2");
			movie.setVideoOutput(getVideoOutput(filename));
			movie.setVideoSource("DVD");
			movie.setLanguage(getLanguage(filename));
		}
		
		// Skip some keywords
		findKeyword(filename, skipKeywords);
		
		// Update the movie file with interpreted movie data
		updateTVShow(filename, movie);
		updateMovie(filename, movie);
	}

	/**
	 * Get the main audio track codec if any
	 * @param filename movie's filename to scan
	 * @return the audio codec name or Unknown if not found
	 */
	protected String getAudioCodec(String filename) {
		return findKeyword(
				filename.toUpperCase(), 
				new String[] { "AC3", "DTS" });
	}
		
	/**
	 * Get the specified filenames video container. 
	 * Simply return the movie file's extension.
	 * @param filename movie's filename to scan
	 * @return the container
	 */
	protected String getContainer(String filename) {
		int lastDotIndex = filename.lastIndexOf(".");
		updateFirstKeywordIndex(lastDotIndex);
		return filename.substring(lastDotIndex+1).toUpperCase();
	}

	/**
	 * @return the movie file frame rate when specified in the filename.
	 * @param filename movie's filename to scan
	 */
	protected int getFPS(String filename) {
		if (hasKeyword(filename, new String[] {"23p", "p23"})) return 23;
		if (hasKeyword(filename, new String[] {"24p", "p24"})) return 24;
		if (hasKeyword(filename, new String[] {"25p", "p25"})) return 25;
		if (hasKeyword(filename, new String[] {"29p", "p29"})) return 29;
		if (hasKeyword(filename, new String[] {"30p", "p30"})) return 30;
		if (hasKeyword(filename, new String[] {"50p", "p50"})) return 50;
		if (hasKeyword(filename, new String[] {"59p", "p59"})) return 59;
		if (hasKeyword(filename, new String[] {"60p", "p60"})) return 60;
		return 60;
	}

	/**
	 * @return the movie file language when specified in the filename.
	 * @param filename movie's filename to scan.
	 */
	protected String getLanguage(String filename) {
		String f = filename.toUpperCase();
        
		f = f.replace("-", ".");
		f = f.replace("_", ".");
		f = f.replace("[", ".");
		f = f.replace("]", ".");
		f = f.replace("(", ".");
		f = f.replace(")", ".");

		if (hasKeyword(f, new String[] {".FRA.",".FR.",".FRENCH.", ".VF.", }))
			return "French";

		if (hasKeyword(f, new String[] {".GER.",".DE.",".GERMAN."}))
			return "German";
	
		if (hasKeyword(f, new String[] {".ITA.",".IT.",".ITALIAN."}))
			return "Italian";
	
		if (hasKeyword(f, new String[] {".SPA.",".ES.",".SPANISH."}))
			return "Spanish";
	
		if (hasKeyword(f, new String[] {".ENG.",".EN.",".ENGLISH."}))
			return "English";
	
		if (hasKeyword(f, new String[] {".POR.",".PT.",".PORTUGUESE."}))
			return "Portuguese";
	
		if (hasKeyword(f, new String[] {".RUS.",".RU.",".RUSSIAN."}))
			return "Russian";
	
		if (hasKeyword(f, new String[] {".VO.", ".VOSTFR."}))
			return "VO";

		if (hasKeyword(f, new String[] {".DL."}))
			return "Dual Language";
	   
		return "Unknown";
	}

	/**
	 * @return the specified movie file's title.
	 * @param filename movie's filename to scan.
	 */
	protected String getName(String filename) {
		String name = filename.substring(0, firstKeywordIndex);
		name = name.replace(".", " ");
		name = name.replace("_", " ");
		name = name.replace("-", " ");
		name = name.replace("[", " ");
		name = name.replace("]", " ");
		name = name.replace("(", " ");
		name = name.replace(")", " ");
		return name.trim();
	}

	protected int getPart(String filename) {
		String f = filename.toUpperCase();
		int index = f.indexOf("CD");
		if(index!= -1) {
			updateFirstKeywordIndex(index);
			StringTokenizer st = new StringTokenizer(f.substring(index+2), " {[-|_)]},.");
			try {
				return Integer.parseInt(st.nextToken());
			} catch (Exception e) {
				return 1;
			}
		}
		return 1;
	}
	
	protected String getVideoCodec(String filename) {
		String f = filename.toUpperCase();
		if (hasKeyword(f, "XVID")) return "XviD";
		if (hasKeyword(f, "DIVX")) return "DivX";
		if (hasKeyword(f, new String[] {"H264", "H.264", "X264" })) return "H.264";
		return "Unknown";
	}

	protected String getVideoOutput(String filename) {
		
		String videoOutput = findKeyword(filename, new String[] { "720p", "1080i", "1080p" });
		
		int fps = getFPS(filename);
		if (!videoOutput.equalsIgnoreCase("Unknown")) {
			switch (fps) {
				case 23: videoOutput = "1080p 23.976Hz"; break;
				case 24: videoOutput = "1080p 24Hz"; break;
				case 25: videoOutput = "1080p 25Hz"; break;
				case 29: videoOutput = "1080p 29.97Hz"; break;
				case 30: videoOutput = "1080p 30Hz"; break;
				case 50: videoOutput += " 50Hz"; break;
				case 59: videoOutput += "1080p 59.94Hz"; break;
				case 60: videoOutput += " 60Hz"; break;
				default : videoOutput += " 60Hz";
			}
		} else {
			switch (fps) {
			case 23: videoOutput = "23p"; break;
			case 24: videoOutput = "24p"; break;
			case 25: videoOutput = "PAL"; break;
			case 29: videoOutput = "NTSC"; break;
			case 30: videoOutput = "NTSC"; break;
			case 49: videoOutput = "PAL"; break;
			case 50: videoOutput = "PAL"; break;
			case 60: videoOutput = "NTSC"; break;
			default: videoOutput = "NTSC"; break; 
			}
		}
			
		return videoOutput;
	}
	
	/**
	 * Get the file's video source as specified in the filename.
	 * @param filename filename of the movie file.
	 * @return the video source as a string
	 * 
	 * @author jjulien, quickfinga 
	 */
	protected String getVideoSource(String filename) {
		String f = filename.toUpperCase();
		if (hasKeyword(f, "HDTV")) return "HDTV";
		if (hasKeyword(f, new String[] { "BLURAY", "BDRIP", "BLURAYRIP" })) return "BDRiP";
		if (hasKeyword(f, "DVDRIP")) return "DVDRip";
		if (hasKeyword(f, "DVDSCR")) return "DVDSCR";
		if (hasKeyword(f, "DSRIP")) return "DSRip";
		if (hasKeyword(filename, new String[] {" TS ", ".TS."})) return "TS";
		if (hasKeyword(filename, "CAM")) return "CAM";
		if (hasKeyword(filename, "R5")) return "R5";
		if (hasKeyword(filename, "LINE")) return "LINE";
		if (hasKeyword(filename, new String[] { "HDDVD", "HD-DVD", "HDDVDRIP"})) return "HDDVD";
		if (hasKeyword(filename, "DTH")) return "D-THEATER";
		return "Unknown";
	}
	
	protected boolean hasSubtitles(File fileToScan) {
		String path = fileToScan.getAbsolutePath();
		int index = path.lastIndexOf(".");
		String basename = path.substring(0, index+1);
		
		if (index >= 0) {
			return ( new File(basename + "srt").exists() || 
				   ( new File(basename + "sub").exists() && new File(basename + "idx").exists()));
		}
		
		String fn = path.toUpperCase();
		if (hasKeyword(fn, "VOST")) return true;
		return false;
	}

	protected void updateFirstKeywordIndex(int index) {
		if (index>0) {
			firstKeywordIndex = (firstKeywordIndex>index)?index:firstKeywordIndex;
		}
	}
	protected void updateMovie(String filename, Movie movie) {
		try {
			StringTokenizer st = new StringTokenizer(filename,". []()-");
			while(st.hasMoreTokens()) {
				String token = st.nextToken();
				
				// Year
				if (token.length()==4 
						&& Character.isDigit(token.charAt(0)) 
						&& Character.isDigit(token.charAt(1)) 
						&& Character.isDigit(token.charAt(2)) 
						&& Character.isDigit(token.charAt(3))) {
					updateFirstKeywordIndex(filename.indexOf(token));
					movie.setYear(token.substring(0,4));
				}
			}

			st = new StringTokenizer(filename,"-");
			while(st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.startsWith(" ")) {
					updateFirstKeywordIndex(filename.indexOf(token));
				}
			}
			
			movie.setTitle(getName(filename));
		} catch (Exception e) {
			movie.setTitle("Unknown");
		}
	}

	protected void updateTVShow(String filename, Movie movie) {
		try {
			StringTokenizer st = new StringTokenizer(filename,". []-");
			while(st.hasMoreTokens()) {
				String token = st.nextToken();
				
				// S??E?? format
				if (token.length()==6 && token.toUpperCase().startsWith("S") && token.toUpperCase().charAt(3)=='E' && Character.isDigit(token.charAt(2)) ) {
					updateFirstKeywordIndex(filename.indexOf(token));
					movie.setSeason(Integer.parseInt(token.substring(1,3)));
					movie.getFirstFile().setPart(Integer.parseInt(token.substring(4,6)));
					
					int beginIndex = filename.lastIndexOf("-");
					int endIndex = filename.lastIndexOf(".");
					if ( beginIndex>=0 && endIndex>beginIndex ) {
						movie.getFirstFile().setTitle(filename.substring(beginIndex+1, endIndex).trim());
					} else {
						movie.getFirstFile().setTitle("Unknown");
					}
				}
				
				// ?x?? format
				if (token.length()==4 && Character.isDigit(token.charAt(0)) && token.toUpperCase().charAt(1)=='X') {
					updateFirstKeywordIndex(filename.indexOf(token));
					movie.setSeason(Integer.parseInt(token.substring(0,1)));
					movie.getFirstFile().setPart(Integer.parseInt(token.substring(2,4)));
					movie.getFirstFile().setTitle(getName(filename));
					
					int beginIndex = filename.lastIndexOf("-");
					int endIndex = filename.lastIndexOf(".");
					if ( beginIndex>=0 && endIndex>beginIndex ) {
						movie.getFirstFile().setTitle(filename.substring(beginIndex+1, endIndex).trim());
					} else {
						movie.getFirstFile().setTitle("Unknown");
					}
				}
			}
		} catch (Exception e) {
			//
		}
	}
	
	protected String findKeyword(String filename, String[] strings) {
		for (String keyword : strings) {
			int index = filename.indexOf(keyword);
			if (index>0) {
				updateFirstKeywordIndex(index);
				return keyword;
			}
		}
		return "Unknown";
	}

	/**
	 * @return true when the specified keyword exist in the specified filename
	 */
	protected boolean hasKeyword(String filename, String keyword) {
		return hasKeyword(filename, new String[] { keyword } );
	}
	
	/**
	 * @return true when one of the specified keywords exist in the specified filename
	 */
	protected boolean hasKeyword(String filename, String[] keywords) {
		for (String keyword : keywords) {
			int index = filename.indexOf(keyword);
			if (index>0) {
				updateFirstKeywordIndex(index);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * @return true when one of the specified keywords exist in the specified filename
	 */
	protected boolean hasKeywordAfterTitle(String filename, String[] keywords) {
		for (String keyword : keywords) {
			int index = filename.indexOf(keyword);
			if (index>=firstKeywordIndex) {
				return true;
			}
		}
		return false;
	}
}