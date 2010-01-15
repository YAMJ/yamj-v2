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

package com.moviejukebox.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.moviejukebox.model.Movie.BooleanYesNoAdapter;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.PropertiesUtil;

@SuppressWarnings("serial")
@XmlType public class MovieFile implements Comparable<MovieFile> {

    private String  filename    = Movie.UNKNOWN;
    private String  title       = Movie.UNKNOWN;
    private int     firstPart   = 1; // #1, #2, CD1, CD2, etc.
    private int     lastPart    = 1;
    private boolean newFile     = true; // is new file or already exists in XML data
    private boolean subtitlesExchange = false; // Are the subtitles for this file already downloaded/uploaded to the server
    private String  playLink    = Movie.UNKNOWN;
    private LinkedHashMap<Integer, String> plots = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> videoImageURL = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> videoImageFilename = new LinkedHashMap<Integer, String>();
    private File file;
    private MovieFileNameDTO info;
    private static boolean playFullBluRayDisk = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.playFullBluRayDisk","true"));
    private boolean includeEpisodePlots = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeEpisodePlots", "false"));
    private boolean includeVideoImages = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.includeVideoImages", "false"));

    private static final Map<String, Pattern> TYPE_SUFFIX_MAP = new HashMap<String, Pattern>() {
        {
            String scannerTypes = PropertiesUtil.getProperty("filename.scanner.types", "ZCD,VOD,RAR");

            HashMap<String, String> scannerTypeDefaults = new HashMap<String, String>() {
                {
                    put("ZCD","ISO,IMG,VOB,MDF,NRG,BIN");
                    put("VOD","");
                    put("RAR","RAR");
                }
            };

            for (String s : scannerTypes.split(",")) {
                // Set the default the long way to allow 'keyword.???=' to blank the value instead of using default
                String mappedScannerTypes = PropertiesUtil.getProperty("filename.scanner.types." + s, null);
                if (null == mappedScannerTypes) {
                    mappedScannerTypes = scannerTypeDefaults.get(s);
                }

                String patt = s;
                if (null != mappedScannerTypes && mappedScannerTypes.length() > 0) {
                    for (String t : mappedScannerTypes.split(",")) {
                        patt += "|" + t;
                    }
                }
                put(s, MovieFilenameScanner.iwpatt(patt));
            }
        }
    };


    @XmlElement(name = "fileURL")
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getPlot(int part) {
        String plot = plots.get(part);
        return plot != null ? plot : Movie.UNKNOWN;
    }

    public void setPlot(int part, String plot) {
        if (plot == null || plot.isEmpty()) {
            plot = Movie.UNKNOWN;
        }
        plots.put(part, plot);
    }

    public String getVideoImageURL(int part) {
        String url = videoImageURL.get(part);
        return url != null ? url : Movie.UNKNOWN;
    }

    public String getVideoImageFilename(int part) {
        String file = videoImageFilename.get(part);
        return file != null ? file : Movie.UNKNOWN;
    }

    public void setVideoImageURL(int part, String videoImageURL) {
        if (videoImageURL == null || videoImageURL.isEmpty()) {
            videoImageURL = Movie.UNKNOWN;
        }
        this.videoImageURL.put(part, videoImageURL);
        // Clear the videoImageFilename associated with this part
        this.videoImageFilename.put(part, Movie.UNKNOWN);
    }

    public void setVideoImageFilename(int part, String videoImageFilename) {
        if (videoImageFilename == null || videoImageFilename.isEmpty()) {
            videoImageFilename = Movie.UNKNOWN;
        }
        this.videoImageFilename.put(part, videoImageFilename);
    }

    @XmlAttribute
    public int getFirstPart() {
        return firstPart;
    }

    @XmlAttribute
    public int getLastPart() {
        return lastPart;
    }

    public void setPart(int part) {
        firstPart = lastPart = part;
    }

    public void setFirstPart(int part) {
        firstPart = part;
        if (firstPart > lastPart)
            lastPart = firstPart;
    }

    public void setLastPart(int part) {
        lastPart = part;
    }

    @XmlAttribute
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null) {
            this.title = Movie.UNKNOWN;
        } else {
            this.title = title;
        }
    }

