/*
 *      Copyright (c) 2004-2010 YAMJ Members
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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.moviejukebox.model.Library;
import com.moviejukebox.model.MediaLibraryPath;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.MovieFileNameDTO;
import com.moviejukebox.scanner.BDRipScanner.BDFilePropertiesMovie;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;

/**
 * DirectoryScanner
 * 
 * @author jjulien
 * @author gaelead
 * @author jriihi
 */
public class MovieDirectoryScanner {

    private int mediaLibraryRootPathIndex; // always includes path delimiter
    private final Set<String> supportedExtensions = new HashSet<String>();
    private String detailFormat;
    private String thumbnailFormat;
    private String posterFormat;
    private String bannerFormat;
    private String fanartFormat;

    private String detailToken;
    private String thumbnailToken;
    private String posterToken;
    private String bannerToken;
    private String fanartToken;

    private String opensubtitles;
    private int hashpathdepth;
    private Boolean excludeFilesWithoutExternalSubtitles;
    private Boolean excludeMultiPartBluRay;
    private Boolean playFullBluRayDisk;
    private static Logger logger = Logger.getLogger("moviejukebox");
    private static int dirCount = 1;
    private static int fileCount = 0;

    // BD rip infos Scanner
    private BDRipScanner localBDRipScanner;

    public MovieDirectoryScanner() {
        for (String ext : PropertiesUtil.getProperty("mjb.extensions", "AVI DIVX MKV WMV M2TS TS RM QT ISO VOB MPG MOV").toUpperCase().split(" ")) {
            supportedExtensions.add(ext);
        }

        detailFormat     = PropertiesUtil.getProperty("posters.format",    "png");
        thumbnailFormat  = PropertiesUtil.getProperty("thumbnails.format", "png");
        posterFormat     = "jpg"; // Default to JPG 
        bannerFormat     = PropertiesUtil.getProperty("banners.format",    "jpg");
        fanartFormat     = PropertiesUtil.getProperty("fanart.format",     "jpg");

        detailToken      = PropertiesUtil.getProperty("artwork.token.detail",        "_large");
        thumbnailToken   = PropertiesUtil.getProperty("artwork.token.thumbnail",     "_small");
        posterToken      = PropertiesUtil.getProperty("poster.scanner.artworkToken", "");
        bannerToken      = PropertiesUtil.getProperty("banner.scanner.artworkToken", ".banner");
        fanartToken      = PropertiesUtil.getProperty("fanart.scanner.artworkToken", ".fanart");

        excludeFilesWithoutExternalSubtitles = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.subtitles.ExcludeFilesWithoutExternal", "false"));
        excludeMultiPartBluRay = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.excludeMultiPartBluRay", "false"));
        opensubtitles = PropertiesUtil.getProperty("opensubtitles.language", ""); // We want to check this isn't set for the exclusion
        hashpathdepth = Integer.parseInt(PropertiesUtil.getProperty("mjb.scanner.hashpathdepth", "0"));
        playFullBluRayDisk = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.playFullBluRayDisk", "false"));
        localBDRipScanner = new BDRipScanner();
    }

    /**
     * Scan the specified directory for movies files.
     * 
     * @param directory
     *            movie library rootfile
     * @return a new library
     */
    public Library scan(MediaLibraryPath srcPath, Library library) {

        File directory = new FileTools.FileEx(srcPath.getPath());

        String mediaLibraryRoot;
        if (directory.isFile()) {
            mediaLibraryRoot = directory.getParentFile().getAbsolutePath();
        } else {
            mediaLibraryRoot = directory.getAbsolutePath();
        }
        // including path delimiter
        mediaLibraryRootPathIndex = FileTools.getDirPathWithSeparator(mediaLibraryRoot).length();

        this.scanDirectory(srcPath, directory, library);
        return library;
    }

