/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.tools;

import com.moviejukebox.model.Filmography;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.plugin.DatabasePluginController;
import static com.moviejukebox.plugin.DatabasePluginController.TYPE_ALTERNATE;
import com.moviejukebox.plugin.ImdbPlugin;
import java.util.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Holds some override tools.
 *
 * @author modmax
 */
public final class OverrideTools {

    private static final Logger LOGGER = Logger.getLogger(OverrideTools.class);
    private static final String LOG_MESSAGE = "OverrideTools: ";
    private static final Map<OverrideFlag, List<String>> MOVIE_PRIORITIES_MAP = new EnumMap<OverrideFlag, List<String>>(OverrideFlag.class);
    private static final Map<OverrideFlag, List<String>> TV_PRIORITIES_MAP = new EnumMap<OverrideFlag, List<String>>(OverrideFlag.class);
    // check skip if not in priority list
    private static final boolean SKIP_NOT_IN_LIST = PropertiesUtil.getBooleanProperty("priority.checks.skipNotInList", Boolean.FALSE);
    // hold max counts for people
    private static final int MAX_COUNT_ACTOR = PropertiesUtil.getReplacedIntProperty("movie.actor.maxCount", "plugin.people.maxCount.actor", 10);
    private static final int MAX_COUNT_DIRECTOR = PropertiesUtil.getReplacedIntProperty("movie.director.maxCount", "plugin.people.maxCount.director", 2);
    private static final int MAX_COUNT_WRITER = PropertiesUtil.getReplacedIntProperty("movie.writer.maxCount", "plugin.people.maxCount.writer", 3);
    // holds inclusions
    private static final boolean INCLUDE_EPISODE_PLOT = PropertiesUtil.getBooleanProperty("mjb.includeEpisodePlots", Boolean.FALSE);
    private static final boolean INCLUDE_EPISODE_RATING = PropertiesUtil.getBooleanProperty("mjb.includeEpisodeRating", Boolean.FALSE);
    // handling for set default plugins
    private static final String TYPE_PLUGIN = "PLUGIN";
    private static final String MOVIE_PLUGIN = DatabasePluginController.getMovieDatabasePluginName(Movie.TYPE_MOVIE).toUpperCase();
    private static final String TVSHOW_PLUGIN = DatabasePluginController.getMovieDatabasePluginName(Movie.TYPE_TVSHOW).toUpperCase();
    private static final String ALTERNATE_PLUGIN = DatabasePluginController.getMovieDatabasePluginName(TYPE_ALTERNATE).toUpperCase();
    private static final String IMDB_PLUGIN = ImdbPlugin.IMDB_PLUGIN_ID.toUpperCase();

