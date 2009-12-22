/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

package com.moviejukebox.scanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.xmm.moviemanager.fileproperties.FilePropertiesMovie;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.XMLHelper;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.ArchiveEntry;
import com.mucommander.file.FileFactory;
import com.mucommander.file.impl.iso.IsoArchiveFile;

/**
 * @author Grael
 */
public class MediaInfoScanner {

    private static Logger logger = Logger.getLogger("moviejukebox");

    // mediaInfo repository
    private File mediaInfoPath;

    // mediaInfo command line, depend on OS
    private String[] mediaInfoExe;
    private String[] mediaInfoExeWindows = {"cmd.exe", "/E:1900", "/C", "MediaInfo.exe", "-f", null};
    private String[] mediaInfoExeLinux = {"./mediainfo", "-f", null};
    public final static String OS_NAME = System.getProperty("os.name");
    public final static String OS_VERSION = System.getProperty("os.version");
    public final static String OS_ARCH = System.getProperty("os.arch");
    private boolean activated;
    private boolean enableMetadata;

    static {
        logger.finer("OS name : " + OS_NAME);
        logger.finer("OS version : " + OS_VERSION);
        logger.finer("OS archi : " + OS_ARCH);
    }
    
    // Dvd rip infos Scanner
    private DVDRipScanner localDVDRipScanner;

    /**
     * @param mediaInfoPath
     */
    public MediaInfoScanner() {
        mediaInfoPath = new File(PropertiesUtil.getProperty("mediainfo.home", "./mediaInfo/"));

        File checkMediainfo = null;

        if (OS_NAME.contains("Windows")) {
            mediaInfoExe = mediaInfoExeWindows;
            checkMediainfo = new File(mediaInfoPath.getAbsolutePath() + File.separator + "MediaInfo.exe");
        } else {
            mediaInfoExe = mediaInfoExeLinux;
            checkMediainfo = new File(mediaInfoPath.getAbsolutePath() + File.separator + "mediainfo");
        }
        //System.out.println(checkMediainfo.getAbsolutePath());
        if (!checkMediainfo.canExecute()) {
            logger.fine("Couldn't find CLI mediaInfo executable tool : Video files data won't be extracted");
            activated = false;
        } else {
            activated = true;
        }
        localDVDRipScanner = new DVDRipScanner();

        enableMetadata = Boolean.parseBoolean(PropertiesUtil.getProperty("mediainfo.metadata.enable", "false"));
    }

    @SuppressWarnings("unchecked")
    public void scan(Movie currentMovie) {
        String randomDirName = "./isoTEMP/" + Thread.currentThread().getName() + "/VIDEO_TS";
        
        if (currentMovie.getFile().isDirectory()) {
            // Scan IFO files
            FilePropertiesMovie mainMovieIFO = localDVDRipScanner.executeGetDVDInfo(currentMovie.getFile());
            if (mainMovieIFO != null) {
                scan(currentMovie, mainMovieIFO.getLocation());
                currentMovie.setRuntime(formatDuration(mainMovieIFO.getDuration()));
            }
        } else if ((currentMovie.getFile().getName().toLowerCase().endsWith(".iso")) || (currentMovie.getFile().getName().toLowerCase().endsWith(".img"))) {
            // extracting IFO files from ISO file
            AbstractFile abstractIsoFile = null;
            
            // Issue 979: Split the reading of the ISO file to catch any errors
            try {
                abstractIsoFile = FileFactory.getFile(currentMovie.getFile().getAbsolutePath());
            } catch (Exception error) {
                logger.finer("Error reading disk Image. Please re-rip and try again");
                logger.fine(error.getMessage());
                return;
            }
            
            IsoArchiveFile scannedIsoFile = new IsoArchiveFile(abstractIsoFile);
            File tempRep = new File(randomDirName);
            tempRep.mkdirs();
            
            try {
                Vector<ArchiveEntry> allEntries = scannedIsoFile.getEntries();
                Iterator<ArchiveEntry> parcoursEntries = allEntries.iterator();
                while (parcoursEntries.hasNext()) {
                    ArchiveEntry currentArchiveEntry = (ArchiveEntry) parcoursEntries.next();
                    if (currentArchiveEntry.getName().toLowerCase().endsWith(".ifo")) {
                        File currentIFO = new File(randomDirName + File.separator + currentArchiveEntry.getName());
                        FileOutputStream fosCurrentIFO = new FileOutputStream(currentIFO);
                        byte[] ifoFileContent = new byte[Integer.parseInt(Long.toString(currentArchiveEntry.getSize()))];
                        scannedIsoFile.getEntryInputStream(currentArchiveEntry).read(ifoFileContent);
                        fosCurrentIFO.write(ifoFileContent);
                        fosCurrentIFO.close();
                    }
                }
            } catch (Exception error) {
                logger.fine(error.getMessage());
            }

            // Scan IFO files
            FilePropertiesMovie mainMovieIFO = localDVDRipScanner.executeGetDVDInfo(tempRep);
            if (mainMovieIFO != null) {
                scan(currentMovie, mainMovieIFO.getLocation());
                currentMovie.setRuntime(formatDuration(mainMovieIFO.getDuration()));
            }

            // Clean up
            File[] isoList = tempRep.listFiles();
            for (int nbFiles = 0; nbFiles < isoList.length; nbFiles++) {
                isoList[nbFiles].delete();
            }
            tempRep.delete();
            new File("./isoTEMP/" + Thread.currentThread().getName()).delete();
        } else {
            scan(currentMovie, currentMovie.getFile().getAbsolutePath());
        }

    }

