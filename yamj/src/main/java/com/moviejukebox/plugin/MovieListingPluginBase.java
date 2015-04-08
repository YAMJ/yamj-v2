/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/orgs/YAMJ/people
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ) project.
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v2
 *
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: JDGJr Date: Feb 15, 2009
 */
public class MovieListingPluginBase implements MovieListingPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(MovieListingPluginBase.class);
    private static final String UNDEFINED = "UNDEFINED";
    private boolean groupByType = Boolean.TRUE;
    private boolean blankUNKNOWN = Boolean.TRUE;
    private String baseFilename = "";
    private String destination = "";

    /**
     * @param jukebox
     */
    protected void initialize(Jukebox jukebox) {
        groupByType = PropertiesUtil.getBooleanProperty("mjb.listing.GroupByType", Boolean.TRUE);
        blankUNKNOWN = PropertiesUtil.getBooleanProperty("mjb.listing.clear.UNKNOWN", Boolean.TRUE);
        baseFilename = PropertiesUtil.getProperty("mjb.listing.output.filename", "MovieJukebox-listing");
        destination = PropertiesUtil.getProperty("mjb.listing.output.destination", jukebox.getJukeboxRootLocation());
    } // initialize()

    public boolean isGroupByType() {
        return groupByType;
    }

    public boolean isBlankUnknown() {
        return blankUNKNOWN;
    }

    public String getBaseFilename() {
        return baseFilename;
    }

    public String getDestination() {
        return destination;
    }

    public static String getUndefined() {
        return UNDEFINED;
    }

    /**
     * @return ArrayList of selected movie types, possibly from .properties file
     */
    protected List<String> getSelectedTypes() {
        List<String> alResult = new ArrayList<>();

        String types = PropertiesUtil.getProperty("mjb.listing.types", TYPE_ALL).trim();
        if (TYPE_ALL.equalsIgnoreCase(types)) {
            types = TYPE_MOVIE + "," + TYPE_EXTRA + "," + TYPE_TV_SHOW;
        }

        //break into a list
        StringTokenizer tokenizer = new StringTokenizer(types, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();

            //easy to skip space
            if (TYPE_TVSHOW.equalsIgnoreCase(token)) {
                token = TYPE_TV_SHOW;
            }
            alResult.add(token);
        }

        return alResult;
    } // getSelectedTypes()

    /**
     * @param file
     * @param filename
     */
    protected void copyListingFile(File file, String filename) {
        // move to configured (default) location
        String dest = destination + File.separator + filename;
        LOG.info("Copying to: {}", dest);
        FileTools.copyFile(file, new File(dest));
    } // copyListingFile()

    /**
     * @param jukebox
     * @param library
     */
    @Override
    public void generate(Jukebox jukebox, Library library) {
        LOG.info("Not generating listing file.");
    } // generate()
} // class MovieListingPluginBase
