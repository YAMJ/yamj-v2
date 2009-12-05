/*
 *      Copyright (c) 2004-2009 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list 
 *  
 *      Web: http://code.google.com/p/moviejukebox/
 *  
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *  
 *      For any reuse or distribution, you must make clear to others the 
 *      license terms of this work.  
 */

package com.moviejukebox.scanner;

import static com.moviejukebox.MovieJukebox.tokenizeToArray;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;

import junit.framework.TestCase;

import com.moviejukebox.model.MovieFileNameDTO;


public class MovieFilenameScannerTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        MovieFilenameScanner.setSkipKeywords(new String[] { "-xor", "REMUX", "vfua", "-SMB", "-hdclub", "[KB]" });
        MovieFilenameScanner.setMovieVersionKeywords(tokenizeToArray("remastered,directors cut,extended cut,final cut", ",;|"));
    }

    public void testPatterns() {
        Matcher matcher = MovieFilenameScanner.TOKEN_DELIMITERS_MATCH_PATTERN.matcher("a]bc.def");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
        assertEquals(0, matcher.end());
        assertTrue(matcher.find());
        assertEquals(1, matcher.start());
        assertEquals(2, matcher.end());
        assertEquals("]", matcher.group());
        assertTrue(matcher.find());
        assertEquals(".", matcher.group());
        assertTrue(matcher.find());
        assertEquals(8, matcher.start());
        assertEquals(8, matcher.end());
        assertFalse(matcher.find());
    }

    public void testScan() {
        MovieFileNameDTO d = scan("Desperate Housewives S04E01E02E03E06.iso");
        assertEquals("Desperate Housewives", d.getTitle());
        assertEquals("iso", d.getExtension());
        assertEquals("ISO", d.getContainer());
        assertEquals(4, d.getSeason());
        assertEquals(Arrays.asList(new Integer[] { 1, 2, 3, 6 }), d.getEpisodes());

        d = scan("The.Matrix.[Trailer-Unrated].avi");
        assertEquals("The Matrix", d.getTitle());
        assertEquals("avi", d.getExtension());
        assertEquals("AVI", d.getContainer());
        assertTrue(d.isExtra());
        assertEquals("Trailer-Unrated", d.getPartTitle());

        d = scan("The.Matrix 720P - Unrated Trailer.avi");
        assertEquals("The Matrix", d.getTitle());
        assertFalse(d.isExtra());
        assertEquals("720p", d.getHdResolution());

        d = scan("The.Trailer[ccc][Rated Trailer][XXX] TRAILER.p23.avi");
        assertTrue(d.isExtra());
        assertEquals("Rated Trailer", d.getPartTitle());
        assertEquals(23, d.getFps());

        d = scan("Postal 2 720x400.iso");
        assertEquals(-1, d.getSeason());
        assertEquals(0, d.getEpisodes().size());
        assertNull(d.getAudioCodec());

        d = scan("House 3x101x19x3.avi");
        assertEquals(3, d.getSeason());
        assertEquals(Arrays.asList(new Integer[] { 101, 19, 3 }), d.getEpisodes());

        d = scan("File (5 DVD)aac.avi");
        assertEquals(-1, d.getSeason());
        assertEquals(5, d.getPart());
        assertEquals("AAC", d.getAudioCodec());
        assertEquals("File", d.getTitle());

        d = scan("[SET Alien Collection] Alien vs Predator.mkv");
        assertEquals(1, d.getSets().size());
        assertEquals("Alien Collection", d.getSets().get(0).getTitle());
        assertEquals("Alien vs Predator", d.getTitle());

        d = scan("Alien vs Predator [SET Alien Collection 2][SET Predator-3].mkv");
        assertEquals(2, d.getSets().size());
        assertEquals("Alien Collection 2", d.getSets().get(0).getTitle());
        assertEquals(-1, d.getSets().get(0).getIndex());
        assertEquals("Predator", d.getSets().get(1).getTitle());
        assertEquals(3, d.getSets().get(1).getIndex());

        d = scan("Dinner.Game.(Diner.De.Cons).1998.FRENCH.720p.BluRay.x264-zzz.ts");
        assertEquals(1998, d.getYear());
        assertEquals("Dinner Game (Diner De Cons)", d.getTitle());
        assertEquals(1, d.getLanguages().size());

        d = scan("The.French.Connection.mkv");
        assertEquals("The French Connection", d.getTitle());
        assertEquals(0, d.getLanguages().size());


        d = scan("Burn-E.[2008].[Pixar.Short].[BDrip.720p.x264].mkv");
        assertEquals(2008, d.getYear());
        assertEquals("Burn-E", d.getTitle());

        d = scan("shrek.avi");
        assertEquals("shrek", d.getTitle());

        d = scan("shrek_2.avi");
        assertEquals("shrek 2", d.getTitle());

        d = scan("Paprika (DVDRip XviD 768x432 24fps AC3 5.1ch).avi");
        assertEquals("Paprika", d.getTitle());

        d = scan("Stargate.SG1.S04E16 - 2010.avi");
        assertEquals("Stargate SG1", d.getTitle());
        assertEquals(4, d.getSeason());
        assertEquals(1, d.getEpisodes().size());
        assertEquals(16, d.getEpisodes().get(0).intValue());
        assertEquals("2010", d.getEpisodeTitle());

        d = scan("The Sopranos - S03E01 DVDrip.XviD-SMB.avi");
        assertEquals("The Sopranos", d.getTitle());
        assertEquals(3, d.getSeason());
        assertEquals(1, d.getEpisodes().size());
        assertEquals(1, d.getEpisodes().get(0).intValue());
        assertEquals(null, d.getEpisodeTitle());

        d = scan("final_fantasy_VII_Advent_Children_1080p_RUS_ENG.mkv");
        assertEquals("final fantasy VII Advent Children", d.getTitle());
        assertEquals("1080p", d.getHdResolution());
        assertEquals(2, d.getLanguages().size());

        d = scan("Chasseurs.De.Dragons.FRENCH.720p.BluRay.x264-HDClub.mkv");
        assertEquals("Chasseurs De Dragons", d.getTitle());
        assertEquals("720p", d.getHdResolution());
        assertEquals("BluRay", d.getVideoSource());
        assertEquals("H.264", d.getVideoCodec());
        assertEquals(1, d.getLanguages().size());
        assertEquals("French", d.getLanguages().get(0));

        d = scan("Steamboy_(2004)_[720p,BluRay,x264,DTS]_-_THORA.mkv");
        assertEquals(2004, d.getYear());
        assertEquals("Steamboy", d.getTitle());
        assertEquals("720p", d.getHdResolution());
        assertEquals("BluRay", d.getVideoSource());
        assertEquals("H.264", d.getVideoCodec());

        d = scan("X-Men 3 [SET X-Men - 99].avi");
        assertEquals(1, d.getSets().size());
        assertEquals(99, d.getSets().get(0).getIndex());
        assertEquals("X-Men", d.getSets().get(0).getTitle());
        assertEquals("X-Men 3", d.getTitle());

        d = scan("Rush hour 2 DVD");
        assertEquals(0, d.getSets().size());
        assertEquals("Rush hour 2", d.getTitle());

        d = scan("21 DVD-RIP");
        assertEquals("21", d.getTitle());
        assertEquals("DVD", d.getVideoSource());

        d = scan("1408 DVD");
        assertEquals("DVD", d.getVideoSource());
        assertEquals("1408", d.getTitle());

        d = scan("[KB]_Cowboy_Bebop_Remastered_07.DVD_(H264.AC3_5.1).mkv");
        assertEquals("Cowboy Bebop", d.getTitle());
        assertEquals(7, d.getPart());
        assertNull(d.getPartTitle());

    }

    public void testScanLanguages() {
        MovieFileNameDTO d = scan("Time masters (Laloux Moebius) (1982) Eng.Hun.Fra.De.Ru.mkv");
        assertEquals(1982, d.getYear());
        assertEquals("Time masters (Laloux Moebius)", d.getTitle());
        assertEquals(5, d.getLanguages().size());
        assertTrue(d.getLanguages().contains("English"));
        assertTrue(d.getLanguages().contains("Hungarian"));
        assertTrue(d.getLanguages().contains("French"));
        assertTrue(d.getLanguages().contains("German"));
        assertTrue(d.getLanguages().contains("Russian"));

        d = scan("The_IT_Crowd.S02E03.rus.lostfilm - Moss and the German.avi");
        assertEquals("The IT Crowd", d.getTitle());
        assertEquals(2, d.getSeason());
        assertEquals(1, d.getEpisodes().size());
        assertEquals(3, d.getEpisodes().get(0).intValue());
        assertEquals(1, d.getLanguages().size());
        assertEquals("Russian", d.getLanguages().get(0));
        assertEquals("Moss and the German", d.getEpisodeTitle());

        d = scan("Misery.1990[German]DTS.720p.BluRay.x264.mkv");
        assertEquals("Misery", d.getTitle());
        assertEquals(1990, d.getYear());
        assertEquals(1, d.getLanguages().size());
        assertTrue(d.getLanguages().contains("German"));

        d = scan("The.Good.German.2006.720p.BluRay.x264.ts");
        assertEquals("The Good German", d.getTitle());
        assertEquals(2006, d.getYear());
        assertEquals(0, d.getLanguages().size());
    }

    public void testScanSeries() {
        MovieFileNameDTO d = scan("Formula.1.S2008E1.avi");
        assertEquals("Formula 1", d.getTitle());
        assertEquals(2008, d.getSeason());
        assertEquals(1, d.getEpisodes().size());
        assertEquals(1, d.getEpisodes().get(0).intValue());
        d = scan("Horizon.S2009E03.avi");
        assertEquals(2009, d.getSeason());
        assertEquals(1, d.getEpisodes().size());
        assertEquals(3, d.getEpisodes().get(0).intValue());
        
        d = scan("World.Series.Of.Poker.S2008E11E12E13.avi.avi");
        assertEquals("World Series Of Poker", d.getTitle());
        assertEquals(2008, d.getSeason());
        assertEquals(3, d.getEpisodes().size());
        assertEquals(11, d.getEpisodes().get(0).intValue());
        assertEquals(12, d.getEpisodes().get(1).intValue());
        assertEquals(13, d.getEpisodes().get(2).intValue());

        d = scan("House.S03E14.HDTV.XviD-XOR.avi");
        assertEquals("House", d.getTitle());
        assertEquals(3, d.getSeason());
        assertEquals(1, d.getEpisodes().size());
        assertNull(d.getPartTitle());
        assertNull(d.getEpisodeTitle());

        d = scan("Dead_Like_Me_S02E12 - Forget Me Not.HDTV.XviD-vfua.avi");
        assertEquals("Dead Like Me", d.getTitle());
        assertEquals(2, d.getSeason());
        assertEquals(1, d.getEpisodes().size());
        assertEquals("HDTV", d.getVideoSource());
        assertEquals("XviD", d.getVideoCodec());
        assertNull(d.getPartTitle());
        assertEquals("Forget Me Not", d.getEpisodeTitle());
    }

    public void testScanVersion() {
        MovieFileNameDTO d = scan("Ghost Rider.2007.Extended.Cut.720p.BluRay.x264.mkv");
        assertEquals("Ghost Rider", d.getTitle());
        assertEquals(2007, d.getYear());
        
        d = scan("Dark.City.1998.directors.cut.dvdrip.xvid-nodlabs.avi");
        assertEquals("Dark City", d.getTitle());
        assertEquals(1998, d.getYear());
        
        d = scan("Troy.Directors.Cut.HDDVDRip.720p.x264.HANSMER.mkv");
        assertEquals("Troy", d.getTitle());
    }
    
    public void testScanParts() {
        MovieFileNameDTO d = scan("How Music Works - part 1 of 4 - Melody.avi");
        assertEquals(-1, d.getSeason());
        assertEquals(-1, d.getPart());

        d = scan("The File [cd3]h.264.avi");
        assertEquals(-1, d.getSeason());
        assertEquals(3, d.getPart());
        assertEquals("H.264", d.getVideoCodec());

        d = scan("The File 60p part66 - The File Strikes Back.avi");
        assertEquals(-1, d.getSeason());
        assertEquals(66, d.getPart());
        assertEquals(60, d.getFps());
        assertEquals("The File", d.getTitle());
        assertEquals("The File Strikes Back", d.getPartTitle());

    }
    
    
    @SuppressWarnings("serial")
    private static MovieFileNameDTO scan(String filename) {
        File file = new File(filename) {
            @Override
            public boolean isFile() {
                return true;
            }
        };

        return MovieFilenameScanner.scan(file);
    }

}