    protected void scanDirectory(MediaLibraryPath srcPath, File directory, Library collection) {
        FileTools.fileCache.fileAdd(directory);
        if (directory.isFile()) {
            scanFile(srcPath, directory, collection);
        } else {
            File[] files = directory.listFiles();

            if (files != null) {
                fileCount += files.length;
            }
            
            System.out.print("\r    Scanning directory #" + dirCount++ + ", " + fileCount + " files scanned");

            if (files != null && files.length > 0) {
                List<File> fileList = Arrays.asList(files);
                Collections.sort(fileList);

                // Prescan files list. Ignore directory if file with predefined name is found.
                // TODO May be read the file and exclude files by mask (similar to .cvsignore)
                for (File file : files) {
                    if (file.getName().equalsIgnoreCase(".mjbignore")) {
                        logger.finest("Scanning of directory " + directory.getAbsolutePath() + " skipped due to override file");
                        return;
                    }
                }

                // add all files to the global cache, after ignore check but before the actual scan
                FileTools.fileCache.addFiles(files);

                for (File file : fileList) {
                    if (!isFiltered(srcPath, file)) {
                        if (file.isDirectory() && file.getName().equalsIgnoreCase("VIDEO_TS")) {
                            scanFile(srcPath, file.getParentFile(), collection);
                        } else if (file.isDirectory() && file.getName().equalsIgnoreCase("BDMV")) {
                            scanFile(srcPath, file.getParentFile(), collection);
                        } else if (file.isDirectory()) {
                            scanDirectory(srcPath, file, collection);
                        } else {
                            scanFile(srcPath, file, collection);
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks the file or directory passed to determine if it should be excluded from the scan
     * 
     * @param srcPath
     * @param file
     * @return boolean flag, true if the file should be excluded, false otherwise
     */
    protected boolean isFiltered(MediaLibraryPath srcPath, File file) {
        boolean isDirectory = file.isDirectory();
        String filename = file.getName();

        // Skip these parts if the file is a directory
        if (!isDirectory) {
            int index = filename.lastIndexOf(".");
            if (index < 0) {
                return true;
            }

            String extension = file.getName().substring(index + 1).toUpperCase();
            if (!supportedExtensions.contains(extension)) {
                return true;
            }

            // Exclude files without external subtitles
            if (opensubtitles.equals("")) { // We are not downloading subtitles, so exclude those that don't have any.
                if (excludeFilesWithoutExternalSubtitles && !hasSubtitles(file)) {
                    logger.fine("File " + filename + " excluded. (no external subtitles)");
                    return true;
                }
            }
        }

        // Compute the relative filename
        String relativeFilename = file.getAbsolutePath().substring(mediaLibraryRootPathIndex);

        String relativeFileNameLower = relativeFilename.toLowerCase();
        String jukeboxName = PropertiesUtil.getProperty("mjb.detailsDirName", "Jukebox");

        for (String excluded : srcPath.getExcludes()) {
            if (excluded.length() > 0) {
                try {
                    Pattern excludePatt = Pattern.compile(excluded, Pattern.CASE_INSENSITIVE);
                    if (excludePatt.matcher(relativeFileNameLower).find()) {
                        logger.finest((isDirectory ? "Directory '" : "File '") + relativeFilename + "' excluded.");
                        return true;
                    }
                } catch (Exception error) {
                    logger.fine("MovieDirectoryScanner: Error processing exclusion pattern: " + excluded);
                }
                
                excluded = excluded.replace("/", File.separator);
                excluded = excluded.replace("\\", File.separator);
                if (relativeFileNameLower.indexOf(excluded.toLowerCase()) >= 0) {
                    // Don't print a message for the exclusion of Jukebox files
                    if (!relativeFileNameLower.contains(jukeboxName)) {
                        logger.finest((isDirectory ? "Directory '" : "File '") + relativeFilename + "' excluded.");
                    }
                    return true;
                }
            }
        }

        return false;
    }

    protected static boolean hasSubtitles(File fileToScan) {

        String path = fileToScan.getAbsolutePath().toUpperCase();
        String basename = path.substring(0, path.lastIndexOf(".") + 1);

        return FileTools.fileCache.fileExists(basename + "SRT") ||
               FileTools.fileCache.fileExists(basename + "SUB") ||
               FileTools.fileCache.fileExists(basename + "SSA") ||
               FileTools.fileCache.fileExists(basename + "SMI") ||
               FileTools.fileCache.fileExists(basename + "PGS") ||
               path.contains("VOST");
    }

    private void scanFile(MediaLibraryPath srcPath, File file, Library library) {
        File contentFiles[];
        int bdDuration = 0;
        boolean isBluRay = false;

        contentFiles = new File[1];
        contentFiles[0] = file;
        
        if (file.isDirectory()) {
            // Scan BD Playlist files
            BDFilePropertiesMovie bdPropertiesMovie = localBDRipScanner.executeGetBDInfo(file);

            if (bdPropertiesMovie != null) {
                isBluRay = true;
                
                // Exclude multi part BluRay that include more than one file
                if (excludeMultiPartBluRay && bdPropertiesMovie.fileList.length > 1) {
                    logger.fine("File " + file.getName() + " excluded. (multi part BluRay)");
                    return;
                }

                bdDuration = bdPropertiesMovie.duration;
                
                contentFiles = bdPropertiesMovie.fileList;
            }
        }

        String hashstr = "";
        for (int i = 0; i < contentFiles.length; i++) {
            Movie movie = new Movie();
            
            // Hopefully this is a fix for issue #670 -- I can't duplicate it, since I don't have an BD rips
            if (contentFiles[i] == null) {
                continue;
            }

            // Compute the baseFilename: This is the filename without the extension
            String baseFileName = file.getName();

            if (!file.isDirectory()) {
                baseFileName = baseFileName.substring(0, file.getName().lastIndexOf("."));
            }
            
            // Compute the relative filename
            String relativeFilename = contentFiles[i].getAbsolutePath().substring(mediaLibraryRootPathIndex);

            MovieFile movieFile = new MovieFile();
            relativeFilename = relativeFilename.replace('\\', '/'); // make it unix!

            if (contentFiles[i].isDirectory()) {
                // For DVD images
                movieFile.setFilename(srcPath.getPlayerRootPath() + HTMLTools.encodeUrlPath(relativeFilename) + "/VIDEO_TS");
                movie.setFormatType(Movie.TYPE_DVD);
            } else {
                if (isBluRay && playFullBluRayDisk) {
                    // A BluRay File and playFullBluRayDisk, so link to the directory and not the file
                    String tempFilename = srcPath.getPlayerRootPath() + HTMLTools.encodeUrlPath(relativeFilename);
                    tempFilename = tempFilename.substring(0, tempFilename.toUpperCase().lastIndexOf("BDMV"));
                    movieFile.setFilename(tempFilename);
                } else {
                    // Normal movie file so link to it
                    movieFile.setFilename(srcPath.getPlayerRootPath() + HTMLTools.encodeUrlPath(relativeFilename));
                    movie.setFormatType(Movie.TYPE_FILE);
                }
            }
            
            if (isBluRay) {
                // This is to overwrite the TYPE_FILE check above if the disk is bluray but not playFullBluRayDisk
                movie.setFormatType(Movie.TYPE_BLURAY);
            }
            
            // FIXME: part and file info are to be taken from filename scanner
            movieFile.setPart(i + 1);
            movieFile.setFile(contentFiles[i]);

            movie.setScrapeLibrary(srcPath.isScrapeLibrary());
            movie.addMovieFile(movieFile);
            movie.setFile(contentFiles[i]);
            movie.setContainerFile(file);
            movie.setBaseFilename(baseFileName);

            // Ensure that filename is unique. Prevent interference between files like "disk1.avi".
            // TODO: Actually it makes sense to use normalized movie name instead of first part name.
            if(hashpathdepth > 0){
                int d, pos = relativeFilename.length();
                for(d = hashpathdepth+1; d > 0 && pos > 0; d-- ) {
                    pos = relativeFilename.lastIndexOf("/", pos-1);
                }
                hashstr = relativeFilename.substring(pos+1);
                hashstr = Integer.toHexString(hashstr.hashCode());
            }
            movie.setBaseName(FileTools.makeSafeFilename(baseFileName) + hashstr);

            movie.setLibraryPath(srcPath.getPath());
            movie.setPosterFilename(movie.getBaseName() + posterToken + "." + posterFormat);
            movie.setThumbnailFilename(movie.getBaseName() + thumbnailToken + "." + thumbnailFormat);
            movie.setDetailPosterFilename(movie.getBaseName() + detailToken + "." + detailFormat);
            movie.setBannerFilename(movie.getBaseName() + bannerToken + "." + bannerFormat);
            movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartFormat);
            movie.setSubtitles(hasSubtitles(movie.getFile())==true?"YES":"NO");
            movie.setLibraryDescription(srcPath.getDescription());
            movie.setPrebuf(srcPath.getPrebuf());
            movie.setFileDate(new Date(file.lastModified()));
            // Issue 1241 - Take care of directory for size.
            movie.setFileSize(this.calculateFileSize(file));
            
            MovieFileNameDTO dto = MovieFilenameScanner.scan(file);
            
            movie.mergeFileNameDTO(dto);

            if (bdDuration == 0) {
                // XXX Why do we not merge Bluray information?
                // Do not merge file information for Bluray
                movieFile.mergeFileNameDTO(dto);
            } else {
                if (playFullBluRayDisk && movie.isTVShow()) {
                    // This is needed for multi-part disks and TV shows
                    movieFile.mergeFileNameDTO(dto);
                }
                // Set duration for BD disks using the data in the playlist + mark Bluray source and container
                // Issue 1176 - Prevent lost of NFO Data
                if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                    movie.setRuntime(MediaInfoScanner.formatDuration(bdDuration));
                }
                
                if (!movie.getVideoSource().equalsIgnoreCase("HDDVD")) {                
                    movie.setContainer("BluRay");
                    movie.setVideoSource("BluRay");
                }
            }

            library.addMovie(movie);
            
            // Stop after first file part if full BluRay Disk
            if (isBluRay && playFullBluRayDisk) {
                break;
            }
        }
    }

    /**
     * Return the file length, or if file is the directory, the sum of all files in directory.
     * Created for issue 1241
     * @param file
     * @return
     */
    private long calculateFileSize(File file) {
        long total = 0;
        if(file.isDirectory()){
            File[] listFiles = file.listFiles();
            // Check for empty directories
            if (listFiles != null && listFiles.length > 0) {
                for (File fileTmp : listFiles) {
                    total+=calculateFileSize(fileTmp);
                }
            }
        }
        else{
            total += file.length();
        }
        return total;
    }
}
