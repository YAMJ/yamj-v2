/*
 *      Copyright (c) 2004-2013 YAMJ Members
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
package com.moviejukebox.scanner;

import com.moviejukebox.model.Codec;
import com.moviejukebox.model.CodecType;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.enumerations.CodecSource;
import com.moviejukebox.model.enumerations.OverrideFlag;
import com.moviejukebox.tools.AspectRatioTools;
import com.moviejukebox.tools.DateTimeTools;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SubtitleTools;
import com.moviejukebox.tools.SystemTools;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveEntry;
import com.mucommander.file.FileFactory;
import com.mucommander.file.impl.iso.IsoArchiveFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.xmm.moviemanager.fileproperties.FilePropertiesMovie;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author Grael
 */
public class MediaInfoScanner {

    private static final Logger LOG = Logger.getLogger(MediaInfoScanner.class);
    private static final String LOG_MESSAGE = "MediaInfoScanner: ";
    private static final String MEDIAINFO_PLUGIN_ID = "mediainfo";
    private static final String SPLIT_GENRE = "(?<!-)/|,|\\|";  // Caters for the case where "-/" is not wanted as part of the split
    private static final Pattern PATTERN_CHANNELS = Pattern.compile(".*(\\d{1,2}).*");
    // mediaInfo repository
    private static final File MI_PATH = new File(PropertiesUtil.getProperty("mediainfo.home", "./mediaInfo/"));
    // mediaInfo command line, depend on OS
    private static final List<String> MI_EXE = new ArrayList<String>();
    private static final String MI_FILENAME_WINDOWS = "MediaInfo.exe";
    private static final String MI_RAR_FILENAME_WINDOWS = "MediaInfo-rar.exe";
    private static final String MI_FILENAME_LINUX = "mediainfo";
    private static final String MI_RAR_FILENAME_LINUX = "mediainfo-rar";
    private static boolean isMediaInfoRar = Boolean.FALSE;
    public static final String OS_NAME = System.getProperty("os.name");
    public static final String OS_VERSION = System.getProperty("os.version");
    public static final String OS_ARCH = System.getProperty("os.arch");
    private static final boolean IS_ACTIVATED;
    private static final boolean ENABLE_METADATA = PropertiesUtil.getBooleanProperty("mediainfo.metadata.enable", Boolean.FALSE);
    private static final boolean ENABLE_UPDATE = PropertiesUtil.getBooleanProperty("mediainfo.update.enable", Boolean.FALSE);
    private static final boolean ENABLE_MULTIPART = PropertiesUtil.getBooleanProperty("mediainfo.multipart.enable", Boolean.TRUE);
    private static final boolean MI_OVERALL_BITRATE = PropertiesUtil.getBooleanProperty("mediainfo.overallbitrate", Boolean.FALSE);
    private static final boolean MI_READ_FROM_FILE = PropertiesUtil.getBooleanProperty("mediainfo.readfromfile", Boolean.FALSE);
    private String randomDirName;
    private static final AspectRatioTools ASPECT_TOOLS = new AspectRatioTools();
    private static final String LANG_DELIM = PropertiesUtil.getProperty("mjb.language.delimiter", Movie.SPACE_SLASH_SPACE);
    private static String AUDIO_LANG_UNKNOWN = PropertiesUtil.getProperty("mjb.language.audio.unknown");
    private static final List<String> MI_DISK_IMAGES = new ArrayList<String>();

    static {
        LOG.debug("Operating System Name   : " + OS_NAME);
        LOG.debug("Operating System Version: " + OS_VERSION);
        LOG.debug("Operating System Type   : " + OS_ARCH);

        File checkMediainfo = findMediaInfo();

        if (OS_NAME.contains("Windows")) {
            if (MI_EXE.isEmpty()) {
                MI_EXE.add("cmd.exe");
                MI_EXE.add("/E:1900");
                MI_EXE.add("/C");
                MI_EXE.add(checkMediainfo.getName());
                MI_EXE.add("-f");
            }
        } else {
            if (MI_EXE.isEmpty()) {
                MI_EXE.add("./" + checkMediainfo.getName());
                MI_EXE.add("-f");
            }
        }

        if (!checkMediainfo.canExecute()) {
            LOG.info(LOG_MESSAGE + "Couldn't find CLI mediaInfo executable tool: Video file data won't be extracted");
            LOG.info(LOG_MESSAGE + "File; " + checkMediainfo.getAbsolutePath());
            IS_ACTIVATED = Boolean.FALSE;
        } else {
            if (isMediaInfoRar) {
                LOG.info(LOG_MESSAGE + "MediaInfo-rar tool found, additional scanning functions enabled.");
            } else {
                LOG.info(LOG_MESSAGE + "MediaInfo tool will be used to extract video data. But not RAR and ISO formats");
            }
            IS_ACTIVATED = Boolean.TRUE;
        }

        // Add a list of supported extensions
        for (String ext : PropertiesUtil.getProperty("mediainfo.rar.diskExtensions", "iso,img,rar,001").split(",")) {
            MI_DISK_IMAGES.add(ext.toLowerCase());
        }
    }
    // DVD rip infos Scanner
    private final DVDRipScanner localDVDRipScanner;