    static {
        String sources;

        // actors
        sources = PropertiesUtil.getProperty("priority.movie.actors", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.ACTORS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.actors", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.ACTORS, sources);
        // aspect ratio
        sources = PropertiesUtil.getProperty("priority.movie.aspectratio", "nfo,mediainfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.ASPECTRATIO, sources);
        sources = PropertiesUtil.getProperty("priority.tv.aspectratio", "nfo,mediainfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.ASPECTRATIO, sources);
        // certification
        sources = PropertiesUtil.getProperty("priority.movie.certification", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.CERTIFICATION, sources);
        sources = PropertiesUtil.getProperty("priority.tv.certification", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.CERTIFICATION, sources);
        // company
        sources = PropertiesUtil.getProperty("priority.movie.company", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.COMPANY, sources);
        sources = PropertiesUtil.getProperty("priority.tv.company", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.COMPANY, sources);
        // container
        sources = PropertiesUtil.getProperty("priority.movie.container", "nfo,mediainfo,filename");
        putMoviePriorities(OverrideFlag.CONTAINER, sources);
        sources = PropertiesUtil.getProperty("priority.tv.container", "nfo,mediainfo,filename");
        putTvPriorities(OverrideFlag.CONTAINER, sources);
        // country
        sources = PropertiesUtil.getProperty("priority.movie.country", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.COUNTRY, sources);
        sources = PropertiesUtil.getProperty("priority.tv.country", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.COUNTRY, sources);
        // directors
        sources = PropertiesUtil.getProperty("priority.movie.directors", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.DIRECTORS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.directors", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.DIRECTORS, sources);
        // genres
        sources = PropertiesUtil.getProperty("priority.movie.genres", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.GENRES, sources);
        sources = PropertiesUtil.getProperty("priority.tv.genres", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.GENRES, sources);
        // frames per seconds
        sources = PropertiesUtil.getProperty("priority.movie.fps", "nfo,mediainfo,filename");
        putMoviePriorities(OverrideFlag.FPS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.fps", "nfo,mediainfo,filename");
        putTvPriorities(OverrideFlag.FPS, sources);
        // language
        sources = PropertiesUtil.getProperty("priority.movie.language", "nfo,mediainfo,PLUGIN,ALTERNATE,filename");
        putMoviePriorities(OverrideFlag.LANGUAGE, sources);
        sources = PropertiesUtil.getProperty("priority.tv.language", "nfo,mediainfo,PLUGIN,ALTERNATE,filename");
        putTvPriorities(OverrideFlag.LANGUAGE, sources);
        // original title
        sources = PropertiesUtil.getProperty("priority.movie.originaltitle", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.ORIGINALTITLE, sources);
        sources = PropertiesUtil.getProperty("priority.tv.originaltitle", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.ORIGINALTITLE, sources);
        // outline
        sources = PropertiesUtil.getProperty("priority.movie.outline", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.OUTLINE, sources);
        sources = PropertiesUtil.getProperty("priority.tv.outline", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.OUTLINE, sources);
        // outline
        sources = PropertiesUtil.getProperty("priority.movie.plot", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.PLOT, sources);
        sources = PropertiesUtil.getProperty("priority.tv.plot", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.PLOT, sources);
        // quote
        sources = PropertiesUtil.getProperty("priority.movie.quote", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.QUOTE, sources);
        sources = PropertiesUtil.getProperty("priority.tv.quote", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.QUOTE, sources);
        // release date
        sources = PropertiesUtil.getProperty("priority.movie.releasedate", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.RELEASEDATE, sources);
        sources = PropertiesUtil.getProperty("priority.tv.releasedate", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.RELEASEDATE, sources);
        // resolution
        sources = PropertiesUtil.getProperty("priority.movie.resolution", "nfo,mediainfo");
        putMoviePriorities(OverrideFlag.RESOLUTION, sources);
        sources = PropertiesUtil.getProperty("priority.tv.resolution", "nfo,mediainfo");
        putTvPriorities(OverrideFlag.RESOLUTION, sources);
        // runtime
        sources = PropertiesUtil.getProperty("priority.movie.runtime", "nfo,mediainfo,filename,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.RUNTIME, sources);
        sources = PropertiesUtil.getProperty("priority.tv.runtime", "nfo,mediainfo,filename,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.RUNTIME, sources);
        // tagline
        sources = PropertiesUtil.getProperty("priority.movie.tagline", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.TAGLINE, sources);
        sources = PropertiesUtil.getProperty("priority.tv.tagline", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.TAGLINE, sources);
        // title
        sources = PropertiesUtil.getProperty("priority.movie.title", "nfo,PLUGIN,ALTERNATE,filename");
        putMoviePriorities(OverrideFlag.TITLE, sources);
        sources = PropertiesUtil.getProperty("priority.tv.title", "nfo,PLUGIN,ALTERNATE,filename");
        putTvPriorities(OverrideFlag.TITLE, sources);
        // video output
        sources = PropertiesUtil.getProperty("priority.movie.videooutput", "nfo,mediainfo,filename");
        putMoviePriorities(OverrideFlag.VIDEOOUTPUT, sources);
        sources = PropertiesUtil.getProperty("priority.tv.videooutput", "nfo,mediainfo,filename");
        putTvPriorities(OverrideFlag.VIDEOOUTPUT, sources);
        // video source
        sources = PropertiesUtil.getProperty("priority.movie.videosource", "nfo,filename,mediainfo");
        putMoviePriorities(OverrideFlag.VIDEOSOURCE, sources);
        sources = PropertiesUtil.getProperty("priority.tv.videosource", "nfo,filename,mediainfo");
        putTvPriorities(OverrideFlag.VIDEOSOURCE, sources);
        // writers
        sources = PropertiesUtil.getProperty("priority.movie.writers", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.WRITERS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.writers", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.WRITERS, sources);
        // year
        sources = PropertiesUtil.getProperty("priority.movie.year", "nfo,PLUGIN,ALTERNATE,filename");
        putMoviePriorities(OverrideFlag.YEAR, sources);
        sources = PropertiesUtil.getProperty("priority.tv.year", "nfo,PLUGIN,ALTERNATE,filename");
        putTvPriorities(OverrideFlag.YEAR, sources);
        
        // EXTRA properties for people scraping (filmography)

        // actors
        sources = PropertiesUtil.getProperty("priority.movie.people.actors", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.PEOPLE_ACTORS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.people.actors", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.PEOPLE_ACTORS, sources);
        // directors
        sources = PropertiesUtil.getProperty("priority.movie.people.directors", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.PEOPLE_DIRECTORS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.people.directors", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.PEOPLE_DIRECTORS, sources);
        // writers
        sources = PropertiesUtil.getProperty("priority.movie.people.writers", "nfo,PLUGIN,ALTERNATE");
        putMoviePriorities(OverrideFlag.PEOPLE_WRITERS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.people.writers", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.PEOPLE_WRITERS, sources);

        // EXTRA properties for episodes
        
        // episode first aired
        sources = PropertiesUtil.getProperty("priority.tv.episode.firstaired", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.EPISODE_FIRST_AIRED, sources);
        // episode plot
        sources = PropertiesUtil.getProperty("priority.tv.episode.plot", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.EPISODE_PLOT, sources);
        // episode rating
        sources = PropertiesUtil.getProperty("priority.tv.episode.rating", "nfo,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.EPISODE_RATING, sources);
        // episode title
        sources = PropertiesUtil.getProperty("priority.tv.episode.title", "nfo,filename,PLUGIN,ALTERNATE");
        putTvPriorities(OverrideFlag.EPISODE_TITLE, sources);
}

    /**
     * Put movie priorities into map.
     *
     * @param overrideFlag
     * @param sources
     */
    private static void putMoviePriorities(OverrideFlag overrideFlag, String sources) {
        List<String> priorities;
        if (StringUtils.isBlank(sources)) {
            priorities = Collections.emptyList();
        } else {
            sources = sources.toUpperCase();
            if (sources.contains(TYPE_PLUGIN) && !sources.contains(MOVIE_PLUGIN)) {
                // replace pattern with database plugin
                sources = sources.replace(TYPE_PLUGIN, MOVIE_PLUGIN);
            }
            if (sources.contains(TYPE_ALTERNATE)) {
                if (StringTools.isValidString(ALTERNATE_PLUGIN) && !sources.contains(ALTERNATE_PLUGIN)) {
                    // replace pattern with alternate plugin
                    sources = sources.replace(TYPE_ALTERNATE, ALTERNATE_PLUGIN);
                } else if (!sources.contains(IMDB_PLUGIN)) {
                    // cause: most plugins extend the IMDB plugin
                    sources = sources.replace(TYPE_ALTERNATE, IMDB_PLUGIN);
                }
            }

            priorities = new ArrayList<String>(Arrays.asList(sources.split(",")));
            priorities.remove(TYPE_PLUGIN);
            priorities.remove(TYPE_ALTERNATE);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(LOG_MESSAGE + overrideFlag.name() + " (Movie) priorities " + priorities.toString().toLowerCase());
            }
        }
        MOVIE_PRIORITIES_MAP.put(overrideFlag, priorities);
    }

    /**
     * Put movie priorities into map.
     *
     * @param overrideFlag
     * @param sources
     */
    private static void putTvPriorities(OverrideFlag overrideFlag, String sources) {
        List<String> priorities;
        if (StringUtils.isBlank(sources)) {
            priorities = Collections.emptyList();
        } else {
            sources = sources.toUpperCase();
            if (sources.contains(TYPE_PLUGIN) && !sources.contains(TVSHOW_PLUGIN)) {
                // replace pattern with database plugin
                sources = sources.replace(TYPE_PLUGIN, TVSHOW_PLUGIN);
            }
            if (sources.contains(TYPE_ALTERNATE)) {
                if (StringTools.isValidString(ALTERNATE_PLUGIN) && !sources.contains(ALTERNATE_PLUGIN)) {
                    // replace pattern with alternate plugin
                    sources = sources.replace(TYPE_ALTERNATE, ALTERNATE_PLUGIN);
                } else if (!sources.contains(IMDB_PLUGIN)) {
                    // cause: most plugins extend the IMDB plugin
                    sources = sources.replace(TYPE_ALTERNATE, IMDB_PLUGIN);
                }
            }

            priorities = new ArrayList<String>(Arrays.asList(sources.split(",")));
            priorities.remove(TYPE_PLUGIN);
            priorities.remove(TYPE_ALTERNATE);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(LOG_MESSAGE + overrideFlag.name() + " (TV) priorities " + priorities.toString().toLowerCase());
            }
        }
        TV_PRIORITIES_MAP.put(overrideFlag, priorities);
    }

    private static boolean skipCheck(Movie movie, OverrideFlag overrideFlag, String source) {
        if (SKIP_NOT_IN_LIST) {

            int index = -1;
            try {
                if (movie.isTVShow()) {
                    index = TV_PRIORITIES_MAP.get(overrideFlag).indexOf(source.toUpperCase());
                } else {
                    index = MOVIE_PRIORITIES_MAP.get(overrideFlag).indexOf(source.toUpperCase());
                }
            } catch (Exception ignore) {
                // ignore this error
            }

            // index < 0 means: not in list, so skip the check
            return (index < 0);
        }

        // no skip
        return Boolean.FALSE;
    }

    private static boolean skipCheck(MovieFile movieFile, OverrideFlag overrideFlag, String source) {
        if (SKIP_NOT_IN_LIST) {

            int index = -1;
            try {
                index = TV_PRIORITIES_MAP.get(overrideFlag).indexOf(source.toUpperCase());
            } catch (Exception ignore) {
                // ignore this error
            }

            // index < 0 means: not in list, so skip the check
            return (index < 0);
        }

        // no skip
        return Boolean.FALSE;
    }

    /**
     * Check the priority of a property to set.
     *
     * @param property the property to test
     * @param actualSource the actual source
     * @param newSource the new source
     * @return true, if new source has higher property than actual source, else false
     */
    private static boolean hasHigherPriority(final OverrideFlag overrideFlag, final String actualSource, final String newSource, boolean isTV) {
        // check sources
        if (StringTools.isNotValidString(newSource)) {
            // new source is not valid
            // -> actual source has higher priority
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(actualSource)) {
            // actual source is not valid
            // -> new source has higher priority
            return Boolean.TRUE;
        } else if (actualSource.equalsIgnoreCase(newSource)) {
            // same source may override itself
            return Boolean.TRUE;
        }

        // both sources are valid so get priorities
        List<String> priorities;
        if (isTV) {
            priorities = TV_PRIORITIES_MAP.get(overrideFlag);
        } else {
            priorities = MOVIE_PRIORITIES_MAP.get(overrideFlag);
        }

        // get and check new priority
        int newPrio = priorities.indexOf(newSource.toUpperCase());
        if (newPrio == -1) {
            // priority for new source not found
            // -> actual source has higher priority
            return Boolean.FALSE;
        }

        // check actual priority
        int actualPrio = priorities.indexOf(actualSource.toUpperCase());
        if ((actualPrio == -1) || (newPrio <= actualPrio)) {
            // -> new source has higher priority
            return Boolean.TRUE;
        }

        // -> actual source has higher priority
        return Boolean.FALSE;
    }

    private static boolean checkOverwrite(Movie movie, OverrideFlag overrideFlag, String source) {
        String actualSource = movie.getOverrideSource(overrideFlag);
        return OverrideTools.hasHigherPriority(overrideFlag, actualSource, source, movie.isTVShow());
    }

    private static boolean checkOverwrite(MovieFile movieFile, OverrideFlag overrideFlag, String source) {
        String actualSource = movieFile.getOverrideSource(overrideFlag);
        return OverrideTools.hasHigherPriority(overrideFlag, actualSource, source, Boolean.TRUE);
    }

    public static boolean checkOneOverwrite(Movie movie, String source, OverrideFlag... overrideFlags) {
        for (OverrideFlag overrideFlag : overrideFlags) {
            boolean check;
            switch (overrideFlag) {
                case OUTLINE:
                    check = checkOverwriteOutline(movie, source);
                    break;
                case PLOT:
                    check = checkOverwritePlot(movie, source);
                    break;
                case TITLE:
                    check = checkOverwriteTitle(movie, source);
                    break;
                case RUNTIME:
                    check = checkOverwriteRuntime(movie, source);
                    break;
                default:
                    check = checkOverwrite(movie, overrideFlag, source);
                    break;

                // TODO until now these checks are enough
            }
            if (check) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkOverwriteActors(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.ACTORS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_ACTOR <= 0) {
            // regard no actors
            return Boolean.FALSE;
        } else if (movie.getCast() == null || movie.getCast().isEmpty()) {
            // no actors set until now
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.ACTORS, source);
    }

    public static boolean checkOverwriteAspectRatio(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.ASPECTRATIO, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getAspectRatio())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.ASPECTRATIO, source);
    }

    public static boolean checkOverwriteCertification(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.CERTIFICATION, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getCertification())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.CERTIFICATION, source);
    }

    public static boolean checkOverwriteCompany(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.COMPANY, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getCompany())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.COMPANY, source);
    }

    public static boolean checkOverwriteContainer(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.CONTAINER, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getContainer())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.CONTAINER, source);
    }

    public static boolean checkOverwriteCountry(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.COUNTRY, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getCountry())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.COUNTRY, source);
    }

    public static boolean checkOverwriteDirectors(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.DIRECTORS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_DIRECTOR <= 0) {
            // regard no directors
            return Boolean.FALSE;
        } else if (movie.getDirectors() == null || movie.getDirectors().isEmpty()) {
            // no directors set until now
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.DIRECTORS, source);
    }

