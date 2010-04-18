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

package com.moviejukebox.tools;

import static com.moviejukebox.tools.PropertiesUtil.getProperty;
import static java.lang.Boolean.parseBoolean;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;

// import com.moviejukebox.tools.HTMLTools;

public class FileTools {

    private static Logger logger = Logger.getLogger("moviejukebox");
    final static int BUFF_SIZE = 100000;

    /**
     * Gabriel Corneanu: One buffer for each thread to allow threaded copies
     */
    private final static ThreadLocal<byte[]> threadBuffer = new ThreadLocal<byte[]>() {
        protected byte[] initialValue() {
            return new byte[BUFF_SIZE];
        };
    };
    
    private static class ReplaceEntry{
        private Pattern patt; 
        private String newtext;
        public ReplaceEntry(CharSequence oldtext, String newtext){
            this.patt = Pattern.compile(oldtext.toString(), Pattern.LITERAL);
            this.newtext = Matcher.quoteReplacement(newtext);
        }
        public String check(String filename){
            return patt.matcher(filename).replaceAll(newtext);
        }
    };

    private static Collection<ReplaceEntry> unsafeChars = new ArrayList<ReplaceEntry>();  
    static Character encodeEscapeChar = null;
    private final static Collection<String> generatedFileNames = Collections.synchronizedCollection(new ArrayList<String>());
    private static boolean videoimageDownload = parseBoolean(getProperty("mjb.includeVideoImages", "false"));
    
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
            final String character = trimToNull(substringBefore(token, "-"));
            if (character == null) {
                // TODO Error message?
                continue;
            }
            final String translation = trimToNull(substringAfter(token, "-"));
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
                	if(stats != null) stats.bytes(amountRead);
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
                    while(p < s)
                        p += inChannel.transferTo(p, 1024*1024, outChannel);
                }
                finally {
                    if (inChannel != null) inChannel.close();
                    if (outChannel != null) outChannel.close();
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
                }
            }
        //no exception to catch here
        }finally{}
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

    /***
     *  @author Stuart Boston
     *  @param  file1 - first file to compare
     *  @param  file2 - second file to compare
     *  @return true if the files exist and file2 is older, false otherwise.
     *
     * Note that file1 will be checked to see if it's newer than file2
     */
    public static boolean isNewer(File file1, File file2) {
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
        return category + '_' + key + '_';
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
        String oldfilename = filename;
        for (ReplaceEntry rep : unsafeChars) {
            filename = rep.check(filename);
        }
        if (!filename.equals(oldfilename)) {
            logger.finest("Encoded filename string " + oldfilename + " to " + filename);
        }
            
        return filename;
    }

    /*
     * Returns the given path in canonical form
     * i.e. no duplicated separators, no ".", ".."..., and ending without trailing separator
     * the only exception is a root! the canonical form for a root INCLUDES the separator
     */
    public static String getCanonicalPath(String path) {
        try {
            path = new File(path).getCanonicalPath();
        } catch (IOException e) {}
        return path;
        
    }

    /*
     * when concatenating paths and the source MIGHT be a root, use this function
     * to safely add the separator 
     */
    public static String getDirPathWithSeparator(String path) {
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    public static String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.')+1);
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
            parentFolder = parentFolder.substring(0, parentFolder.length() - 12);
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
        if (!filename.equalsIgnoreCase(Movie.UNKNOWN) && filename != null) {
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
    
    /*
     * Gabriel Corneanu
     * special file whith "cached" attributes
     * used to minimize file system access which slows down everything
     */
    @SuppressWarnings("serial")
    public static class FileEx extends File{
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

        private Boolean _isdir=null;
        @Override
        public boolean isDirectory() {
            if(_isdir == null){
                synchronized(this){
                    if(_isdir == null) _isdir = super.isDirectory();
                }
            }
            return _isdir;
        }

        private Boolean _exists=null;
        @Override
        public boolean exists() {
            if(_exists == null){
                synchronized(this){
                    if(_exists == null) _exists = super.exists();
                }
            }
            return _exists;
        }

        private Boolean _isfile=null;
        @Override
        public boolean isFile() {
            if(_isfile == null){
                synchronized(this){
                    if(_isfile == null) _isfile = super.isFile();
                }
            }
            return _isfile;
        }

        private Long _len=null;
        @Override
        public long length() {
            if(_len == null){
                synchronized(this){
                    if(_len == null) _len = super.length();
                }
            }
            return _len;
        }

        private Long _lastModified=null;
        @Override
        public long lastModified() {
            if(_lastModified == null){
                synchronized(this){
                    if(_lastModified == null) _lastModified = super.lastModified();
                }
            }
            return _lastModified;
        }

        @Override
        public File getParentFile() {
            String p = this.getParent();
            if (p == null) return null;
            return new FileEx(p);
        }

        @Override
        public File[] listFiles() {
            String[] ss = list();
            if (ss == null) return null;
            int n = ss.length;
            FileEx[] fs = new FileEx[n];
            for (int i = 0; i < n; i++) {
                fs[i] = new FileEx(this, ss[i]);
                fs[i]._exists = true;
            }
            return fs;
        }

        public File[] listFiles(FilenameFilter filter) {
            String[] ss = list();
            if (ss == null) return null;
            ArrayList<FileEx> v = new ArrayList<FileEx>();
            FileEx f;
            for (int i = 0 ; i < ss.length ; i++) {
                if ((filter == null) || filter.accept(this, ss[i])) {
                    f = new FileEx(this, ss[i]);
                    f._exists = true;
                    v.add(f);
                }
            }
            return (FileEx[])(v.toArray(new FileEx[v.size()]));
            }

    }

    /*
     * Gabriel Corneanu
     * cached File instances
     * the key is always absolute path in upper-case, so it will NOT work for case only differences
     */
    public static class ScannedFilesCache {       
        //cache for ALL files found during initial scan
        private Map<String, File> cachedFiles = new ConcurrentHashMap<String, File>(1000);
        /**
         * Check whether the file exists 
         */
        public boolean fileExists(String absPath){
            return cachedFiles.containsKey(absPath.toUpperCase());
        }
        public boolean fileExists(File file){
            try {
                return cachedFiles.containsKey(file.getCanonicalPath().toUpperCase());
            } catch (IOException e) {
                return false;
            }
        }
        /**
         * Add a file instance to cache 
         */
        public void fileAdd(File file){
            try {
                cachedFiles.put(file.getCanonicalPath().toUpperCase(), file);
            } catch (IOException e) {
                //nothing, just skip
            }
        }
        /*
         * Retrieve a file from cache
         * If it is NOT found, construct one instance and mark it as non-existing
         * The exist() test is used very often throughout the library to search for specific files
         * The path MUST be canonical (i.e. carefully constructed)
         * We do NOT want here to make it canonical because it goes to the file system and it's slow
         */
        public File getFile(String path){
            File f = cachedFiles.get(path.toUpperCase());
            return f == null ? new FileEx(path, false) : f;
        }
        /*
         * Add a full directory listing; used for existing jukebox
         */
        public void addDir(File dir, int depth) {
           File[] files=dir.listFiles();
           if(files.length == 0) return;
           addFiles(files);
           if (depth <= 0) return;
           depth --;
           for(File f : files){
               if(f.isDirectory()){
                   addDir(f, depth);
               }
           }
        }

        public void addFiles(File[] files) {
            if(files.length == 0) return;
            Map<String, File> map = new HashMap<String, File>(files.length);
            for(File f : files){
                try {
                    map.put(f.getCanonicalPath().toUpperCase(), f);
                } catch (IOException e) {
                    //skip errors
                }
            }            
            cachedFiles.putAll(map);
        }

        public long size(){
            return cachedFiles.size();
        }
        
        public void saveFileList(String filename) throws FileNotFoundException{
            PrintWriter p = new PrintWriter(filename);
            Set<String> names = cachedFiles.keySet();
            String[] sortednames = names.toArray(new String[names.size()]);
            Arrays.sort(sortednames);
            for(String f : sortednames){
                p.println(f);
            }
            p.close();
        }
    }
    
    public static ScannedFilesCache fileCache = new ScannedFilesCache();
  }
