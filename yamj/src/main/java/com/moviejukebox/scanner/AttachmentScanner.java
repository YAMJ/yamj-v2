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
package com.moviejukebox.scanner;

import static com.moviejukebox.tools.PropertiesUtil.TRUE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Attachment.Attachment;
import com.moviejukebox.model.Attachment.AttachmentType;
import com.moviejukebox.model.Attachment.ContentType;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;

/**
 * Scans and extracts attachments within a file i.e. matroska files.
 *
 * @author modmax
 */
public class AttachmentScanner {

    private static final Logger logger = Logger.getLogger(AttachmentScanner.class);
    private static final String LOG_MESSAGE = "AttachmentScanner: ";
    
    // mkvToolnix
    private static final File MT_PATH = new File(PropertiesUtil.getProperty("attachment.mkvtoolnix.home", "./mkvToolnix/"));
    private static final String MT_LANGUAGE = PropertiesUtil.getProperty("attachment.mkvtoolnix.language", "");

    // mkvToolnix command line, depend on OS
    private static final List<String> MT_INFO_EXE = new ArrayList<String>();
    private static final List<String> MT_EXTRACT_EXE = new ArrayList<String>();
    private static final String MT_INFO_FILENAME_WINDOWS = "mkvinfo.exe";
    private static final String MT_INFO_FILENAME_LINUX = "mkvinfo";
    private static final String MT_EXTRACT_FILENAME_WINDOWS = "mkvextract.exe";
    private static final String MT_EXTRACT_FILENAME_LINUX = "mkvextract";
    private static boolean activated = false;

    // temporary directory
    private static File tempDirectory;
    private static boolean tempCleanup = PropertiesUtil.getBooleanProperty("attachment.temp.cleanup", TRUE);

    // enable/disable recheck
    private static boolean recheckEnabled = PropertiesUtil.getBooleanProperty("attachment.recheck.enabled", TRUE);

    // the operating system name
    public static final String OS_NAME = System.getProperty("os.name");
    
    // properties for NFO handling
    private static final String[] NFOExtensions = PropertiesUtil.getProperty("filename.nfo.extensions", "NFO").toLowerCase().split(",");
    
    // scan for multiple fanart image names
    private static final List<String> fanartImageNames = StringTools.splitList(PropertiesUtil.getProperty("attachment.fanart.imageNames", "fanart,backdrop,background"),",");
    // image tokens
    private static final String fanartToken = PropertiesUtil.getProperty("attachment.token.fanart", ".fanart").toLowerCase();
    private static final String bannerToken = PropertiesUtil.getProperty("attachment.token.banner", ".banner").toLowerCase();
    private static String videoimageToken = PropertiesUtil.getProperty("attachment.token.videoimage", ".videoimage").toLowerCase();
    private static String posterToken = PropertiesUtil.getProperty("attachment.token.poster", ".poster").toLowerCase();
    
    // valid MIME types
    private static List<String> validTextMimeTypes;
    private static Map<String,String> validImageMimeTypes; 
    
    protected AttachmentScanner() {
        throw new UnsupportedOperationException("AttachmentScanner is a utility class and cannot be instatiated");
    }
    
