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
package com.moviejukebox.tools;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.scanner.IArchiveScanner;
import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import org.apache.log4j.Logger;

public final class FileTools {

    private static final Logger LOG = Logger.getLogger(FileTools.class);
    private static final String LOG_MESSAGE = "FileTools: ";
    private static final int BUFF_SIZE = 16 * 1024;
    private static final Collection<String> SUBTITLE_EXTENSIONS = new ArrayList<String>();
    private static final Collection<ReplaceEntry> UNSAFE_CHARS = new ArrayList<ReplaceEntry>();
    private static final Collection<String> GENERATED_FILENAMES = Collections.synchronizedCollection(new ArrayList<String>());
    private static boolean videoimageDownload = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", Boolean.FALSE);
    private static int footerImageEnabled = PropertiesUtil.getIntProperty("mjb.footer.count", 0);
    private static String indexFilesPrefix = getProperty("mjb.indexFilesPrefix", "");
    // Literals
    private static final String TO = "' to '";
    private static final String BDMV_STREAM = File.separator + "BDMV" + File.separator + "STREAM";
    // File Cache
    public static ScannedFilesCache fileCache = new ScannedFilesCache();
    // Lock for mkdirs
    private static Lock fsLock = new ReentrantLock();
    private static final String DEFAULT_CHARSET = "UTF-8";

    public static void initSubtitleExtensions() {
        if (SUBTITLE_EXTENSIONS.isEmpty()) {
            SUBTITLE_EXTENSIONS.addAll(Arrays.asList(PropertiesUtil.getProperty("filename.scanner.subtitle", "SRT,SUB,SSA,SMI,PGS").split(",")));
        }
    }
    /**
     * Gabriel Corneanu: One buffer for each thread to allow threaded copies
     */
    private static final ThreadLocal<byte[]> THREAD_BUFFER = new ThreadLocal<byte[]>() {
        @Override
        protected byte[] initialValue() {
            return new byte[BUFF_SIZE];
        }
    ;

    };

    private static class ReplaceEntry {

        private String oldText, newText;
        private int oldLength;

        public ReplaceEntry(String oldtext, String newtext) {
            this.oldText = oldtext;
            this.newText = newtext;
            oldLength = oldtext.length();
        }

        public String check(String filename) {
            String newFilename = filename;
            int pos = newFilename.indexOf(oldText, 0);
            while (pos >= 0) {
                newFilename = newFilename.substring(0, pos) + newText + newFilename.substring(pos + oldLength);
                pos = newFilename.indexOf(oldText, pos + oldLength);
            }
            return newFilename;
        }
    };

