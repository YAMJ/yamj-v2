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
package com.moviejukebox.model;

import com.moviejukebox.model.enumerations.CodecSource;
import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class Codec {

    private static final String AUDIO_LANGUAGE_UNKNOWN = PropertiesUtil.getProperty("mjb.language.audio.unknown");

    /*
     * Properties
     */
    private CodecType codecType;
    private String codec = Movie.UNKNOWN;
    private String codecFormat = Movie.UNKNOWN;
    private String codecId = Movie.UNKNOWN;
    private String codecIdHint = Movie.UNKNOWN;
    private String codecFormatVersion = Movie.UNKNOWN;
    private String codecFormatProfile = Movie.UNKNOWN;
    private String codecLanguage = Movie.UNKNOWN;
    private String codecFullLanguage = Movie.UNKNOWN;   // Determined automatically from the codecLanguage
    private String codecBitRate = Movie.UNKNOWN;
    private CodecSource codecSource = CodecSource.UNKNOWN;
    private int codecChannels = 0;
    /*
     * List of the expected names in the MediaInfo data
     */
    public static final String MI_CODEC = "Codec";
    public static final String MI_CODEC_FORMAT = "Format";
    public static final String MI_CODEC_ID = "Codec ID";
    public static final String MI_CODEC_ID_HINT = "Codec ID/Hint";
    public static final String MI_CODEC_FORMAT_VERSION = "Format Version";
    public static final String MI_CODEC_FORMAT_PROFILE = "Format profile";
    public static final String MI_CODEC_LANGUAGE = "Language";
    public static final List<String> MI_CODEC_CHANNELS = new ArrayList<String>();
    public static final String MI_CODEC_BITRATE = "Bit rate";
    public static final String MI_CODEC_NOMINAL_BITRATE = "Nominal bit rate";
    public static final String MI_CODEC_OVERALL_BITRATE = "Overall bit rate";

    static {
        MI_CODEC_CHANNELS.add("Channel(s)");
        MI_CODEC_CHANNELS.add("Channel count");
    }

    /**
     * Constructor with just the codec type
     *
     * @param codecType
     */
    public Codec(CodecType codecType) {
        this.codecType = codecType;
        this.codecSource = CodecSource.UNKNOWN;
    }

    /**
     * Default constructor for use by XML bindings.
     *
     * Not intended for use by anything else.
     */
    protected Codec() {
        this.codecType = CodecType.AUDIO;
        this.codecSource = CodecSource.UNKNOWN;
    }

    /**
     * Simple constructor with just the type and codec.
     *
     * @param codecType
     * @param codec
     */
    public Codec(CodecType codecType, String codec) {
        this.codecType = codecType;
        this.codec = codec;
        this.codecSource = CodecSource.UNKNOWN;
    }

    //<editor-fold defaultstate="collapsed" desc="Setter Methods">
    public void setCodec(String codec) {
        if (StringUtils.isBlank(codec)) {
            this.codecFormat = Movie.UNKNOWN;
        } else {
            this.codec = codec;
        }
    }

    public void setCodecFormat(String codecFormat) {
        if (StringUtils.isBlank(codecFormat)) {
            this.codecFormat = Movie.UNKNOWN;
        } else {
            this.codecFormat = codecFormat;
        }
    }

    public void setCodecFormatProfile(String codecFormatProfile) {
        if (StringUtils.isBlank(codecFormatProfile)) {
            this.codecFormatProfile = Movie.UNKNOWN;
        } else {
            this.codecFormatProfile = codecFormatProfile;
        }
    }

    public void setCodecFormatVersion(String codecFormatVersion) {
        if (StringUtils.isBlank(codecFormatVersion)) {
            this.codecFormatVersion = Movie.UNKNOWN;
        } else {
            this.codecFormatVersion = codecFormatVersion;
        }
    }

    public void setCodecId(String codecId) {
        if (StringUtils.isBlank(codecId)) {
            this.codecId = Movie.UNKNOWN;
        } else {
            this.codecId = codecId;
        }
    }

    public void setCodecIdHint(String codecIdHint) {
        if (StringUtils.isBlank(codecIdHint)) {
            this.codecIdHint = Movie.UNKNOWN;
        } else {
            this.codecIdHint = codecIdHint;
        }
    }

    public void setCodecType(CodecType codecType) {
        this.codecType = codecType;
    }

    public void setCodecLanguage(String codecLanguage) {
        if (StringTools.isNotValidString(codecLanguage)) {
            if (CodecType.AUDIO.equals(this.codecType) && StringTools.isValidString(AUDIO_LANGUAGE_UNKNOWN)) {
                this.codecLanguage = AUDIO_LANGUAGE_UNKNOWN;
                if (StringTools.isNotValidString(codecFullLanguage)) {
                    this.codecFullLanguage = MovieFilenameScanner.determineLanguage(AUDIO_LANGUAGE_UNKNOWN);
                }
            } else {
                this.codecLanguage = Movie.UNKNOWN;
            }
        } else {
            this.codecLanguage = codecLanguage;
            if (StringTools.isNotValidString(codecFullLanguage)) {
                this.codecFullLanguage = MovieFilenameScanner.determineLanguage(this.codecLanguage);
            }
        }
    }

    public void setCodecBitRate(String codecBitRate) {
        if (StringUtils.isBlank(codecBitRate)) {
            this.codecBitRate = Movie.UNKNOWN;
        } else {
            this.codecBitRate = codecBitRate;
        }
    }

    public void setCodecChannels(int codecChannels) {
        this.codecChannels = codecChannels;
    }

    public void setCodecChannels(String codecChannels) {
        if (StringUtils.isNumeric(codecChannels)) {
            this.codecChannels = Integer.parseInt(codecChannels);
        }
    }

    public void setCodecFullLanguage(String codecFullLanguage) {
        this.codecFullLanguage = codecFullLanguage;
    }

    public void setCodecSource(CodecSource codecSource) {
        this.codecSource = codecSource;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Getter Methods">
    public String getCodec() {
        return codec;
    }

    public String getCodecFormat() {
        return codecFormat;
    }

    public String getCodecFormatProfile() {
        return codecFormatProfile;
    }

    public String getCodecFormatVersion() {
        return codecFormatVersion;
    }

    public String getCodecId() {
        return codecId;
    }

    public String getCodecIdHint() {
        return codecIdHint;
    }

    public CodecType getCodecType() {
        return codecType;
    }

    public String getCodecLanguage() {
        return codecLanguage;
    }

    public String getCodecBitRate() {
        return codecBitRate;
    }

    public int getCodecChannels() {
        return codecChannels;
    }

    public String getCodecFullLanguage() {
        return codecFullLanguage;
    }

    public CodecSource getCodecSource() {
        return codecSource;
    }
    //</editor-fold>

    /**
     * Check to see if this is a MediaInfo codec or an empty NFO codec
     *
     * @return
     */
    public boolean isFullCodec() {
        return (StringTools.isValidString(codecIdHint)
                && StringTools.isValidString(codecFormat)
                && StringTools.isValidString(codecId));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[Codec=");
        sb.append("[codecType=").append(codecType.toString());
        sb.append("], [codec=").append(codec);
        sb.append("], [codecFormat=").append(codecFormat);
        sb.append("], [codecId=").append(codecId);
        sb.append("], [codecIdHint=").append(codecIdHint);
        sb.append("], [codecFormatVersion=").append(codecFormatVersion);
        sb.append("], [codecFormatProfile=").append(codecFormatProfile);
        sb.append("], [codecLanguage=").append(codecLanguage);
        sb.append("], [codecBitRate=").append(codecBitRate);
        sb.append("], [codecChannels=").append(codecChannels);
        sb.append("], [codecSource=").append(codecSource.toString());
        sb.append("]]");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.codecType != null ? this.codecType.hashCode() : 0);
        hash = 17 * hash + (this.codec != null ? this.codec.hashCode() : 0);
        hash = 17 * hash + (this.codecLanguage != null ? this.codecLanguage.hashCode() : 0);
        hash = 17 * hash + (this.codecSource != null ? this.codecSource.hashCode() : 0);
        hash = 17 * hash + this.codecChannels;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final Codec other = (Codec) obj;
        if (this.codecType != other.codecType) {
            return false;
        }

        if (!StringUtils.equals(this.codec, other.codec)) {
            return false;
        }

        // Are both languages the same? or is either one "UNKNOWN" which we will treat as a match
        if (!StringUtils.equalsIgnoreCase(this.codecLanguage, other.codecLanguage)
                && StringTools.isValidString(this.codecLanguage)
                && StringTools.isValidString(other.codecLanguage)) {
            return true;
        }

        return this.codecChannels == other.codecChannels;
    }
}
