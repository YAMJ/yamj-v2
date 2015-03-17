/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.model.Artwork;

import com.moviejukebox.model.Movie;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A class to store all the artwork associated with a movie object
 *
 * @author stuart.boston
 *
 */
public class Artwork {

    private ArtworkType type;       // The type of the artwork.
    private String sourceSite; // Where the artwork originated from
    private String url;        // The original URL of the artwork (may be used as key)
    private final Map<ArtworkSize, ArtworkFile> sizes; // The hash should be the size that is passed as part of the ArtworkSize

    /**
     * Create an Artwork object with a set of sizes
     *
     * @param type The type of the artwork
     * @param sourceSite The source site the artwork came from
     * @param url The URL of the artwork
     * @param sizes A list of the artwork files to add
     */
    public Artwork(ArtworkType type, String sourceSite, String url, Collection<ArtworkFile> sizes) {
        this.type = type;
        this.sourceSite = sourceSite;
        this.url = url;

        this.sizes = new EnumMap<>(ArtworkSize.class);
        for (ArtworkFile artworkFile : sizes) {
            this.addSize(artworkFile);
        }
    }

    /**
     * Create an Artwork object with a single size
     *
     * @param type The type of the artwork
     * @param sourceSite The source site the artwork came from
     * @param url The URL of the artwork
     * @param size An artwork files to add
     */
    public Artwork(ArtworkType type, String sourceSite, String url, ArtworkFile size) {
        this.type = type;
        this.sourceSite = sourceSite;
        this.url = url;
        this.sizes = new EnumMap<>(ArtworkSize.class);
        this.addSize(size);
    }

    /**
     * Create a blank Artwork object
     */
    public Artwork() {
        this.sourceSite = Movie.UNKNOWN;
        this.type = null;
        this.url = Movie.UNKNOWN;
        this.sizes = new EnumMap<>(ArtworkSize.class);
    }

    /**
     * Add the ArtworkFile to the list, overwriting anything already there
     *
     * @param size
     */
    public final void addSize(ArtworkFile size) {
        sizes.put(size.getSize(), size);
    }

    public Collection<ArtworkFile> getSizes() {
        return sizes.values();
    }

    public ArtworkFile getSize(String size) {
        return sizes.get(ArtworkSize.valueOf(size.toUpperCase()));
    }

    public ArtworkFile getSize(ArtworkSize size) {
        return sizes.get(size);
    }

    /**
     * @return the sourceSite
     */
    public String getSourceSite() {
        return sourceSite;
    }

    /**
     * @return the type
     */
    public ArtworkType getType() {
        return type;
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param sourceSite the sourceSite to set
     */
    public void setSourceSite(String sourceSite) {
        this.sourceSite = sourceSite;
    }

    /**
     * @param type the type to set
     */
    public void setType(ArtworkType type) {
        this.type = type;
    }

    /**
     * @param url the URL to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.type)
                .append(this.sourceSite)
                .append(this.url)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }

        Artwork other = (Artwork) obj;
        return new EqualsBuilder()
                .appendSuper(super.equals(obj))
                .append(type, other.type)
                .append(sourceSite, other.sourceSite)
                .append(url, other.url)
                .isEquals();
    }

    public int compareTo(Artwork other) {
        return new CompareToBuilder()
                .append(this.sourceSite, other.sourceSite)
                .append(this.type, other.type)
                .append(this.url, other.url)
                .toComparison();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
