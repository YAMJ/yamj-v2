/*
 *      Copyright (c) 20042009 YAMJ Members
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

public class Image implements IImage {
    public static final IImage UNKNOWN = new Image();

    private String url = Movie.UNKNOWN;
    private String subimage = Movie.UNKNOWN;

    public Image() {
    }

    public Image(String url) {
        this.url = url;
    }

    public Image(String url, String subimage) {
        this.url = url;
        this.subimage = subimage;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubimage() {
        return subimage;
    }

    public void setSubimage(String subimage) {
        this.subimage = subimage;
    }

    public String toString() {
        return "Image{" +
                "url='" + url + '\'' +
                ", subimage='" + subimage + '\'' +
                '}';
    }

}
