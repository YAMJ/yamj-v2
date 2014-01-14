package com.moviejukebox.model.enumerations;

import java.util.EnumSet;
import org.apache.commons.lang3.StringUtils;

/**
 * Check the watched file with the extension of the video, without or both
 *
 * @author Stuart
 */
public enum WatchedWithExtension {

    EXTENSION("true"),
    NOEXTENSION("false"),
    BOTH("both");
    private final String type;

    private WatchedWithExtension(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    /**
     * Convert a string into an Enum type
     *
     * @param extensionString
     * @return
     */
    public static WatchedWithExtension fromString(String extensionString) {
        if (StringUtils.isNotBlank(extensionString)) {
            for (final WatchedWithExtension extension : EnumSet.allOf(WatchedWithExtension.class)) {
                if (extensionString.equalsIgnoreCase(extension.type)) {
                    return extension;
                }
            }
        }
        // We've not found the type, so return both
        return BOTH;
    }
}