    public static void initUnsafeChars() {
        if (!UNSAFE_CHARS.isEmpty()) {
            return;
        }

        // What to do if the user specifies a blank encodeEscapeChar? I guess disable encoding.
        String encodeEscapeCharString = PropertiesUtil.getProperty("mjb.charset.filenameEncodingEscapeChar", "$");
        if (encodeEscapeCharString.length() > 0) {
            // What to do if the user specifies a >1 character long string? I guess just use the first char.
            final Character ENCODE_ESCAPE_CHAR = encodeEscapeCharString.charAt(0);

            String repChars = PropertiesUtil.getProperty("mjb.charset.unsafeFilenameChars", "<>:\"/\\|?*");
            for (String repChar : repChars.split("")) {
                if (repChar.length() > 0) {
                    char ch = repChar.charAt(0);
                    // Don't encode characters that are hex digits
                    // Also, don't encode the escape char -- it is safe by definition!
                    if (!Character.isDigit(ch) && -1 == "AaBbCcDdEeFf".indexOf(ch) && !ENCODE_ESCAPE_CHAR.equals(ch)) {
                        String hex = Integer.toHexString(ch).toUpperCase();
                        UNSAFE_CHARS.add(new ReplaceEntry(repChar, ENCODE_ESCAPE_CHAR + hex));
                    }
                }
            }
        }

        // Parse transliteration map: (source_character [-] transliteration_sequence [,])+
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("mjb.charset.filename.translate", ""), ",");
        while (st.hasMoreElements()) {
            final String token = st.nextToken();
            String beforeStr = substringBefore(token, "-");
            final String character = beforeStr.length() == 1 && (beforeStr.equals("\t") || beforeStr.equals(" ")) ? beforeStr : trimToNull(beforeStr);
            if (character == null) {
                // TODO Error message?
                continue;
            }
            String afterStr = substringAfter(token, "-");
            final String translation = afterStr.length() == 1 && (afterStr.equals("\t") || afterStr.equals(" ")) ? afterStr : trimToNull(afterStr);
            if (translation == null) {
                // TODO Error message?
                // TODO Allow empty transliteration?
                continue;
            }
            UNSAFE_CHARS.add(new ReplaceEntry(character.toUpperCase(), translation.toUpperCase()));
            UNSAFE_CHARS.add(new ReplaceEntry(character.toLowerCase(), translation.toLowerCase()));
        }
    }

    private FileTools() {
        throw new UnsupportedOperationException("Class cannot be instantiated");
    }

    public static int copy(InputStream is, OutputStream os) throws IOException {
        int bytesCopied = 0;
        byte[] buffer = THREAD_BUFFER.get();
        try {
            while (Boolean.TRUE) {
                int amountRead = is.read(buffer);
                if (amountRead == -1) {
                    break;
                } else {
                    bytesCopied += amountRead;
                }
                os.write(buffer, 0, amountRead);
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException error) {
                // ignore
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException error) {
                // ignore
            }
        }
        return bytesCopied;
    }

    /**
     * Copy the source file to the destination
     *
     * @param src
     * @param dst
     * @return
     */
    public static boolean copyFile(String src, String dst) {
        File srcFile = new File(src);
        File dstFile = new File(dst);
        return copyFile(srcFile, dstFile);
    }

    /**
     * Copy the source file to the destination
     *
     * @param src
     * @param dst
     * @return
     */
    public static boolean copyFile(File src, File dst) {
        boolean returnValue = Boolean.FALSE;

        if (!src.exists()) {
            LOG.error(LOG_MESSAGE + "The file '" + src + "' does not exist!");
            return returnValue;
        }

        if (dst.isDirectory()) {
            makeDirectories(dst);
            returnValue = copyFile(src, new File(dst + File.separator + src.getName()));
        } else {
            FileInputStream inSource = null;
            FileOutputStream outSource = null;
            FileChannel inChannel = null;
            FileChannel outChannel = null;
            try {
                // gc: copy using file channels, potentially much faster
                inSource = new FileInputStream(src);
                outSource = new FileOutputStream(dst);
                inChannel = inSource.getChannel();
                outChannel = outSource.getChannel();

                long p = 0, s = inChannel.size();
                while (p < s) {
                    p += inChannel.transferTo(p, 1024 * 1024, outChannel);
                }
                return Boolean.TRUE;
            } catch (IOException error) {
                LOG.error(LOG_MESSAGE + "Failed copying file '" + src + TO + dst + "'");
                LOG.error(SystemTools.getStackTrace(error));
                returnValue = Boolean.FALSE;
            } finally {
                if (inChannel != null) {
                    try {
                        inChannel.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
                if (inSource != null) {
                    try {
                        inSource.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }

                if (outChannel != null) {
                    try {
                        outChannel.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
                if (outSource != null) {
                    try {
                        outSource.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
            }
        }

        return returnValue;
    }

    /**
     * Copy files from one directory to another
     *
     * @param srcPathName The source directory to copy from
     * @param dstPathName The target directory to copy to
     * @param updateDisplay Display an update to the console
     */
    public static void copyDir(String srcPathName, String dstPathName, boolean updateDisplay) {
        copyDir(srcPathName, dstPathName, updateDisplay, null);
    }

    /**
     * Copy files from one directory to another
     *
     * @param srcPathName The source directory to copy from
     * @param dstPathName The target directory to copy to
     * @param updateDisplay Display an update to the console
     * @param pathRoot The root of the srcPath for display purposes
     */
    public static void copyDir(String srcPathName, String dstPathName, boolean updateDisplay, String pathRoot) {
        String displayRoot;
        if (StringTools.isNotValidString(pathRoot)) {
            int pos = srcPathName.lastIndexOf(File.separator);
            if (pos > 0) {
                try {
                    displayRoot = srcPathName.substring(pos + 1);
                } catch (Exception error) {
                    displayRoot = srcPathName;
                }
            } else {
                displayRoot = srcPathName;
            }
        } else {
            displayRoot = pathRoot;
        }

        try {
            File srcDir = new File(srcPathName);
            if (!srcDir.exists()) {
                LOG.error(LOG_MESSAGE + "Source directory " + srcPathName + " does not exist!");
                return;
            }

            File dstDir = new File(dstPathName);
            makeDirectories(dstDir);

            if (!dstDir.exists()) {
                LOG.error(LOG_MESSAGE + "Target directory " + dstPathName + " does not exist!");
                return;
            }

            if (srcDir.isFile()) {
                copyFile(srcDir, dstDir);
            } else {

                File[] contentList = srcDir.listFiles();
                if (contentList != null) {
                    List<File> files = Arrays.asList(contentList);
                    Collections.sort(files);

                    int totalSize = files.size();
                    int currentFile = 0;

                    String displayPath = srcDir.getCanonicalPath();

                    int pos = displayPath.lastIndexOf(displayRoot);
                    if (pos > 0) {
                        displayPath = displayPath.substring(pos);
                    }

                    for (File file : files) {
                        currentFile++;
                        if (!file.getName().equals(".svn")) {
                            if (file.isDirectory()) {
                                copyDir(file.getAbsolutePath(), dstPathName + File.separator + file.getName(), updateDisplay, pathRoot);
                            } else {
                                if (updateDisplay) {
                                    System.out.print("\r    Copying directory " + displayPath + " (" + currentFile + "/" + totalSize + ")");
                                    if (LOG.isTraceEnabled()) {
                                        LOG.trace(LOG_MESSAGE + "Copying: " + file.getName());
                                    }
                                }
                                copyFile(file, dstDir);
                            }
                        }
                    }

                    if (updateDisplay && files.size() > 0) {
                        System.out.print("\n");
                    }

                    LOG.debug(LOG_MESSAGE + "Copied " + totalSize + " files from " + srcDir.getCanonicalPath());
                }
            }
        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed to copy '" + srcPathName + TO + dstPathName + "'");
            LOG.error(SystemTools.getStackTrace(error));
        }
    }

    /**
     * Read a file and return it as a string using default encoding
     *
     * @param file
     * @return
     */
    public static String readFileToString(File file) {
        return readFileToString(file, DEFAULT_CHARSET);
    }

    /**
     * Read a file and return it as a string
     *
     * @param file
     * @param encoding
     * @return
     */
    public static String readFileToString(File file, String encoding) {
        String data = "";
        if (file == null) {
            LOG.error(LOG_MESSAGE + "Failed reading file, file is null");
        } else {
            try {
                data = FileUtils.readFileToString(file, encoding);
            } catch (IOException ex) {
                LOG.error(LOG_MESSAGE + "Failed reading file " + file.getName() + " - Error: " + ex.getMessage());
            }
        }
        return data;
    }

    /**
     * Write string to a file (Used for debugging)
     *
     * @param filename
     * @param outputString
     */
    public static void writeStringToFile(String filename, String outputString) {
        File outFile = new File(filename);
        try {
            LOG.debug(LOG_MESSAGE + "Writing string to '" + outFile.getAbsolutePath() + "'");
            FileUtils.writeStringToFile(outFile, outputString);
        } catch (IOException ex) {
            LOG.warn(LOG_MESSAGE + "Failed to write to file '" + outFile.getAbsolutePath() + "': " + ex.getMessage(), ex);
        }
    }

    /**
     * *
     * @author Stuart Boston
     * @param file1 - first file to compare
     * @param file2 - second file to compare
     * @return true if the files exist and file2 is older, false otherwise.
     *
     * Note that file1 will be checked to see if it's newer than file2
     */
    public static boolean isNewer(File file1, File file2) {
        // TODO: Update this routine to use fileCache
        // If file1 exists and file2 doesn't then return true
        if (file1.exists()) {
            // If file2 doesn't exist then file1 is newer
            if (!file2.exists()) {
                return Boolean.TRUE;
            }
        } else {
            // File1 doesn't exist so return false
            return Boolean.FALSE;
        }

        // Compare the file dates. This is only true if the first file is newer than the second, as the second file is the file2 file
        if (file1.lastModified() <= file2.lastModified()) {
            // file1 is older than the file2.
            return Boolean.FALSE;
        } else {
            // file1 is newer than file2
            return Boolean.TRUE;
        }
    }

    public static String createCategoryKey(String key2) {
        return key2;
    }

    public static String createPrefix(String category, String key) {
        StringBuilder prefix = new StringBuilder(indexFilesPrefix);
        prefix.append(category).append('_').append(key).append('_');
        return prefix.toString();
    }

    public static OutputStream createFileOutputStream(File f, int size) throws FileNotFoundException {
        // return new FileOutputStream(f);
        return new BufferedOutputStream(new FileOutputStream(f), size);
    }

    public static OutputStream createFileOutputStream(File f) throws FileNotFoundException {
        return createFileOutputStream(f, 10 * 1024);
    }

    public static OutputStream createFileOutputStream(String f) throws FileNotFoundException {
        return createFileOutputStream(new File(f));
    }

    public static OutputStream createFileOutputStream(String f, int size) throws FileNotFoundException {
        return createFileOutputStream(new File(f), size);
    }

    public static InputStream createFileInputStream(File f) throws FileNotFoundException {
        // return new FileInputStream(f);
        return new BufferedInputStream(new FileInputStream(f), 10 * 1024);
    }

    public static InputStream createFileInputStream(String f) throws FileNotFoundException {
        return createFileInputStream(new File(f));
    }

    public static String makeSafeFilename(String filename) {
        String newFilename = filename;

        for (ReplaceEntry rep : UNSAFE_CHARS) {
            newFilename = rep.check(newFilename);
        }

        if (!newFilename.equals(filename)) {
            LOG.debug(LOG_MESSAGE + "Encoded filename string '" + filename + TO + newFilename + "'");
        }

        return newFilename;
    }

    /**
     * Returns the given path in canonical form
     *
     * i.e. no duplicated separators, no ".", ".."..., and ending without trailing separator the only exception is a root! the
     * canonical form for a root INCLUDES the separator
     *
     * @param path
     * @return
     */
    public static String getCanonicalPath(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            return path;
        }
    }

    /**
     * when concatenating paths and the source MIGHT be a root, use this function to safely add the separator
     *
     * @param path
     * @return
     */
    public static String getDirPathWithSeparator(String path) {
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    /**
     * Get the extension of a file.
     *
     * @param filename
     * @return
     */
    public static String getFileExtension(String filename) {
        return FilenameUtils.getExtension(filename);
    }

    /**
     * Returns the parent folder name only; used when searching for artwork...
     *
     * @param file
     * @return
     */
    public static String getParentFolderName(File file) {
        if (file == null) {
            return "";
        }
        String path = file.getParent();
        return path.substring(path.lastIndexOf(File.separator) + 1);
    }

    /**
     *
     * Pass in the filename and a list of extensions.
     *
     * This function will scan for the filename plus extensions and return the File
     *
     * @param fullBaseFilename
     * @param fileExtensions
     * @return always a File, to be tested with exists() for valid file
     */
    public static File findFileFromExtensions(String fullBaseFilename, Collection<String> fileExtensions) {
        File localFile = null;

        for (String extension : fileExtensions) {
            localFile = fileCache.getFile(fullBaseFilename + "." + extension);
            if (localFile.exists()) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace(LOG_MESSAGE + "Found " + localFile + " in the file cache");
                }
                return localFile;
            }
        }

        return (localFile == null ? new File(Movie.UNKNOWN) : localFile); //just in case
    }

    /**
     * Search for the filename in the cache and look for each with the extensions
     *
     * @param searchFilename
     * @param fileExtensions
     * @param jukebox
     * @param logPrefix
     * @return
     */
    public static File findFilenameInCache(String searchFilename, Collection<String> fileExtensions, Jukebox jukebox, String logPrefix) {
        return findFilenameInCache(searchFilename, fileExtensions, jukebox, logPrefix, Boolean.FALSE, Movie.UNKNOWN);
    }

    /**
     * Search for the filename in the cache and look for each with the extensions
     *
     * @param searchFilename
     * @param fileExtensions
     * @param jukebox
     * @param logPrefix
     * @param includeJukebox
     * @return
     */
    public static File findFilenameInCache(String searchFilename, Collection<String> fileExtensions, Jukebox jukebox, String logPrefix, boolean includeJukebox) {
        return findFilenameInCache(searchFilename, fileExtensions, jukebox, logPrefix, includeJukebox, Movie.UNKNOWN);
    }

    /**
     * Search for the filename in the cache and look for each with the extensions
     *
     * @param searchFilename
     * @param fileExtensions
     * @param jukebox
     * @param logPrefix
     * @param includeJukebox
     * @param subFolder
     * @return
     */
    public static File findFilenameInCache(String searchFilename, Collection<String> fileExtensions, Jukebox jukebox, String logPrefix, boolean includeJukebox, String subFolder) {
        File searchFile = null;
        String safeFilename = makeSafeFilename(searchFilename);

        safeFilename = File.separator + safeFilename;

        StringBuilder jukeboxSubFolder = new StringBuilder(jukebox.getJukeboxRootLocationDetails().toLowerCase());
        if (StringTools.isValidString(subFolder)) {
            jukeboxSubFolder.append(File.separator).append((subFolder.toLowerCase()));
        }

        Collection<File> files = FileTools.fileCache.searchFilename(safeFilename, Boolean.TRUE);

        if (files.size() > 0) {
            // Copy the synchronized list to avoid ConcurrentModificationException
            Iterator<File> iter = new ArrayList<File>(FileTools.fileCache.searchFilename(safeFilename, Boolean.TRUE)).iterator();

            while (iter.hasNext() && (searchFile == null)) {
                File file = iter.next();
                String abPath = file.getAbsolutePath().toLowerCase();

                if (!includeJukebox && abPath.startsWith(jukeboxSubFolder.toString())) {
                    // Skip any files found in the jukebox
                    continue;
                }

                // Loop round the filename+extension to see if any exist and add them to the array
                String checkFilename;
                for (String extension : fileExtensions) {
                    checkFilename = safeFilename + "." + extension;
                    if (abPath.endsWith((checkFilename).toLowerCase())) {
                        searchFile = file;
                        // We've found the file, so we should quit the loop
                        break;
                    }
                }
            }

            if (searchFile != null) {
                LOG.debug(logPrefix + "Using first one found: " + searchFile.getAbsolutePath());
            } else {
                LOG.debug(logPrefix + "No matching files found for " + safeFilename);
            }
        } else {
            LOG.debug(logPrefix + "No scanned files found for " + searchFilename);
        }

        return searchFile;
    }

    /**
     * Download the image for the specified URL into the specified file. Utilises the WebBrowser downloadImage function to allow for
     * proxy connections.
     *
     * @param imageFile
     * @param imageURL
     * @return
     * @throws IOException
     */
    public static boolean downloadImage(File imageFile, String imageURL) throws IOException {
        WebBrowser webBrowser = new WebBrowser();
        return webBrowser.downloadImage(imageFile, imageURL);
    }

    /**
     * Find the parent directory of the movie file.
     *
     * @param movieFile
     * @return Parent folder
     * @author Stuart Boston
     */
    public static String getParentFolder(File movieFile) {
        String parentFolder;

        if (movieFile.isDirectory()) { // for VIDEO_TS
            parentFolder = movieFile.getPath();
        } else {
            parentFolder = movieFile.getParent();
        }

        // Issue 1070, /BDMV/STREAM is being appended to the parent path
        if (parentFolder.toUpperCase().endsWith(BDMV_STREAM)) {
            parentFolder = parentFolder.substring(0, parentFolder.length() - 12);
        }

        return parentFolder;
    }

    /**
     * Recursively delete a directory
     *
     * @param dir
     * @return
     */
    public static boolean deleteDir(String dir) {
        return deleteDir(new File(dir));
    }

    /**
     * Recursively delete a directory
     *
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String children1 : children) {
                boolean success = deleteDir(new File(dir, children1));
                if (!success) {
                    // System.out.println("Failed");
                    return Boolean.FALSE;
                }
            }
        }
        // The directory is now empty so delete it
        // System.out.println("Deleting: " + dir.getAbsolutePath());
        return dir.delete();
    }

    /**
     * Add a list of files to the jukebox filenames
     *
     * @param filenames
     */
    public static void addJukeboxFiles(Collection<String> filenames) {
        GENERATED_FILENAMES.addAll(filenames);
    }

    /**
     * Add an individual filename to the jukebox cleaning exclusion list
     *
     * @param filename
     */
    public static void addJukeboxFile(String filename) {
        if (StringTools.isValidString(filename)) {
            GENERATED_FILENAMES.add(filename);

            if (LOG.isTraceEnabled()) {
                LOG.trace(LOG_MESSAGE + "Adding " + filename + " to safe jukebox files");
            }
        }
    }

    /**
     * Process the movie and add all the files to the jukebox cleaning exclusion list
     *
     * @param movie
     */
    public static void addMovieToJukeboxFilenames(Movie movie) {
        addJukeboxFile(movie.getPosterFilename());
        addJukeboxFile(movie.getDetailPosterFilename());
        addJukeboxFile(movie.getThumbnailFilename());
        addJukeboxFile(movie.getBannerFilename());
        addJukeboxFile(movie.getFanartFilename());

        if (videoimageDownload) {
            for (MovieFile mf : movie.getFiles()) {
                for (int part = mf.getFirstPart(); part <= mf.getLastPart(); part++) {
                    addJukeboxFile(mf.getVideoImageFilename(part));
                }
            }
        }

        addJukeboxFile(movie.getClearArtFilename());
        addJukeboxFile(movie.getClearLogoFilename());
        addJukeboxFile(movie.getSeasonThumbFilename());
        addJukeboxFile(movie.getTvThumbFilename());
        addJukeboxFile(movie.getMovieDiscFilename());

        // Are footer images enabled?
        if (footerImageEnabled > 0) {
            addJukeboxFiles(movie.getFooterFilename());
        }
    }

    public static Collection<String> getJukeboxFiles() {
        return GENERATED_FILENAMES;
    }

    /**
     * Special File with "cached" attributes used to minimize file system access which slows down everything
     *
     * @author Gabriel Corneanu
     */
    public static class FileEx extends File {

        private static final long serialVersionUID = 1L;
        private volatile Boolean isDir = null;
        private volatile Boolean fileExists = null;
        private volatile Boolean isfile = null;
        private volatile Long fileLen = null;
        private volatile Long fileLastModified = null;
        private IArchiveScanner[] archiveScanners;
        private volatile File[] listFiles;

        //Standard constructors
        public FileEx(String parent, String child) {
            super(parent, child);
        }

        public FileEx(String pathname) {
            super(pathname);
        }

        public FileEx(File parent, String child) {
            super(parent, child);
        }

        private FileEx(String pathname, boolean exists) {
            this(pathname);
            fileExists = exists;
        }

        // archive scanner supporting constructors
        public FileEx(String pathname, IArchiveScanner[] archiveScanners) {
            super(pathname);
            if (archiveScanners == null) {
                this.archiveScanners = null;
            } else {
                this.archiveScanners = (IArchiveScanner[]) archiveScanners.clone();
            }
        }

        public FileEx(File parent, String child, IArchiveScanner[] archiveScanners) {
            this(parent, child);
            if (archiveScanners == null) {
                this.archiveScanners = null;
            } else {
                this.archiveScanners = (IArchiveScanner[]) archiveScanners.clone();
            }
        }

        @Override
        public boolean isDirectory() {
            if (isDir == null) {
                synchronized (this) {
                    if (isDir == null) {
                        isDir = super.isDirectory();
                    }
                }
            }
            return isDir;
        }

        @Override
        public boolean exists() {
            if (fileExists == null) {
                synchronized (this) {
                    if (fileExists == null) {
                        fileExists = super.exists();
                    }
                }
            }
            return fileExists;
        }

        @Override
        public boolean isFile() {
            if (isfile == null) {
                synchronized (this) {
                    if (isfile == null) {
                        isfile = super.isFile();
                    }
                }
            }
            return isfile;
        }

        @Override
        public long length() {
            if (fileLen == null) {
                synchronized (this) {
                    if (fileLen == null) {
                        fileLen = super.length();
                    }
                }
            }
            return fileLen;
        }

        @Override
        public long lastModified() {
            if (fileLastModified == null) {
                synchronized (this) {
                    if (fileLastModified == null) {
                        fileLastModified = super.lastModified();
                    }
                }
            }
            return fileLastModified;
        }

        @Override
        public File getParentFile() {
            String p = this.getParent();
            if (p == null) {
                return null;
            }
            return new FileEx(p, archiveScanners);
        }

        @Override
        public File[] listFiles() {
            synchronized (this) {
                if (listFiles != null) {
                    return listFiles;
                }

                String[] nameStrings = list();
                if (nameStrings == null) {
                    return null;
                }

                List<String> mutableNames = new ArrayList<String>(Arrays.asList(nameStrings));
                List<File> files = new ArrayList<File>();
                if (archiveScanners != null) {
                    for (IArchiveScanner as : archiveScanners) {
                        files.addAll(as.getArchiveFiles(this, mutableNames));
                    }
                }

                for (String name : mutableNames) {
                    FileEx fe = new FileEx(this, name, archiveScanners);
                    fe.fileExists = Boolean.TRUE;
                    files.add(fe);
                }

                listFiles = (File[]) files.toArray(new File[files.size()]);
            }
            return listFiles;
        }

        @Override
        public File[] listFiles(FilenameFilter filter) {
            File[] src = listFiles();

            if (src == null) {
                return null;
            }

            if (filter == null) {
                return Arrays.copyOf(src, src.length);
            }

            List<File> l = new ArrayList<File>();
            for (File f : src) {
                if (filter.accept(this, f.getName())) {
                    l.add(f);
                }
            }
            return (File[]) l.toArray(new File[l.size()]);
        }
    }

    /**
     * cached File instances the key is always absolute path in upper-case, so it will NOT work for case only differences
     *
     * @author Gabriel Corneanu
     */
    public static class ScannedFilesCache {
        //cache for ALL files found during initial scan

        private final Map<String, File> cachedFiles = new ConcurrentHashMap<String, File>(1000);

        /**
         * Check whether the file exists
         *
         * @param absPath
         * @return
         */
        public boolean fileExists(String absPath) {
            return cachedFiles.containsKey(absPath.toUpperCase());
        }

        /**
         * Check whether the file exists
         *
         * @param file
         * @return
         */
        public boolean fileExists(File file) {
            return cachedFiles.containsKey(file.getAbsolutePath().toUpperCase());
        }

        /**
         * Add a file instance to cache
         *
         * @param file
         */
        public void fileAdd(File file) {
            cachedFiles.put(file.getAbsolutePath().toUpperCase(), file);
        }

        /**
         * Retrieve a file from cache
         *
         * If it is NOT found, construct one instance and mark it as non-existing.
         *
         * The exist() test is used very often throughout the library to search for specific files.
         *
         * The path MUST be canonical (i.e. carefully constructed)
         *
         * We do NOT want here to make it canonical because it goes to the file system and it's slow.
         *
         * @param path
         * @return
         */
        public File getFile(String path) {
            File f = cachedFiles.get(path.toUpperCase());
            return (f == null ? new FileEx(path, Boolean.FALSE) : f);
        }

        /**
         * Add a full directory listing; used for existing jukebox
         *
         * @param dir
         * @param depth
         */
        public void addDir(File dir, int depth) {
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }

            if (files.length == 0) {
                return;
            }

            addFiles(files);
            if (depth <= 0) {
                return;
            }

            for (File f : files) {
                if (f.isDirectory()) {
                    addDir(f, depth - 1);
                }
            }
        }

        public void addFiles(File[] files) {
            if (files.length == 0) {
                return;
            }
            Map<String, File> map = new HashMap<String, File>(files.length);
            for (File f : files) {
                map.put(f.getAbsolutePath().toUpperCase(), f);
            }
            cachedFiles.putAll(map);
        }

        public long size() {
            return cachedFiles.size();
        }

        public Collection<File> searchFilename(String searchName, boolean findAll) {
            ArrayList<File> files = new ArrayList<File>();

            String upperName = searchName.toUpperCase();

            for (String listName : cachedFiles.keySet()) {
                if (listName.contains(upperName)) {
                    files.add(cachedFiles.get(listName));
                    if (!findAll) {
                        // We only look for the first
                        break;
                    }
                }
            }

            return files;
        }

        public void saveFileList(String filename) throws FileNotFoundException, UnsupportedEncodingException {
            PrintWriter p = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename, Boolean.TRUE), DEFAULT_CHARSET));

            Set<String> names = cachedFiles.keySet();
            String[] sortednames = names.toArray(new String[names.size()]);
            Arrays.sort(sortednames);
            for (String f : sortednames) {
                p.println(f);
            }
            p.close();
        }
    }

    /**
     * Look for any subtitle files for a file
     *
     * @param fileToScan
     * @return
     */
    public static File findSubtitles(File fileToScan) {
        String basename = FilenameUtils.removeExtension(fileToScan.getAbsolutePath().toUpperCase());
        return findFileFromExtensions(basename, SUBTITLE_EXTENSIONS);
    }

    /**
     * Create a directory hash from the filename
     *
     * @param filename
     * @return
     */
    public static String createDirHash(final String filename) {
        // Skip if the filename is invalid OR has already been hashed
        if (StringTools.isNotValidString(filename) || filename.contains(File.separator)) {
            return filename;
        }

        // Remove all the non-word characters from the filename, replacing with an underscore
        String cleanFilename = filename.replaceAll("[^\\p{L}\\p{N}]", "_").toLowerCase().trim();

        StringBuilder dirHash = new StringBuilder();
        dirHash.append(cleanFilename.substring(0, 1)).append(File.separator);
        dirHash.append(cleanFilename.substring(0, cleanFilename.length() > 1 ? 2 : 1)).append(File.separator);
        dirHash.append(filename);

        return dirHash.toString();
    }

    /**
     * Create a directory has from the filename of a file
     *
     * @param file
     * @return
     */
    public static String createDirHash(final File file) {
        return createDirHash(file.getName());
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param file Source directory or file to create the directories directories
     * @return
     */
    public static Boolean makeDirectories(File file) {
        return makeDirectories(file, 10);
    }

    /**
     * Create all directories up to the level of the file passed
     *
     * @param sourceDirectory Source directory or file to create the directories
     * @param numOfTries Number of attempts that will be made to create the directories
     * @return
     */
    public static Boolean makeDirectories(final File sourceDirectory, int numOfTries) {
        File targetDirectory;
        if (sourceDirectory.isDirectory()) {
            targetDirectory = sourceDirectory;
        } else {
            targetDirectory = sourceDirectory.getParentFile();
        }

        if (targetDirectory.exists()) {
            return Boolean.TRUE;
        }
        LOG.trace(LOG_MESSAGE + "Creating directories for " + targetDirectory.getAbsolutePath());

        fsLock.lock();
        try {
            boolean status = targetDirectory.mkdirs();
            int looper = 1;
            while (!status && looper++ <= numOfTries) {
                status = targetDirectory.mkdirs();
            }
            if (status && looper > 10) {
                LOG.error("Failed creating the directory (" + targetDirectory.getAbsolutePath() + "). Ensure this directory is read/write!");
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        } finally {
            fsLock.unlock();
        }
    }
}
