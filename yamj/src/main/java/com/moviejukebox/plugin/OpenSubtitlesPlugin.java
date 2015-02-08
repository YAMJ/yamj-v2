/*
 *      Copyright (c) 2004-2015 YAMJ Members
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
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.enumerations.DirtyFlag;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SubtitleTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on some code from the opensubtitles.org subtitle upload java applet
 */
public class OpenSubtitlesPlugin {

    private static final Logger LOG = LoggerFactory.getLogger(OpenSubtitlesPlugin.class);
    private static final String OS_DB_SERVER = "http://api.opensubtitles.org/xml-rpc";
    private static String loginToken = "";
    // Literals
    private static final String OS_USER_AGENT = "moviejukebox 1.0.15";
    private static final String OS_METHOD_START = "<?xml version=\"1.0\" encoding=\"utf-8\"?><methodCall><methodName>";
    private static final String OS_METHOD_END = "</methodName><params><param><value><string>";
    private static final String OS_PARAM = "</string></value></param><param><value><struct>";
    private static final String OS_MEMBER = "</struct></value></member>";
    private static final String OS_PARAMS = "</struct></value></param></params></methodCall>";
    // Properties
    private static final boolean IS_ENABLED = PropertiesUtil.getBooleanProperty("opensubtitles.enable", Boolean.TRUE);
    private static final String SUB_LANGUAGE_ID = PropertiesUtil.getProperty("opensubtitles.language", "");
    private static final String osUsername = PropertiesUtil.getProperty("opensubtitles.username", "");
    private static final String osPassword = PropertiesUtil.getProperty("opensubtitles.password", "");

    static {
        if (IS_ENABLED) {
            // Check if subtitle language was selected
            if (StringUtils.isNotBlank(SUB_LANGUAGE_ID)) {
                // Part of opensubtitles.org protocol requirements
                LOG.info("Subtitles service allowed by www.OpenSubtitles.org");

                // Login to opensubtitles.org system
                logIn();
            } else {
                LOG.debug("No language selected in properties file");
            }
        } else {
            LOG.trace("Plugin disabled.");
        }
    }

    public OpenSubtitlesPlugin() {
    }

    /**
     * Login to OpenSubtitles
     */
    private static void logIn() {
        try {
            if (StringUtils.isBlank(loginToken)) {
                String[] parm = {osUsername, osPassword, "", OS_USER_AGENT};
                String xml = generateXMLRPC("LogIn", parm);
                String ret = sendRPC(xml);
                getValue("status", ret);
                loginToken = getValue("token", ret);
                if (loginToken.equals("")) {
                    LOG.error("Login error.\n", ret);
                } else {
                    LOG.debug("Login successful.");
                }
                // String l1 = login.equals("") ? "Anonymous" : login;
            }
        } catch (Exception ex) {
            LOG.error("Login Failed: {}", ex.getMessage());
        }
    }

    /**
     * Logout of OpenSubtitles
     */
    public static void logOut() {
        // Check if plugin is enabled, subtitle language was selected and that the login was successful
        if (IS_ENABLED && StringUtils.isNotBlank(SUB_LANGUAGE_ID) && StringUtils.isNotBlank(loginToken)) {
            try {
                String[] p1 = {loginToken};
                String xml = generateXMLRPC("LogOut", p1);
                sendRPC(xml);
            } catch (Exception error) {
                LOG.error("Logout Failed");
            }
        }
    }

