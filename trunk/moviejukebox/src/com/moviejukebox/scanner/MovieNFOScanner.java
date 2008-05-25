package com.moviejukebox.scanner;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import com.moviejukebox.model.Movie;

/**
 * NFO file parser.
 * 
 * Search a NFO file for IMDb URL.
 * 
 * @author jjulien
 */
public class MovieNFOScanner {
	
	static final int BUFF_SIZE = 100000;
	static final byte[] buffer = new byte[BUFF_SIZE];

	/**
	 * Search the IMDBb id of the specified movie in the NFO file if it exists.
	 * @param movie movie to scan
	 */
	public void scan(Movie movie){     
		
		String fn = movie.getFile().getAbsolutePath();
		int i = fn.lastIndexOf(".");
		
		File nfoFile = new File(fn.substring(0, i) + ".nfo");
		
		if (nfoFile.exists()) {
		
		   InputStream in = null;
		   ByteArrayOutputStream out = null; 
		   try {
		      in = new FileInputStream(nfoFile);
		      out = new ByteArrayOutputStream();
		      while (true) {
		         synchronized (buffer) {
		            int amountRead = in.read(buffer);
		            if (amountRead == -1) {
		               break;
		            }
		            out.write(buffer, 0, amountRead); 
		         }
		      } 
		      
		      String nfo = out.toString();
		      
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
