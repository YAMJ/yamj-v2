/*
 *      Copyright (c) 2004-2014 YAMJ Members
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
package com.moviejukebox.writer;

import com.moviejukebox.MovieJukebox;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.log4j.Logger;

/**
 *
 * @author stuart.boston
 */
public class CompleteMoviesWriter {

    private static final Logger LOG = Logger.getLogger(CompleteMoviesWriter.class);
    private static final String LOG_MESSAGE = "CompleteMovies: ";
    private static final String COMPLETE_MOVIES_XML = "CompleteMovies.xml";
    private static final String RSS_XML_FILENAME = "rss.xml";
    private static final String RSS_XSL_FILENAME = "rss.xsl";

    protected CompleteMoviesWriter() {
        throw new UnsupportedOperationException("Class cannot be initialised");
    }

    /**
     * Write the CompleteMovies file to the jukebox
     *
     * @param library
     * @param jukebox
     * @return
     */
    public static boolean generate(Library library, Jukebox jukebox) {
        JAXBContext context;

        try {
            LOG.info(LOG_MESSAGE + "Generating " + COMPLETE_MOVIES_XML);
            context = JAXBContext.newInstance(MovieJukebox.JukeboxXml.class);
        } catch (JAXBException error) {
            LOG.warn(LOG_MESSAGE + "RSS is not generated (Context creation error).");
            LOG.warn(SystemTools.getStackTrace(error));
            return Boolean.FALSE;
        }

        MovieJukebox.JukeboxXml jukeboxXml = new MovieJukebox.JukeboxXml();
        jukeboxXml.movies = library.values();

        File totalMoviesXmlFile = new File(jukebox.getJukeboxTempLocationDetails(), COMPLETE_MOVIES_XML);
        File rootTotalMoviesFile = FileTools.fileCache.getFile(StringTools.appendToPath(jukebox.getJukeboxRootLocationDetails(), COMPLETE_MOVIES_XML));

        if (library.isDirty() || !rootTotalMoviesFile.exists()) {
            OutputStream marStream = null;
            try {
                marStream = FileTools.createFileOutputStream(totalMoviesXmlFile);
                context.createMarshaller().marshal(jukeboxXml, marStream);
            } catch (JAXBException ex) {
                LOG.warn(LOG_MESSAGE + "RSS is not generated (JAXB error): " + ex.getMessage());
                LOG.warn(SystemTools.getStackTrace(ex));
                return Boolean.FALSE;
            } catch (FileNotFoundException ex) {
                LOG.warn(LOG_MESSAGE + "RSS is not generated (Jukebox error): " + ex.getMessage());
                LOG.warn(SystemTools.getStackTrace(ex));
                return Boolean.FALSE;
            } finally {
                if (marStream != null) {
                    try {
                        marStream.flush();
                        marStream.close();
                    } catch (IOException ex) {
                        LOG.trace(LOG_MESSAGE + "Failed to close marshal file: " + ex.getMessage());
                    }
                }
            }

            try {
                Transformer transformer = MovieJukeboxHTMLWriter.getTransformer(new File(RSS_XSL_FILENAME), jukebox.getJukeboxRootLocationDetails());

                Result xmlResult = new StreamResult(new File(jukebox.getJukeboxTempLocationDetails(), RSS_XML_FILENAME));
                transformer.transform(new StreamSource(totalMoviesXmlFile), xmlResult);

                LOG.debug(LOG_MESSAGE + "RSS has been generated.");
            } catch (TransformerException ex) {
                LOG.warn(LOG_MESSAGE + "RSS is not generated (Transformer error): " + ex.getMessage());
                LOG.warn(SystemTools.getStackTrace(ex));
                return Boolean.FALSE;
            }
        }

        // These should be added to the list of jukebox files regardless of the state of the library
        FileTools.addJukeboxFile(COMPLETE_MOVIES_XML);
        FileTools.addJukeboxFile(RSS_XML_FILENAME);
        return Boolean.TRUE;
    }
}
