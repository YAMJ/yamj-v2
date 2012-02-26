/*
 *      Copyright (c) 2004-2012 YAMJ Members
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

package com.moviejukebox.allocine;

import com.moviejukebox.allocine.jaxb.*;

/**
 *  This is the TvSeason bean for the api.allocine.fr search
 *
 *  @author Yves.Blusseau
 */

public class TvSeasonInfos extends Season {

    public Episode getEpisode(int numEpisode) {
        Episode episode = null;
        for (Episode checkEpisode : getEpisodeList()) {
            if (checkEpisode.getEpisodeNumberSeason() == numEpisode) {
                episode = checkEpisode;
                break;
            }
        }
        return episode;
    }

}