    public static boolean checkOverwriteGenres(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.GENRES, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (movie.getGenres() == null || movie.getGenres().isEmpty()) {
            // no genres set until now
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.GENRES, source);
    }

    public static boolean checkOverwriteFPS(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.FPS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (movie.getFps() == 60f) {
            // assume 60 as default value
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.FPS, source);
    }

    public static boolean checkOverwriteLanguage(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.LANGUAGE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getLanguage())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.LANGUAGE, source);
    }

    public static boolean checkOverwriteOriginalTitle(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.ORIGINALTITLE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getOriginalTitle())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.ORIGINALTITLE, source);
    }

    public static boolean checkOverwriteOutline(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.OUTLINE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getOutline())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.OUTLINE, source);
    }

    public static boolean checkOverwritePlot(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.PLOT, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getPlot())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.PLOT, source);
    }

    public static boolean checkOverwriteQuote(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.QUOTE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getQuote())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.QUOTE, source);
    }

    public static boolean checkOverwriteReleaseDate(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.RELEASEDATE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getReleaseDate())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.RELEASEDATE, source);
    }

    public static boolean checkOverwriteResolution(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.RESOLUTION, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getResolution())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.RESOLUTION, source);
    }

    public static boolean checkOverwriteRuntime(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.RUNTIME, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getRuntime())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.RUNTIME, source);
    }

    public static boolean checkOverwriteTagline(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.TAGLINE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getTagline())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.TAGLINE, source);
    }

    public static boolean checkOverwriteTitle(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.TITLE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getTitle())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.TITLE, source);
    }

    public static boolean checkOverwriteVideoOutput(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.VIDEOOUTPUT, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getVideoOutput())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.VIDEOOUTPUT, source);
    }

    public static boolean checkOverwriteVideoSource(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.VIDEOSOURCE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getVideoSource())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.VIDEOSOURCE, source);
    }

    public static boolean checkOverwriteWriters(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.WRITERS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_WRITER <= 0) {
            // regard no writers
            return Boolean.FALSE;
        } else if (movie.getWriters() == null || movie.getWriters().isEmpty()) {
            // no writers set until now
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.WRITERS, source);
    }

    public static boolean checkOverwriteYear(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.YEAR, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getYear())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.YEAR, source);
    }

    // extra for people scraping
    
    public static boolean checkOverwritePeopleActors(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.PEOPLE_ACTORS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_ACTOR <= 0) {
            // regard no actors
            return Boolean.FALSE;
        } else if (movie.getPerson(Filmography.DEPT_ACTORS).isEmpty()) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.PEOPLE_ACTORS, source);
    }

    public static boolean checkOverwritePeopleDirectors(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.PEOPLE_DIRECTORS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_DIRECTOR <= 0) {
            // regard no directors
            return Boolean.FALSE;
        } else if (movie.getPerson(Filmography.DEPT_DIRECTING).isEmpty()) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.PEOPLE_DIRECTORS, source);
    }

    public static boolean checkOverwritePeopleWriters(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.PEOPLE_WRITERS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_WRITER <= 0) {
            // regard no writers
            return Boolean.FALSE;
        } else if (movie.getPerson(Filmography.DEPT_WRITING).isEmpty()) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.PEOPLE_WRITERS, source);
    }

    // extra for TV episodes

    public static boolean checkOverwriteEpisodeFirstAired(MovieFile movieFile, int part, String source) {
        if (skipCheck(movieFile, OverrideFlag.EPISODE_FIRST_AIRED, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movieFile.getFirstAired(part))) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movieFile, OverrideFlag.EPISODE_FIRST_AIRED, source);
    }

    public static boolean checkOverwriteEpisodePlot(MovieFile movieFile, int part, String source) {
        if (!INCLUDE_EPISODE_PLOT) {
            // episode plots should not be included
            return Boolean.FALSE;
        } else if (skipCheck(movieFile, OverrideFlag.EPISODE_PLOT, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movieFile.getPlot(part))) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movieFile, OverrideFlag.EPISODE_PLOT, source);
    }

    public static boolean checkOverwriteEpisodeRating(MovieFile movieFile, int part, String source) {
        if (!INCLUDE_EPISODE_RATING) {
            // episode ratings should not be included
            return Boolean.FALSE;
        } else if (skipCheck(movieFile, OverrideFlag.EPISODE_RATING, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movieFile.getRating(part))) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movieFile, OverrideFlag.EPISODE_RATING, source);
    }

    public static boolean checkOverwriteEpisodeTitle(MovieFile movieFile, int part, String source) {
        if (skipCheck(movieFile, OverrideFlag.EPISODE_TITLE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movieFile.getTitle(part))) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movieFile, OverrideFlag.EPISODE_TITLE, source);
    }
}
