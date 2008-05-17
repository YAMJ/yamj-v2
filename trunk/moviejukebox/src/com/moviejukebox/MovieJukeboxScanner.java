package com.moviejukebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.StringTokenizer;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.sun.xml.internal.ws.util.ByteArrayBuffer;

public class MovieJukeboxScanner {
	
	private static final String[] skipKeywords = {
		"LIMITED", "CAM", "XviD", "DiAMOND", "AXXO", "PUKKA", "BDRiP", "iDHD", "DivX5", "R5", "DvDRip", "PROPER", "REPACK"
	};
	
	private String mediaLibraryRoot;
	private int mediaLibraryRootPathIndex;
	private int firstKeywordIndex = 0;
	
	public Library scan(File directory) {

		if (directory.isFile())
			mediaLibraryRoot = directory.getParent();
		else {
			mediaLibraryRoot = directory.getAbsolutePath();
		}
		
		mediaLibraryRootPathIndex = mediaLibraryRoot.length()+1;
		
		Library library = new Library();
		this.scanDirectory(directory, library);
		return library;
	}
	
	protected void scanDirectory(File directory, Library collection) {
		if (directory.isFile())
			scanFile(directory, collection);
		else {
			for (File file : directory.listFiles()) {
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
		if ("AVI DIVX MKV WMV M2TS TS RM QT".indexOf(extension) >= 0) {
			
			Movie movie = scanFilename(fileToScan);
			scanNFO(fileToScan, movie);
			library.addMovie(movie);
	    }
	}

	protected Movie scanFilename(File fileToScan) {
		String relativeFilename = fileToScan.getAbsolutePath().substring(mediaLibraryRootPathIndex);
		String filename = fileToScan.getName();
		
		firstKeywordIndex = filename.indexOf("[");
		firstKeywordIndex = (firstKeywordIndex==-1)?filename.length():firstKeywordIndex;

		Movie movie = new Movie();		
		movie.setAudioCodec(getAudioCodec(filename));
		movie.setContainer(getContainer(filename));
		movie.setFps(getFPS(filename));
		movie.setSubtitles(hasSubtitles(fileToScan));
		movie.setLanguage(getLanguage(filename));
		movie.setVideoCodec(getVideoCodec(filename));
		movie.setVideoOutput(getVideoOutput(filename));
		movie.setVideoSource(getVideoSource(filename));
		
		this.findKeyword(filename, skipKeywords);
		
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

	private String getVideoSource(String filename) {
		return findKeyword(
				filename.toUpperCase(), 
				new String[] { "HDTV", "CAM", "BDRIP", "DVDRIP", "R5", "DVDSCR" });
	}

	private int getPart(String filename) {
		int index;
		if ((index = filename.lastIndexOf("CD1")) >= 0) { this.updateFirstKeywordIndex(index); return 1; }
		if ((index = filename.lastIndexOf("CD 1")) >= 0) { this.updateFirstKeywordIndex(index); return 1; }
		if ((index = filename.lastIndexOf("CD2")) >= 0) { this.updateFirstKeywordIndex(index); return 2; }
		if ((index = filename.lastIndexOf("CD 2")) >= 0) { this.updateFirstKeywordIndex(index); return 2; }
		if ((index = filename.lastIndexOf("CD3")) >= 0) { this.updateFirstKeywordIndex(index); return 3; }
		if ((index = filename.lastIndexOf("CD 3")) >= 0) { this.updateFirstKeywordIndex(index); return 3; }
		return 1;
	}
	
	private String getVideoOutput(String filename) {
		
		String videoOutput = findKeyword(filename, new String[] { "720p", "1080i", "1080p" });
		
		int fps = getFPS(filename);
		if (!videoOutput.equalsIgnoreCase("Unknown")) {
			switch (fps) {
				case 23: videoOutput += " 23.976Hz"; break;
				case 24: videoOutput += " 24Hz"; break;
				case 25: videoOutput += " 50Hz"; break;
				case 29: videoOutput += " 60Hz"; break;
				case 30: videoOutput += " 60Hz"; break;
				case 49: videoOutput += " 50Hz"; break;
				case 50: videoOutput += " 50Hz"; break;
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
			default : videoOutput = "NTSC"; break; 
			}
		}
			
		return videoOutput;
	}

	protected String getVideoCodec(String filename) {
		return findKeyword(
				filename.toUpperCase(), 
				new String[] { "XVID", "DIVX", "X264", "H264", "H.264" });
	}
	
	protected String getLanguage(String filename) {
		String keyword = findKeyword(
				filename, 
				new String[] { "FRENCH", "[Fr]", "[FR]", "GERMAN", "ENGLISH", "SPANISH", "ITALIAN", "VO", "VF" });
		
		if (keyword.toUpperCase().indexOf("FR") !=-1) {
			return "FRENCH";
		}
		
		return keyword;
	}

	protected boolean hasSubtitles(File fileToScan) {
		String path = fileToScan.getAbsolutePath();

		String fn = path.toUpperCase();
		if (fn.indexOf("VOST") >= 0) return true;

		int index = path.lastIndexOf(".");
		
		if (index >= 0) {
			return new File(path.substring(0, index+1) + "srt").exists();
		}
		
		return false;
	}
	
	protected String getAudioCodec(String filename) {
		return findKeyword(
				filename.toUpperCase(), 
				new String[] { "AC3", "DTS" });
	}

	protected int getFPS(String filename) {
		
		int index;
		if ((index = filename.lastIndexOf("23p")) >= 0) { this.updateFirstKeywordIndex(index); return 23; }
		if ((index = filename.lastIndexOf("p23")) >= 0) { this.updateFirstKeywordIndex(index); return 23; }
		if ((index = filename.lastIndexOf("24p")) >= 0) { this.updateFirstKeywordIndex(index); return 24; }
		if ((index = filename.lastIndexOf("p24")) >= 0) { this.updateFirstKeywordIndex(index); return 24; }
		if ((index = filename.lastIndexOf("25p")) >= 0) { this.updateFirstKeywordIndex(index); return 25; }
		if ((index = filename.lastIndexOf("p25")) >= 0) { this.updateFirstKeywordIndex(index); return 25; }
		if ((index = filename.lastIndexOf("29p")) >= 0) { this.updateFirstKeywordIndex(index); return 29; }
		if ((index = filename.lastIndexOf("p29")) >= 0) { this.updateFirstKeywordIndex(index); return 29; }
		if ((index = filename.lastIndexOf("30p")) >= 0) { this.updateFirstKeywordIndex(index); return 30; }
		if ((index = filename.lastIndexOf("p30")) >= 0) { this.updateFirstKeywordIndex(index); return 30; }
		if ((index = filename.lastIndexOf("p49")) >= 0) { this.updateFirstKeywordIndex(index); return 49; }
		if ((index = filename.lastIndexOf("49p")) >= 0) { this.updateFirstKeywordIndex(index); return 49; }
		if ((index = filename.lastIndexOf("50p")) >= 0) { this.updateFirstKeywordIndex(index); return 50; }
		if ((index = filename.lastIndexOf("p50")) >= 0) { this.updateFirstKeywordIndex(index); return 50; }
		return 60;
	}

	protected String getContainer(String filename) {
		int lastDotIndex = filename.lastIndexOf(".");
		updateFirstKeywordIndex(lastDotIndex);
		return filename.substring(lastDotIndex+1).toUpperCase();
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

	private void updateFirstKeywordIndex(int index) {
		if (index>0) {
			firstKeywordIndex = (firstKeywordIndex>index)?index:firstKeywordIndex;
		}
	}
	
	static final int BUFF_SIZE = 100000;
	static final byte[] buffer = new byte[BUFF_SIZE];

	private void scanNFO(File fileToScan, Movie movie){     
		
		String fn = fileToScan.getAbsolutePath();
		int i = fn.lastIndexOf(".");
		
		File nfoFile = new File(fn.substring(0, i) + ".nfo");
		
		if (nfoFile.exists()) {
		
		   InputStream in = null;
		   ByteArrayBuffer out = null; 
		   try {
		      in = new FileInputStream(nfoFile);
		      StringWriter sr = new StringWriter();
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
}