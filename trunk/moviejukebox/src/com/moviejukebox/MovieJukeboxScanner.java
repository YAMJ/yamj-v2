package com.moviejukebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.sun.xml.internal.ws.util.ByteArrayBuffer;

public class MovieJukeboxScanner {
	
	private static final String[] skipKeywords = {
		"LIMITED","LiMiTED", "Limited", "DiAMOND", "AXXO", "PUKKA", "iDHD", "PROPER", "REPACK", "DSR", "STV", "UNRATED", "RERIP"
	};
	
	static final int BUFF_SIZE = 100000;
	static final byte[] buffer = new byte[BUFF_SIZE];
	
	private String mediaLibraryRoot;
	private int mediaLibraryRootPathIndex;
	private int firstKeywordIndex = 0;



	protected String getAudioCodec(String filename) {
		return findKeyword(
				filename.toUpperCase(), 
				new String[] { "AC3", "DTS" });
	}
		
	protected String getContainer(String filename) {
		int lastDotIndex = filename.lastIndexOf(".");
		updateFirstKeywordIndex(lastDotIndex);
		return filename.substring(lastDotIndex+1).toUpperCase();
	}

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

	protected String getLanguage(String filename) {
		if (hasKeyword(filename, new String[] {"FRENCH", "French", "VF", "[FR]", "[fr]", "[Fr]", "[fR]", ".FR.", ".fr."})) 
			return "French";

		if (hasKeyword(filename, new String[] {"GERMAN",})) 
			return "German";

		if (hasKeyword(filename, new String[] {"ITALIAN", "iTALiAN", "[ITA]"})) 
			return "Italian";

		if (hasKeyword(filename, new String[] {"SPANISH", "[SPA]"})) 
			return "Spanish";

		if (hasKeyword(filename, new String[] {"ENGLISH", "[ENG]"})) 
			return "English";

		if (hasKeyword(filename, new String[] {"VO"})) 
			return "VO";
		
		return "Unknown";
	}
	
	private String getName(String filename) {
		String name = filename.substring(0, firstKeywordIndex);
		name = name.replace(".", " ");
		name = name.replace("-", " ");
		name = name.replace("[", " ");
		name = name.replace("]", " ");
		name = name.replace("(", " ");
		name = name.replace(")", " ");
		return name.trim();
	}

	private int getPart(String filename) {
		if (hasKeyword(filename, new String[] { "CD1", "CD 1" } )) return 1;
		if (hasKeyword(filename, new String[] { "CD2", "CD 2" } )) return 2;
		if (hasKeyword(filename, new String[] { "CD3", "CD 3" } )) return 3;
		if (hasKeyword(filename, new String[] { "CD4", "CD 4" } )) return 4;
		return 1;
	}
	
	protected String getVideoCodec(String filename) {
		String f = filename.toUpperCase();
		if (hasKeyword(f, "XVID")) return "XviD";
		if (hasKeyword(f, "DIVX")) return "DivX";
		if (hasKeyword(f, new String[] {"H264", "H.264", "X264" })) return "H.264";
		return "Unknown";
	}

