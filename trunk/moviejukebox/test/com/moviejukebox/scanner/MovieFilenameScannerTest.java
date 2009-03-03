package com.moviejukebox.scanner;

import java.util.regex.Matcher;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;

import junit.framework.TestCase;


public class MovieFilenameScannerTest extends TestCase {

	public void testPatterns() {
		Matcher matcher = MovieFilenameScanner.TOKEN_DELIMITERS_MATCH_PATTERN.matcher("a]bc.def");
		matcher.region(2, 7);
		assertTrue(matcher.find());
		assertEquals(4, matcher.start());
		assertEquals(5, matcher.end());
		assertFalse(matcher.find());
		
		//matcher = MovieFilenameScanner.TV_PATTERNS.get(1).matcher("Desperate Housewives S04E01E02E03E06.iso");
		
		MovieFilenameScanner scanner = new MovieFilenameScanner();
		Movie movie = new Movie();
		MovieFile mfile = new MovieFile();
		movie.addMovieFile(mfile);
		
		scanner.updateTVShow("Desperate Housewives S04E01E02E03E06.iso", movie);
		assertEquals(4, movie.getSeason());
		assertEquals(1, mfile.getFirstPart());
		assertEquals(6, mfile.getLastPart());

		scanner.updateTVShow("Desperate Housewives s99E10e20e13e11 - Test Test.mkv", movie);
		assertEquals(99, movie.getSeason());
		assertEquals(10, mfile.getFirstPart());
		assertEquals(20, mfile.getLastPart());
		assertEquals("Test Test", mfile.getTitle());

		movie.setSeason(0);
		scanner.updateTVShow("Postal 2 720x400.iso", movie);
		assertEquals(0, movie.getSeason());

		scanner.updateTVShow("House 3x101x19x3.avi", movie);
		assertEquals(3, movie.getSeason());
		assertEquals(3, mfile.getFirstPart());
		assertEquals(101, mfile.getLastPart());
	}
	
	
	public void testParts() {
		MovieFilenameScanner scanner = new MovieFilenameScanner();
		String filename = "Test movie";
		scanner.firstKeywordIndex = filename.length();
		assertEquals(1, scanner.getPart(filename));
		assertEquals(filename.length(), scanner.firstKeywordIndex);
		assertNull(scanner.getPartTitle(filename));

//		Matcher m = Pattern.compile("(?i) (?:(?:CD)|(?:DISC))([0-9]+) ").matcher("XXXXX cd1  XXXXXXXXXX");
//		assertTrue(m.find());
//		assertEquals("sss", m.group(1));
//		assertEquals(2, m.groupCount());

		filename = "File [cd].avi";
		scanner.firstKeywordIndex = filename.length();
		assertEquals(1, scanner.getPart(filename));
		assertEquals(filename.length(), scanner.firstKeywordIndex);
		assertNull(scanner.getPartTitle(filename));
		
		filename = "The File [cd3].avi";
		scanner.firstKeywordIndex = filename.length();
		assertEquals(3, scanner.getPart(filename));
		assertEquals(9, scanner.firstKeywordIndex);
		assertNull(scanner.getPartTitle(filename));

		filename = "The File cd3.avi";
		scanner.firstKeywordIndex = filename.length();
		assertEquals(3, scanner.getPart(filename));
		assertEquals(8, scanner.firstKeywordIndex);
		assertNull(scanner.getPartTitle(filename));

		filename = "The File part66 - The File Strikes Back.avi";
		scanner.firstKeywordIndex = filename.length();
		assertEquals(66, scanner.getPart(filename));
		assertEquals(8, scanner.firstKeywordIndex);
		assertEquals("The File Strikes Back", scanner.getPartTitle(filename));

		filename = "The File - The Wrong Part Title disk8.avi";
		scanner.firstKeywordIndex = filename.length();
		assertEquals(8, scanner.getPart(filename));
		assertEquals(31, scanner.firstKeywordIndex);
		assertNull(scanner.getPartTitle(filename));

		filename = "File (5 DVD).avi";
		scanner.firstKeywordIndex = filename.length();
		assertEquals(5, scanner.getPart(filename));
		assertEquals(5, scanner.firstKeywordIndex);
		assertNull(scanner.getPartTitle(filename));
	}
}
