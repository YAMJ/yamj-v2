package com.moviejukebox.tools;

import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import com.moviejukebox.tools.XMLHelper;
import com.moviejukebox.jaxb.allocine.*;

class Person {
}

public class XMLAllocineAPIHelper {

    private static final JAXBContext context = initContext();

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance("com.moviejukebox.jaxb.allocine");
        }
        catch (Exception error) {
            throw new RuntimeException("XMLAllocineAPIHelper: Got error during initialization", error);

        }
    }

    public static MovieInfos getMovieInfos(String AllocineId) throws IOException, JAXBException, XMLStreamException {
        XMLHelper xmlHelper   = new XMLHelper();
        XMLEventReader reader = xmlHelper.getEventReader("http://api.allocine.fr/xml/movie?partner=3&profile=large&code=" + AllocineId);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        // Unmarshalles the given XML Document.
        Object rootElement = unmarshaller.unmarshal(reader);
        if (rootElement instanceof MovieInfos) {
            return (MovieInfos) rootElement;
        }
        // Error
        return null;
	}
}
