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
package com.moviejukebox.allocine;

import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviejukebox.allocine.jaxb.ActivityType;
import com.moviejukebox.allocine.jaxb.CastMember;
import com.moviejukebox.allocine.jaxb.Distributor;
import com.moviejukebox.allocine.jaxb.Episode;
import com.moviejukebox.allocine.jaxb.HtmlSynopsisType;
import com.moviejukebox.allocine.jaxb.Media;
import com.moviejukebox.allocine.jaxb.Movie;
import com.moviejukebox.allocine.jaxb.Person;
import com.moviejukebox.allocine.jaxb.PosterType;
import com.moviejukebox.allocine.jaxb.RatingType;
import com.moviejukebox.allocine.jaxb.Release;
import com.moviejukebox.allocine.jaxb.Season;
import com.moviejukebox.allocine.jaxb.Statistics;
import com.moviejukebox.allocine.jaxb.ThumbnailType;
import com.moviejukebox.allocine.jaxb.Tvseries;
import com.moviejukebox.allocine.jaxb.TypeType;

/**
 * Implementation for JSON format
 *
 * @author modmax
 */
public final class JSONAllocineAPIHelper extends AbstractAllocineAPI {

    /**
     * The JSON object mapper
     */
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructor.
     *
     * @param apiKey The API key for allocine
     */
    public JSONAllocineAPIHelper(String apiKey) {
        super(apiKey, "json");
    }

    @Override
    public Search searchMovieInfos(String query) throws Exception {
        JsonNode rootNode = null;
        URLConnection connection = null;
        InputStream inputStream = null;

        try {
            connection = connectSearchMovieInfos(query);
            inputStream = connection.getInputStream();

            rootNode = mapper.readTree(inputStream);
        } finally {
            close(connection, inputStream);
        }

        Search search = new Search();
        if (rootNode == null || rootNode.isNull()) {
            return search;
        }
        
        JsonNode feedNode = rootNode.get("feed");
        if (feedNode != null && !feedNode.isNull()) {
            JsonNode moviesNode = feedNode.get("movie");
            if (moviesNode != null && !moviesNode.isNull() && moviesNode.size() > 0) {
                for (int i=0; i < moviesNode.size(); i++) {
                    JsonNode movieNode = moviesNode.get(i);
                    if (movieNode != null && !movieNode.isNull()) {
                        Movie movie = new Movie();
                        movie.setCode(getValueAsInt(movieNode.get("code")));
                        movie.setTitle(getValueAsString(movieNode.get("title")));
                        movie.setOriginalTitle(getValueAsString(movieNode.get("originalTitle")));
                        movie.setProductionYear(getValueAsString(movieNode.get("productionYear")));
                        // enough values for search result
    
                        search.getMovie().add(movie);
                    }
                }
                
                // NOTE: pagination not supported; so totalsResults may be higher than count
                search.setTotalResults(getValueAsInt(feedNode.get("totalResults"))); 
            }
        }
        return search;
    }

    @Override
    public Search searchTvseriesInfos(String query) throws Exception {
        JsonNode rootNode = null;
        URLConnection connection = null;
        InputStream inputStream = null;

        try {
            connection = connectSearchTvseriesInfos(query);
            inputStream = connection.getInputStream();

            rootNode = mapper.readTree(inputStream);
        } finally {
            close(connection, inputStream);
        }

        Search search = new Search();
        if (rootNode == null || rootNode.isNull()) {
            return search;
        }
        
        JsonNode feedNode = rootNode.get("feed");
        if (feedNode != null && !feedNode.isNull())  {
            JsonNode tvseriesNode = feedNode.get("tvseries");
            if (tvseriesNode != null && !tvseriesNode.isNull() && tvseriesNode.size() > 0) {
                for (int i=0; i < tvseriesNode.size(); i++) {
                    JsonNode tvserieNode = tvseriesNode.get(i);
                    if (tvserieNode != null && !tvserieNode.isNull()) {
                        Tvseries tvseries = new Tvseries();
                        tvseries.setCode(getValueAsInt(tvserieNode.get("code")));
                        tvseries.setTitle(getValueAsString(tvserieNode.get("title")));
                        tvseries.setOriginalTitle(getValueAsString(tvserieNode.get("originalTitle")));
                        tvseries.setYearStart(getValueAsString(tvserieNode.get("yearStart")));
                        tvseries.setYearEnd(getValueAsString(tvserieNode.get("yearEnd")));
                        // enough values for search result
                        
                        search.getTvseries().add(tvseries);
                    }
                }

                // NOTE: pagination not supported; so totalsResults may be higher than count
                search.setTotalResults(getValueAsInt(feedNode.get("totalResults"))); 
            }
        }
        return search;
    }

