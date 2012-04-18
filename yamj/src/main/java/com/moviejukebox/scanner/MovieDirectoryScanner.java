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

import com.moviejukebox.model.*;
import com.moviejukebox.scanner.BDRipScanner.BDFilePropertiesMovie;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

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
    private String thumbnailsFormat;
    private String postersFormat;
    private String posterToken;
    private String thumbnailToken;
    private String bannersFormat;
    private String bannerToken;
    private String opensubtitles;
    private int hashpathdepth;
    private Boolean excludeFilesWithoutExternalSubtitles;
    private Boolean excludeMultiPartBluRay;
    private Boolean playFullBluRayDisk;
    private static final Logger logger = Logger.getLogger(MovieDirectoryScanner.class);
    private static int dirCount = 1;
    private static int fileCount = 0;
    private static Pattern patternRarPart = Pattern.compile("\\.part(\\d+)\\.rar");

    // BD rip infos Scanner
    private BDRipScanner localBDRipScanner;

    // Archived virtual directories (f.ex. rar, zip, tar.gz etc.)
    private IArchiveScanner archiveScanners[];

    public MovieDirectoryScanner() {
        supportedExtensions.addAll(Arrays.asList(PropertiesUtil.getProperty("mjb.extensions", "AVI DIVX MKV WMV M2TS TS RM QT ISO VOB MPG MOV").toUpperCase().split(" ")));
        thumbnailsFormat = PropertiesUtil.getProperty("thumbnails.format", "png");
        postersFormat = PropertiesUtil.getProperty("posters.format", "png");
        bannersFormat = PropertiesUtil.getProperty("banners.format", "png");
        thumbnailToken = PropertiesUtil.getProperty("mjb.scanner.thumbnailToken", "_small");
        posterToken = PropertiesUtil.getProperty("mjb.scanner.posterToken", "_large");
        bannerToken = PropertiesUtil.getProperty("mjb.scanner.bannerToken", ".banner");
        excludeFilesWithoutExternalSubtitles = PropertiesUtil.getBooleanProperty("mjb.subtitles.ExcludeFilesWithoutExternal", Boolean.FALSE.toString());
        excludeMultiPartBluRay = PropertiesUtil.getBooleanProperty("mjb.excludeMultiPartBluRay", Boolean.FALSE.toString());
        opensubtitles = PropertiesUtil.getProperty("opensubtitles.language", ""); // We want to check this isn't set for the exclusion
        hashpathdepth = PropertiesUtil.getIntProperty("mjb.scanner.hashpathdepth", "0");
        playFullBluRayDisk = PropertiesUtil.getBooleanProperty("mjb.playFullBluRayDisk", Boolean.FALSE.toString());
        localBDRipScanner = new BDRipScanner();
    }

    /**
     * Scan the specified directory for video files.
     * @param srcPath
     * @param library
     * @return a new library
     */
    public Library scan(MediaLibraryPath srcPath, Library library) {

        File directory = new FileTools.FileEx(srcPath.getPath(), archiveScanners);

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

    /**
     * Recursively scan the directory for video files
     * @param srcPath
     * @param directory
     * @param collection
     */
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
                        logger.debug("Scanning of directory " + directory.getAbsolutePath() + " skipped due to override file");
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
            int index = filename.lastIndexOf('.');
            if (index < 0) {
                return true;
            }

            String extension = new String(file.getName().substring(index + 1)).toUpperCase();
            if (!supportedExtensions.contains(extension)) {
                return true;
            }

            // Exclude files without external subtitles
            if (opensubtitles.equals("")) { // We are not downloading subtitles, so exclude those that don't have any.
                if (excludeFilesWithoutExternalSubtitles && !hasSubtitles(file)) {
                    logger.info("File " + filename + " excluded. (no external subtitles)");
                    return true;
                }
            }
        }

        // Compute the relative filename
        String relativeFilename = new String(file.getAbsolutePath().substring(mediaLibraryRootPathIndex));

        String relativeFileNameLower = relativeFilename.toLowerCase();
        String jukeboxName = PropertiesUtil.getProperty("mjb.detailsDirName", "Jukebox");

        for (String excluded : srcPath.getExcludes()) {
            if (excluded.length() > 0) {
                try {
                    Pattern excludePatt = Pattern.compile(excluded, Pattern.CASE_INSENSITIVE);
                    if (excludePatt.matcher(relativeFileNameLower).find()) {
                        logger.debug((isDirectory ? "Directory '" : "File '") + relativeFilename + "' excluded.");
                        return true;
                    }
                } catch (Exception error) {
                    logger.info("MovieDirectoryScanner: Error processing exclusion pattern: " + excluded);
                }

                excluded = excluded.replace("/", File.separator);
                excluded = excluded.replace("\\", File.separator);
                if (relativeFileNameLower.indexOf(excluded.toLowerCase()) >= 0) {
                    // Don't print a message for the exclusion of Jukebox files
                    if (!relativeFileNameLower.contains(jukeboxName)) {
                        logger.debug((isDirectory ? "Directory '" : "File '") + relativeFilename + "' excluded.");
                    }
                    return true;
                }
            }
        }

        // Handle special case of RARs. If ".rar", and it is ".partXXX.rar"
        // exclude all NON-"part001.rar" files.

        Matcher m = patternRarPart.matcher(relativeFileNameLower);

        if(m.find() && (m.groupCount() == 1)) {
            if (Integer.parseInt(m.group(1)) != 1) {
                logger.debug("MovieDirectoryScanner: Excluding file " + relativeFilename + " as it is a non-first part RAR archive ("+m.group(1)+")");
                return true;
            }
        }

        return false;
    }

    /**
     * Check to see if the file has subtitles or not
     * @param fileToScan
     * @return
     */
    protected static boolean hasSubtitles(File fileToScan) {

        File found = FileTools.findSubtitles(fileToScan);
        return found.exists();
    }

    /**
     * Scan the file for the information to be added to the object
     * @param srcPath
     * @param file
     * @param library
     */
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
                    logger.info("File " + file.getName() + " excluded. (multi part BluRay)");
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
                baseFileName = new String(baseFileName.substring(0, file.getName().lastIndexOf(".")));
                movie.setFormatType(Movie.TYPE_FILE);
            }

            // Compute the relative filename
            String relativeFilename = new String(contentFiles[i].getAbsolutePath().substring(mediaLibraryRootPathIndex));

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
                    tempFilename = new String(tempFilename.substring(0, tempFilename.toUpperCase().lastIndexOf("BDMV")));
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
                hashstr = new String(relativeFilename.substring(pos+1));
                hashstr = Integer.toHexString(hashstr.hashCode());
            }
            movie.setBaseName(FileTools.makeSafeFilename(baseFileName) + hashstr);

            movie.setLibraryPath(srcPath.getPath());
            movie.setPosterFilename(movie.getBaseName() + ".jpg");
            movie.setThumbnailFilename(movie.getBaseName() + thumbnailToken + "." + thumbnailsFormat);
            movie.setDetailPosterFilename(movie.getBaseName() + posterToken + "." + postersFormat);
            movie.setBannerFilename(movie.getBaseName() + bannerToken + "." + bannersFormat);
            movie.setSubtitles(hasSubtitles(movie.getFile())==true?"YES":"NO");
            movie.setLibraryDescription(srcPath.getDescription());
            movie.setPrebuf(srcPath.getPrebuf());

            movie.addFileDate(new Date(file.lastModified()));

            // Issue 1241 - Take care of directory for size.
            movie.setFileSize(this.calculateFileSize(file));

            MovieFileNameDTO dto = MovieFilenameScanner.scan(file);

            movie.mergeFileNameDTO(dto);

            if (bdDuration == 0 || dto.getPart() > 0) {
                // Do not merge file information for Blu-Ray unless it's a multi-part disk
                movieFile.mergeFileNameDTO(dto);
            } else {
                if (playFullBluRayDisk && movie.isTVShow()) {
                    // This is needed for multi-part disks and TV shows
                    movieFile.mergeFileNameDTO(dto);
                }
                // Set duration for BD disks using the data in the playlist + mark Bluray source and container
                // Issue 1176 - Prevent lost of NFO Data
                if (movie.getRuntime().equals(Movie.UNKNOWN)) {
                    movie.setRuntime(StringTools.formatDuration(bdDuration));
                }

                // Pretend that HDDVD is also BluRay
                if (!movie.getVideoSource().equalsIgnoreCase("HDDVD")) {
                    movie.setContainer("BluRay");
                    movie.setVideoSource("BluRay");
                    movie.setFormatType(Movie.TYPE_BLURAY);
                }
            }

            // Issue 1679: Determine the file date of the movie by the VIDEO_TS or BDMV folders
            if (movie.isDVD()) {
                movie.setFileDate(new Date((new File(file, "/VIDEO_TS")).lastModified()));
            } else if (movie.isBluray()) {
                movie.setFileDate(new Date((new File(file, "/BDMV")).lastModified()));
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