    static {
        File checkMkvInfo = findMkvInfo();
        File checkMkvExtract = findMkvExtract();

        if (OS_NAME.contains("Windows")) {
            if (MT_INFO_EXE.isEmpty()) {
                MT_INFO_EXE.add("cmd.exe");
                MT_INFO_EXE.add("/E:1900");
                MT_INFO_EXE.add("/C");
                MT_INFO_EXE.add(checkMkvInfo.getName());
                MT_INFO_EXE.add("--ui-language");
                if (StringUtils.isBlank(MT_LANGUAGE)) {
                    MT_INFO_EXE.add("en");
                } else {
                    MT_INFO_EXE.add(MT_LANGUAGE);
                }
            }
            if (MT_EXTRACT_EXE.isEmpty()) {
                MT_EXTRACT_EXE.add("cmd.exe");
                MT_EXTRACT_EXE.add("/E:1900");
                MT_EXTRACT_EXE.add("/C");
                MT_EXTRACT_EXE.add(checkMkvExtract.getName());
            }
        } else {
            if (MT_INFO_EXE.isEmpty()) {
                MT_INFO_EXE.add("./" + checkMkvInfo.getName());
                MT_INFO_EXE.add("--ui-language");
                if (StringUtils.isBlank(MT_LANGUAGE)) {
                    MT_INFO_EXE.add("en_US");
                } else {
                    MT_INFO_EXE.add(MT_LANGUAGE);
                }
            }
            if (MT_EXTRACT_EXE.isEmpty()) {
                MT_EXTRACT_EXE.add("./" + checkMkvExtract.getName());
            }
        }
        
        if (!checkMkvInfo.canExecute()) {
            logger.info(LOG_MESSAGE + "Couldn't find MKV toolnix executable tool 'mkvinfo'");
            activated = false;
        } else if (!checkMkvExtract.canExecute()) {
            logger.info(LOG_MESSAGE + "Couldn't find MKV toolnix executable tool 'mkvextract'");
            activated = false;
        } else {
            logger.info(LOG_MESSAGE + "MkvToolnix will be used to extract matroska attachments");
            activated = true;
        }
        
        if (activated) {
            // just create temporary directories if MkvToolnix is activated
            try {
                String tempLocation = PropertiesUtil.getProperty("attachment.temp.directory", "");
                if (StringUtils.isBlank(tempLocation)) {
                    tempLocation = StringTools.appendToPath(PropertiesUtil.getProperty("mjb.jukeboxTempDir", "./temp"),"attachments");
                }
                
                File tempFile = new File(FileTools.getCanonicalPath(tempLocation));
                if (tempFile.exists()) {
                    tempDirectory = tempFile;
                } else {
                    logger.debug(LOG_MESSAGE + "Creating temporary attachment location: (" + tempLocation + ")");
                    boolean status = tempFile.mkdirs();
                    int i = 1;
                    while (!status && i++ <= 10) {
                        Thread.sleep(1000);
                        status = tempFile.mkdirs();
                    }
        
                    if (status && i > 10) {
                        logger.error("Failed creating the temporary attachment directory: (" + tempLocation + ")");
                    } else  {
                        tempDirectory = tempFile;
                    }
                }
            } catch (Exception ignore) {}
        }
        
        if (!isActivated()) {
            logger.info(LOG_MESSAGE + "Scanner is deactivated");
        } else {
            validTextMimeTypes = new ArrayList<String>();
            validTextMimeTypes.add("text/xml");
            validTextMimeTypes.add("application/xml");
            validTextMimeTypes.add("text/html");

            validImageMimeTypes = new HashMap<String,String>();
            validImageMimeTypes.put("image/jpeg", ".jpg");
            validImageMimeTypes.put("image/png", ".png");
            validImageMimeTypes.put("image/gif", ".gif");
            validImageMimeTypes.put("image/x-ms-bmp", ".bmp");
        }
    }

    private static boolean isActivated() {
        return (activated && (tempDirectory != null));
    }

    private static boolean isFileScannable(File file) {
        if (file == null) {
            return false;
        } else if (!file.exists()) {
            return false;
        }  else if (!"MKV".equalsIgnoreCase(FileTools.getFileExtension(file.getName()))) {
            // no matroska file
            return false;
        }
        return true;
    }
    
    public static void scan(Movie movie) {
        if (!isActivated()) {
            return;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            // movie to scan must have file format
            return;
        }

        for (MovieFile movieFile : movie.getMovieFiles()) {
            if (isFileScannable(movieFile.getFile())) {
                scanMatroskaAttachments(movieFile);
            }
        }
    }
    
