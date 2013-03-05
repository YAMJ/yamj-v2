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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

import com.moviejukebox.allocine.jaxb.ObjectFactory;

/**
 * Implementation for XML format
 *
 * @author Yves Blusseau
 */
public final class XMLAllocineAPIHelper extends AbstractAllocineAPI {

    private static final JAXBContext JAXB_CONTEXT = initContext();

    /**
     * Constructor.
     *
     * @param apiKey The API key for allocine
     */
    public XMLAllocineAPIHelper(String apiKey) {
        super(apiKey, "xml");
    }

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance("com.moviejukebox.allocine.jaxb");
        } catch (JAXBException error) {
            throw new Error("XMLAllocineAPIHelper: Got error during initialization", error);
        }
    }

    private static Unmarshaller createAllocineUnmarshaller() throws JAXBException {
        Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
        // Use our own ObjectFactory so we can add behaviors in our classes
        unmarshaller.setProperty("com.sun.xml.bind.ObjectFactory", new ObjectFactory());
        return unmarshaller;
    }

    @Override
    public Search searchMovieInfos(String query) throws IOException, JAXBException, XMLStreamException {
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        
        URLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = connectSearchMovieInfos(query);
            inputStream = connection.getInputStream();
            return validSearchElement(unmarshaller.unmarshal(inputStream));
        } finally {
            close(connection, inputStream);
        }
    }

    @Override
    public Search searchTvseriesInfos(String query) throws IOException, JAXBException, XMLStreamException {
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        
        URLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = connectSearchTvseriesInfos(query);
            inputStream = connection.getInputStream();
            return validSearchElement(unmarshaller.unmarshal(inputStream));
        } finally {
            close(connection, inputStream);
        }
    }

    @Override
    public MovieInfos getMovieInfos(String allocineId) throws IOException, JAXBException, XMLStreamException {
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        
        URLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = connectGetMovieInfos(allocineId);
            inputStream = connection.getInputStream();
            return validMovieElement(unmarshaller.unmarshal(inputStream));
        } finally {
            close(connection, inputStream);
        }
    }

    protected static MovieInfos getMovieInfos(File file) throws IOException, JAXBException {
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        return validMovieElement(unmarshaller.unmarshal(file));
    }

    @Override
    public TvSeriesInfos getTvSeriesInfos(String allocineId) throws IOException, JAXBException, XMLStreamException {
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        
        URLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = connectGetTvSeriesInfos(allocineId);
            inputStream = connection.getInputStream();
            return validTvSeriesElement(unmarshaller.unmarshal(inputStream));
        } finally {
            close(connection, inputStream);
        }
    }

    @Override
    public TvSeasonInfos getTvSeasonInfos(Integer seasonCode) throws IOException, JAXBException, XMLStreamException {
        Unmarshaller unmarshaller = createAllocineUnmarshaller();
        
        URLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = connectGetTvSeasonInfos(seasonCode);
            inputStream = connection.getInputStream();
            return validTvSeasonElement(unmarshaller.unmarshal(inputStream));
        } finally {
            close(connection, inputStream);
        }
    }

    private static Search validSearchElement(Object rootElement) {
        if (rootElement.getClass() == Search.class) {
            return (Search) rootElement;
        }
        // Error
        return new Search();
    }

    private static MovieInfos validMovieElement(Object rootElement) {
        if (rootElement.getClass() == MovieInfos.class) {
            return (MovieInfos) rootElement;
        }
        // Error
        return new MovieInfos();
    }

    private static TvSeriesInfos validTvSeriesElement(Object rootElement) {
        if (rootElement.getClass() == TvSeriesInfos.class) {
            return (TvSeriesInfos) rootElement;
        }
        // Error
        return new TvSeriesInfos();
    }

    private static TvSeasonInfos validTvSeasonElement(Object rootElement) {
        if (rootElement.getClass() == TvSeasonInfos.class) {
            return (TvSeasonInfos) rootElement;
        }
        // Error
        return new TvSeasonInfos();
    }
}