    public void scan(Movie currentMovie, String movieFilePath) {

        if (!activated) {
            return;
        }

        try {
            String[] commandMedia = mediaInfoExe;
            commandMedia[commandMedia.length - 1] = movieFilePath;

            ProcessBuilder pb = new ProcessBuilder(commandMedia);

            // set up the working directory.
            pb.directory(mediaInfoPath);

            Process p = pb.start();

            HashMap<String, String> infosGeneral = new HashMap<String, String>();
            ArrayList<HashMap<String, String>> infosVideo = new ArrayList<HashMap<String, String>>();
            ArrayList<HashMap<String, String>> infosAudio = new ArrayList<HashMap<String, String>>();
            ArrayList<HashMap<String, String>> infosText = new ArrayList<HashMap<String, String>>();

            parseMediaInfo(p, infosGeneral, infosVideo, infosAudio, infosText);

            updateMovieInfo(currentMovie, infosGeneral, infosVideo, infosAudio, infosText);

        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    private String localInputReadLine(BufferedReader input) throws IOException {
        // Suppress empty lines
        String line = input.readLine();
        while ((line != null) && (line.equals(""))) {
            line = input.readLine();
        }
        return line;
    }

    private void parseMediaInfo(Process p, HashMap<String, String> infosGeneral, ArrayList<HashMap<String, String>> infosVideo,
            ArrayList<HashMap<String, String>> infosAudio, ArrayList<HashMap<String, String>> infosText) throws IOException {

        BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = localInputReadLine(input);

        while (line != null) {
            if (line.equals("General")) {
                int indexSeparateur = -1;
                while (((line = localInputReadLine(input)) != null) && ((indexSeparateur = line.indexOf(" : ")) != -1)) {
                    int longueurUtile = indexSeparateur - 1;
                    while (line.charAt(longueurUtile) == ' ') {
                        longueurUtile--;
                    }
                    if (infosGeneral.get(line.substring(0, longueurUtile + 1)) == null) {
                        infosGeneral.put(line.substring(0, longueurUtile + 1), line.substring(indexSeparateur + 3));
                    }
                }
            } else if (line.startsWith("Video")) {
                HashMap<String, String> vidCourante = new HashMap<String, String>();

                int indexSeparateur = -1;
                while (((line = localInputReadLine(input)) != null) && ((indexSeparateur = line.indexOf(" : ")) != -1)) {
                    int longueurUtile = indexSeparateur - 1;
                    while (line.charAt(longueurUtile) == ' ') {
                        longueurUtile--;
                    }
                    if (vidCourante.get(line.substring(0, longueurUtile + 1)) == null) {
                        vidCourante.put(line.substring(0, longueurUtile + 1), line.substring(indexSeparateur + 3));
                    }
                }
                infosVideo.add(vidCourante);
            } else if (line.startsWith("Audio")) {
                HashMap<String, String> audioCourant = new HashMap<String, String>();

                int indexSeparateur = -1;
                while (((line = localInputReadLine(input)) != null) && ((indexSeparateur = line.indexOf(" : ")) != -1)) {
                    int longueurUtile = indexSeparateur - 1;
                    while (line.charAt(longueurUtile) == ' ') {
                        longueurUtile--;
                    }
                    if (audioCourant.get(line.substring(0, longueurUtile + 1)) == null) {
                        audioCourant.put(line.substring(0, longueurUtile + 1), line.substring(indexSeparateur + 3));
                    }
                }
                infosAudio.add(audioCourant);
            } else if (line.startsWith("Text")) {
                HashMap<String, String> textCourant = new HashMap<String, String>();

                int indexSeparateur = -1;
                while (((line = localInputReadLine(input)) != null) && ((indexSeparateur = line.indexOf(" : ")) != -1)) {
                    int longueurUtile = indexSeparateur - 1;
                    while (line.charAt(longueurUtile) == ' ') {
                        longueurUtile--;
                    }
                    textCourant.put(line.substring(0, longueurUtile + 1), line.substring(indexSeparateur + 3));
                }
                infosText.add(textCourant);
            } else {
                line = localInputReadLine(input);
            }

        }
        input.close();

    }

    private void updateMovieInfo(Movie movie, HashMap<String, String> infosGeneral, ArrayList<HashMap<String, String>> infosVideo,
            ArrayList<HashMap<String, String>> infosAudio, ArrayList<HashMap<String, String>> infosText) {

        String infoValue;

        // update movie with meta tags if present
        if (enableMetadata) {
            if (!movie.isOverrideTitle()) {
                infoValue = infosGeneral.get("Movie");
                if (infoValue == null) {
                    infoValue = infosGeneral.get("Movie name");
                }
                if (infoValue != null) {
                    movie.setTitle(infoValue);
                    movie.setOverrideTitle(true);
                }
            }
            infoValue = infosGeneral.get("Director");
            if (infoValue != null) {
                movie.setDirector(infoValue);
            }
            infoValue = infosGeneral.get("Summary");
            if (infoValue == null) {
                infoValue = infosGeneral.get("Comment");
            }
            if (infoValue != null) {
                movie.setPlot(infoValue);
            }
            infoValue = infosGeneral.get("Genre");
            if (infoValue != null) {
                List<String> list = XMLHelper.parseList(infoValue, "|/,");
                if (!list.isEmpty()) {
                    movie.setGenres(list);
                }
            }
            infoValue = infosGeneral.get("Actor");
            if (infoValue == null) {
                infoValue = infosGeneral.get("Performer");
            }
            if (infoValue != null) {
                List<String> list = XMLHelper.parseList(infoValue, "|/,");
                if (!list.isEmpty()) {
                    movie.setCast(list);
                }
            }
            infoValue = infosGeneral.get("LawRating");
            if (infoValue == null) {
                infoValue = infosGeneral.get("Law rating");
            }
            if (infoValue != null) {
                movie.setCertification(infoValue);
            }
            infoValue = infosGeneral.get("Rating");
            if (infoValue != null) {
                try {
                    float r = Float.parseFloat(infoValue);
                    r = r * 20.0f;
                    movie.setRating(Math.round(r));
                } catch (Exception ignore) {
                }
            }
            infoValue = infosGeneral.get("Country");
            if (infoValue == null) {
                infoValue = infosGeneral.get("Movie/Country");
            }
            if (infoValue == null) {
                infoValue = infosGeneral.get("Movie name/Country");
            }
            if (infoValue != null) {
                movie.setCountry(infoValue);
            }
            infoValue = infosGeneral.get("Released_Date");
            if (infoValue != null) {
                movie.setReleaseDate(infoValue);
            }
        }

        // get Container from General Section
        infoValue = infosGeneral.get("Format");
        if (infoValue != null) {
            movie.setContainer(infoValue);
        }

        // get Infos from first Video Stream
        // - can evolve to get info from longuest Video Stream
        if (infosVideo.size() > 0) {
            HashMap<String, String> infosMainVideo = infosVideo.get(0);

            // Check that movie is not multi part
            if (movie.getMovieFiles().size() == 1) {
                // Duration
                infoValue = infosMainVideo.get("Duration");
                if (infoValue != null) {

                    int duration;
                    duration = Integer.parseInt(infoValue) / 1000;

                    movie.setRuntime(formatDuration(duration));
                }
            }

            // Codec (most relevant Info depending on mediainfo result)
            infoValue = infosMainVideo.get("Codec ID/Hint");
            if (infoValue != null) {
                movie.setVideoCodec(infoValue);
            } else {
                infoValue = infosMainVideo.get("Codec ID");
                if (infoValue != null) {
                    movie.setVideoCodec(infoValue);
                } else {
                    infoValue = infosMainVideo.get("Format");
                    if (infoValue != null) {
                        movie.setVideoCodec(infoValue);
                    }
                }
            }

            // Resolution
            int width = 0;

            infoValue = infosMainVideo.get("Width");
            if (infoValue != null) {
                width = Integer.parseInt(infoValue);

                infoValue = infosMainVideo.get("Height");
                if (infoValue != null) {
                    movie.setResolution(width + "x" + infoValue);
                }
            }

            // Frames per second
            infoValue = infosMainVideo.get("Frame rate");
            if (infoValue != null) {
                Float fps;
                fps = Float.parseFloat(infoValue);

                movie.setFps(fps);
            }

            // Guessing Video Output (Issue 988)
            String normeHD = "SD";
            if (width > 1280) { // If width is greater than 720p then it must be 1080
                normeHD = "1080";
            } else if (width > 1024) { // 1024 is the maximum width that SD content could be, this compensates for cropped HD720 content 
                normeHD = "720";
            }

            if (!normeHD.equals("SD")) {
                infoValue = infosMainVideo.get("Scan type");
                if (infoValue != null) {
                    if (infoValue.equals("Progressive")) {
                        normeHD += "p";
                    } else {
                        normeHD += "i";
                    }
                }
                movie.setVideoOutput(normeHD + " " + Math.round(movie.getFps()) + "Hz");

            } else {
                String videoOutput;
                switch (Math.round(movie.getFps())) {
                    case 24:
                        videoOutput = "24";
                        break;
                    case 25:
                        videoOutput = "PAL 25";
                        break;
                    case 30:
                        videoOutput = "NTSC 30";
                        break;
                    case 50:
                        videoOutput = "PAL 50";
                        break;
                    case 60:
                        videoOutput = "NTSC 60";
                        break;
                    default:
                        videoOutput = "NTSC";
                        break;
                }
                infoValue = infosMainVideo.get("Scan type");
                if (infoValue != null) {
                    if (infoValue.equals("Progressive")) {
                        videoOutput += "p";
                    } else {
                        videoOutput += "i";
                    }
                }
                movie.setVideoOutput(videoOutput);

            }
        }

        // Cycle through Audio Streams
        movie.setAudioCodec(Movie.UNKNOWN);

        for (int numAudio = 0; numAudio < infosAudio.size(); numAudio++) {
            HashMap<String, String> infosCurAudio = infosAudio.get(numAudio);

            String infoLanguage = "";
            infoValue = infosCurAudio.get("Language");
            if (infoValue != null) {
                infoLanguage = " (" + infoValue + ")";
            }

            infoValue = infosCurAudio.get("Codec ID/Hint");
            if (infoValue == null) {
                infoValue = infosCurAudio.get("Codec");
            }

            if (infoValue != null) {
                String oldInfo = movie.getAudioCodec();
                if (oldInfo.toUpperCase().equals(Movie.UNKNOWN)) {
                    movie.setAudioCodec(infoValue + infoLanguage);
                } else {
                    movie.setAudioCodec(oldInfo + " / " + infoValue + infoLanguage);
                }
            }

            infoValue = infosCurAudio.get("Channel(s)");
            if (infoValue != null) {
                String oldInfo = movie.getAudioChannels();
                if (oldInfo.toUpperCase().equals(Movie.UNKNOWN)) {
                    movie.setAudioChannels(infoValue);
                } else {
                    movie.setAudioChannels(oldInfo + " / " + infoValue);
                }
            }

        }

        // Cycle through Text Streams
        for (int numText = 0; numText < infosText.size(); numText++) {
            HashMap<String, String> infosCurText = infosText.get(numText);

            String infoFormat = "";
            infoValue = infosCurText.get("Format");
            if (infoValue != null) {
                infoFormat = infoValue;
            }

            // Check that the format can be viewed on the popcorn
            if (infoFormat.equals("SRT") || infoFormat.equals("UTF-8") || infoFormat.equals("RLE")) {
                movie.setSubtitles(true);
            } else {
                logger.finest("MediaInfo Scanner - Subtitle format skipped: " + infoFormat);
            }
        }
    }

    public static String formatDuration(int duration) {
        StringBuffer returnString = new StringBuffer("");

        int nbHours = duration / 3600;
        if (nbHours != 0) {
            returnString.append(nbHours).append("h");
            duration = duration - nbHours * 3600;
        }

        int nbMinutes = duration / 60;
        if (nbMinutes != 0) {
            if (nbHours != 0) {
                returnString.append(" ");
            }
            returnString.append(nbMinutes).append("mn");
        }

        return returnString.toString();
    }
}
