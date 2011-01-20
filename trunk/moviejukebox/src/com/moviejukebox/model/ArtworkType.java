package com.moviejukebox.model;

public enum ArtworkType {
    // Define the lowercase equivalents of the Enum names
    Poster("poster"),
    Fanart("fanart"),
    Banner("banner"),
    // VideoImage("videoimage"),    // We don't store VideoImages in this artwork
    ClearArt("clearart"),
    ClearLogo("clearlogo"),
    TvThumb("tvthumb"),
    SeasonThumb("seasonthumb");
    
    private String type;
    
    /**
     * Constructor
     * @param type
     */
    private ArtworkType(String type) {
        this.type = type;
    }
    
    public String getType() {
        return this.type;
    }
    
    public static ArtworkType fromString(String type) {
        if (type != null) {
            for (ArtworkType artworkType : ArtworkType.values()) {
                if (type.equalsIgnoreCase(artworkType.type)) {
                    return artworkType;
                }
            }
        }
        // We've not found the type, so raise an exception
        throw new IllegalArgumentException("No ArtworkType " + type + " exists");
    }

}
