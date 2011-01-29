package com.moviejukebox.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.moviejukebox.model.Movie.BooleanYesNoAdapter;

public interface IMovieBasicInformation {

    public abstract String getBaseName();

    public abstract String getLanguage();

    public abstract int getSeason();

    public abstract String getTitle();

    public abstract String getTitleSort();

    public abstract String getOriginalTitle();

    public abstract String getYear();

    @XmlAttribute(name = "isTV")
    public abstract boolean isTVShow();

    @XmlJavaTypeAdapter(BooleanYesNoAdapter.class)
    public abstract Boolean isTrailerExchange();

    @XmlAttribute(name = "isSet")
    public abstract boolean isSetMaster();

}