    public static boolean rescan(Movie movie, File xmlFile) {
        if (!isActivated()) {
            return false;
        } else if (!recheckEnabled) {
            return false;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            // movie the scan must be a file
            return false;
        }

        // holds the return value
        boolean returnValue = false;

        // flag to indicate the first movie
        boolean firstMovie = true;
        
        for (MovieFile movieFile : movie.getMovieFiles()) {
            if (isFileScannable(movieFile.getFile())) {
                if (FileTools.isNewer(movieFile.getFile(), xmlFile)) {
                    // scan attachments
                    scanMatroskaAttachments(movieFile);
                    
                    // check attachments and determine changes
                    for (Attachment attachment : movieFile.getAttachments()) {
                        if (firstMovie) {
                            if (ContentType.NFO == attachment.getContentType()) {
                                returnValue = true;
                                movie.setDirty(DirtyFlag.NFO);
                            } else if (ContentType.POSTER == attachment.getContentType()) {
                                returnValue = true;
                                movie.setDirty(DirtyFlag.POSTER);
                                // Set to unknown for not taking poster from Jukebox
                                movie.setPosterURL(Movie.UNKNOWN);
                            } else if (ContentType.FANART == attachment.getContentType()) {
                                returnValue = true;
                                movie.setDirty(DirtyFlag.FANART);
                                // Set to unknown for not taking fanart from Jukebox
                                movie.setFanartURL(Movie.UNKNOWN);
                            } else if (ContentType.BANNER == attachment.getContentType()) {
                                returnValue = true;
                                movie.setDirty(DirtyFlag.BANNER);
                                // Set to unknown for not taking banner from Jukebox
                                movie.setBannerURL(Movie.UNKNOWN);
                            }
                        } else {
                            // TODO videoimage handling
                        }
                    }
                }
            }
        }
        
        return returnValue;
    }
    
