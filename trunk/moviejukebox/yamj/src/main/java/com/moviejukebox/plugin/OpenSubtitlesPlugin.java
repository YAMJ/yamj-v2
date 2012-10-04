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
// Based on some code from the opensubtitles.org subtitle upload java applet
package com.moviejukebox.plugin;

import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Scanner;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class OpenSubtitlesPlugin {

    private static final Logger LOGGER = Logger.getLogger(OpenSubtitlesPlugin.class);
    private static final String LOG_MESSAGE = "OpenSubtitles Plugin: ";
    private static final String SUB_LANGUAGE_ID = PropertiesUtil.getProperty("opensubtitles.language", "");
    private static String login = PropertiesUtil.getProperty("opensubtitles.username", "");
    private static String pass = PropertiesUtil.getProperty("opensubtitles.password", "");
    private static String useragent = "moviejukebox 1.0.15";
    //private static String useragent = "Yet Another Movie Jukebox";
    private static final String OS_DB_SERVER = "http://api.opensubtitles.org/xml-rpc";
    private static String token = "";
    // Literals
    private static final String OS_METHOD_START = "<?xml version=\"1.0\" encoding=\"utf-8\"?><methodCall><methodName>";
    private static final String OS_METHOD_END = "</methodName><params><param><value><string>";
    private static final String OS_PARAM = "</string></value></param><param><value><struct>";
    private static final String OS_MEMBER = "</struct></value></member>";
    private static final String OS_PARAMS = "</struct></value></param></params></methodCall>";

    static {
        // Check if subtitle language was selected
        if (StringUtils.isNotBlank(SUB_LANGUAGE_ID)) {
            // Part of opensubtitles.org protocol requirements
            LOGGER.info(LOG_MESSAGE + "Subtitles service allowed by www.OpenSubtitles.org");

            // Login to opensubtitles.org system
            logIn();
        } else {
            LOGGER.debug(LOG_MESSAGE + "No language selected in moviejukebox.properties");
        }
    }

    public OpenSubtitlesPlugin() {
    }

    private static void logIn() {
        try {
            if (token.equals("")) {
                String parm[] = {login, pass, "", useragent};
                String xml = generateXMLRPC("LogIn", parm);
                String ret = sendRPC(xml);
                getValue("status", ret);
                token = getValue("token", ret);
                if (token.equals("")) {
                    LOGGER.error(LOG_MESSAGE + "Login error." + "\n" + ret);
                } else {
                    LOGGER.debug(LOG_MESSAGE + "Login successful.");
                }
                // String l1 = login.equals("") ? "Anonymous" : login;
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Login Failed");
        }
    }

    public static void logOut() {

        // Check if subtitle language was selected
        if (StringUtils.isBlank(SUB_LANGUAGE_ID)) {
            return;
        }

        // Check that the login was successful
        if (StringUtils.isBlank(token)) {
            return;
        }

        try {
            String p1[] = {token};
            String xml = generateXMLRPC("LogOut", p1);
            sendRPC(xml);
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Logout Failed");
        }

    }

    public void generate(Movie movie) {

        if (StringTools.isNotValidString(movie.getSubtitles()) || movie.getSubtitles().equalsIgnoreCase("NO") || movie.isTVShow()) {
            // Check if subtitle language was selected
            if (StringUtils.isBlank(SUB_LANGUAGE_ID)) {
                return;
            }

            // Check to see if we scrape the library, if we don't then skip the download
            if (!movie.isScrapeLibrary()) {
                LOGGER.debug(LOG_MESSAGE + "Skipped for " + movie.getTitle() + " due to scrape library flag");
                return;
            }

            // Check that the login was successful
            if (StringUtils.isBlank(token)) {
                LOGGER.debug(LOG_MESSAGE + "Login failed");
                return;
            }

            // Check if all files have subtitle
            boolean allSubtitleExist = true;
            boolean allSubtitleExchange = true;

            // Go over all the movie files and check subtitle status
            for (MovieFile mf : movie.getMovieFiles()) {

                if (!mf.isSubtitlesExchange()) {
                    allSubtitleExchange = false;
                }

                // Check if this movie already have subtitles for it
                if (mf.getFile() == null) {
                    // The file pointer doesn't exist, so skip the file
                    continue;
                }


                String path = mf.getFile().getAbsolutePath();
                int index = path.lastIndexOf('.');
                String basename = path.substring(0, index + 1);

                File subtitleFile = FileTools.fileCache.getFile(basename + "srt");

                if (!subtitleFile.exists()) {
                    allSubtitleExchange = false;
                    allSubtitleExist = false;
                    break;
                }
            }

            if (allSubtitleExchange) {
                LOGGER.debug(LOG_MESSAGE + "All subtitles exist for " + movie.getTitle());
                // Don't return yet, we might want to upload the files.
                //return;
            }

            // Check if all files have subtitle , or this is a tv show, that each episode is by itself
            if (!allSubtitleExist || movie.getMovieType().equals(Movie.TYPE_TVSHOW)) {

                // Go over all the movie files
                for (MovieFile mf : movie.getMovieFiles()) {

                    // Check if this movie already have subtitles for it
                    String path = mf.getFile().getAbsolutePath();
                    int index = path.lastIndexOf('.');
                    String basename = path.substring(0, index + 1);

                    File subtitleFile = FileTools.fileCache.getFile(basename + "srt");

                    if (!subtitleFile.exists()) {
                        if (subtitleDownload(movie, mf.getFile(), subtitleFile) == true) {
                            movie.setDirty(DirtyFlag.INFO, true);
                            mf.setSubtitlesExchange(true);
                        }
                    } else {
                        if (!mf.isSubtitlesExchange()) {
                            File movieFileArray[] = new File[1];
                            File subtitleFileArray[] = new File[1];

                            movieFileArray[0] = mf.getFile();
                            subtitleFileArray[0] = subtitleFile;

                            if (subtitleUpload(movie, movieFileArray, subtitleFileArray) == true) {
                                movie.setDirty(DirtyFlag.INFO, true);
                                mf.setSubtitlesExchange(true);
                            }
                        }
                    }
                }
            } else {
                // Upload all movie files as a group
                File movieFileArray[] = new File[movie.getMovieFiles().size()];
                File subtitleFileArray[] = new File[movie.getMovieFiles().size()];
                int i = 0;

                // Go over all the movie files
                for (MovieFile mf : movie.getMovieFiles()) {

                    // Check if this movie already have subtitles for it
                    String path = mf.getFile().getAbsolutePath();
                    int index = path.lastIndexOf('.');
                    String basename = path.substring(0, index + 1);

                    File subtitleFile = new File(basename + "srt");

                    movieFileArray[i] = mf.getFile();
                    subtitleFileArray[i] = subtitleFile;
                    i++;
                }

                if (subtitleUpload(movie, movieFileArray, subtitleFileArray) == true) {
                    movie.setDirty(DirtyFlag.INFO, true);

                    // Go over all the movie files and mark the exchange
                    for (MovieFile mf : movie.getMovieFiles()) {
                        mf.setSubtitlesExchange(true);
                    }
                }
            }
        } else {
            LOGGER.debug(LOG_MESSAGE + "Skipping subtitle download for " + movie.getTitle() + ", subtitles already exist: " + movie.getSubtitles());
        }
    }

    private boolean subtitleDownload(Movie movie, File movieFile, File subtitleFile) {
        try {
            String ret;
            String xml;

            String moviehash = getHash(movieFile);
            String moviebytesize = String.valueOf(movieFile.length());

            xml = generateXMLRPCSS(moviehash, moviebytesize);
            ret = sendRPC(xml);

            String subDownloadLink = getValue("SubDownloadLink", ret);

            if (subDownloadLink.equals("")) {
                String moviefilename = movieFile.getName();

                // Do not search by file name for BD rip files in the format 0xxxx.m2ts
                if (!(moviefilename.toUpperCase().endsWith(".M2TS") && moviefilename.startsWith("0"))) {

                    // Try to find the subtitle using file name
                    String subfilename = subtitleFile.getName();
                    int index = subfilename.lastIndexOf('.');

                    String query = subfilename.substring(0, index);

                    xml = generateXMLRPCSS(query);
                    ret = sendRPC(xml);

                    subDownloadLink = getValue("SubDownloadLink", ret);
                }
            }

            if (subDownloadLink.equals("")) {
                LOGGER.debug(LOG_MESSAGE + "Subtitle not found for " + movieFile.getName());
                return false;
            }

            LOGGER.debug(LOG_MESSAGE + "Download subtitle for " + movie.getBaseName());

            URL url = new URL(subDownloadLink);
            HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
            connection.setRequestProperty("Connection", "Close");
            InputStream inputStream = connection.getInputStream();

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                LOGGER.error(LOG_MESSAGE + "Download Failed");
                return false;
            }

            FileTools.copy(new GZIPInputStream(inputStream), new FileOutputStream(subtitleFile));
            connection.disconnect();

            String subLanguageID = getValue("SubLanguageID", ret);
            if (StringUtils.isNotBlank(subLanguageID)) {
                movie.setSubtitles(subLanguageID);
            } else {
                movie.setSubtitles("YES");
            }

            return true;

        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Download Exception (Movie Not Found)");
            return false;
        }

    }

    private boolean subtitleUpload(Movie movie, File movieFile[], File subtitleFile[]) {
        ByteArrayOutputStream baos = null;
        DeflaterOutputStream deflaterOS = null;
        FileInputStream fisSubtitleFile = null;

        try {

            String ret;
            String xml;

            String idmovieimdb = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID).substring(2);
            idmovieimdb = String.valueOf(Integer.parseInt(idmovieimdb));

            String subfilename[] = new String[movieFile.length];
            String subhash[] = new String[movieFile.length];
            String subcontent[] = new String[movieFile.length];

            String moviehash[] = new String[movieFile.length];
            String moviebytesize[] = new String[movieFile.length];
            String movietimems[] = new String[movieFile.length];
            String movieframes[] = new String[movieFile.length];
            String moviefps[] = new String[movieFile.length];
            String moviefilename[] = new String[movieFile.length];

            for (int i = 0; i < movieFile.length; i++) {

                subfilename[i] = subtitleFile[i].getName();
                subhash[i] = "";
                subcontent[i] = "";

                moviehash[i] = getHash(movieFile[i]);
                moviebytesize[i] = String.valueOf(movieFile[i].length());
                movietimems[i] = "";
                movieframes[i] = "";
                moviefps[i] = String.valueOf(movie.getFps());
                moviefilename[i] = movieFile[i].getName();

                fisSubtitleFile = new FileInputStream(subtitleFile[i]);
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte s[] = new byte[fisSubtitleFile.available()];
                fisSubtitleFile.read(s);
                fisSubtitleFile.close();

                md.update(s);
                subhash[i] = hashstring(md.digest());
                baos = new ByteArrayOutputStream();
                deflaterOS = new DeflaterOutputStream(baos);
                deflaterOS.write(s);
                deflaterOS.finish();

                subcontent[i] = tuBase64(baos.toByteArray());
            }

            // Check if upload of this subtitle is required
            xml = generateXMLRPCTUS(subhash, subfilename, moviehash, moviebytesize, movietimems, movieframes, moviefps, moviefilename);

            ret = sendRPC(xml);

            String alreadyindb = getIntValue("alreadyindb", ret);

            if (!alreadyindb.equals("0")) {
                LOGGER.debug(LOG_MESSAGE + "Subtitle already in db for " + movie.getBaseName());
                return true;
            }

            LOGGER.debug(LOG_MESSAGE + "Upload Subtitle for " + movie.getBaseName());

            // Upload the subtitle
            xml = generateXMLRPCUS(idmovieimdb, subhash, subcontent, subfilename, moviehash, moviebytesize, movietimems, movieframes, moviefps, moviefilename);

            sendRPC(xml);

            return true;

        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Upload Failed");
            return false;
        } finally {
            try {
                if (fisSubtitleFile != null) {
                    fisSubtitleFile.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            try {
                if (deflaterOS != null) {
                    deflaterOS.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            try {
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private static String generateXMLRPCSS(String moviehash, String moviebytesize) {
        StringBuilder sb = new StringBuilder(OS_METHOD_START);
        sb.append("SearchSubtitles");
        sb.append(OS_METHOD_END);
        sb.append(token);
        sb.append(OS_PARAM);

        sb.append("<member><value><struct>");

        sb.append(addymember("sublanguageid", SUB_LANGUAGE_ID));
        sb.append(addymember("moviehash", moviehash));
        sb.append(addymember("moviebytesize", moviebytesize));

        sb.append(OS_MEMBER);

        sb.append(OS_PARAMS);
        return sb.toString();
    }

    private static String generateXMLRPCSS(String query) {
        StringBuilder sb = new StringBuilder(OS_METHOD_START);
        sb.append("SearchSubtitles");
        sb.append(OS_METHOD_END);
        sb.append(token);
        sb.append(OS_PARAM);

        sb.append("<member><value><struct>");

        sb.append(addymember("sublanguageid", SUB_LANGUAGE_ID));
        sb.append(addymember("query", query));

        sb.append(OS_MEMBER);

        sb.append(OS_PARAMS);
        return sb.toString();
    }

    ;

    private static String generateXMLRPCUS(String idmovieimdb, String subhash[], String subcontent[], String subfilename[], String moviehash[],
            String moviebytesize[], String movietimems[], String movieframes[], String moviefps[], String moviefilename[]) {
        StringBuilder sb = new StringBuilder(OS_METHOD_START);
        sb.append("UploadSubtitles");
        sb.append(OS_METHOD_END);
        sb.append(token);
        sb.append(OS_PARAM);

        for (int i = 0; i < subhash.length; i++) {
            sb.append("<member><name>");
            sb.append("cd");
            sb.append(i + 1);
            sb.append("</name><value><struct>");
            sb.append(addymember("movietimems", movietimems[i]));
            sb.append(addymember("moviebytesize", moviebytesize[i]));
            sb.append(addymember("subfilename", subfilename[i]));
            sb.append(addymember("subcontent", subcontent[i]));
            sb.append(addymember("subhash", subhash[i]));
            sb.append(addymember("movieframes", movieframes[i]));
            sb.append(addymember("moviefps", moviefps[i]));
            sb.append(addymember("moviefilename", moviefilename[i]));
            sb.append(addymember("moviehash", moviehash[i]));

            sb.append(OS_MEMBER);
        }

        sb.append("<member><name>baseinfo</name><value><struct>");
        sb.append(addymember("sublanguageid", SUB_LANGUAGE_ID));
        sb.append(addymember("idmovieimdb", idmovieimdb));
        sb.append(addymember("subauthorcomment", ""));
        sb.append(addymember("movieaka", ""));
        sb.append(addymember("moviereleasename", ""));

        sb.append(OS_MEMBER);

        sb.append(OS_PARAMS);
        return sb.toString();
    }

    private static String generateXMLRPCTUS(String subhash[], String subfilename[], String moviehash[], String moviebytesize[], String movietimems[],
            String movieframes[], String moviefps[], String moviefilename[]) {
        StringBuilder sb = new StringBuilder(OS_METHOD_START);
        sb.append("TryUploadSubtitles");
        sb.append(OS_METHOD_END);
        sb.append(token);
        sb.append(OS_PARAM);

        for (int i = 0; i < subhash.length; i++) {
            sb.append("<member><name>");
            sb.append("cd");
            sb.append(i + 1);
            sb.append("</name><value><struct>");
            sb.append(addymember("movietimems", movietimems[i]));
            sb.append(addymember("moviebytesize", moviebytesize[i]));
            sb.append(addymember("subfilename", subfilename[i]));
            sb.append(addymember("subhash", subhash[i]));
            sb.append(addymember("movieframes", movieframes[i]));
            sb.append(addymember("moviefps", moviefps[i]));
            sb.append(addymember("moviefilename", moviefilename[i]));
            sb.append(addymember("moviehash", moviehash[i]));

            sb.append(OS_MEMBER);
        }

        sb.append(OS_PARAMS);
        return sb.toString();
    }

    ;

    /*
     *
     *
     * private static String sendRPCDetectLang(byte text[]) throws MalformedURLException, IOException { String str = ""; String strona = OSdbServer; // String
     * logowanie=xml; URL url = new URL(strona); URLConnection connection = url.openConnection(); connection.setRequestProperty("Connection", "Close");
     *
     * // connection.setRequestProperty("Accept","text/html"); connection.setRequestProperty("Content-Type", "text/xml"); connection.setDoOutput(true);
     * //PrintWriter out= new PrintWriter(connection.getOutputStream()); //out.print(logowanie); String str2 =
     * PART_1 + "DetectLanguage" + "</methodName><params>" + "<param><value><string>" + token +
     * "</string></value></param>" + "<param><value>" + "<struct><value><string>";
     *
     *
     * String str3 = "</string></value></struct></value></param></params></methodCall>";
     *
     * connection.getOutputStream().write(str2.getBytes("UTF-8")); connection.getOutputStream().write(text);
     * connection.getOutputStream().write(str3.getBytes("UTF-8"));
     *
     * Scanner in; in = new Scanner(connection.getInputStream()); while (in.hasNextLine()) { str += in.nextLine(); } ; return str; } ;
     */

    private static String sendRPC(String xml) throws IOException {

        StringBuilder str = new StringBuilder();
        String strona = OS_DB_SERVER;
        String logowanie = xml;
        URL url = new URL(strona);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Connection", "Close");

        // connection.setRequestProperty("Accept","text/html");
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setDoOutput(true);

        connection.getOutputStream().write(logowanie.getBytes("UTF-8"));

        Scanner in;
        in = new Scanner(connection.getInputStream());

        while (in.hasNextLine()) {
            str.append(in.nextLine());
        }

        in.close();
        ((HttpURLConnection) connection).disconnect();

        return str.toString();
    }

    private static String getValue(String find, String xml) {
        int a = xml.indexOf(find);
        if (a != -1) {
            int b = xml.indexOf("<string>", a);
            int c = xml.indexOf("</string>", b);
            if ((b != -1) && (c != -1)) {
                return xml.substring(b + 8, c);
            }
        }
        return "";
    }

    private static String getIntValue(String find, String xml) {
        String str = "";
        int a = xml.indexOf(find);
        if (a != -1) {
            int b = xml.indexOf("<int>", a);
            int c = xml.indexOf("</int>", b);
            if ((b != -1) && (c != -1)) {
                return xml.substring(b + 5, c);
            }
        }
        return str;
    }

    private static String hashstring(byte[] arayhash) {
        StringBuilder str = new StringBuilder();
        String hex = "0123456789abcdef";
        for (int i = 0; i < arayhash.length; i++) {
            int m = arayhash[i] & 0xff;
            str.append(hex.charAt(m >> 4) + hex.charAt(m & 0xf));
        }
        return str.toString();
    }

    @SuppressWarnings("unused")
    private static String generateXMLRPCDetectLang(String body) {
        StringBuilder sb = new StringBuilder(OS_METHOD_START);
        sb.append("DetectLanguage");
        sb.append("</methodName><params>");

        sb.append("<param><value><string>");
        sb.append(token);
        sb.append("</string></value></param>");
        sb.append("<param><value>");
        sb.append(body);
        sb.append(" </value></param>");

        sb.append("</params></methodCall>");
        return sb.toString();
    }

    private static String generateXMLRPC(String procname, String s[]) {
        StringBuilder str = new StringBuilder();
        str.append(OS_METHOD_START);
        str.append(procname).append("</methodName><params>");

        for (int i = 0; i < s.length; i++) {
            str.append("<param><value><string>").append(s[i]).append("</string></value></param>");
        }

        str.append("</params></methodCall>");
        return str.toString();
    }

    private static String addEncje(String inputString) {
        String cleanString = inputString.replace("&", "&amp;");
        cleanString = cleanString.replace("<", "&lt;");
        cleanString = cleanString.replace(">", "&gt;");
        cleanString = cleanString.replace("'", "&apos;");
        cleanString = cleanString.replace("\"", "&quot;");
        return cleanString;
    }

    private static String tuBase64(byte s[]) {
        // You may use this for lower applet size
        // return new sun.misc.BASE64Encoder().encode(s);

        // char tx;
        // long mili = Calendar.getInstance().getTimeInMillis();
        String t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        char t2[] = t.toCharArray();
        char wynik[] = new char[(s.length / 3 + 1) * 4];

        int tri;
        for (int i = 0; i < (s.length / 3 + 1); i++) {
            tri = 0;
            int iii = i * 3;
            try {
                tri |= (s[iii] & 0xff) << 16;
                tri |= (s[iii + 1] & 0xff) << 8;
                tri |= (s[iii + 2] & 0xff);
            } catch (Exception error) {
            }

            for (int j = 0; j < 4; j++) {
                wynik[i * 4 + j] = (iii * 8 + j * 6 >= s.length * 8) ? '=' : t2[(tri >> 6 * (3 - j)) & 0x3f];
            }
            // if((i+1) % 19 ==0 ) str +="\n";
        }

        String str = new String(wynik);
        if (str.endsWith("====")) {
            str = str.substring(0, str.length() - 4);
        }

        return str;
    }

    private static String addymember(String name, String value) {
        return "<member><name>" + name + "</name><value><string>" + OpenSubtitlesPlugin.addEncje(value) + "</string></value></member>";
    }

    private static String getHash(File f) throws IOException {
        // Open the file and then get a channel from the stream
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        long sz = fc.size();

        if (sz < 65536) {
            fc.close();
            return "NoHash";
        }

        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, 65536);
        long sum = sz;

        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 65536 / 8; i++) {
            sum += bb.getLong();// sum(bb);
        }
        bb = fc.map(FileChannel.MapMode.READ_ONLY, sz - 65536, 65536);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 65536 / 8; i++) {
            sum += bb.getLong();// sum(bb);
        }
        sum = sum & 0xffffffffffffffffL;

        String s = String.format("%016x", sum);
        fc.close();
        fis.close();
        return s;
    }
}