    public MediaInfoScanner() {
        localDVDRipScanner = new DVDRipScanner();
        randomDirName = PropertiesUtil.getProperty("mjb.jukeboxTempDir", "./temp") + "/isoTEMP/" + Thread.currentThread().getName();
    }

    public boolean extendedExtention(String filename) {
        if (isMediaInfoRar && (MI_DISK_IMAGES.contains(FilenameUtils.getExtension(filename).toLowerCase()))) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    public void update(Movie currentMovie) {
        if (!ENABLE_UPDATE) {
            // update not enabled
            return;
        }

        if (currentMovie.getFile().isDirectory()) {
            // no update needed if movie file is a directory (DVD structure)
            return;
        }

        // TODO add check if movie file is newer than generated movie XML
        //      in order to update possible changed media info values
        // no update if movie has no new files
        if (!currentMovie.hasNewMovieFiles()) {
            return;
        }

        // check if main file has changed
        boolean mainFileIsNew = Boolean.FALSE;

        try {
            // get the canonical path for movie file
            String movieFilePath = currentMovie.getFile().getCanonicalPath();
            String newFilePath;
            for (MovieFile movieFile : currentMovie.getMovieFiles()) {
                if (movieFile.isNewFile()) {
                    try {
                        // just compare paths to be sure that
                        // the main movie file is new
                        newFilePath = movieFile.getFile().getCanonicalPath();
                        if (movieFilePath.equalsIgnoreCase(newFilePath)) {
                            mainFileIsNew = Boolean.TRUE;
                            break;
                        }
                    } catch (IOException ignore) {
                        // nothing to do
                    }
                }
            }
        } catch (IOException ignore) {
            // nothing to do
        }

        if (mainFileIsNew) {
            LOG.debug(LOG_MESSAGE + "Main movie file has changed; rescan media info");
            this.scan(currentMovie);
        }
    }

    public void scan(Movie currentMovie) {
        if (currentMovie.getFile().isDirectory()) {
            // Scan IFO files
            FilePropertiesMovie mainMovieIFO = localDVDRipScanner.executeGetDVDInfo(currentMovie.getFile());
            if (mainMovieIFO != null) {
                if (IS_ACTIVATED) {
                    scan(currentMovie, mainMovieIFO.getLocation(), Boolean.FALSE);
                    // Issue 1176 - Prevent lost of NFO Data
                    if (StringTools.isNotValidString(currentMovie.getRuntime())) {
                        currentMovie.setRuntime(DateTimeTools.formatDuration(mainMovieIFO.getDuration()), MEDIAINFO_PLUGIN_ID);
                    }
                } else if (OverrideTools.checkOverwriteRuntime(currentMovie, MEDIAINFO_PLUGIN_ID)) {
                    currentMovie.setRuntime(DateTimeTools.formatDuration(mainMovieIFO.getDuration()), MEDIAINFO_PLUGIN_ID);
                }
            }
        } else if (!isMediaInfoRar && (MI_DISK_IMAGES.contains(FilenameUtils.getExtension(currentMovie.getFile().getName())))) {
            // extracting IFO files from ISO file
            AbstractFile abstractIsoFile;

            // Issue 979: Split the reading of the ISO file to catch any errors
            try {
                abstractIsoFile = FileFactory.getFile(currentMovie.getFile().getAbsolutePath());
            } catch (Exception error) {
                LOG.debug(LOG_MESSAGE + "Error reading disk Image. Please re-rip and try again");
                LOG.info(error.getMessage());
                return;
            }

            IsoArchiveFile scannedIsoFile = new IsoArchiveFile(abstractIsoFile);
            File tempRep = new File(randomDirName + "/VIDEO_TS");
            FileTools.makeDirs(tempRep);

            OutputStream fosCurrentIFO = null;
            try {
                @SuppressWarnings("unchecked")
                Vector<ArchiveEntry> allEntries = scannedIsoFile.getEntries();
                Iterator<ArchiveEntry> parcoursEntries = allEntries.iterator();
                while (parcoursEntries.hasNext()) {
                    ArchiveEntry currentArchiveEntry = (ArchiveEntry) parcoursEntries.next();
                    if (currentArchiveEntry.getName().toLowerCase().endsWith(".ifo")) {
                        File currentIFO = new File(randomDirName + "/VIDEO_TS" + File.separator + currentArchiveEntry.getName());
                        fosCurrentIFO = FileTools.createFileOutputStream(currentIFO);
                        byte[] ifoFileContent = new byte[Integer.parseInt(Long.toString(currentArchiveEntry.getSize()))];
                        scannedIsoFile.getEntryInputStream(currentArchiveEntry).read(ifoFileContent);
                        fosCurrentIFO.write(ifoFileContent);
                    }
                }
            } catch (IOException error) {
                LOG.info(error.getMessage());
            } catch (NumberFormatException error) {
                LOG.info(error.getMessage());
            } finally {
                if (fosCurrentIFO != null) {
                    try {
                        fosCurrentIFO.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }

            // Scan IFO files
            FilePropertiesMovie mainMovieIFO = localDVDRipScanner.executeGetDVDInfo(tempRep);
            if (mainMovieIFO != null) {
                if (IS_ACTIVATED) {
                    scan(currentMovie, mainMovieIFO.getLocation(), Boolean.FALSE);
                    // Issue 1176 - Prevent lost of NFO Data
                    if (StringTools.isNotValidString(currentMovie.getRuntime())) {
                        currentMovie.setRuntime(DateTimeTools.formatDuration(mainMovieIFO.getDuration()), MEDIAINFO_PLUGIN_ID);
                    }
                } else if (OverrideTools.checkOverwriteRuntime(currentMovie, MEDIAINFO_PLUGIN_ID)) {
                    currentMovie.setRuntime(DateTimeTools.formatDuration(mainMovieIFO.getDuration()), MEDIAINFO_PLUGIN_ID);
                }
            }

            // Clean up
            FileTools.deleteDir(randomDirName);
        } else if (IS_ACTIVATED) {
            if (isMediaInfoRar && MI_DISK_IMAGES.contains(FilenameUtils.getExtension(currentMovie.getFile().getName()))) {
                LOG.debug(LOG_MESSAGE + "Using MediaInfo-rar to scan " + currentMovie.getFile().getName());
            }
            scan(currentMovie, currentMovie.getFile().getAbsolutePath(), Boolean.TRUE);
        }

    }

    private void scan(Movie currentMovie, String movieFilePath, boolean processMultiPart) {
        // retrieve values usable for multipart values
        Map<String, String> infosMultiPart = new HashMap<String, String>();
        if (isMultiPartsScannable(currentMovie, processMultiPart)) {
            for (MovieFile movieFile : currentMovie.getMovieFiles()) {
                if (movieFile.getFile() == null) {
                    continue;
                }
                String filePath = movieFile.getFile().getAbsolutePath();
                // avoid double scanning of files
                if (!movieFilePath.equalsIgnoreCase(filePath)) {
                    scanMultiParts(filePath, infosMultiPart);
                }
            }
        }

        InputStream is = null;
        try {
            is = createInputStream(movieFilePath);

            Map<String, String> infosGeneral = new HashMap<String, String>();
            List<Map<String, String>> infosVideo = new ArrayList<Map<String, String>>();
            List<Map<String, String>> infosAudio = new ArrayList<Map<String, String>>();
            List<Map<String, String>> infosText = new ArrayList<Map<String, String>>();

            parseMediaInfo(is, infosGeneral, infosVideo, infosAudio, infosText);

            updateMovieInfo(currentMovie, infosGeneral, infosVideo, infosAudio, infosText, infosMultiPart);
        } catch (IOException ex) {
            LOG.warn(LOG_MESSAGE + "Failed reading mediainfo output for " + movieFilePath);
            LOG.error(SystemTools.getStackTrace(ex));
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                    // ignore this error
                }
            }
        }
    }

    private boolean isMultiPartsScannable(Movie movie, boolean processMultiPart) {
        if (!ENABLE_MULTIPART) {
            return Boolean.FALSE;
        } else if (!processMultiPart) {
            return Boolean.FALSE;
        } else if (movie.isTVShow()) {
            return Boolean.FALSE;
        } else if (movie.getMovieFiles().size() <= 1) {
            return Boolean.FALSE;
        } else if (!OverrideTools.checkOneOverwrite(movie, MEDIAINFO_PLUGIN_ID, OverrideFlag.RUNTIME)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private void scanMultiParts(String movieFilePath, Map<String, String> infosMultiPart) {
        InputStream is = null;
        try {
            is = createInputStream(movieFilePath);

            Map<String, String> infosGeneral = new HashMap<String, String>();
            List<Map<String, String>> infosVideo = new ArrayList<Map<String, String>>();
            List<Map<String, String>> infosAudio = new ArrayList<Map<String, String>>();
            List<Map<String, String>> infosText = new ArrayList<Map<String, String>>();

            parseMediaInfo(is, infosGeneral, infosVideo, infosAudio, infosText);

            // resolve duration
            int duration = getDuration(infosGeneral, infosVideo);
            // add already stored multipart runtime
            duration = duration + getMultiPartDuration(infosMultiPart);
            if (duration > 0) {
                infosMultiPart.put("MultiPart_Duration", String.valueOf(duration));
            }
        } catch (IOException ex) {
            LOG.warn(LOG_MESSAGE + "Failed reading mediainfo output for " + movieFilePath);
            LOG.error(SystemTools.getStackTrace(ex));
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {
                    // ignore this error
                }
            }
        }
    }

    private InputStream createInputStream(String movieFilePath) throws IOException {
        if (MI_READ_FROM_FILE) {
            // check file
            String filename = FilenameUtils.removeExtension(movieFilePath) + ".mediainfo";
            Collection<File> files = FileTools.fileCache.searchFilename(filename, Boolean.FALSE);
            if (files != null && files.size() > 0) {
                // create new input stream for reading
                LOG.debug(LOG_MESSAGE + "Reading from file " + filename);
                return new FileInputStream(files.iterator().next());
            }
        }

        // Create the command line
        List<String> commandMedia = new ArrayList<String>(MI_EXE);
        commandMedia.add(movieFilePath);

        ProcessBuilder pb = new ProcessBuilder(commandMedia);

        // set up the working directory.
        pb.directory(MI_PATH);

        Process p = pb.start();
        return p.getInputStream();
    }

    /**
     * Read the input skipping any blank lines
     *
     * @param input
     * @return
     * @throws IOException
     */
    private String localInputReadLine(BufferedReader input) throws IOException {
        String line = input.readLine();
        while ((line != null) && (line.equals(""))) {
            line = input.readLine();
        }
        return line;
    }

    public void parseMediaInfo(InputStream in,
            Map<String, String> infosGeneral,
            List<Map<String, String>> infosVideo,
            List<Map<String, String>> infosAudio,
            List<Map<String, String>> infosText) throws IOException {

        InputStreamReader isr = null;
        BufferedReader bufReader = null;

        try {
            isr = new InputStreamReader(in);
            bufReader = new BufferedReader(isr);

            // Improvement, less code line, each cat have same code, so use the same for all.
            Map<String, List<Map<String, String>>> matches = new HashMap<String, List<Map<String, String>>>();

            // Create a fake one for General, we got only one, but to use the same algo we must create this one.
            String generalKey[] = {"General", "Géneral", "* Général"};
            matches.put(generalKey[0], new ArrayList<Map<String, String>>());
            matches.put(generalKey[1], matches.get(generalKey[0])); // Issue 1311 - Create a "link" between General and Général
            matches.put(generalKey[2], matches.get(generalKey[0])); // Issue 1311 - Create a "link" between General and * Général
            matches.put("Video", infosVideo);
            matches.put("Vidéo", matches.get("Video")); // Issue 1311 - Create a "link" between Vidéo and Video
            matches.put("Audio", infosAudio);
            matches.put("Text", infosText);

            String line = localInputReadLine(bufReader);
            String label;

            while (line != null) {
                // In case of new format : Text #1, Audio #1
                if (line.indexOf('#') >= 0) {
                    line = line.substring(0, line.indexOf('#')).trim();
                }

                // Get cat ArrayList from cat name.
                List<Map<String, String>> currentCat = matches.get(line);

                if (currentCat != null) {
                    Map<String, String> currentData = new HashMap<String, String>();
                    int indexSeparator = -1;
                    while (((line = localInputReadLine(bufReader)) != null) && ((indexSeparator = line.indexOf(" : ")) != -1)) {
                        label = line.substring(0, indexSeparator).trim();
                        if (currentData.get(label) == null) {
                            currentData.put(label, line.substring(indexSeparator + 3));
                        }
                    }
                    currentCat.add(currentData);
                } else {
                    line = localInputReadLine(bufReader);
                }
            }

            // Setting General Info - Beware of lose data if infosGeneral already have some ...
            try {
                for (String singleKey : generalKey) {
                    List<Map<String, String>> arrayList = matches.get(singleKey);
                    if (arrayList.size() > 0) {
                        Map<String, String> datas = arrayList.get(0);
                        if (datas.size() > 0) {
                            infosGeneral.putAll(datas);
                            break;
                        }
                    }
                }
            } catch (Exception ignore) {
                // We don't care about this exception
            }
        } finally {
            if (isr != null) {
                isr.close();
            }

            if (bufReader != null) {
                bufReader.close();
            }
        }
    }

    private void updateMovieInfo(Movie movie, Map<String, String> infosGeneral, List<Map<String, String>> infosVideo,
            List<Map<String, String>> infosAudio, List<Map<String, String>> infosText,
            Map<String, String> infosMultiPart) {

        String infoValue;

        // update movie with meta tags if present
        if (ENABLE_METADATA) {
            if (OverrideTools.checkOverwriteTitle(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosGeneral.get("Movie");
                if (infoValue == null) {
                    infoValue = infosGeneral.get("Movie name");
                }
                movie.setTitle(infoValue, MEDIAINFO_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteDirectors(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosGeneral.get("Director");
                movie.setDirector(infoValue, MEDIAINFO_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwritePlot(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosGeneral.get("Summary");
                if (infoValue == null) {
                    infoValue = infosGeneral.get("Comment");
                }
                movie.setPlot(infoValue, MEDIAINFO_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteGenres(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosGeneral.get("Genre");
                if (infoValue != null) {
                    List<String> newGenres = StringTools.splitList(infoValue, SPLIT_GENRE);
                    movie.setGenres(newGenres, MEDIAINFO_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteActors(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosGeneral.get("Actor");
                if (infoValue == null) {
                    infoValue = infosGeneral.get("Performer");
                }
                if (infoValue != null) {
                    List<String> list = StringTools.splitList(infoValue, SPLIT_GENRE);
                    movie.setCast(list, MEDIAINFO_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteCertification(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosGeneral.get("LawRating");
                if (infoValue == null) {
                    infoValue = infosGeneral.get("Law rating");
                }
                movie.setCertification(infoValue, MEDIAINFO_PLUGIN_ID);
            }

            infoValue = infosGeneral.get("Rating");
            if (infoValue != null) {
                try {
                    float r = Float.parseFloat(infoValue);
                    r = r * 20.0f;
                    movie.addRating(MEDIAINFO_PLUGIN_ID, Math.round(r));
                } catch (NumberFormatException ignore) {
                    // nothing to do
                }
            }

            if (OverrideTools.checkOverwriteCountry(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosGeneral.get("Country");
                if (infoValue == null) {
                    infoValue = infosGeneral.get("Movie/Country");
                }
                if (infoValue == null) {
                    infoValue = infosGeneral.get("Movie name/Country");
                }
                movie.setCountry(infoValue, MEDIAINFO_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteReleaseDate(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosGeneral.get("Released_Date");
                movie.setReleaseDate(infoValue, MEDIAINFO_PLUGIN_ID);
            }
        } // enableMetaData

        // get Container from General Section
        if (OverrideTools.checkOverwriteContainer(movie, MEDIAINFO_PLUGIN_ID)) {
            infoValue = infosGeneral.get("Format");
            movie.setContainer(infoValue, MEDIAINFO_PLUGIN_ID);
        }

        if (OverrideTools.checkOverwriteRuntime(movie, MEDIAINFO_PLUGIN_ID)) {
            int duration = getDuration(infosGeneral, infosVideo);
            duration = duration + getMultiPartDuration(infosMultiPart);
            if (duration > 0) {
                movie.setRuntime(DateTimeTools.formatDuration(duration / 1000), MEDIAINFO_PLUGIN_ID);
            }
        }

        // get Info from first Video Stream
        // - can evolve to get info from longest Video Stream
        if (infosVideo.size() > 0) {
            // At this point there is only a codec pulled from the filename, so we can clear that now
            movie.getCodecs().clear();

            Map<String, String> infosMainVideo = infosVideo.get(0);

            // Add the video codec to the list
            Codec codecToAdd = getCodecInfo(CodecType.VIDEO, infosMainVideo);
            if (MI_OVERALL_BITRATE && StringTools.isNotValidString(codecToAdd.getCodecBitRate())) {
                infoValue = infosGeneral.get(Codec.MI_CODEC_OVERALL_BITRATE);
                if (StringUtils.isNotBlank(infoValue) && infoValue.length() > 3) {
                    infoValue = infoValue.substring(0, infoValue.length() - 3);
                    codecToAdd.setCodecBitRate(infoValue);
                }
            }
            movie.addCodec(codecToAdd);

            if (OverrideTools.checkOverwriteResolution(movie, MEDIAINFO_PLUGIN_ID)) {
                String width = infosMainVideo.get("Width");
                String height = infosMainVideo.get("Height");
                movie.setResolution(width, height, MEDIAINFO_PLUGIN_ID);
            }

            // Frames per second
            if (OverrideTools.checkOverwriteFPS(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosMainVideo.get("Frame rate");
                if (infoValue == null) {
                    // use original frame rate
                    infoValue = infosMainVideo.get("Original frame rate");
                }

                if (infoValue != null) {
                    int inxDiv = infoValue.indexOf(Movie.SPACE_SLASH_SPACE);
                    if (inxDiv > -1) {
                        infoValue = infoValue.substring(0, inxDiv);
                    }
                    try {
                        movie.setFps(Float.parseFloat(infoValue), MEDIAINFO_PLUGIN_ID);
                    } catch (NumberFormatException nfe) {
                        LOG.debug(nfe.getMessage());
                    }
                }
            }

            // Save the aspect ratio for the video
            if (OverrideTools.checkOverwriteAspectRatio(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosMainVideo.get("Display aspect ratio");
                if (infoValue != null) {
                    movie.setAspectRatio(ASPECT_TOOLS.cleanAspectRatio(infoValue), MEDIAINFO_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteVideoOutput(movie, MEDIAINFO_PLUGIN_ID)) {
                // Guessing Video Output (Issue 988)
                if (movie.isHD()) {
                    StringBuilder normeHD = new StringBuilder();
                    if (movie.isHD1080()) {
                        normeHD.append("1080");
                    } else {
                        normeHD.append("720");
                    }

                    infoValue = infosMainVideo.get("Scan type");
                    if (infoValue != null) {
                        if (infoValue.equals("Progressive")) {
                            normeHD.append("p");
                        } else {
                            normeHD.append("i");
                        }
                    }
                    normeHD.append(" ").append(Math.round(movie.getFps())).append("Hz");
                    movie.setVideoOutput(normeHD.toString(), MEDIAINFO_PLUGIN_ID);
                } else {
                    StringBuilder videoOutput = new StringBuilder();
                    switch (Math.round(movie.getFps())) {
                        case 24:
                            videoOutput.append("24");
                            break;
                        case 25:
                            videoOutput.append("PAL 25");
                            break;
                        case 30:
                            videoOutput.append("NTSC 30");
                            break;
                        case 50:
                            videoOutput.append("PAL 50");
                            break;
                        case 60:
                            videoOutput.append("NTSC 60");
                            break;
                        default:
                            videoOutput.append("NTSC");
                            break;
                    }
                    infoValue = infosMainVideo.get("Scan type");
                    if (infoValue != null) {
                        if (infoValue.equals("Progressive")) {
                            videoOutput.append("p");
                        } else {
                            videoOutput.append("i");
                        }
                    }
                    movie.setVideoOutput(videoOutput.toString(), MEDIAINFO_PLUGIN_ID);
                }
            }

            if (OverrideTools.checkOverwriteVideoSource(movie, MEDIAINFO_PLUGIN_ID)) {
                infoValue = infosMainVideo.get("MultiView_Count");
                if ("2".equals(infoValue)) {
                    movie.setVideoSource("3D", MEDIAINFO_PLUGIN_ID);
                }
            }
        }

        // Cycle through Audio Streams
        // boolean previousAudioCodec = !movie.getAudioCodec().equals(Movie.UNKNOWN); // Do we have AudioCodec already?
        // boolean previousAudioChannels = !movie.getAudioChannels().equals(Movie.UNKNOWN); // Do we have AudioChannels already?
        Set<String> foundLanguages = new HashSet<String>();

        for (int numAudio = 0; numAudio < infosAudio.size(); numAudio++) {
            Map<String, String> infosCurAudio = infosAudio.get(numAudio);

            infoValue = infosCurAudio.get("Language");
            if (infoValue != null) {
                // Issue 1227 - Make some clean up in mediainfo datas.
                if (infoValue.contains("/")) {
                    infoValue = infoValue.substring(0, infoValue.indexOf('/')).trim(); // In this case, language are "doubled", just take the first one.
                }
                // Add determination of language.
                String determineLanguage = MovieFilenameScanner.determineLanguage(infoValue);
                foundLanguages.add(determineLanguage);
            }

            infoValue = infosCurAudio.get("Codec ID/Hint");
            if (infoValue == null) {
                infoValue = infosCurAudio.get("Codec");
            }

            // Add the audio codec to the list
            Codec codecToAdd = getCodecInfo(CodecType.AUDIO, infosCurAudio);
            movie.addCodec(codecToAdd);
        }

        if (OverrideTools.checkOverwriteLanguage(movie, MEDIAINFO_PLUGIN_ID)) {
            StringBuilder movieLanguage = new StringBuilder();
            for (String language : foundLanguages) {
                if (movieLanguage.length() > 0) {
                    movieLanguage.append(LANG_DELIM);
                }
                movieLanguage.append(language);
            }

            if (StringTools.isValidString(movieLanguage.toString())) {
                movie.setLanguage(movieLanguage.toString(), MEDIAINFO_PLUGIN_ID);
            } else if (StringTools.isValidString(AUDIO_LANG_UNKNOWN)) {
                String determineLanguage = MovieFilenameScanner.determineLanguage(AUDIO_LANG_UNKNOWN);
                movie.setLanguage(determineLanguage, MEDIAINFO_PLUGIN_ID);
            }
        }

        // Cycle through Text Streams
        for (int numText = 0; numText < infosText.size(); numText++) {
            Map<String, String> infosCurText = infosText.get(numText);

            String infoLanguage = "";
            infoValue = infosCurText.get("Language");

            // Issue 1450 - If we are here, we have subtitles, but didn't have the language, setting an value of "UNDEFINED" to make it appear
            if (StringTools.isNotValidString(infoValue)) {
                infoValue = "UNDEFINED";
            }

            if (StringTools.isValidString(infoValue)) {
                // Issue 1227 - Make some clean up in mediainfo datas.
                if (infoValue.contains("/")) {
                    infoValue = infoValue.substring(0, infoValue.indexOf('/')).trim(); // In this case, languages are "doubled", just take the first one.
                }
                infoLanguage = MovieFilenameScanner.determineLanguage(infoValue);
            }

            String infoFormat = "";
            infoValue = infosCurText.get("Format");

            if (StringTools.isValidString(infoValue)) {
                infoFormat = infoValue;
            } else {
                // Issue 1450 - If we are here, we have subtitles, but didn't have the language
                // Take care of label for "Format" in mediaInfo 0.6.1.1
                infoValue = infosCurText.get("Codec");
                if (StringTools.isValidString(infoValue)) {
                    infoFormat = infoValue;
                }
            }

            // Make sure we have a codec & language before continuing
            if (StringTools.isValidString(infoFormat) && StringTools.isValidString(infoLanguage)) {
                if (infoFormat.equalsIgnoreCase("SRT")
                        || infoFormat.equalsIgnoreCase("UTF-8")
                        || infoFormat.equalsIgnoreCase("RLE")
                        || infoFormat.equalsIgnoreCase("PGS")
                        || infoFormat.equalsIgnoreCase("ASS")
                        || infoFormat.equalsIgnoreCase("VobSub")) {
                    SubtitleTools.addMovieSubtitle(movie, infoLanguage);
                } else {
                    LOG.debug(LOG_MESSAGE + "Subtitle format skipped: " + infoFormat);
                }
            }
        }
    }

    private int getDuration(Map<String, String> infosGeneral, List<Map<String, String>> infosVideo) {
        String runtimeValue = null;
        if (runtimeValue == null) {
            runtimeValue = infosGeneral.get("PlayTime");
        }
        if ((runtimeValue == null) && (infosVideo.size() > 0)) {
            Map<String, String> infosMainVideo = infosVideo.get(0);
            runtimeValue = infosMainVideo.get("Duration");
        }
        if (runtimeValue == null) {
            runtimeValue = infosGeneral.get("Duration");
        }
        if (runtimeValue != null) {
            if (runtimeValue.indexOf('.') >= 0) {
                runtimeValue = runtimeValue.substring(0, runtimeValue.indexOf('.'));
            }
            try {
                return Integer.parseInt(runtimeValue);
            } catch (NumberFormatException nfe) {
                LOG.debug(nfe.getMessage());
            }
        }
        return 0;
    }

    private int getMultiPartDuration(Map<String, String> infosMultiPart) {
        if (infosMultiPart.isEmpty()) {
            return 0;
        }

        String runtimeValue = infosMultiPart.get("MultiPart_Duration");
        return NumberUtils.toInt(runtimeValue, 0);
    }

    /**
     * Create a Codec object with the information from the file
     *
     * @param codecType
     * @param codecInfos
     * @return
     */
    protected Codec getCodecInfo(CodecType codecType, Map<String, String> codecInfos) {
        Codec codec = new Codec(codecType);
        codec.setCodecSource(CodecSource.MEDIAINFO);

        codec.setCodec(codecInfos.get(Codec.MI_CODEC));
        codec.setCodecFormat(codecInfos.get(Codec.MI_CODEC_FORMAT));
        codec.setCodecFormatProfile(codecInfos.get(Codec.MI_CODEC_FORMAT_PROFILE));
        codec.setCodecFormatVersion(codecInfos.get(Codec.MI_CODEC_FORMAT_VERSION));
        codec.setCodecId(codecInfos.get(Codec.MI_CODEC_ID));
        codec.setCodecIdHint(codecInfos.get(Codec.MI_CODEC_ID_HINT));
        codec.setCodecLanguage(codecInfos.get(Codec.MI_CODEC_LANGUAGE));

        String[] keyBitrates = {Codec.MI_CODEC_BITRATE, Codec.MI_CODEC_NOMINAL_BITRATE};
        for (String key : Arrays.asList(keyBitrates)) {
            String infoValue = codecInfos.get(key);
            if (StringUtils.isNotBlank(infoValue)) {
                if (infoValue.indexOf(Movie.SPACE_SLASH_SPACE) > -1) {
                    infoValue = infoValue.substring(0, infoValue.indexOf(Movie.SPACE_SLASH_SPACE));
                }
                infoValue = infoValue.substring(0, infoValue.length() - 3);
                codec.setCodecBitRate(infoValue);
                break;
            }
            if (codecType.equals(CodecType.AUDIO) && key.equals(Codec.MI_CODEC_BITRATE)) {
                break;
            }
        }

        // Cycle through the codec channel labels
        String codecChannels = "";
        for (String cc : Codec.MI_CODEC_CHANNELS) {
            codecChannels = codecInfos.get(cc);
            if (StringUtils.isNotBlank(codecChannels)) {
                // We have our channels
                break;
            }
        }

        if (StringUtils.isNotBlank(codecChannels)) {
            if (codecChannels.contains("/")) {
                codecChannels = codecChannels.substring(0, codecChannels.indexOf('/'));
            }
            Matcher codecMatch = PATTERN_CHANNELS.matcher(codecChannels);
            if (codecMatch.matches()) {
                codec.setCodecChannels(Integer.parseInt(codecMatch.group(1)));
            }
        }
        return codec;
    }

    public String archiveScan(Movie currentMovie, String movieFilePath) {
        if (!IS_ACTIVATED) {
            return null;
        }

        LOG.debug(LOG_MESSAGE + "mini-scan on " + movieFilePath);

        try {
            // Create the command line
            List<String> commandMedia = new ArrayList<String>(MI_EXE);
            // Technically, mediaInfoExe has "-f" in it from above, but "-s" will override it anyway.
            // "-s" will dump just "$size $path" inside RAR/ISO.
            commandMedia.add("-s");
            commandMedia.add(movieFilePath);

            ProcessBuilder pb = new ProcessBuilder(commandMedia);

            // set up the working directory.
            pb.directory(MI_PATH);

            Process p = pb.start();

            BufferedReader input = new BufferedReader(
                    new InputStreamReader(
                            p.getInputStream()));
            String line;
            String mediaArchive = null;

            while ((line = localInputReadLine(input)) != null) {
                Pattern patternArchive
                        = Pattern.compile("^\\s*\\d+\\s(.*)$");
                Matcher m = patternArchive.matcher(line);
                if (m.find() && (m.groupCount() == 1)) {
                    mediaArchive = m.group(1);
                }
            }
            input.close();

            LOG.debug(LOG_MESSAGE + "Returning with archivename " + mediaArchive);

            return mediaArchive;

        } catch (IOException error) {
            LOG.error(SystemTools.getStackTrace(error));
        }

        return null;
    }

    /**
     * Look for the mediaInfo filename and return it. Will check first for the mediainfo-rar file and then mediainfo
     *
     * @return
     */
    protected static File findMediaInfo() {
        File mediaInfoFile;

        if (OS_NAME.contains("Windows")) {
            mediaInfoFile = FileUtils.getFile(MI_PATH.getAbsolutePath(), MI_RAR_FILENAME_WINDOWS);
            if (!mediaInfoFile.exists()) {
                // Fall back to the normal filename
                mediaInfoFile = FileUtils.getFile(MI_PATH.getAbsolutePath(), MI_FILENAME_WINDOWS);
            } else {
                // Enable the extra mediainfo-rar features
                isMediaInfoRar = Boolean.TRUE;
            }
        } else {
            mediaInfoFile = FileUtils.getFile(MI_PATH.getAbsolutePath(), MI_RAR_FILENAME_LINUX);
            if (!mediaInfoFile.exists()) {
                // Fall back to the normal filename
                mediaInfoFile = FileUtils.getFile(MI_PATH.getAbsolutePath(), MI_FILENAME_LINUX);
            } else {
                // Enable the extra mediainfo-rar features
                isMediaInfoRar = Boolean.TRUE;
            }
        }

        return mediaInfoFile;
    }
}
