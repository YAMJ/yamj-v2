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
package com.moviejukebox.tools;

import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.apache.commons.lang.StringUtils.trimToNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
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
import java.util.logging.Logger;

import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;

// import com.moviejukebox.tools.HTMLTools;

@SuppressWarnings("unused")
public class FileTools {

    private static Logger logger = Logger.getLogger("moviejukebox");
    final static int BUFF_SIZE = 16*1024;

    /**
     * Gabriel Corneanu: One buffer for each thread to allow threaded copies
     */
    private final static ThreadLocal<byte[]> threadBuffer = new ThreadLocal<byte[]>() {
        protected byte[] initialValue() {
            return new byte[BUFF_SIZE];
        };
    };
    
    private static class ReplaceEntry {
        private String oldText, newText;
        private int oldLength;
        public ReplaceEntry(String oldtext, String newtext){
            this.oldText = oldtext;
            this.newText = newtext;
            oldLength = oldtext.length();
        }
        
        public String check(String filename) {
            String newFilename = filename;
            int pos = newFilename.indexOf(oldText, 0);
            while(pos >= 0) {
                newFilename = new String(newFilename.substring(0, pos)) + newText + new String(newFilename.substring(pos + oldLength));
                pos = newFilename.indexOf(oldText, pos + oldLength);
            }
            return newFilename;
        }
    };

    private static Collection<ReplaceEntry> unsafeChars = new ArrayList<ReplaceEntry>();
    static Character encodeEscapeChar = null;
    private final static Collection<String> generatedFileNames = Collections.synchronizedCollection(new ArrayList<String>());
    private static boolean videoimageDownload = PropertiesUtil.getBooleanProperty("mjb.includeVideoImages", "false");
    private static String indexFilesPrefix = getProperty("mjb.indexFilesPrefix", "");

    static {
        // What to do if the user specifies a blank encodeEscapeChar? I guess disable encoding.
        String encodeEscapeCharString = PropertiesUtil.getProperty("mjb.charset.filenameEncodingEscapeChar", "$");
        if (encodeEscapeCharString.length() > 0) {
            // What to do if the user specifies a >1 character long string? I guess just use the first char.
            encodeEscapeChar = encodeEscapeCharString.charAt(0);
        
            String repChars = PropertiesUtil.getProperty("mjb.charset.unsafeFilenameChars", "<>:\"/\\|?*");
            for (String repChar : repChars.split("")) {
                if (repChar.length() > 0) {
                    char ch = repChar.charAt(0);
                    // Don't encode characters that are hex digits
                    // Also, don't encode the escape char -- it is safe by definition!
                    if (!Character.isDigit(ch) && -1 == "AaBbCcDdEeFf".indexOf(ch) && !encodeEscapeChar.equals(ch)) {
                        String hex = Integer.toHexString(ch).toUpperCase();
                        unsafeChars.add(new ReplaceEntry(repChar, encodeEscapeChar + hex));
                    }
                }
            }
        }
        
        // Parse transliteration map: (source_character [-] transliteration_sequence [,])+
        StringTokenizer st = new StringTokenizer(PropertiesUtil.getProperty("mjb.charset.filename.translate", ""), ",");
        while (st.hasMoreElements()) {
            final String token = st.nextToken();
            final String character = new String(trimToNull(substringBefore(token, "-")));
            if (character == null) {
                // TODO Error message?
                continue;
            }
            final String translation = new String(trimToNull(substringAfter(token, "-")));
            if (translation == null) {
                // TODO Error message?
                // TODO Allow empty transliteration?
                continue;
            }
            unsafeChars.add(new ReplaceEntry(character.toUpperCase(), translation.toUpperCase()));
            unsafeChars.add(new ReplaceEntry(character.toLowerCase(), translation.toLowerCase()));
        }
    }