    /**
     * Get subtitles for the video
     *
     * @param movie
     */
    public void generate(Movie movie) {
        if (!IS_ENABLED) {
            return;
        }

        if (StringTools.isNotValidString(movie.getSubtitles()) || movie.getSubtitles().equalsIgnoreCase("NO") || movie.isTVShow()) {
            // Check if subtitle language was selected
            if (StringUtils.isBlank(SUB_LANGUAGE_ID)) {
                return;
            }

            // Check to see if we scrape the library, if we don't then skip the download
            if (!movie.isScrapeLibrary()) {
                LOG.debug("Skipped for {} due to scrape library flag", movie.getTitle());
                return;
            }

            // Check that the login was successful
            if (StringUtils.isBlank(loginToken)) {
                LOG.debug("Login failed");
                return;
            }

            // Check if all files have subtitle
            boolean allSubtitleExist = Boolean.TRUE;
            boolean allSubtitleExchange = Boolean.TRUE;

            // Go over all the movie files and check subtitle status
            for (MovieFile mf : movie.getMovieFiles()) {

                if (!mf.isSubtitlesExchange()) {
                    allSubtitleExchange = Boolean.FALSE;
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
                    allSubtitleExchange = Boolean.FALSE;
                    allSubtitleExist = Boolean.FALSE;
                    break;
                }
            }

            if (allSubtitleExchange) {
                LOG.debug("All subtitles exist for {}", movie.getTitle());
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
                        if (subtitleDownload(movie, mf.getFile(), subtitleFile) == Boolean.TRUE) {
                            movie.setDirty(DirtyFlag.INFO, Boolean.TRUE);
                            mf.setSubtitlesExchange(Boolean.TRUE);
                        }
                    } else {
                        if (!mf.isSubtitlesExchange()) {
                            File[] movieFileArray = new File[1];
                            File[] subtitleFileArray = new File[1];

                            movieFileArray[0] = mf.getFile();
                            subtitleFileArray[0] = subtitleFile;

                            if (subtitleUpload(movie, movieFileArray, subtitleFileArray) == Boolean.TRUE) {
                                movie.setDirty(DirtyFlag.INFO, Boolean.TRUE);
                                mf.setSubtitlesExchange(Boolean.TRUE);
                            }
                        }
                    }
                }
            } else {
                // Upload all movie files as a group
                File[] movieFileArray = new File[movie.getMovieFiles().size()];
                File[] subtitleFileArray = new File[movie.getMovieFiles().size()];
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

                if (subtitleUpload(movie, movieFileArray, subtitleFileArray) == Boolean.TRUE) {
                    movie.setDirty(DirtyFlag.INFO, Boolean.TRUE);

                    // Go over all the movie files and mark the exchange
                    for (MovieFile mf : movie.getMovieFiles()) {
                        mf.setSubtitlesExchange(Boolean.TRUE);
                    }
                }
            }
        } else {
            LOG.debug("Skipping subtitle download for {}, subtitles already exist: {}", movie.getTitle(), movie.getSubtitles());
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
                LOG.debug("Subtitle not found for {}", movieFile.getName());
                return Boolean.FALSE;
            }

            LOG.debug("Download subtitle for {}", movie.getBaseName());

            URL url = new URL(subDownloadLink);
            HttpURLConnection connection = (HttpURLConnection) (url.openConnection(WebBrowser.PROXY));
            connection.setRequestProperty("Connection", "Close");
            InputStream inputStream = connection.getInputStream();

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                LOG.error("Download Failed");
                return Boolean.FALSE;
            }

            FileTools.copy(new GZIPInputStream(inputStream), new FileOutputStream(subtitleFile));
            connection.disconnect();

            String subLanguageID = getValue("SubLanguageID", ret);
            if (StringUtils.isNotBlank(subLanguageID)) {
                SubtitleTools.addMovieSubtitle(movie, subLanguageID);
            } else {
                SubtitleTools.addMovieSubtitle(movie, "YES");
            }

            return Boolean.TRUE;

        } catch (Exception error) {
            LOG.error("Download Exception (Movie Not Found)");
            return Boolean.FALSE;
        }

    }

    private boolean subtitleUpload(Movie movie, File[] movieFile, File[] subtitleFile) {
        ByteArrayOutputStream baos = null;
        DeflaterOutputStream deflaterOS = null;
        FileInputStream fisSubtitleFile = null;

        try {
            String ret;
            String xml;

            String idmovieimdb = movie.getId(ImdbPlugin.IMDB_PLUGIN_ID);
            if (StringUtils.isNotBlank(idmovieimdb) && idmovieimdb.length() >= 6) {
                idmovieimdb = String.valueOf(NumberUtils.toInt(idmovieimdb.substring(2)));
            }
            String[] subfilename = new String[movieFile.length];
            String[] subhash = new String[movieFile.length];
            String[] subcontent = new String[movieFile.length];

            String[] moviehash = new String[movieFile.length];
            String[] moviebytesize = new String[movieFile.length];
            String[] movietimems = new String[movieFile.length];
            String[] movieframes = new String[movieFile.length];
            String[] moviefps = new String[movieFile.length];
            String[] moviefilename = new String[movieFile.length];

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
                byte[] s = new byte[fisSubtitleFile.available()];
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
                LOG.debug("Subtitle already in db for {}", movie.getBaseName());
                return Boolean.TRUE;
            }

            LOG.debug("Upload Subtitle for {}", movie.getBaseName());

            // Upload the subtitle
            xml = generateXMLRPCUS(idmovieimdb, subhash, subcontent, subfilename, moviehash, moviebytesize, movietimems, movieframes, moviefps, moviefilename);

            sendRPC(xml);

            return Boolean.TRUE;

        } catch (NumberFormatException | IOException | NoSuchAlgorithmException ex) {
            LOG.error("Upload Failed: {}", ex.getMessage());
            return Boolean.FALSE;
        } finally {
            try {
                if (fisSubtitleFile != null) {
                    fisSubtitleFile.close();
                }
            } catch (IOException ex) {
                // Ignore
            }

            try {
                if (deflaterOS != null) {
                    deflaterOS.close();
                }
            } catch (IOException ex) {
                // Ignore
            }

            try {
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    private static String generateXMLRPCSS(String moviehash, String moviebytesize) {
        StringBuilder sb = new StringBuilder(OS_METHOD_START);
        sb.append("SearchSubtitles");
        sb.append(OS_METHOD_END);
        sb.append(loginToken);
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
        sb.append(loginToken);
        sb.append(OS_PARAM);

        sb.append("<member><value><struct>");

        sb.append(addymember("sublanguageid", SUB_LANGUAGE_ID));
        sb.append(addymember("query", query));

        sb.append(OS_MEMBER);

        sb.append(OS_PARAMS);
        return sb.toString();
    }

    private static String generateXMLRPCUS(String idmovieimdb, String[] subhash, String[] subcontent, String[] subfilename, String[] moviehash,
            String[] moviebytesize, String[] movietimems, String[] movieframes, String[] moviefps, String[] moviefilename) {
        StringBuilder sb = new StringBuilder(OS_METHOD_START);
        sb.append("UploadSubtitles");
        sb.append(OS_METHOD_END);
        sb.append(loginToken);
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

    private static String generateXMLRPCTUS(String[] subhash, String[] subfilename, String[] moviehash, String[] moviebytesize, String[] movietimems,
            String[] movieframes, String[] moviefps, String[] moviefilename) {
        StringBuilder sb = new StringBuilder(OS_METHOD_START);
        sb.append("TryUploadSubtitles");
        sb.append(OS_METHOD_END);
        sb.append(loginToken);
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

    private static String sendRPC(String xml) throws IOException {

        StringBuilder str = new StringBuilder();
        String strona = OS_DB_SERVER;
        String logowanie = xml;
        URL url = new URL(strona);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Connection", "Close");

        // connection.setRequestProperty("Accept","text/html");
        connection.setRequestProperty("Content-Type", "text/xml");
        connection.setDoOutput(Boolean.TRUE);

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
        sb.append(loginToken);
        sb.append("</string></value></param>");
        sb.append("<param><value>");
        sb.append(body);
        sb.append(" </value></param>");

        sb.append("</params></methodCall>");
        return sb.toString();
    }

    private static String generateXMLRPC(String procname, String[] s) {
        StringBuilder str = new StringBuilder();
        str.append(OS_METHOD_START);
        str.append(procname).append("</methodName><params>");
        for (String item : s) {
            str.append("<param><value><string>").append(item).append("</string></value></param>");
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

    private static String tuBase64(byte[] s) {
        // You may use this for lower applet size
        // return new sun.misc.BASE64Encoder().encode(s);

        // char tx;
        // long mili = Calendar.getInstance().getTimeInMillis();
        String t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        char[] t2 = t.toCharArray();
        char[] wynik = new char[(s.length / 3 + 1) * 4];

        int tri;
        for (int i = 0; i < (s.length / 3 + 1); i++) {
            tri = 0;
            int iii = i * 3;
            try {
                tri |= (s[iii] & 0xff) << 16;
                tri |= (s[iii + 1] & 0xff) << 8;
                tri |= (s[iii + 2] & 0xff);
            } catch (Exception ex) {
                LOG.trace("Failed to convert string in tuBase64", ex);
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
        String s;
        try ( // Open the file and then get a channel from the stream
                FileInputStream fis = new FileInputStream(f);
                FileChannel fc = fis.getChannel()) {
            long sz = fc.size();
            if (sz < 65536) {
                fc.close();
                fis.close();
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
            s = String.format("%016x", sum);
        }
        return s;
    }
}
