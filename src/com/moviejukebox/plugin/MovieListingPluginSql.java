/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

package com.moviejukebox.plugin;

import static com.moviejukebox.tools.StringTools.isValidString;
import static com.moviejukebox.tools.StringTools.processRuntime;

import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import org.pojava.datetime.DateTime;

import com.moviejukebox.mjbsqldb.MjbSqlDb;
import com.moviejukebox.mjbsqldb.dao.VideoDAOSQLite;
import com.moviejukebox.mjbsqldb.dao.mjbDAOSQLite;
import com.moviejukebox.mjbsqldb.dto.ArtworkDTO;
import com.moviejukebox.mjbsqldb.dto.GenreDTO;
import com.moviejukebox.mjbsqldb.dto.PersonDTO;
import com.moviejukebox.mjbsqldb.dto.VideoDTO;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;

public class MovieListingPluginSql extends MovieListingPluginBase implements MovieListingPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static HashMap<String, ArtworkDTO> artworkList = new HashMap<String, ArtworkDTO>();
    private static HashMap<String, GenreDTO> genreList = new HashMap<String, GenreDTO>();
    private static HashMap<String, PersonDTO> personList = new HashMap<String, PersonDTO>();
    
    private static VideoDAOSQLite videoDAO = MjbSqlDb.getVideoDAO();
    private static mjbDAOSQLite mjbDAO = MjbSqlDb.getMjbDAO();
    
    public void generate(Jukebox jukebox, Library library) {

        MjbSqlDb mjbSqlDb = new MjbSqlDb("./", "listing.db");
        
        int videoID = 0;
        VideoDTO videoDTO;
        
        for (Movie movie : library.values()) {
            logger.fine("MovieListingPluginSql: Writing movie to SQL - " + movie.getTitle());
            videoDTO = new VideoDTO();

            try {
                videoID = mjbDAO.getNextVideoId();
            } catch (Throwable error) {
                logger.severe("SQL: Error getting next videoID");
                continue; // XXX Should we break and quit?
            }
            
            videoDTO.setId(videoID);
            videoDTO.setMjbVersion(movie.getMjbVersion());
            videoDTO.setMjbRevision(Integer.parseInt(movie.getMjbRevision()));
            videoDTO.setMjbUpdateDate(new Date());
            videoDTO.setBaseFilename(movie.getBaseFilename());
            videoDTO.setTitle(movie.getTitle());
            videoDTO.setTitleSort(movie.getTitleSort());
            videoDTO.setTitleOriginal(movie.getOriginalTitle());
            
            if (isValidString(movie.getReleaseDate())) {
                // A bit kludgy, but it works. Use DateTime to parse the string date into a Date
                videoDTO.setReleaseDate((new DateTime(movie.getReleaseDate())).toDate());
            }
            
            videoDTO.setRating(movie.getRating());
            videoDTO.setTop250(movie.getTop250());
            videoDTO.setPlot(movie.getPlot());
            videoDTO.setOutline(movie.getOutline());
            videoDTO.setQuote(movie.getQuote());
            videoDTO.setTagline(movie.getTagline());
            videoDTO.setRuntime(processRuntime(movie.getRuntime()));
            videoDTO.setSeason(movie.getSeason());
            videoDTO.setSubtitles(movie.getSubtitles());
            videoDTO.setLibraryDescription(movie.getLibraryDescription());
            
            if (movie.isTVShow()) {
                videoDTO.setVideoType(VideoDTO.TYPE_TVSHOW);
            } else {
                videoDTO.setVideoType(VideoDTO.TYPE_MOVIE);
            }

            for (String genre : movie.getGenres()) {
                addGenre(genre);
            }
            
            for (String person : movie.getCast()) {
                addPerson(person, "Actor");
            }
            
            for (String person : movie.getDirectors()) {
                addPerson(person, "Director");
            }

            for (String person : movie.getWriters()) {
                addPerson(person, "Writer");
            }
            
            addArtwork(movie.getPosterFilename(), movie.getPosterURL(), ArtworkDTO.TYPE_POSTER, videoID);
            addArtwork(movie.getFanartFilename(), movie.getFanartURL(), ArtworkDTO.TYPE_FANART, videoID);
            addArtwork(movie.getBannerFilename(), movie.getBannerURL(), ArtworkDTO.TYPE_BANNER, videoID);
            
            try {
                videoDAO.insertVideo(videoDTO);
            } catch (Throwable error) {
                logger.severe("SQL: Error adding the video to the database: " + error.getMessage());
            }
        }
        
        try {
            int id;
            // Write the Artwork to the database
            for (ArtworkDTO artwork : artworkList.values()) {
                id = videoDAO.getArtworkId(artwork.getFilename());
                
                // If we didn't find the artwork, add it
                if (id == 0) {
                    if (artwork.getId() == 0) {
                        artwork.setId(mjbDAO.getNextArtworkId());
                    }
                    videoDAO.insertArtwork(artwork);
                }
            }
            
            // Write the Genres to the database
            for (GenreDTO genre : genreList.values()) {
                id = videoDAO.getGenreId(genre.getName());
                
                // If we didn't find the genre, add it
                if (id == 0) {
                    if (genre.getId() == 0) {
                        genre.setId(mjbDAO.getNextGenreId());
                    }
                    videoDAO.insertGenre(genre);
                }
            }
            
            // Write the People to the database
            for (PersonDTO person : personList.values()) {
                if (person.getId() == 0) {
                    person.setId(mjbDAO.getNextPersonId());
                }
                videoDAO.insertPerson(person);
            }
        } catch (Throwable error) {
            logger.severe("SQL: Error adding data to the database: " + error.getMessage());
        }
        
        mjbSqlDb.close();
    }

    private int addGenre(String genre) {
        if (!isValidString(genre)) {
            return 0;
        }

        videoDAO.insertGenre();
        
        if (genreList.containsKey(genre.toUpperCase())) {
            return;
        }
        
        GenreDTO g = new GenreDTO();
        g.setId(0);
        g.setName(genre);
        g.setForeignKey("");
        
        genreList.put(genre.toUpperCase(), g);
    }
    
    private void addArtwork(String artwork, String url, String type, int videoID) {
        if (!isValidString(artwork) || !isValidString(url) || !isValidString(type)) {
            return;
        }
        
        if (artworkList.containsKey(artwork.toUpperCase())) {
            return;
        }
        
        ArtworkDTO a = new ArtworkDTO();
        a.setId(0);
        a.setFilename(artwork);
        a.setType(type);
        a.setForeignKey("");
        a.setRelatedKey(videoID);
        a.setUrl(url);
        
        artworkList.put(artwork.toUpperCase(), a);
    }

    /**
     * Check to see if a person is in the database and add them
     * TODO: Add table to associate people with movies
     * @param person
     * @param job
     */
    private void addPerson(String person, String job) {
        if (!isValidString(person) || !isValidString(job)) {
            return;
        }
        
        if (personList.containsKey((person+job).toUpperCase())) {
            return;
        }
        
        PersonDTO p = new PersonDTO();
        p.setId(0);
        p.setName(person);
        p.setJob(job);
        p.setBiography("");
        p.setBirthday("1970-01-01");
        p.setForeignKey("");
        p.setUrl("");
        
        personList.put((person+job).toUpperCase(), p);
    }
}