    public static int copy(InputStream is, OutputStream os, WebStats stats) throws IOException {
      int bytesCopied = 0;
      byte[] buffer = threadBuffer.get(); 
      try {
            while (true) {
                int amountRead = is.read(buffer);
                if (amountRead == -1) {
                    break;
                }
                else {
                    bytesCopied += amountRead;
                    if (stats != null) {
                        stats.bytes(amountRead);
                    }
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

    public static int copy(InputStream is, OutputStream os) throws IOException {
        return copy(is, os, null);
    }

    public static void copyFile(String src, String dst) {
        File srcFile, dstFile;
        
        try {
            srcFile = new File(src);
        } catch (Exception error) {
            logger.severe("Failed copying file " + src + " to " + dst);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return;
        }

        try {
            dstFile = new File(dst);
        } catch (Exception error) {
            logger.severe("Failed copying file " + src + " to " + dst);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
            return;
        }
        copyFile(srcFile, dstFile);
    }

    public static void copyFile(File src, File dst) {
        try {
            if (!src.exists()) {
                logger.severe("The specified " + src + " file does not exist!");
                return;
            }

            if (dst.isDirectory()) {
                dst.mkdirs();
                copyFile(src, new File(dst + File.separator + src.getName()));
            } else {
                // gc: copy using file channels, potentially much faster
                FileChannel inChannel = new FileInputStream(src).getChannel();
                FileChannel outChannel = new FileOutputStream(dst).getChannel();
                try {
                    long p = 0, s = inChannel.size();
                    while(p < s) {
                        p += inChannel.transferTo(p, 1024*1024, outChannel);
                    }
                } finally {
                    if (inChannel != null) {
                        inChannel.close();
                    }
                    
                    if (outChannel != null) {
                        outChannel.close();
                    }
                }
            }

        } catch (IOException error) {
            logger.severe("Failed copying file " + src + " to " + dst);
            final Writer eResult = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(eResult);
            error.printStackTrace(printWriter);
            logger.severe(eResult.toString());
        }
    }

    public static void copyDir(String srcDir, String dstDir, boolean updateDisplay) {
        try {
            File src = new File(srcDir);
            if (!src.exists()) {
                logger.severe("The specified " + srcDir + " file or directory does not exist!");
                return;
            }

            File dst = new File(dstDir);
            dst.mkdirs();

            if (!dst.exists()) {
                logger.severe("The specified " + dstDir + " output directory does not exist!");
                return;
            }

            if (src.isFile()) {
                copyFile(src, dst);
            } else {
                File[] contentList = src.listFiles();
                if (contentList != null) {
                    List<File> files = Arrays.asList(contentList);
                    Collections.sort(files);

                    int totalSize = files.size();
                    int currentFile = 0;
                    for (File file : files) {
                        currentFile++;
                        if (!file.getName().equals(".svn")) {
                            if (file.isDirectory()) {
                                copyDir(file.getAbsolutePath(), dstDir + File.separator + file.getName(), updateDisplay);
                            } else {
                                if (updateDisplay) {
                                    System.out.print("\r    Copying directory " + srcDir + " (" + currentFile + "/" + totalSize + ")");
                                }
                                copyFile(file, dst);
                            }
                        }
                    }
                    if (updateDisplay) {
                        System.out.print("\n");
                    }
                    logger.finest("Copied " + totalSize + " files from " + srcDir);
                }
            }
        } catch (Exception ignore) {
            // We're not concerned about the exception
            return;
        }
    }

    public static String readFileToString(File file) {
        StringBuffer out = new StringBuffer();
        
        if (file != null) {
            try {
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new FileReader(file));
                    String line = in.readLine();
                    while (line != null) {
                        out.append(line+ " "); // Add a space to avoid unwanted concatenation
                        line = in.readLine();
                    }
                } finally {
                    if (in != null) {
                        in.close();
                    }
                }
            } catch (IOException error) {
                logger.severe("Failed reading file " + file.getName());
            }
        }
        
        return out.toString();
    }
    
    /**
     * Write string to a file (Used for debugging)
     * @param filename
     * @param outputString
     */
    public static void writeStringToFile(String filename, String outputString) {
        FileWriter out = null;
        
        try {
            File outFile = new File(filename);
            
            out = new FileWriter(outFile);
            out.write(outputString);
        } catch (Exception ignore) {
            logger.finer("Error writing string to " + filename);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                // Failed to close out file.
            }
        }
    }

    /***
     *  @author Stuart Boston
     *  @param  file1 - first file to compare
     *  @param  file2 - second file to compare
     *  @return true if the files exist and file2 is older, false otherwise.
     *
     * Note that file1 will be checked to see if it's newer than file2
     */
    public static boolean isNewer(File file1, File file2) {
        // TODO: Update this routine to use fileCache
        // If file1 exists and file2 doesn't then return true
        if (file1.exists()) {
            // If file2 doesn't exist then file1 is newer 
            if (!file2.exists()) {
                return true;
            }
        } else {
            // File1 doesn't exist so return false
            return false;
        }

        // Compare the file dates. This is only true if the first file is newer than the second, as the second file is the file2 file
        if (file1.lastModified() <= file2.lastModified()) {
            // file1 is older than the file2.
            return false;
        } else {
            // file1 is newer than file2
            return true;
        }
    }

    public static String createCategoryKey(String key2) {
        return key2;
    }

    public static String createPrefix(String category, String key) {
        return indexFilesPrefix  + category + '_' + key + '_';
    }

    public static OutputStream createFileOutputStream(File f, int size) throws FileNotFoundException {
//        return new FileOutputStream(f);
        return new BufferedOutputStream(new FileOutputStream(f), size);
    }

    public static OutputStream createFileOutputStream(File f) throws FileNotFoundException {
        return createFileOutputStream(f, 10*1024);
    }

    public static OutputStream createFileOutputStream(String f) throws FileNotFoundException {
        return createFileOutputStream(new File(f));
    }

    public static OutputStream createFileOutputStream(String f, int size) throws FileNotFoundException {
        return createFileOutputStream(new File(f), size);
    }

    public static InputStream createFileInputStream(File f) throws FileNotFoundException {
//        return new FileInputStream(f);
        return new BufferedInputStream(new FileInputStream(f), 10*1024);
    }

    public static InputStream createFileInputStream(String f) throws FileNotFoundException {
        return createFileInputStream(new File(f));
    }

    public static String makeSafeFilename(String filename) {
        String newFilename = filename;
        
        for (ReplaceEntry rep : unsafeChars) {
            newFilename = rep.check(newFilename);
        }
        
        if (!newFilename.equals(filename)) {
            logger.finest("Encoded filename string " + filename + " to " + newFilename);
        }
            
        return newFilename;
    }

    /*
     * Returns the given path in canonical form
     * i.e. no duplicated separators, no ".", ".."..., and ending without trailing separator
     * the only exception is a root! the canonical form for a root INCLUDES the separator
     */
    public static String getCanonicalPath(String path) {
        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            return path;
        }
    }

