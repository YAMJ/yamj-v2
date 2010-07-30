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
package com.moviejukebox.model;

import java.nio.charset.Charset;

public class ImdbSiteDataDefinition {
    private String site;
    private String director;
    private String cast;
    private String release_date;
    private String runtime;
    private String country;
    private String company;
    private String genre;
    private String quotes;
    private String plot;
    private String rated;
    private String certification;
    private String original_air_date;
    private String writer;
    private Charset charset;

    public ImdbSiteDataDefinition(String site, String charsetName, String director, String cast, String releaseDate, String runtime, String country, String company,
                    String genre, String quotes, String plot, String rated, String certification, String originalAirDate, String writer) {
        super();
        this.site = site;
        this.director = director;
        this.cast= cast;
        release_date = releaseDate;
        this.runtime = runtime;
        this.country = country;
        this.company = company;
        this.genre = genre;
        this.quotes = quotes;
        this.plot = plot;
        this.rated = rated;
        this.certification = certification;
        original_air_date = originalAirDate;
        this.writer = writer;
        if (charsetName == null || charsetName.length() == 0) {
            charset = Charset.defaultCharset();
        } else {
            charset = Charset.forName(charsetName);
        }
    }

    public String getSite() {
        return site;
    }

    public String getDirector() {
        return director;
    }

    public String getCast() {
        return cast;
    }

    public String getReleaseDate() {
        return release_date;
    }

    public String getRuntime() {
        return runtime;
    }

    public String getCountry() {
        return country;
    }

    public String getCompany() {
        return company;
    }

    public String getGenre() {
        return genre;
    }

    public String getQuotes() {
        return quotes;
    }

    public String getPlot() {
        return plot;
    }

    public String getRated() {
        return rated;
    }

    public String getCertification() {
        return certification;
    }

    public String getOriginalAirDate() {
        return original_air_date;
    }

    public String getWriter() {
        return writer;
    }

    public Charset getCharset() {
        return charset;
    }

}
