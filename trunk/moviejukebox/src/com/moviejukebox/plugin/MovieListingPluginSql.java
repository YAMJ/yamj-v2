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

import static com.moviejukebox.tvrage.tools.StringTools.isValidString;

import java.util.HashMap;
import java.util.logging.Logger;

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
    
    public void generate(Jukebox jukebox, Library library) {

        MjbSqlDb mjbSqlDb = new MjbSqlDb("./", "listing.db");
        VideoDAOSQLite videoDAO = MjbSqlDb.getVideoDAO();
        mjbDAOSQLite mjbDAO = MjbSqlDb.getMjbDAO();
        
        int videoID = 0;
        VideoDTO videoDTO;
        
        for (Movie movie : library.values()) {
            logger.fine("MovieListingPluginSql: Writing movie to SQL - " + movie.getTitle());
            videoDTO = new VideoDTO();

            try {
                videoID = mjbDAO.getLastVideoId() + 1;
                logger.fine("Adding video #" + videoID);
            } catch (Throwable e) {
                logger.severe("SQL: Error getting next videoID");
                continue; // XXX Should we break and quit?
            }
            
            videoDTO.setId(videoID);
            videoDTO.setTitle(movie.getTitle());
            videoDTO.setCertification(movie.getCertification());
            videoDTO.setPlot(movie.getPlot());
            videoDTO.setRating(movie.getRating());
            videoDTO.setRuntime(100);
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
            } catch (Throwable e) {
                logger.severe("SQL: Error adding the video to the database: " + e.getMessage());
            }
        }
        
        try {
            // Write the Artwork to the database
            for (ArtworkDTO artwork : artworkList.values()) {
                videoDAO.insertArtwork(artwork);
            }
            
            // Write the Genres to the database
            for (GenreDTO genre : genreList.values()) {
                videoDAO.insertGenre(genre);
            }
            
            // Write the People to the database
            for (PersonDTO person : personList.values()) {
                videoDAO.insertPerson(person);
            }
        } catch (Throwable e) {
            logger.severe("SQL: Error adding data to the database: " + e.getMessage());
        }
        
        mjbSqlDb.close();
    }

    private void addGenre(String genre) {
        if (!isValidString(genre)) {
            return;
        }

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
