/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
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
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.model.Artwork;

import com.moviejukebox.model.Movie;
import java.awt.Dimension;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ArtworkFile {

    private ArtworkSize size;       // Descriptive size of the artwork, e.g. Small, medium, large
    private Dimension dimension;  // Target size of the artwork - populated from the properties file
    private String filename;   // The filename to save the artwork too
    private boolean downloaded; // Has the artwork been downloaded (to the jukebox)

    public ArtworkFile() {
        this.size = ArtworkSize.LARGE;
        this.dimension = new Dimension();
        this.filename = Movie.UNKNOWN;
        this.downloaded = false;
    }

    public ArtworkFile(ArtworkSize size, String filename, boolean downloaded) {
        this.size = size;
        this.dimension = new Dimension();
        this.filename = filename;
        this.downloaded = downloaded;
    }

    /**
     * @return the size
     */
    public ArtworkSize getSize() {
        return size;
    }

    /**
     * @return the dimension
     */
    public Dimension getDimension() {
        return dimension;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the downloaded
     */
    public boolean isDownloaded() {
        return downloaded;
    }

    /**
     * @param size the size to set
     */
    public void setSize(ArtworkSize size) {
        this.size = size;
    }

    /**
     * @param size the String size to set.
     */
    public void setSize(String size) {
        this.size = ArtworkSize.valueOf(size.toUpperCase());
    }

    /**
     * @param dimension the dimension to set
     */
    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    /**
     * @param width The width to set
     * @param height The height to set
     */
    public void setDimension(int width, int height) {
        this.dimension = new Dimension(width, height);
    }

    /**
     * @param filename the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @param downloaded the downloaded to set
     */
    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