    @Override
    public MovieInfos getMovieInfos(String allocineId) throws Exception {
        JsonNode rootNode = null;
        URLConnection connection = null;
        InputStream inputStream = null;

        try {
            connection = connectGetMovieInfos(allocineId);
            inputStream = connection.getInputStream();

            rootNode = mapper.readTree(inputStream);
        } finally {
            close(connection, inputStream);
        }

        MovieInfos infos = new MovieInfos();
        if (rootNode == null || rootNode.isNull()) {
            return infos;
        }
        
        JsonNode movieNode = rootNode.get("movie");
        if (movieNode != null && !movieNode.isNull()) {
            infos.setCode(getValueAsInt(movieNode.get("code")));
            infos.setTitle(getValueAsString(movieNode.get("title")));
            infos.setOriginalTitle(getValueAsString(movieNode.get("originalTitle")));
            infos.setProductionYear(getValueAsString(movieNode.get("productionYear")));
            infos.setRuntime(getValueAsInt(movieNode.get("runtime")));
            
            // parse synopsis
            infos.setHtmlSynopsis(parseHtmlSynopsis(movieNode));

            // parse release
            infos.setRelease(parseRelease(movieNode));

            // parse poster
            infos.setPoster(parsePosterType(movieNode));

            // parse genres
            parseListValues(infos.getGenreList(), movieNode.get("genre"));

            // parse nationality
            parseListValues(infos.getNationalityList(), movieNode.get("nationality"));

            // parse certificates
            parseListValues(infos.getMovieCertificate(), movieNode.get("movieCertificate"));
             
            // parse media
            parseMedia(infos.getMediaList(), movieNode);

            // parse casting
            parseCasting(infos.getCasting(), movieNode);
            
            // parse statistics
            infos.setStatistics(parseStatistics(movieNode));
        }
        
        return infos;
    }

    @Override
    public TvSeriesInfos getTvSeriesInfos(String allocineId) throws Exception {
        JsonNode rootNode = null;
        URLConnection connection = null;
        InputStream inputStream = null;

        try {
            connection = connectGetTvSeriesInfos(allocineId);
            inputStream = connection.getInputStream();

            rootNode = mapper.readTree(inputStream);
        } finally {
            close(connection, inputStream);
        }

        TvSeriesInfos infos = new TvSeriesInfos();
        if (rootNode == null || rootNode.isNull()) {
            return infos;
        }
        
        JsonNode tvseriesNode = rootNode.get("tvseries");
        if (tvseriesNode != null && !tvseriesNode.isNull()) {
            infos.setCode(getValueAsInt(tvseriesNode.get("code")));
            infos.setTitle(getValueAsString(tvseriesNode.get("title")));
            infos.setOriginalTitle(getValueAsString(tvseriesNode.get("originalTitle")));
            infos.setYearStart(getValueAsString(tvseriesNode.get("yearStart")));
            infos.setYearEnd(getValueAsString(tvseriesNode.get("yearEnd")));
            infos.setSeasonCount(getValueAsInt(tvseriesNode.get("seasonCount")));
            
            // parse original channel
            infos.setOriginalChannel(parseOriginalChannel(tvseriesNode));
            
            // parse synopsis
            infos.setHtmlSynopsis(parseHtmlSynopsis(tvseriesNode));

            // parse release
            infos.setRelease(parseRelease(tvseriesNode));

            // parse genres
            parseListValues(infos.getGenreList(), tvseriesNode.get("genre"));

            // parse nationality
            parseListValues(infos.getNationalityList(), tvseriesNode.get("nationality"));

            // parse media
            parseMedia(infos.getMediaList(), tvseriesNode);

            // parse casting
            parseCasting(infos.getCasting(), tvseriesNode);
            
            // parse statistics
            infos.setStatistics(parseStatistics(tvseriesNode));

            // parse seasons
            parseSeasonList(infos.getSeasonList(), tvseriesNode);
        }
        
        return infos;
    }

