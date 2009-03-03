package com.moviejukebox.scanner;

import java.util.regex.Matcher;

import junit.framework.TestCase;


public class MovieFilenameScannerTest extends TestCase {

	public void testPatterns() {
		Matcher matcher = MovieFilenameScanner.TOKEN_DELIMITERS_MATCH_PATTERN.matcher("a]bc.def");
		matcher.region(2, 7);
		assertTrue(matcher.find());
		assertEquals(4, matcher.start());
		assertEquals(5, matcher.end());
		assertFalse(matcher.find());
		
		matcher = MovieFilenameScanner.TV_PATTERN.matcher("Desperate Housewives S04E01E02E03E04.iso");
		
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
