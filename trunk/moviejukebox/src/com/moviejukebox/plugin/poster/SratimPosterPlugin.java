package com.moviejukebox.plugin.poster;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.ImdbInfo;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.WebBrowser;

public class SratimPosterPlugin implements IMoviePosterPlugin {
    protected static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;
    private ImdbInfo imdbInfo;

    public SratimPosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;
        try {
            imdbInfo = new ImdbInfo();
            response = imdbInfo.getImdbId(title, year);

        } catch (Exception error) {
            logger.severe("Failed retreiving sratim informations for movie : " + title);
            logger.severe("Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            try {
                String xml = webBrowser.request("http://www.sratim.co.il/movies/search.aspx?Keyword=" + id);

                String detailsUrl = HTMLTools.extractTag(xml, "cellpadding=\"0\" cellspacing=\"0\" onclick=\"document.location='", 0, "'");

                String sratimUrl = "http://www.sratim.co.il" + detailsUrl;

                xml = webBrowser.request(sratimUrl);

                posterURL = "http://www.sratim.co.il/movies/" + HTMLTools.extractTag(xml, "<img src=\"/movies/", 0, "\"");
            } catch (Exception error) {
                logger.severe("sratim: Failed retreiving poster for movie : " + id);
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        }
        return posterURL;
    }

    @Override
    public String getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "sratim";
    }

}
