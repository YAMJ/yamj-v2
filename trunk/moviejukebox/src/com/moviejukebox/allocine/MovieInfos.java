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

package com.moviejukebox.allocine;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

// import java content classes generated by binding compiler
import com.moviejukebox.allocine.jaxb.*;

/**
 *  This is the Movie Search bean for the api.allocine.fr search
 *
 *  @author Yves.Blusseau
 */

public class MovieInfos extends Movie {

    private static final Pattern ageRegex = Pattern.compile("\\s(\\d{1,2})\\san");
    private LinkedHashSet<String> actors;
    private LinkedHashSet<String> writers;
    private LinkedHashSet<String> directors;

    public String getSynopsis() {
        String synopsis = "";
        for (Object obj : getHtmlSynopsis().getContent()) {
            String str = "";
            if (obj instanceof String) {
                str = (String) obj;
            } else if (obj instanceof Element) {
                Element element = (Element) obj;
                str = element.getTextContent();
            }
            if (!StringUtils.isBlank(str)) {
                // Normalize the string (remove LF and collapse WhiteSpaces)
                str = str.replaceAll("\\r+","\n").replaceAll("\\n+"," ").replaceAll("\\s+"," ").trim();
                synopsis = synopsis.concat(str);
            }
        }
        return synopsis;
    }

    public int getRating() {
        float note   = 0;
        int   sum    = 0;
        int   result = -1;

        if (getStatistics() != null ) {
            for ( RatingType rating : getStatistics().getRatingStats() ) {
                int count = rating.getValue();
                note += rating.getNote() * count;
                sum  += count;
            }
            if (sum > 0) {
                result = (int) ((note / sum) / 5.0 * 100);
            }
        }
        return result;
    }

    public String getCertification() {
        String certification = "All"; // Default value
        if (!StringUtils.isBlank(getMovieCertificate())) {
            Matcher match    = ageRegex.matcher(getMovieCertificate());
            if (match.find()) {
                certification=match.group(1);
            }
        }
        return certification;
    }

    protected void parseCasting() {
        if (actors == null) {
            actors = new LinkedHashSet<String>();
        }
        if (writers == null) {
            writers = new LinkedHashSet<String>();
        }
        if (directors == null) {
            directors = new LinkedHashSet<String>();
        }
        LinkedHashSet<String> scripts = new LinkedHashSet<String>();

        for (CastMember member : getCasting()) {
            if (member.getActivity().getCode() == 8001) {        // actor
                actors.add(member.getPerson());
            } else if (member.getActivity().getCode() == 8002) { // director
                directors.add(member.getPerson());
            } else if (member.getActivity().getCode() == 8004) { // writer
                writers.add(member.getPerson());
            } else if (member.getActivity().getCode() == 8043) { // script
                scripts.add(member.getPerson());
            }
        }
        // Add scripts to writers
        writers.addAll(scripts);
    }

    public Set<String> getActors() {
        if (actors == null) {
            parseCasting();
        }
        return actors;
    }

    public Set<String> getDirectors() {
        if (directors == null) {
            parseCasting();
        }
        return directors;
    }

    public Set<String> getWriters() {
        if (writers == null) {
            parseCasting();
        }
        return writers;
    }

}
