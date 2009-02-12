package com.moviejukebox.scanner;

import java.util.regex.Matcher;

import com.moviejukebox.tools.PropertiesUtil;

import junit.framework.TestCase;


public class MovieFilenameScannerTest extends TestCase {

	@Override
	protected void setUp() throws Exception {
		PropertiesUtil.setPropertiesStreamName("moviejukebox.properties");
	}

	public void testPatterns() {
		Matcher matcher = MovieFilenameScanner.TOKEN_DELIMITERS_MATCH_PATTERN.matcher("a]bc.def");
		matcher.region(2, 7);
		assertTrue(matcher.find());
		assertEquals(4, matcher.start());
		assertEquals(5, matcher.end());
		assertFalse(matcher.find());
		
		matcher = MovieFilenameScanner.TV_PATTERN.matcher("Desperate Housewives S04E01E02E03E04.iso");
		
	}
}