    @Override
    public TvSeasonInfos getTvSeasonInfos(Integer seasonCode) throws Exception {
        JsonNode rootNode = null;
        URLConnection connection = null;
        InputStream inputStream = null;

        try {
            connection = connectGetTvSeasonInfos(seasonCode);
            inputStream = connection.getInputStream();

            rootNode = mapper.readTree(inputStream);
        } finally {
            close(connection, inputStream);
        }

        TvSeasonInfos infos = new TvSeasonInfos();
        if (rootNode == null || rootNode.isNull()) {
            return infos;
        }
        
        JsonNode seasonNode = rootNode.get("season");
        if (seasonNode != null && !seasonNode.isNull()) {
            infos.setCode(getValueAsInt(seasonNode.get("code")));
            infos.setSeasonNumber(getValueAsInt(seasonNode.get("seasonNumber")));
            infos.setYearStart(getValueAsString(seasonNode.get("yearStart")));
            infos.setYearEnd(getValueAsString(seasonNode.get("yearEnd")));
            
            // parse episodes
            parseEpisodeList(infos.getEpisodeList(), seasonNode);
        }
        
        return infos;
    }
    
    private static int getValueAsInt(JsonNode node) {
        if (node == null) {
            return -1;
        } else if (node.isNull()) {
            return -1;
        }
        try {
            return Integer.parseInt(node.asText());
        } catch (Exception ignore) {
            return -1;
        }
    }

    private static float getValueAsFloat(JsonNode node) {
        if (node == null) {
            return 0f;
        } else if (node.isNull()) {
            return 0f;
        }
        try {
            return Float.parseFloat(node.asText());
        } catch (Exception ignore) {
            return 0f;
        }
    }

    private static String getValueAsString(JsonNode node) {
        if (node == null) {
            return null;
        } else if (node.isNull()) {
            return null;
        }
        return String.valueOf(node.asText());
    }

    private static void parseListValues(List<String> values, JsonNode valuesNode) {
        if (valuesNode != null && !valuesNode.isNull() && (valuesNode.size() > 0)) {
            for (int i=0; i<valuesNode.size(); i++) {
                JsonNode valueNode = valuesNode.get(i);
                if (valueNode != null && !valueNode.isNull()) {
                    String value = getValueAsString(valueNode.get("$"));
                    if (value != null) {
                        values.add(value);
                    }
                }
            }
        }
    }

    private static HtmlSynopsisType parseHtmlSynopsis(JsonNode rootNode) {
        JsonNode node = rootNode.get("synopsis");
        if (node != null && !node.isNull()) {
            HtmlSynopsisType synopsis = new HtmlSynopsisType();
            synopsis.getContent().add(getValueAsString(node));
            return synopsis;
        }
        return null;
    }

    private static Release parseRelease(JsonNode rootNode) {
        // parse release
        JsonNode node = rootNode.get("release");
        if (node != null && !node.isNull()) {
            Release release = new Release();
            release.setReleaseDate(getValueAsString(node.get("releaseDate")));
            node = node.get("distributor");
            if (node != null && !node.isNull()) {
                Distributor distributor = new Distributor();
                distributor.setName(getValueAsString(node.get("name")));
                release.setDistributor(distributor);
            }
            return release;
        }
        return null;
    }

    private static PosterType parsePosterType(JsonNode rootNode) {
        JsonNode node = rootNode.get("poster");
        if (node != null && !node.isNull()) {
            String href = getValueAsString(node.get("href"));
            if (href != null) {
                PosterType posterType = new PosterType();
                posterType.setHref(href);
                return posterType;
            }
        }
        return null;
    }
    
    private static Statistics parseStatistics(JsonNode rootNode) {
        JsonNode node = rootNode.get("statistics");
        if (node != null && !node.isNull()) {
            JsonNode ratingNode = node.get("rating");
            if (ratingNode != null && !ratingNode.isNull() && (ratingNode.size() > 0)) {
                Statistics statistics = new Statistics();
                for (int i=0; i<ratingNode.size(); i++) {
                    node = ratingNode.get(i);
                    if (node != null && !node.isNull()) {
                        RatingType ratingType = new RatingType();
                        ratingType.setNote(getValueAsFloat(node.get("note")));
                        ratingType.setValue(getValueAsInt(node.get("$")));
                        statistics.getRatingStats().add(ratingType);
                    }
                }
                return statistics;
            }
        }
        return null;
    }
 
