package com.moviejukebox.plugin.poster;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.IImage;
import com.moviejukebox.model.Image;
import com.moviejukebox.tools.WebBrowser;

public class MotechnetPosterPlugin extends AbstractMoviePosterPlugin {
    protected static Logger logger = Logger.getLogger("moviejukebox");
    private WebBrowser webBrowser;

    public MotechnetPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        webBrowser = new WebBrowser();
    }

    private String getUrl(String html) {
        String response = null;

        String searchString = "http://www.motechposters.com/title/";
        int beginIndex = html.indexOf(searchString);
        if (beginIndex > -1) {
            response = html.substring(beginIndex + searchString.length(), html.indexOf("</a>", beginIndex + searchString.length()));
        }
        return response;
    }

    private List<String> getResult(String html) {
        List<String> result = new ArrayList<String>();
        String tmp = null;
        String tempHtml = html;

        while ((tmp = getUrl(tempHtml)) != null) {
            result.add(tmp);
            tempHtml = tempHtml.substring(tempHtml.indexOf(tmp) + tmp.length());
        }
        return result;
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        String response = Movie.UNKNOWN;

        try {
            StringBuffer sb = new StringBuffer("http://www.google.com/cse?cx=partner-pub-1232145664289710%3Aleabr0-srj4&cof=FORID%3A10&ie=ISO-8859-1&q=");
            sb.append(URLEncoder.encode(title, "ISO-8859-1"));
            sb.append("&sa=Search&siteurl=www.motechposters.com%2F&ad=w9&num=10&rurl=http%3A%2F%2Fwww.motechposters.com%2Fsearch%2F%3Fcx%3Dpartner-pub-1232145664289710%253Aleabr0-srj4%26cof%3DFORID%253A10%26ie%3DISO-8859-1%26q%3Dgladiator%26sa%3DSearch%26siteurl%3Dwww.motechposters.com%252F");
            String xml = webBrowser.request(sb.toString(), Charset.forName("ISO-8859-1"));
            List<String> results = getResult(xml);

            if (results != null && results.size() > 0) {
                for (String resultTmp : results) {
                    if (resultTmp.toLowerCase().contains(title.toLowerCase())) {
                        if (year != null) {
                            if (resultTmp.contains("(" + year + ")")) {
                                response = resultTmp;
                                break;
                            }
                        } else {
                            // No year, taking first matching result.
                            response = resultTmp;
                            break;
                        }
                    }
                }
                
                if (!response.equalsIgnoreCase(Movie.UNKNOWN)) {
                    int pos = response.indexOf("/");
                    if (pos > -1) {
                        response = response.substring(0, pos);
                    }
                }
            }
        } catch (Exception error) {
            logger.severe("MotechnetPosterPlugin: Failed retreiving poster id movie : " + title);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
        return response;
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (!Movie.UNKNOWN.equalsIgnoreCase(id)) {
            String xml = "";
            try {
                xml = webBrowser.request("http://www.motechposters.com/title/" + id + "/");
                String searchString = "<img id=\"poster_img\" src=\"";
                int beginIndex = xml.indexOf(searchString);
                if (beginIndex > -1) {
                    posterURL = "http://www.motechposters.com"
                                    + xml.substring(beginIndex + searchString.length(), xml.indexOf("\"", beginIndex + searchString.length()));
                }
            } catch (Exception error) {
                logger.severe("MotechnetPosterPlugin: Failed retreiving poster for movie : " + id);
                final Writer eResult = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(eResult);
                error.printStackTrace(printWriter);
                logger.severe(eResult.toString());
            }
        }
        if (!Movie.UNKNOWN.equalsIgnoreCase(posterURL)) {
            return new Image(posterURL);
        }
        return Image.UNKNOWN;
    }

    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public String getName() {
        return "motechnet";
    }
}
