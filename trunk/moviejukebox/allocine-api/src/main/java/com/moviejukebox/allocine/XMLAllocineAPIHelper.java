package com.moviejukebox.allocine;

import com.moviejukebox.allocine.jaxb.ObjectFactory;
import com.moviejukebox.allocine.jaxb.Feed;
import com.moviejukebox.allocine.jaxb.Tvseries;
import com.moviejukebox.allocine.jaxb.Season;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

public final class XMLAllocineAPIHelper {

    // Suppresses default constructor, ensuring non-instantiability.
    private XMLAllocineAPIHelper() {
    }

    private static final JAXBContext JAXB_CONTEXT = initContext();

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance("com.moviejukebox.allocine.jaxb");
        } catch (JAXBException error) {
            throw new Error("XMLAllocineAPIHelper: Got error during initialization", error);
        }
    }

    protected static Unmarshaller createAllocineUnmarshaller() throws JAXBException {
        Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
        // Use our own ObjectFactory so we can add behaviors in our classes
        unmarshaller.setProperty("com.sun.xml.bind.ObjectFactory", new ObjectFactory());
        return unmarshaller;
    }

    protected static Search validSearchElement(Object rootElement) {
        if (rootElement.getClass() == Search.class) {
            return (Search) rootElement;
        }
        // Error
        return new Search();
    }

    public static Search searchMovieInfos(String query) throws IOException, JAXBException, XMLStreamException {
        URL url = new URL("http://api.allocine.fr/rest/v3/search?partner=YW5kcm9pZC12M3M&format=XML&filter=movie&q=" + query);
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        return validSearchElement(unmarshaller.unmarshal(url));
    }
    public static Search searchTvseriesInfos(String query) throws IOException, JAXBException, XMLStreamException {
        URL url = new URL("http://api.allocine.fr/rest/v3/search?partner=YW5kcm9pZC12M3M&format=XML&filter=tvseries&q=" + query);
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        return validSearchElement(unmarshaller.unmarshal(url));
    }

    protected static MovieInfos validMovieElement(Object rootElement) {
        if (rootElement.getClass() == MovieInfos.class) {
            return (MovieInfos) rootElement;
        }
        // Error
        return new MovieInfos();
    }

    public static MovieInfos getMovieInfos(String allocineId) throws IOException, JAXBException, XMLStreamException {
        // HTML tags are remove from synopsis & synopsisshort
        URL url = new URL("http://api.allocine.fr/rest/v3/movie?partner=YW5kcm9pZC12M3M&profile=large&mediafmt=mp4-lc&format=XML&filter=movie&striptags=synopsis,synopsisshort&code=" + allocineId);
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        return validMovieElement(unmarshaller.unmarshal(url));
    }

    public static MovieInfos getMovieInfos(File file) throws IOException, JAXBException {
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        return validMovieElement(unmarshaller.unmarshal(file));
    }

    protected static TvSeriesInfos validTvSeriesElement(Object rootElement) {
        if (rootElement.getClass() == TvSeriesInfos.class) {
            return (TvSeriesInfos) rootElement;
        }
        // Error
        return new TvSeriesInfos();
    }

    public static TvSeriesInfos getTvSeriesInfos(String allocineId) throws IOException, JAXBException, XMLStreamException {
        // HTML tags are remove from synopsis & synopsisshort
        URL url = new URL("http://api.allocine.fr/rest/v3/tvseries?partner=YW5kcm9pZC12M3M&profile=large&mediafmt=mp4-lc&format=XML&filter=movie&striptags=synopsis,synopsisshort&code=" + allocineId);
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        return validTvSeriesElement(unmarshaller.unmarshal(url));
    }

    protected static TvSeasonInfos validTvSeasonElement(Object rootElement) {
        if (rootElement.getClass() == TvSeasonInfos.class) {
            return (TvSeasonInfos) rootElement;
        }
        // Error
        return new TvSeasonInfos();
    }

    public static TvSeasonInfos getTvSeasonInfos(Integer seasonCode) throws IOException, JAXBException, XMLStreamException {
        // HTML tags are remove from synopsis & synopsisshort
        URL url = new URL("http://api.allocine.fr/rest/v3/season?partner=YW5kcm9pZC12M3M&profile=large&mediafmt=mp4-lc&format=XML&filter=movie&striptags=synopsis,synopsisshort&code=" + seasonCode);
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        return validTvSeasonElement(unmarshaller.unmarshal(url));
    }

}