    /*
     * when concatenating paths and the source MIGHT be a root, use this function
     * to safely add the separator 
     */
    public static String getDirPathWithSeparator(String path) {
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    public static String getFileExtension(String filename) {
        return new String(filename.substring(filename.lastIndexOf('.')+1));
    }

    /*
     * Returns the parent folder name only; used when searching for posters...
     */
    public static String getParentFolderName(File file) {
        if (file == null) {
            return "";
        }
        String path = file.getParent();
        return new String(path.substring(path.lastIndexOf(File.separator)+1));
    }

    /***
     * Pass in the filename and a list of extensions, this function will scan for the filename plus extensions and return the File
     * 
     * @param filename
     * @param artworkExtensions
     * @return always a File, to be tested with exists() for valid file
     */
    public static File findFileFromExtensions(String fullBaseFilename, Collection<String> artworkExtensions) {
        File localFile = null;

        for (String extension : artworkExtensions) {
            localFile = fileCache.getFile(fullBaseFilename + "." + extension);
            if (localFile.exists()) {
                logger.finest("The file " + localFile + " found");
                return localFile;
            }
        }

        return (localFile != null ? localFile : new File(localFile + Movie.UNKNOWN) ); //just in case
    }

    /**
     * Search for the filename in the cache and look for each with the extensions
     * @param searchFilename
     * @param artworkExtensions
     * @param jukebox
     * @param logPrefix
     * @return
     */
    public static File findFilenameInCache(String searchFilename, Collection<String> artworkExtensions, Jukebox jukebox, String logPrefix) {
        File searchFile = null;
        String safeFilename = makeSafeFilename(searchFilename);
        
        logger.finer(logPrefix + "Scanning fileCache for " + safeFilename);
        safeFilename = File.separator + safeFilename;
        
        Collection<File> files = FileTools.fileCache.searchFilename(safeFilename, true);

        if (files.size() > 0) {
            // Copy the synchronized list to avoid ConcurrentModificationException
            Iterator<File> iter = new ArrayList<File>(FileTools.fileCache.searchFilename(safeFilename, true)).iterator();
            
            while (iter.hasNext()) {
                File file = iter.next();
                String abPath = file.getAbsolutePath().toLowerCase();
                
                if (abPath.startsWith(jukebox.getJukeboxRootLocationDetails().toLowerCase())) {
                    // Skip any files found in the jukebox
                    continue;
                }
                
                // Loop round the filename+extension to see if any exist and add them to the array
                for (String extension : artworkExtensions) {
                    if (abPath.endsWith((safeFilename + "." + extension).toLowerCase())) {
                        files.add(file);
                        
                        logger.finest(logPrefix + "Found: " + file.getAbsolutePath());
                        
                        if (searchFile == null) {
                            searchFile = file;
                        }
                        
                        // We've found the file, so we should quit the loop
                        break;
                    }
                }
            }
            
            if (searchFile != null) {
                logger.finest(logPrefix + "Using first one found: " + searchFile.getAbsolutePath());
            } else {
                logger.finest(logPrefix + "No matching files found for " + safeFilename);
            }
        } else {
            logger.finest(logPrefix + "No scanned files found...");
        }
        
        return searchFile;
    }
    
    /**
     * Download the image for the specified URL into the specified file. 
     * Utilises the WebBrowser downloadImage function to allow for proxy connections.
     * 
     * @param imageFile
     * @param imageURL
     * @throws IOException
     */
    public static void downloadImage(File imageFile, String imageURL) throws IOException {
        WebBrowser webBrowser = new WebBrowser();
        webBrowser.downloadImage(imageFile, imageURL);
    }
    
    /**
     * Find the parent directory of the movie file.
     * @param movie
     * @return Parent folder
     * @author Stuart Boston
     */
    public static String getParentFolder(File movieFile) {
        String parentFolder = null;
        
        if (movieFile.isDirectory()) { // for VIDEO_TS
            parentFolder = movieFile.getPath();
        } else {
            parentFolder = movieFile.getParent();
        }
        
        // Issue 1070, /BDMV/STREAM is being appended to the parent path
        if (parentFolder.toUpperCase().endsWith(File.separator + "BDMV" + File.separator + "STREAM")) {
            parentFolder = new String(parentFolder.substring(0, parentFolder.length() - 12));
        }

        return parentFolder;
    }
    
    /**
     * Recursively delete a directory
     * @param dir
     * @return
     */
    public static boolean deleteDir(String dir) {
        return deleteDir(new File(dir));
    }
    
    /**
     * Recursively delete a directory 
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    // System.out.println("Failed");
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        // System.out.println("Deleting: " + dir.getAbsolutePath());
        return dir.delete();
    }
   
    /**
     * Add a list of files to the jukebox filenames
     * @param filenames
     */
    public static void addJukeboxFiles(Collection<String> filenames) {
        generatedFileNames.addAll(filenames);
    }
   
    /**
     * Add an individual filename to the jukebox cleaning exclusion list
     * @param filename
     */
    public static void addJukeboxFile(String filename) {
        if (StringTools.isValidString(filename)) {
            generatedFileNames.add(filename);
        }
    }
   
    /**
     * Process the movie and add all the files to the jukebox cleaning exclusion list
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
    }

    public static Collection<String> getJukeboxFiles() {
        return generatedFileNames;
    }
   
    /**
     * special file with "cached" attributes
     * used to minimize file system access which slows down everything
     * @author Gabriel Corneanu
     */
    @SuppressWarnings("serial")
    public static class FileEx extends File {
        private volatile Boolean _isdir = null;
        private volatile Boolean _exists = null;
        private volatile Boolean _isfile = null;
        private volatile Long    _len = null;
        private volatile Long    _lastModified = null;

        private ArchiveScanner[] archiveScanners;
        
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
            _exists = exists;
        }

        // archive scanner supporting constructors
        public FileEx(String pathname, ArchiveScanner[] archiveScanners) {
            super(pathname);
            this.archiveScanners=archiveScanners;
        }
        
        public FileEx(File parent, String child, ArchiveScanner[] archiveScanners) {
            this(parent, child);
            this.archiveScanners=archiveScanners;
        }

        @Override
        public boolean isDirectory() {
            if (_isdir == null) {
                synchronized(this) {
                    if (_isdir == null) {
                        _isdir = super.isDirectory();
                    }
                }
            }
            return _isdir;
        }
        
        @Override
        public boolean exists() {
            if (_exists == null) {
                synchronized(this) {
                    if (_exists == null) {
                        _exists = super.exists();
                    }
                }
            }
            return _exists;
        }

        @Override
        public boolean isFile() {
            if (_isfile == null) {
                synchronized(this) {
                    if (_isfile == null) {
                        _isfile = super.isFile();
                    }
                }
            }
            return _isfile;
        }
        
        @Override
        public long length() {
            if (_len == null) {
                synchronized(this) {
                    if (_len == null) {
                        _len = super.length();
                    }
                }
            }
            return _len;
        }
        
        @Override
        public long lastModified() {
            if (_lastModified == null) {
                synchronized(this) {
                    if (_lastModified == null) {
                        _lastModified = super.lastModified();
                    }
                }
            }
            return _lastModified;
        }

        @Override
        public File getParentFile() {
            String p = this.getParent();
            if (p == null) {
                return null;
            }
            return new FileEx(p, archiveScanners);
        }

        private volatile File[] _listFiles;

        @Override
        public File[] listFiles() {
            synchronized(this) {
                if (_listFiles != null) {
                    return _listFiles;
                }

                String[] nameStrings = list();
                if (nameStrings == null) {
                    return null;
                }
                
                List<String> mutableNames = new ArrayList<String>(Arrays.asList(nameStrings));
                List<File> files = new ArrayList<File>();
                if (archiveScanners != null) {
                    for (ArchiveScanner as: archiveScanners) {
                        files.addAll(as.getArchiveFiles(this,mutableNames));
                    }
                }
                
                for (String name: mutableNames) {
                    FileEx fe = new FileEx(this, name, archiveScanners);
                    fe._exists = true;
                    files.add(fe);
                }
                
                _listFiles = (File[])files.toArray(new File[files.size()]);
            }
            return _listFiles;
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
            for (File f: src) {
                if (filter.accept(this, f.getName())) {
                    l.add(f);
                }
            }
            return (File[])l.toArray(new File[l.size()]);
        }

    }

