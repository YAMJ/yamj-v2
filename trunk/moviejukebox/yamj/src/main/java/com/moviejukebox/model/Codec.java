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
package com.moviejukebox.model;

import com.moviejukebox.scanner.MovieFilenameScanner;
import com.moviejukebox.tools.StringTools;
import org.apache.commons.lang3.StringUtils;

public class Codec {

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
    public static final String MI_CODEC_CHANNELS = "Channel(s)";
    public static final String MI_CODEC_BITRATE = "Bit rate";
    public static final String MI_CODEC_NOMINAL_BITRATE = "Nominal bit rate";
    public static final String MI_CODEC_OVERALL_BITRATE = "Overall bit rate";

    /**
     * Constructor with just the codec type
     *
     * @param codecType
     */
    public Codec(CodecType codecType) {
        this.codecType = codecType;
    }

    /**
     * Default constructor for use by XML bindings.
     *
     * Not intended for use by anything else.
     */
    protected Codec() {
        this.codecType = CodecType.AUDIO;
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
    }

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
        if (StringUtils.isBlank(codecLanguage)) {
            this.codecLanguage = Movie.UNKNOWN;
        } else {
            this.codecLanguage = codecLanguage;
            if (StringTools.isNotValidString(codecFullLanguage)) {
                this.codecFullLanguage = MovieFilenameScanner.determineLanguage(codecLanguage);
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

    public enum CodecType {

        AUDIO,
        VIDEO;
    };

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
        sb.append("]]");
        return sb.toString();
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
        if ((this.codec == null) ? (other.codec != null) : !this.codec.equals(other.codec)) {
            return false;
        }
        if ((this.codecFormat == null) ? (other.codecFormat != null) : !this.codecFormat.equals(other.codecFormat)) {
            return false;
        }
        if ((this.codecId == null) ? (other.codecId != null) : !this.codecId.equals(other.codecId)) {
            return false;
        }
        if ((this.codecIdHint == null) ? (other.codecIdHint != null) : !this.codecIdHint.equals(other.codecIdHint)) {
            return false;
        }
        if ((this.codecFormatVersion == null) ? (other.codecFormatVersion != null) : !this.codecFormatVersion.equals(other.codecFormatVersion)) {
            return false;
        }
        if ((this.codecFormatProfile == null) ? (other.codecFormatProfile != null) : !this.codecFormatProfile.equals(other.codecFormatProfile)) {
            return false;
        }
        if ((this.codecLanguage == null) ? (other.codecLanguage != null) : !this.codecLanguage.equals(other.codecLanguage)) {
            return false;
        }
        if (this.codecChannels != other.codecChannels) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + (this.codecType != null ? this.codecType.hashCode() : 0);
        hash = 17 * hash + (this.codec != null ? this.codec.hashCode() : 0);
        hash = 17 * hash + (this.codecFormat != null ? this.codecFormat.hashCode() : 0);
        hash = 17 * hash + (this.codecId != null ? this.codecId.hashCode() : 0);
        hash = 17 * hash + (this.codecIdHint != null ? this.codecIdHint.hashCode() : 0);
        hash = 17 * hash + (this.codecFormatVersion != null ? this.codecFormatVersion.hashCode() : 0);
        hash = 17 * hash + (this.codecFormatProfile != null ? this.codecFormatProfile.hashCode() : 0);
        hash = 17 * hash + (this.codecLanguage != null ? this.codecLanguage.hashCode() : 0);
        hash = 17 * hash + this.codecChannels;
        return hash;
    }
}