    private static void scanMatroskaAttachments(MovieFile movieFile) {
        if (movieFile.isAttachmentsScanned()) {
            // attachments has been scanned during rescan of movie
            return;
        }
        
        // clear existing attachments
        movieFile.clearAttachments();
        
        // the file with possible attachments
        File scanFile = movieFile.getFile();
        
        logger.debug(LOG_MESSAGE + "Scanning file "+scanFile.getName()); 
        int attachmentId = 0;
        try {
            // create the command line
            List<String> commandMedia = new ArrayList<String>(MT_INFO_EXE);
            commandMedia.add(scanFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(commandMedia);

            // set up the working directory.
            pb.directory(MT_PATH);

            Process p = pb.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = localInputReadLine(input);
            while (line != null) {
                if (line.contains("+ Attached")) {
                    // increase the attachment id
                    attachmentId++;
                    // next line contains file name
                    String fileNameLine =  localInputReadLine(input);
                    // next line contains MIME type
                    String mimeTypeLine =  localInputReadLine(input);
                    
                    Attachment attachment = createMatroskaAttachment(attachmentId, fileNameLine, mimeTypeLine);
                    if (attachment != null) {
                        attachment.setSourceFile(movieFile.getFile());
                        movieFile.addAttachment(attachment);
                    }
                }
                
                line = localInputReadLine(input);
            }
            
            if (p.waitFor() != 0) {
                logger.error(LOG_MESSAGE + "Error during attachment retrieval - ErrorCode=" + p.exitValue());
            }
        } catch (Exception error) {
            logger.error(SystemTools.getStackTrace(error));
        }

        // attachments has been scanned; no double scan of attachments needed
        movieFile.setAttachmentsScanned(Boolean.TRUE);
    }
    
    private static String localInputReadLine(BufferedReader input) {
        String line = null;
        try {
            line = input.readLine();
            while ((line != null) && (line.equals(""))) {
                line = input.readLine();
            }
        } catch (IOException ignore) {}
        return line;
    }


    /**
     * Look for the mkvinfo filename and return it.
     *
     * @return
     */
    private static File findMkvInfo() {
        File mkvInfoFile;

        if (OS_NAME.contains("Windows")) {
            mkvInfoFile = new File(StringTools.appendToPath(MT_PATH.getAbsolutePath(), MT_INFO_FILENAME_WINDOWS));
        } else {
            mkvInfoFile = new File(StringTools.appendToPath(MT_PATH.getAbsolutePath(), MT_INFO_FILENAME_LINUX));
        }
        
        return mkvInfoFile;
    }

    /**
     * Look for the mkvextract filename and return it.
     *
     * @return
     */
    private static File findMkvExtract() {
        File mkvExtractFile;

        if (OS_NAME.contains("Windows")) {
            mkvExtractFile = new File(StringTools.appendToPath(MT_PATH.getAbsolutePath(), MT_EXTRACT_FILENAME_WINDOWS));
        } else {
            mkvExtractFile = new File(StringTools.appendToPath(MT_PATH.getAbsolutePath(), MT_EXTRACT_FILENAME_LINUX));
        }
        
        return mkvExtractFile;
    }

    /**
     * Creates an matroska attachment.
     * 
     * @param id
     * @param filename
     * @param mimetype
     * @return Attachment or null
     */
    private static Attachment createMatroskaAttachment(int id, String filename, String mimetype) {
        String fixedFileName = null;
        if (filename.contains("File name:")) {
            fixedFileName = filename.substring(filename.indexOf("File name:")+10).trim();
        }
        String fixedMimeType = null;
        if (mimetype.contains("Mime type:")) {
            fixedMimeType = mimetype.substring(mimetype.indexOf("Mime type:")+10).trim();
        }
        
        ContentType type = determineContentType(fixedFileName, fixedMimeType);
 
        Attachment attachment = null;
        if (type == null) {
            logger.debug(LOG_MESSAGE + "Failed to dertermine attachment type for '"+fixedFileName+"' ("+fixedMimeType+")");
        } else {
            attachment = new Attachment();
            attachment.setType(AttachmentType.MATROSKA);
            attachment.setAttachmentId(id);
            attachment.setContentType(type);
            attachment.setMimeType(fixedMimeType.toLowerCase());
            logger.debug(LOG_MESSAGE + "Found attachment "+attachment);
        }
        return attachment;
    }

    private static ContentType determineContentType(String inFileName, String inMimeType) {
        if (inFileName == null) return null;
        if (inMimeType == null) return null;
        String fileName = inFileName.toLowerCase();
        String mimeType = inMimeType.toLowerCase();
        
        if (validTextMimeTypes.contains(mimeType)) {
            // NFO
            for (String extension : NFOExtensions) {
                if (extension.equalsIgnoreCase(FilenameUtils.getExtension(fileName))) {
                    return ContentType.NFO;
                }
            }
        } else if (validImageMimeTypes.containsKey(mimeType)) {
            String check = FilenameUtils.removeExtension(fileName);
            for (String imageName : fanartImageNames) {
                if (check.endsWith(imageName)) return ContentType.FANART;
            }
            if (check.endsWith(posterToken)) return ContentType.POSTER;
            if (check.endsWith(bannerToken)) return ContentType.BANNER;
            if (check.endsWith(videoimageToken)) return ContentType.VIDEOIMAGE;
            // check for equality (i.e. poster.jpg)
            if (check.equals(posterToken.substring(1))) return ContentType.POSTER;
            if (check.equals(bannerToken.substring(1))) return ContentType.BANNER;
            if (check.equals(videoimageToken.substring(1))) return ContentType.VIDEOIMAGE;
        }
        
        // no content type determined
        return null;
    }

    public static void addAttachedNfo(Movie movie, List<File> nfoFiles) {
        if (!isActivated()) {
            return;
        }
        
        // only use attached NFO if there are no locale NFOs
        if (nfoFiles.isEmpty()) {
            File nfoFile = extractToLocalFile(ContentType.NFO, movie, -1);
            if (nfoFile != null) {
                logger.debug(LOG_MESSAGE +"Extracted nfo "+nfoFile.getAbsolutePath());
                nfoFiles.add(nfoFile);
            }
        }
    }
    
    
    public static File extractAttachedFanart(Movie movie) {
        File fanartFile = extractToLocalFile(ContentType.FANART, movie, -1);
        if (fanartFile != null) {
            logger.debug(LOG_MESSAGE +"Extracted fanart "+fanartFile.getAbsolutePath());
        }
        return fanartFile;
    }

    public static File extractAttachedPoster(Movie movie) {
        File posterFile =  extractToLocalFile(ContentType.POSTER, movie, -1);
        if (posterFile != null) {
            logger.debug(LOG_MESSAGE +"Extracted poster "+posterFile.getAbsolutePath());
        }
        return posterFile;
    }

    public static File extractAttachedBanner(Movie movie) {
        File bannerFile =  extractToLocalFile(ContentType.BANNER, movie, -1);
        if (bannerFile != null) {
            logger.debug(LOG_MESSAGE +"Extracted banner "+bannerFile.getAbsolutePath());
        }
        return bannerFile;
    }

    public static File extractAttachedVideoimage(Movie movie, int part) {
        File videoimageFile =  extractToLocalFile(ContentType.VIDEOIMAGE, movie, part);
        if (videoimageFile != null) {
            logger.debug(LOG_MESSAGE +"Extracted videoimage "+videoimageFile.getAbsolutePath());
        }
        return videoimageFile;
    }
    
    private static File extractToLocalFile(ContentType contentType, Movie movie, int part)  {
        if (!isActivated()) {
            return null;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            return null;
        }

        List<Attachment> attachments = findAttachments(movie, contentType, part);

        File localFile = null;
        if (attachments != null && !attachments.isEmpty()) {
            for (Attachment attachment : attachments) {
                localFile = extractAttachment(attachment);
                if (localFile != null && localFile.exists()) {
                    // found extracted local file
                    break;
                } else {
                    // case where local file is set but does not exist
                    localFile = null;
                }
            }
        }
        return localFile;
    }
    
    private static List<Attachment> findAttachments(Movie movie, ContentType contentType, int part) {
        MovieFile movieFile = null;
        Iterator<MovieFile> it = movie.getMovieFiles().iterator();
        while (it.hasNext()) {
            if (part <= 0) {
                // use first movie file
                movieFile = it.next();
                break;
            } else {
                MovieFile mv = it.next();
                if (mv.getFirstPart() == part) {
                    movieFile = mv;
                    break;
                }
            }
        }
        
        if (movieFile == null) {
            // no matching movie file found
            return null;
        } else if (movieFile.getAttachments().isEmpty()) {
            // movie file must have attachments
            return null;
        }

        List<Attachment> attachments = new ArrayList<Attachment>();
        for (Attachment attachment : movieFile.getAttachments()) {
            if (contentType.compareTo(attachment.getContentType()) == 0) {
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    private static File extractAttachment(Attachment attachment) {
        File returnFile = null;
        if (AttachmentType.MATROSKA == attachment.getType()) {
            returnFile = extractMatroskaAttachment(attachment);
        }
        return returnFile;
    }

    private static final File extractMatroskaAttachment(Attachment attachment) {
        File sourceFile = attachment.getSourceFile();
        if (sourceFile == null) {
            // source file must exist
            return null;
        } else if (!sourceFile.exists()) {
            // source file must exist
            return null;
        }
        
        // build return file name
        StringBuffer returnFileName = new StringBuffer();
        returnFileName.append(tempDirectory.getAbsolutePath());
        returnFileName.append(File.separatorChar);
        returnFileName.append(FilenameUtils.removeExtension(sourceFile.getName()));
        switch (attachment.getContentType()) {
            case NFO:
                returnFileName.append(".nfo");
                break;
            case POSTER:
                returnFileName.append(posterToken);
                returnFileName.append(validImageMimeTypes.get(attachment.getMimeType()));
                break;
            case FANART:
                returnFileName.append(fanartToken);
                returnFileName.append(validImageMimeTypes.get(attachment.getMimeType()));
                break;
            case BANNER:
                returnFileName.append(bannerToken);
                returnFileName.append(validImageMimeTypes.get(attachment.getMimeType()));
                break;
            case VIDEOIMAGE:
                returnFileName.append(videoimageToken);
                returnFileName.append(validImageMimeTypes.get(attachment.getMimeType()));
                break;
        }
        
        File returnFile = new File(returnFileName.toString());
        if (returnFile.exists()) {
            // already present or extracted
            return returnFile;
        }

        //logger.debug(LOG_MESSAGE + "Extract attachement ("+attachment+")");
        
        try {
            // Create the command line
            List<String> commandMedia = new ArrayList<String>(MT_EXTRACT_EXE);
            commandMedia.add("attachments");
            commandMedia.add(sourceFile.getAbsolutePath());
            commandMedia.add(attachment.getAttachmentId()+":"+returnFileName.toString());
            
            ProcessBuilder pb = new ProcessBuilder(commandMedia);
            pb.directory(MT_PATH);
            Process p = pb.start();
            
            if (p.waitFor() != 0) {
                logger.error(LOG_MESSAGE + "Error during extraction - ErrorCode=" + p.exitValue());
                returnFile = null;
            }
        } catch (Exception error) {
            logger.error(SystemTools.getStackTrace(error));
            returnFile = null;
        }

        if (returnFile != null) {
            //  need to reset last modification date to last modification date
             // of source file to fulfill later checks
             try {
                 returnFile.setLastModified(sourceFile.lastModified());
             } catch (Exception ignore) {}
         }
         return returnFile;
    }
    
    public static void cleanUp() {
        if (tempCleanup && (tempDirectory != null) && tempDirectory.exists()) {
            FileTools.deleteDir(tempDirectory);
        }
    }

}