    /**
     * cached File instances
     * the key is always absolute path in upper-case, so it will NOT work for case only differences
     * @author Gabriel Corneanu
     */
    public static class ScannedFilesCache {       
        //cache for ALL files found during initial scan
        private Map<String, File> cachedFiles = new ConcurrentHashMap<String, File>(1000);
        
        /**
         * Check whether the file exists 
         */
        public boolean fileExists(String absPath) {
            return cachedFiles.containsKey(absPath.toUpperCase());
        }
        
        public boolean fileExists(File file) {
            return cachedFiles.containsKey(file.getAbsolutePath().toUpperCase());
        }
        
        /**
         * Add a file instance to cache 
         */
        public void fileAdd(File file) {
            cachedFiles.put(file.getAbsolutePath().toUpperCase(), file);
        }
        
        /*
         * Retrieve a file from cache
         * If it is NOT found, construct one instance and mark it as non-existing
         * The exist() test is used very often throughout the library to search for specific files
         * The path MUST be canonical (i.e. carefully constructed)
         * We do NOT want here to make it canonical because it goes to the file system and it's slow
         */
        public File getFile(String path) {
            File f = cachedFiles.get(path.toUpperCase());
            return (f == null ? new FileEx(path, false) : f);
        }
        
        /*
         * Add a full directory listing; used for existing jukebox
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
        
        public void saveFileList(String filename) throws FileNotFoundException {
            PrintWriter p = new PrintWriter(filename);
            Set<String> names = cachedFiles.keySet();
            String[] sortednames = names.toArray(new String[names.size()]);
            Arrays.sort(sortednames);
            for (String f : sortednames) {
                p.println(f);
            }
            p.close();
        }
    }
    
    public static ScannedFilesCache fileCache = new ScannedFilesCache();
 
}
