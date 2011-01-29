/*
 *      Copyright (c) 2004-2011 YAMJ Members
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

package com.moviejukebox.plugin;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.FileTools;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.File;

/**
 * User: JDGJr
 * Date: Feb 15, 2009
 */
public class MovieListingPluginBase implements MovieListingPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    protected static final String UNDEFINED = "UNDEFINED";

    protected boolean groupByType = true;
    protected boolean blankUNKNOWN = true;
    protected String baseFilename = "";
    protected String destination = "";

    /**
     * @param jukeboxRoot
     */
    protected void initialize(Jukebox jukebox) {
        groupByType = PropertiesUtil.getBooleanProperty("mjb.listing.GroupByType", "true");
        blankUNKNOWN = PropertiesUtil.getBooleanProperty("mjb.listing.clear.UNKNOWN", "true");
        baseFilename = PropertiesUtil.getProperty("mjb.listing.output.filename", "MovieJukebox-listing");
        destination = PropertiesUtil.getProperty("mjb.listing.output.destination", jukebox.getJukeboxRootLocation());
    } // initialize()

    /**
     * @return ArrayList of selected movie types, possibly from .properties file
     */
    protected ArrayList<String> getSelectedTypes() {
        ArrayList<String> alResult = new ArrayList<String>();

        String types = PropertiesUtil.getProperty("mjb.listing.types", typeAll).trim();
        if (typeAll.equalsIgnoreCase(types)) {
            types = typeMovie + "," + typeExtra + "," + typeTVShow;
        }

        //break into a list
        StringTokenizer tokenizer = new StringTokenizer(types, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();

            //easy to skip space
            if (typeTVShowNoSpace.equalsIgnoreCase(token)) {
                token = typeTVShow;
            }
            alResult.add(token);
        }

        return alResult;
    } // getSelectedTypes()

    /**
     * @param file
     */
    protected void copyListingFile(File file, String filename) {
        // move to configured (default) location
        String dest = destination + File.separator + filename;
        logger.fine("  Copying to: " + dest);
        FileTools.copyFile(file, new File(dest));
    } // copyListingFile()

    /**
     * @param Jukebox
     * @param library
     */
    public void generate(Jukebox jukebox, Library library) {
        logger.fine("  MovieListingPluginBase: not generating listing file.");
    } // generate()

} // class MovieListingPluginBase