/**
 * http://www.networkedmediatank.com/archive/index.php/thread-111.html
 * 
 * (08-10-2008 04:53 AM)tux99 Wrote: [ -> ]What is zcd="2" supposed to do? I haven't seen it in the technical docs.
        It's supposed to launch the amp_test app, used by PCH to play DVD structures.
        
        But if I remember well, it doesn't like relative path name.
        Try with a full pathname :
        Code:
        <a href="file:///opt/sybhttpd/localhost.drives/[NFS] ServerName::ShareName/DvdRepository/VIDEO_TS" TVID="PLAY" zcd="2" vod><b>P L A Y</b></a>
 * 
 * 
 * 
                        <xsl:if test="substring(.,string-length(.)-2) = 'ISO'">
                          <xsl:attribute name="zcd">2</xsl:attribute>
                        </xsl:if>
                        <xsl:if test="substring(.,string-length(.)-2) = 'iso'">
                          <xsl:attribute name="zcd">2</xsl:attribute>
                        </xsl:if>
                        <xsl:if test="substring(.,string-length(.)-2) = 'IMG'">
                          <xsl:attribute name="zcd">2</xsl:attribute>
                        </xsl:if>
                        <xsl:if test="substring(.,string-length(.)-2) = 'img'">
                          <xsl:attribute name="zcd">2</xsl:attribute>
                        </xsl:if>
                        <xsl:if test="substring(.,string-length(.)-7) = 'VIDEO_TS'">
                          <xsl:attribute name="zcd">2</xsl:attribute>
                        </xsl:if>
                        <!-- For BluRay playback on the C-200 -->
                        <xsl:if test="substring(.,string-length(.)) = '/'">
                          <xsl:attribute name="zcd">2</xsl:attribute>
                        </xsl:if>
 */
    @XmlAttribute
    public boolean isDiscImage() {
        if (filename == null) {
            return false;
        }
        String fn = filename.toUpperCase();
        return fn.endsWith("ISO") 
            || fn.endsWith("IMG") 
            || fn.endsWith("VIDEO_TS") 
            // For BluRay playback on the C-200
            || fn.endsWith("/"); 
    }

    public boolean hasTitle() {
        return !title.equals(Movie.UNKNOWN);
    }

    @XmlAttribute
    public boolean isNewFile() {
        return newFile;
    }

    public void setNewFile(boolean newFile) {
        this.newFile = newFile;
    }

    @XmlAttribute
    @XmlJavaTypeAdapter(BooleanYesNoAdapter.class)
    public Boolean isSubtitlesExchange() {
        return subtitlesExchange;
    }

    public void setSubtitlesExchange(boolean subtitlesExchange) {
        this.subtitlesExchange = subtitlesExchange;
    }

    @XmlAttribute
    public long getSize() {
        if (getFile() == null) return 0;
        return getFile().length();
    }

    /*
     * Compares this object with the specified object for order. Returns a negative integer, zero, or a positive integer as this object is less than, equal to,
     * or greater than the specified object. The implementor must ensure sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) for all x and y. (This implies that
     * x.compareTo(y) must throw an exception if y.compareTo(x) throws an exception.)
     * 
     * The implementor must also ensure that the relation is transitive: (x.compareTo(y)>0 && y.compareTo(z)>0) implies x.compareTo(z)>0.
     * 
     * Finally, the implementor must ensure that x.compareTo(y)==0 implies that sgn(x.compareTo(z)) == sgn(y.compareTo(z)), for all z.
     * 
     * It is strongly recommended, but not strictly required that (x.compareTo(y)==0) == (x.equals(y)). Generally speaking, any class that implements the
     * Comparable interface and violates this condition should clearly indicate this fact. The recommended language is "Note: this class has a natural ordering
     * that is inconsistent with equals."
     * 
     * In the foregoing description, the notation sgn(expression) designates the mathematical signum function, which is defined to return one of -1, 0, or 1
     * according to whether the value of expression is negative, zero or positive.
     * 
     * 
     * Parameters: o - the object to be compared. Returns: a negative integer, zero, or a positive integer as this object is less than, equal to, or greater
     * than the specified object. Throws: ClassCastException - if the specified object's type prevents it from being compared to this object.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MovieFile anotherMovieFile) {
        return this.getFirstPart() - anotherMovieFile.getFirstPart();
    }

    @XmlTransient
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void mergeFileNameDTO(MovieFileNameDTO dto) {
        
        info = dto;
        
        // TODO do not skip titles (store all provided)
        setTitle(dto.isExtra() ? dto.getPartTitle() : (dto.getSeason() >= 0 ? dto.getEpisodeTitle() : dto.getPartTitle()));

        if (dto.getEpisodes().size() > 0) {
            lastPart = 1;
            firstPart = Integer.MAX_VALUE;
            for (int e : dto.getEpisodes()) {
                if (e >= lastPart) {
                    lastPart = e;
                }
                if (e <= firstPart) {
                    firstPart = e;
                }
            }

            // TODO bulletproof? :)
            if (firstPart > lastPart) {
                firstPart = lastPart;
            }

        } else if (dto.getPart() > 0) {
            firstPart = lastPart = dto.getPart();
        } else {
            firstPart = 1;
            lastPart = 1;
        }
    }

    @XmlElement public MovieFileNameDTO getInfo() {
        return info;
    }

    public void setInfo(MovieFileNameDTO info) {
        this.info = info;
    }

    public String getPlayLink() {
        if (playLink.equals("") || playLink.equals(Movie.UNKNOWN)) {
            this.playLink = calculatePlayLink();
        }
        return playLink;
    }
    
    public void setPlayLink(String playLink) {
        if (playLink == null || playLink.equals("")) {
            this.playLink = calculatePlayLink();
        } else {
            this.playLink = playLink;
        }
    }
    
    public static class PartDataDTO {
        @XmlAttribute public int part;
        @XmlValue public String value;
    }
    
    @XmlElement(name = "filePlot")
    public List<PartDataDTO> getFilePlots() {
        if (!includeEpisodePlots) return null;
        return toPartDataList(plots);
    }
    
    public void setFilePlots(List<PartDataDTO> list) {
        fromPartDataList(plots, list);
    }

    @XmlElement(name = "fileImageURL")
    public List<PartDataDTO> getFileImageUrls() {
        if (!includeVideoImages) return null;
        return toPartDataList(videoImageURL);
    }
    
    public void setFileImageUrls(List<PartDataDTO> list) {
        fromPartDataList(videoImageURL, list);
    }

    @XmlElement(name = "fileImageFile")
    public List<PartDataDTO> getFileImageFiles() {
        if (!includeVideoImages) return null;
        return toPartDataList(videoImageFilename);
    }
    
    public void setFileImageFiles(List<PartDataDTO> list) {
        fromPartDataList(videoImageFilename, list);
    }
    
    private static List<PartDataDTO> toPartDataList(LinkedHashMap<Integer, String> map) {
        List<PartDataDTO> list = new ArrayList<PartDataDTO>();
        for (Integer part : map.keySet()) {
            PartDataDTO p = new PartDataDTO();
            p.part = part;
            p.value = map.get(part);
            list.add(p);
        }
        return list;
    }
    
    private static void fromPartDataList(LinkedHashMap<Integer, String> map, List<PartDataDTO> list) {
        map.clear();
        for (PartDataDTO p : list) {
            map.put(p.part, p.value);
        }
    }
    
    
    /**
     * Calculate the playlink additional information for the file
     * 
     * TODO: Check if the method is used anywhere (impossible to use in XSL). Remove.
     */
    private String calculatePlayLink() {
        File file = getFile();
        //String filename = file.getName();
        String returnValue = "";
        
        if (playFullBluRayDisk && file.getAbsolutePath().toUpperCase().contains("BDMV")) {
            //System.out.println(filename + " matched to BLURAY");
            returnValue += PropertiesUtil.getProperty("filename.scanner.types.suffix.BLURAY", "zcd=2") + " ";
            // We can return at this point because there won't be additional playlinks
            return returnValue.trim();
        }
        
        if (file.isDirectory() && (new File(file.getAbsolutePath() + File.separator + "VIDEO_TS").exists())) {
            //System.out.println(filename + " matched to VIDEO_TS");
            returnValue += PropertiesUtil.getProperty("filename.scanner.types.suffix.VIDEO_TS", "zcd=2") + " ";
            // We can return at this point because there won't be additional playlinks
            return returnValue.trim();
        }

        for (Map.Entry<String, Pattern> e : TYPE_SUFFIX_MAP.entrySet()) {
            Matcher matcher = e.getValue().matcher(getExtension(file));
            if (matcher.find()) {
                //System.out.println(filename + " matched to " + e.getKey());
                returnValue += PropertiesUtil.getProperty("filename.scanner.types.suffix." + e.getKey(), "") + " ";
            }
        }
        
        // Default to VOD if there's no other type found
        if (returnValue.equals("")) {
            //System.out.println(filename + " not matched, defaulted to VOD");
            returnValue += PropertiesUtil.getProperty("filename.scanner.types.suffix.VOD", "vod") + " ";
        }
        
        return returnValue.trim();
    }

    private String getExtension(File file) {
        String filename = file.getName();
        
        if (file.isFile()) {
            int i = filename.lastIndexOf(".");
            if (i > 0) {
                return (filename.substring(i + 1));
            }
        }
        return "";
    }
}