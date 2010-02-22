package com.moviejukebox.plugin;

import java.net.URLEncoder;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.WebBrowser;

public class CaratulasdecinePosterPlugin implements IPosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    private WebBrowser webBrowser;

    public CaratulasdecinePosterPlugin() {
        super();
        webBrowser = new WebBrowser();
    }

    @Override
    public String getIdFromMovieInfo(String title, String year, boolean isTvShow) {
        String response = Movie.UNKNOWN;
        try {
            StringBuffer sb = new StringBuffer("http://www.google.es/custom?hl=es&domains=caratulasdecine.com&ie=ISO-8859-1&oe=ISO-8859-1&q=");

            sb.append(URLEncoder.encode(title, "UTF-8"));
            sb.append("&btnG=Buscar&sitesearch=caratulasdecine.com&meta=");

            String xml = webBrowser.request(sb.toString());
            String searchString = "http://www.caratulasdecine.com/caratula.php?pel=";
            int beginIndex = xml.indexOf(searchString);
            if (beginIndex > -1) {
                response = xml.substring(beginIndex + searchString.length(), xml.indexOf("\"", beginIndex + searchString.length()));
            }

        } catch (Exception e) {
            logger.severe("Failed retreiving CaratulasdecinePoster Id for movie : " + title);
            logger.severe("Error : " + e.getMessage());
        }
        return response;
    }

    @Override
    public String getPosterUrl(String id) {
        // <td><img src="
        String response = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equals(id)) {
            try {
                StringBuffer sb = new StringBuffer("http://www.caratulasdecine.com/caratula.php?pel=");
                sb.append(id);

                String xml = webBrowser.request(sb.toString());
                String searchString = "<td><img src=\"";
                int beginIndex = xml.indexOf(searchString);
                if (beginIndex > -1) {
                    response = "http://www.caratulasdecine.com/"
                                    + xml.substring(beginIndex + searchString.length(), xml.indexOf("\"", beginIndex + searchString.length()));
                }

            } catch (Exception e) {
                logger.severe("Failed retreiving CaratulasdecinePoster url for movie : " + id);
                logger.severe("Error : " + e.getMessage());
            }
        }
        return response;
    }

    @Override
    public String getPosterUrl(String title, String year, boolean isTvShow) {
        return getPosterUrl(getIdFromMovieInfo(title, year, isTvShow));
    }

}