    private static String parseOriginalChannel(JsonNode rootNode) {
        JsonNode node = rootNode.get("originalChannel");
        if (node != null && !node.isNull()) {
            return getValueAsString(node.get("$"));
        }
        return null;
    }
    
    private static void parseMedia(List<Media> mediaList, JsonNode rootNode) {
        JsonNode node = rootNode.get("media");
        if (node != null && !node.isNull() && (node.size() > 0)) {
            for (int i=0; i<node.size(); i++) {
                JsonNode mediaNode = node.get(i);
                if (mediaNode != null && !mediaNode.isNull()) {
                    Media media = new Media();
                    
                    JsonNode innerNode = mediaNode.get("type");
                    if (innerNode != null && !innerNode.isNull()) {
                        TypeType typeType = new TypeType();
                        typeType.setCode(getValueAsInt(innerNode.get("code")));
                        media.setType(typeType);
                   }
    
                    innerNode= mediaNode.get("thumbnail");
                    if (innerNode != null && !innerNode.isNull()) {
                        ThumbnailType thumbnailType = new ThumbnailType();
                        thumbnailType.setHref(getValueAsString(innerNode.get("href")));
                        media.setThumbnail(thumbnailType);
                    }
                    
                    mediaList.add(media);
                }
            }
        }
    }

    private static void parseCasting(List<CastMember> members, JsonNode rootNode) {
        JsonNode node = rootNode.get("castMember");
        if (node != null && !node.isNull() && (node.size() > 0)) {
            for (int i=0; i<node.size(); i++) {
                JsonNode memberNode = node.get(i);
                if (memberNode != null && !memberNode.isNull()) {
    
                    CastMember member = new CastMember();
                    member.setRole(getValueAsString(memberNode.get("role")));
                    
                    JsonNode personNode = memberNode.get("person");
                    if (personNode != null && !personNode.isNull()) {
                        Person person = new Person();
                        person.setCode(getValueAsInt(personNode.get("code")));
                        person.setName(getValueAsString(personNode.get("name")));
                        member.setPerson(person);
                    }
                    
                    JsonNode activityNode = memberNode.get("activity");
                    if (activityNode != null && !activityNode.isNull()) {
                        ActivityType activityType = new ActivityType();
                        activityType.setCode(Integer.valueOf(getValueAsInt(activityNode.get("code"))));
                        member.setActivity(activityType);
                    }
                    
                    members.add(member);
                }
            }
        }
    }
    
    private static void parseSeasonList(List<Season> seasons, JsonNode rootNode) {
        JsonNode node = rootNode.get("season");
        if (node != null && !node.isNull() && (node.size() > 0)) {
            for (int i=0; i<node.size(); i++) {
                JsonNode seasonNode = node.get(i);
                if (seasonNode != null && !seasonNode.isNull()) {
                    Season season = new Season();
                    season.setCode(getValueAsInt(seasonNode.get("code")));
                    season.setSeasonNumber(getValueAsInt(seasonNode.get("seasonNumber")));
                    season.setYearStart(getValueAsString(seasonNode.get("yearStart")));
                    season.setYearEnd(getValueAsString(seasonNode.get("yearEnd")));
                    seasons.add(season);
                }
            }
        }
    }

    private static void parseEpisodeList(List<Episode> episodes, JsonNode rootNode) {
        JsonNode node = rootNode.get("episode");
        if (node != null && !node.isNull() && (node.size() > 0)) {
            for (int i=0; i<node.size(); i++) {
                JsonNode episodeNode = node.get(i);
                if (episodeNode != null && !episodeNode.isNull()) {
                    Episode episode = new Episode();
                    episode.setCode(getValueAsInt(episodeNode.get("code")));
                    episode.setTitle(getValueAsString(episodeNode.get("title")));
                    episode.setOriginalTitle(getValueAsString(episodeNode.get("originalTitle")));
                    episode.setEpisodeNumberSeries(getValueAsInt(episodeNode.get("episodeNumberSeries")));
                    episode.setEpisodeNumberSeason(getValueAsInt(episodeNode.get("episodeNumberSeason")));
                    episode.setSynopsis(getValueAsString(episodeNode.get("synopsis")));
                    episodes.add(episode);
                }
            }
        }
    }
}
