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

package com.moviejukebox.plugin.poster;

import java.util.List;
import java.util.logging.Logger;

import org.pojava.datetime.DateTime;

import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.thetvdb.TheTVDB;
import com.moviejukebox.thetvdb.model.Banner;
import com.moviejukebox.thetvdb.model.Banners;
import com.moviejukebox.thetvdb.model.Series;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.ThreadExecutor;
import com.moviejukebox.tools.WebBrowser;

public class TheTvDBPosterPlugin implements ITvShowPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static final String API_KEY = PropertiesUtil.getProperty("API_KEY_TheTVDb");

    private String language;
    private TheTVDB tvDB;
    private static String webhost = "thetvdb.com";
    private WebBrowser webBrowser;

    public TheTvDBPosterPlugin() {
        super();
        tvDB = new TheTVDB(API_KEY);
        language = PropertiesUtil.getProperty("thetvdb.language", "en");
        webBrowser = new WebBrowser();

    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        ThreadExecutor.enterIO(webhost);
        try {
            List<Series> seriesList = null;

            if (!title.equals(Movie.UNKNOWN)) {
                seriesList = tvDB.searchSeries(title, language);
            }

            if (seriesList != null && !seriesList.isEmpty()) {
                Series series = null;
                for (Series s : seriesList) {
                    if (s.getFirstAired() != null && !s.getFirstAired().isEmpty()) {
                        if (year != null && !year.equals(Movie.UNKNOWN)) {
                            try {
                                DateTime firstAired = DateTime.parse(s.getFirstAired());
                                if (Integer.parseInt(firstAired.toString("yyyy")) == Integer.parseInt(year)) {
                                    series = s;
                                    break;
                                }
                            } catch (Exception ignore) {
                            }
                        } else {
                            series = s;
                            break;
                        }
                    }
                }
                if (series == null) {
                    series = seriesList.get(0);
                }
                response = series.getId();
            }

        } catch (Exception e) {
            logger.severe("Failed retreiving TvDb Id for movie : " + title);
            logger.severe("Error : " + e.getMessage());
        }
        ThreadExecutor.leaveIO();
        return response;
    }

    @Override
    public IImage getPosterUrl(String id, int season) {
        String posterURL = Movie.UNKNOWN;
        ThreadExecutor.enterIO(webhost);
        
        try {
            // TODO Fix the TheTvDB API banner.language issue
            StringBuffer sb = new StringBuffer("http://www.thetvdb.com/data/series/");
            sb.append(id);
            sb.append("/banners.xml");
            String xmlBanners = webBrowser.request(sb.toString());
        
            if (!(id.equals(Movie.UNKNOWN) || (id.equals("-1"))) || (id.equals("0"))) {
                String urlNormal = null;
                Banners banners = tvDB.getBanners(id);

                if (!banners.getSeasonList().isEmpty()) {
                    for (Banner banner : banners.getSeasonList()) {
                        // Grab only localized banner
                        if (checkBannerLanguage(banner.getUrl(), xmlBanners) && (banner.getSeason() == season)) { // only check for the correct season
                            if (urlNormal == null && banner.getBannerType2().equalsIgnoreCase("season")) {
                                urlNormal = banner.getUrl();
                            }

                            if (urlNormal != null) {
                                break;
                            }
                        }
                    }
                }
                if (urlNormal == null && !banners.getPosterList().isEmpty()) {
                    urlNormal = banners.getPosterList().get(0).getUrl();
                }
                if (urlNormal == null) {
                    Series series = tvDB.getSeries(id, language);
                    if (series.getPoster() != null && !series.getPoster().isEmpty()) {
                        urlNormal = series.getPoster();
                    }
                }

                if (urlNormal != null) {
                    posterURL = urlNormal;
                }
            }
            ThreadExecutor.leaveIO();
            if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
                return new Image(posterURL);
            }
        } catch (Exception error) {
            logger.severe("TheTvDBPlugin: Failed to retrieve alloCine Id for movie : " + id);
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year, int tvSeason) {
        return getPosterUrl(getIdFromMovieInfo(title, year, tvSeason), tvSeason);
    }

    @Override
    public String getName() {
        return "thetvdb";
    }

    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);
        if (Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (movieInformation.isTVShow()) {
                id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear(), movieInformation.getSeason());
            }
            // Id found
            if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
                ident.setId(getName(), id);
            }
        }

        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            if (movieInformation.isTVShow()) {
                return getPosterUrl(id, movieInformation.getSeason());
            }

        }
        return Image.UNKNOWN;
    }

    private String getId(Identifiable ident) {
        String response = Movie.UNKNOWN;
        if (ident != null) {
            String id = ident.getId(this.getName());
            if (id != null) {
                response = id;
            }
        }
        return response;
    }
    
    private boolean checkBannerLanguage(String bannerURL, String xml) {
        for (String bannerXML : HTMLTools.extractTags(xml, "<Banners>", "</Banners>", "<Banner>", "</Banner>", false)) {
            if (bannerURL.endsWith(HTMLTools.extractTag(bannerXML, "<BannerPath>", "</BannerPath>"))) {
                if (HTMLTools.extractTag(bannerXML, "<Language>", "</Language>").equalsIgnoreCase(language)) {
                    logger.finer("TheTVDB plugin found a " + language + " banner : " + bannerURL);
                    return true;
                }
            }
        }
        return false;
    }
}
