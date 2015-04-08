/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.scanner.artwork;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;

public interface IArtworkScanner {

    String scan(Jukebox jukebox, Movie movie);

    String scanLocalArtwork(Jukebox jukebox, Movie movie);

    String scanOnlineArtwork(Movie movie);

    boolean validateArtwork(IImage artworkImage);

    boolean validateArtwork(IImage artworkImage, int artworkWidth, int artworkHeight, boolean checkAspect);

    boolean saveArtworkToJukebox(Jukebox jukebox, Movie movie);

    /**
     * Updates the correct original filename based on the artwork type
     *
     * @param movie
     * @param artworkFilename
     */
    void setOriginalFilename(Movie movie, String artworkFilename);

    /**
     * Updates the correct jukebox filename based on the artwork type
     *
     * @param movie
     * @param artworkFilename
     */
    void setJukeboxFilename(Movie movie, String artworkFilename);

    /**
     * Returns the correct original filename based on the artwork type.
     *
     * This should be overridden at the artwork specific class level
     *
     * @param movie
     * @return
     */
    String getOriginalFilename(Movie movie);

    /**
     * Returns the correct jukebox filename based on the artwork type.
     *
     * This should be overridden at the artwork specific class level
     *
     * @param movie
     * @return
     */
    String getJukeboxFilename(Movie movie);

    /**
     * Updates the correct URL based on the artwork type
     *
     * @param movie
     * @param artworkUrl
     */
    void setArtworkUrl(Movie movie, String artworkUrl);

    /**
     * Returns the correct URL based on the artwork type
     *
     * @param movie
     * @return
     */
    String getArtworkUrl(Movie movie);

    /**
     * Sets the correct image plugin for the artwork type
     */
    void setArtworkImagePlugin();

    /**
     * Return the value of the appropriate "dirty" setting for the artwork and movie
     *
     * @param movie
     * @return
     */
    boolean isDirtyArtwork(Movie movie);

    /**
     * Set the appropriate "dirty" setting for the artwork and movie
     *
     * @param movie
     * @param dirty
     */
    void setDirtyArtwork(Movie movie, boolean dirty);

    /**
     * Determine if the artwork type is required.
     *
     * @return true if the artwork is required
     */
    boolean isSearchRequired();

    /**
     * Determine if the artwork type is required for local searching
     *
     * @return
     */
    boolean isSearchLocal();

    /**
     * Determine if an online search should be performed for a particular video.
     *
     * Properties should be checked as should scrape library and ID = 0/-1
     *
     * @param movie
     * @return true if online scraping should be done
     */
    boolean isSearchOnline(Movie movie);

    /**
     * Determine if an online search should be performed for a particular artwork.
     *
     * This only checks the properties and can't determine if the specific video artwork should be downloaded.
     *
     * @return true if online scraping should be done
     */
    boolean isSearchOnline();

    /**
     * Create an operating system safe filename for the jukebox artwork
     *
     * This will use the "jukebox" token for the filename
     *
     * @param movie
     * @param appendFormat Add the format to the filename
     * @return
     */
    String makeSafeJukeboxFilename(Movie movie, boolean appendFormat);

    /**
     * Create an operating system safe filename for the original artwork
     *
     * This will use the "original" token for the filename
     *
     * @param movie
     * @param appendForamt
     * @return
     */
    String makeSafeOriginalFilename(Movie movie, boolean appendForamt);
}