	private String getVideoOutput(String filename) {
		
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
	
	private String getVideoSource(String filename) {
		String f = filename.toUpperCase();
		if (hasKeyword(f, "HDTV")) return "HDTV";
		if (hasKeyword(f, new String[] { "BLURAY", "BDRIP" })) return "BDRiP";
		if (hasKeyword(f, "DVDRIP")) return "DVDRip";
		if (hasKeyword(f, "DVDSCR")) return "DVDSCR";
		if (hasKeyword(f, "DSRIP")) return "DSRip";
		if (hasKeyword(filename, new String[] {" TS ", ".TS."})) return "TS";
		if (hasKeyword(filename, "CAM")) return "CAM";
		if (hasKeyword(filename, "R5")) return "R5";
		if (hasKeyword(filename, "LINE")) return "LINE";
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

	public Library scan(File directory) {

		if (directory.isFile())
			mediaLibraryRoot = directory.getParentFile().getAbsolutePath();
		else {
			mediaLibraryRoot = directory.getAbsolutePath();
		}
		
		mediaLibraryRootPathIndex = mediaLibraryRoot.length();
		
		Library library = new Library();
		this.scanDirectory(directory, library);
		return library;
	}

	
	protected void scanDirectory(File directory, Library collection) {
		if (directory.isFile())
			scanFile(directory, collection);
		else {
			List<File> files = Arrays.asList(directory.listFiles());
			Collections.sort(files);
			
			for (File file : files) {
				if (file.isDirectory()) {
					scanDirectory(file, collection);
				} else {
					scanFile(file, collection);
				}
			}
		}		
	}
	
	protected void scanFile(File fileToScan, Library library) {
		int index = fileToScan.getName().lastIndexOf(".");
		if (index < 0) return;
		
		String extension = fileToScan.getName().substring(index+1).toUpperCase();
		if ("AVI DIVX MKV WMV M2TS TS RM QT ISO VOB".indexOf(extension) >= 0) {
			Movie movie = scanFilename(fileToScan);
			scanNFO(fileToScan, movie);
			library.addMovie(movie);
	    }
	}	

	protected Movie scanFilename(File fileToScan) {
		String relativeFilename = fileToScan.getAbsolutePath().substring(mediaLibraryRootPathIndex);
		
		if ( relativeFilename.startsWith(File.separator) ) {
			 relativeFilename = relativeFilename.substring(1); 
		}
		
		String filename = fileToScan.getName();
		
		firstKeywordIndex = filename.indexOf("[");
		firstKeywordIndex = (firstKeywordIndex==-1)?filename.length():firstKeywordIndex;

		Movie movie = new Movie();		
		movie.setAudioCodec(getAudioCodec(filename));
		movie.setContainer(getContainer(filename));
		movie.setFps(getFPS(filename));
		movie.setSubtitles(hasSubtitles(fileToScan));
		movie.setVideoCodec(getVideoCodec(filename));
		movie.setVideoOutput(getVideoOutput(filename));
		movie.setVideoSource(getVideoSource(filename));
		movie.setLanguage(getLanguage(filename));
		
		// Skip some keywords
		findKeyword(filename, skipKeywords);
		
		movie.setBaseName(filename.substring(0, filename.lastIndexOf(".")));

		MovieFile movieFile = new MovieFile();
		relativeFilename = relativeFilename.replace('\\', '/'); // make it unix!
		movieFile.setFilename(relativeFilename);
		movieFile.setPart(getPart(filename));
		movie.addMovieFile(movieFile);
		
		// Update the movie file with interpreted movie data
		updateTVShow(filename, movie);
		updateMovie(filename, movie);
				
		return movie;
	}

	private void scanNFO(File fileToScan, Movie movie){     
		
		String fn = fileToScan.getAbsolutePath();
		int i = fn.lastIndexOf(".");
		
		File nfoFile = new File(fn.substring(0, i) + ".nfo");
		
		if (nfoFile.exists()) {
		
		   InputStream in = null;
		   ByteArrayBuffer out = null; 
		   try {
		      in = new FileInputStream(nfoFile);
		      out = new ByteArrayBuffer();
		      while (true) {
		         synchronized (buffer) {
		            int amountRead = in.read(buffer);
		            if (amountRead == -1) {
		               break;
		            }
		            out.write(buffer, 0, amountRead); 
		         }
		      } 
		      
		      String nfo = new String(out.toByteArray());
		      
		      int beginIndex = nfo.indexOf("/tt");
		      if ( beginIndex != -1) {
		    	  StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex+1),"/ \n,:!&й\"'(--и_за)=$");
		    	  movie.setId(st.nextToken());
		      }
		      
		      
		   } catch (IOException e) {
			  System.err.println("Failed reading " + nfoFile.getName());
			  e.printStackTrace();
		   } finally {
			   try {
			      if (in != null) {
			         in.close();
			      }
			      if (out != null) {
			         out.close();
			      }
			   } catch(IOException e) {
				   
			   }
		   }
		} 
	}
	
	private void updateFirstKeywordIndex(int index) {
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
					movie.getFirstFile().setTitle("Unknown");
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
	
	private String findKeyword(String filename, String[] strings) {
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
	private boolean hasKeyword(String filename, String keyword) {
		return hasKeyword(filename, new String[] { keyword } );
	}
	
	/**
	 * @return true when one of the specified keywords exist in the specified filename
	 */
	private boolean hasKeyword(String filename, String[] keywords) {
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
	private boolean hasKeywordAfterTitle(String filename, String[] keywords) {
		for (String keyword : keywords) {
			int index = filename.indexOf(keyword);
			if (index>=firstKeywordIndex) {
				return true;
			}
		}
		return false;
	}
}