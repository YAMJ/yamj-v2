/*
 *      Copyright (c) 2004-2012 YAMJ Members
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

import com.moviejukebox.tools.BooleanYesNoAdapter;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public interface IMovieBasicInformation {

    String getBaseName();

    String getLanguage();

    int getSeason();

    String getTitle();

    String getTitleSort();

    String getOriginalTitle();

    String getYear();

    @XmlAttribute(name = "isTV")
    boolean isTVShow();

    @XmlJavaTypeAdapter(BooleanYesNoAdapter.class)
    Boolean isTrailerExchange();

    @XmlAttribute(name = "isSet")
    boolean isSetMaster();
}