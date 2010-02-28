package com.moviejukebox.plugin.poster;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.AllocinePlugin;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class AllocinePosterPlugin implements IPosterPlugin {
    protected static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;
    private AllocinePlugin allocinePlugin;

    public AllocinePosterPlugin() {
        super();
        webBrowser = new WebBrowser();
        allocinePlugin = new AllocinePlugin();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        // Check alloCine first only for movies because TV Show posters are wrong.
        if (tvSeason == -1) {
            try {
                response = allocinePlugin.getAllocineId(title, year, tvSeason);
            } catch (ParseException error) {
                logger.severe("AllocinePosterPlugin: Failed retreiving poster id movie : " + title);
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        }
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            String xml = "";
            try {
                xml = webBrowser.request("http://www.allocine.fr/film/fichefilm-" + id + "/affiches/");
                String posterMediaId = HTMLTools.extractTag(xml, "<a href=\"/film/fichefilm-" + id + "/affiches/detail/?cmediafile=", "\" ><img");
                if (!Movie.UNKNOWN.equalsIgnoreCase(posterMediaId)) {
                    String mediaFileURL = "http://www.allocine.fr/film/fichefilm-" + id + "/affiches/detail/?cmediafile=" + posterMediaId;
                    logger.finest("AllocinePlugin: mediaFileURL : " + mediaFileURL);
                    xml = webBrowser.request(mediaFileURL);

                    String posterURLTag = HTMLTools.extractTag(xml, "<div class=\"tac\" style=\"\">", "</div>");
                    // logger.finest("AllocinePlugin: posterURLTag : " + posterURLTag);
                    posterURL = HTMLTools.extractTag(posterURLTag, "<img src=\"", "\"");

                    if (!posterURL.equalsIgnoreCase(Movie.UNKNOWN)) {
                        logger.finest("AllocinePlugin: Movie PosterURL from Allocine: " + posterURL);
                    }
                }
            } catch (Exception error) {
                logger.severe("AllocinePlugin: Failed retreiving poster for movie : " + id);
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        }
        return posterURL;
    }

    @Override
    public String getPosterUrl(String title, String year, int tvSeason) {
        String response = Movie.UNKNOWN;
        // Check alloCine first only for movies because TV Show posters are wrong.
        if (tvSeason == -1) {
            response = getPosterUrl(getIdFromMovieInfo(title, year, tvSeason));
        }
        return response;
    }

    @Override
    public String getName() {
        return "allocine";
    }

    @Override
    public String getPosterUrl(String id, int season) {
        return getPosterUrl(id);
    }
}
