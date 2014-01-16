/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import com.moviejukebox.model.Movie;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

public class AspectRatioTools {

    private static List<AspectRatio> aspectList = new ArrayList<AspectRatio>();
    private static int aspectRationPrecision = PropertiesUtil.getIntProperty("mjb.aspectRatioPrecision", 3);
    private static DecimalFormatSymbols symbols = new DecimalFormatSymbols();

    public AspectRatioTools() {
        // Don't create the aspectList if it's already populated
        if (aspectList == null || aspectList.isEmpty()) {
            createAspectList();

            if (aspectRationPrecision > 3) {
                aspectRationPrecision = 3;
            }

            if (aspectRationPrecision < 1) {
                aspectRationPrecision = 1;
            }

            // Use the "." as a decimal format separator, ignoring localisation
            symbols.setDecimalSeparator('.');
        }
    }

    /**
     * Fully clean the aspect ratio
     *
     * @param uncleanRatio
     * @return
     */
    public String cleanAspectRatio(String uncleanRatio) {
        if (StringTools.isNotValidString(uncleanRatio)) {
            return Movie.UNKNOWN;
        }

        String newAspectRatio = uncleanRatio; // We can't alter the parameter, so use a new one
        boolean appendRatio = false;

        // Format the aspect slightly and change "16/9" to "16:9"
        newAspectRatio = newAspectRatio.replaceAll("/", ":");

        // If we don't have a ":" then we should add ":1" after processing
        if (!newAspectRatio.contains(":")) {
            appendRatio = true;
        }

        // Remove the ":1" at the end of the ratio so we can format it.
        // Other ":?" values should be left alone
        if (newAspectRatio.endsWith(":1")) {
            newAspectRatio = newAspectRatio.substring(0, newAspectRatio.length() - 2);
            appendRatio = true;
        }

        AspectRatio aspectRatioFind = findAspectRatio(newAspectRatio);

        // Check that we got a return value and then try and format it.
        if (aspectRatioFind != null) {
            switch (aspectRationPrecision) {
                case 1:
                    newAspectRatio = new DecimalFormat("#.0", symbols).format(aspectRatioFind.getRatio1digit());
                    break;
                case 2:
                    newAspectRatio = new DecimalFormat("#.00", symbols).format(aspectRatioFind.getRatio2digit());
                    break;
                default:
                    newAspectRatio = new DecimalFormat("#.000", symbols).format(aspectRatioFind.getRatio3digit());
                    break;
            }
        }

        // Add the ratio back to the end of the value if needed
        if (appendRatio) {
            newAspectRatio = newAspectRatio + ":1";
        }

        return newAspectRatio;
    }

    /**
     * Return the AspectRatio object for the corresponding ratio value
     *
     * @param ratioValue
     * @return
     */
    public AspectRatio findAspectRatio(String ratioValue) {
        try {
            return findAspectRatio(Float.parseFloat(ratioValue));
        } catch (NumberFormatException error) {
            return null;
        }
    }

    /**
     * Return the AspectRatio object for the corresponding ratio value
     *
     * @param ratioValue
     * @return
     */
    public AspectRatio findAspectRatio(float ratioValue) {
        AspectRatio aspectRatioFind = null;
        boolean found = false;

        for (int looper = 0; looper < aspectList.size(); looper++) {
            aspectRatioFind = aspectList.get(looper);
            if (aspectRatioFind.testAspectRatio(ratioValue)) {
                found = true;
                break;
            }
        }

        if (!found && aspectRatioFind != null) {
            if (ratioValue > aspectRatioFind.getMaxFloat()) {
                // Too big! Use last value
                return aspectRatioFind;
            } else if (ratioValue < aspectList.get(0).getMinFloat()) {
                // Too small! Use lowest value
                return aspectList.get(0);
            } else {
                // Some other error!
                return null;
            }
        }

        return aspectRatioFind;
    }

    /**
     * Populate the aspect list with the values These are static values because calculated values give slight wrong figures
     *
     * @param aspectList
     */
    private void createAspectList() {
        aspectList.add(new AspectRatio("1:1", 1.000f, 1.075f, 1.000f, 1.00f, 1.0f));
        aspectList.add(new AspectRatio("Movietone", 1.076f, 1.200f, 1.150f, 1.15f, 1.2f));
        aspectList.add(new AspectRatio("SVGA", 1.201f, 1.292f, 1.250f, 1.25f, 1.3f));
        aspectList.add(new AspectRatio("SDTV", 1.293f, 1.352f, 1.333f, 1.33f, 1.3f));
        aspectList.add(new AspectRatio("Academy Ratio", 1.353f, 1.400f, 1.370f, 1.37f, 1.4f));
        aspectList.add(new AspectRatio("IMAX", 1.401f, 1.465f, 1.430f, 1.43f, 1.4f));
        aspectList.add(new AspectRatio("VistaVision", 1.466f, 1.510f, 1.500f, 1.50f, 1.5f));
        aspectList.add(new AspectRatio("Demeny Gaumont", 1.511f, 1.538f, 1.520f, 1.52f, 1.5f));
        aspectList.add(new AspectRatio("Widescreen", 1.539f, 1.578f, 1.555f, 1.56f, 1.6f));
        aspectList.add(new AspectRatio("Widescreen PC", 1.579f, 1.633f, 1.600f, 1.60f, 1.6f));
        aspectList.add(new AspectRatio("15:9", 1.634f, 1.698f, 1.666f, 1.66f, 1.7f));
        aspectList.add(new AspectRatio("Natural Vision", 1.699f, 1.740f, 1.730f, 1.73f, 1.7f));
        aspectList.add(new AspectRatio("Widescreen", 1.741f, 1.764f, 1.750f, 1.75f, 1.8f));
        aspectList.add(new AspectRatio("HDTV (16:9)", 1.765f, 1.814f, 1.777f, 1.77f, 1.8f));
        aspectList.add(new AspectRatio("Cinema film", 1.815f, 1.925f, 1.850f, 1.85f, 1.9f));
        aspectList.add(new AspectRatio("SuperScope Univisium", 1.926f, 2.025f, 2.000f, 2.00f, 2.0f));
        aspectList.add(new AspectRatio("Magnifilm", 2.026f, 2.075f, 2.050f, 2.05f, 2.1f));
        aspectList.add(new AspectRatio("11:10", 2.076f, 2.115f, 2.100f, 2.10f, 2.1f));
        aspectList.add(new AspectRatio("Fox Grandeur", 2.116f, 2.155f, 2.130f, 2.13f, 2.1f));
        aspectList.add(new AspectRatio("Magnifilm", 2.156f, 2.190f, 2.180f, 2.18f, 2.2f));
        aspectList.add(new AspectRatio("70mm standard", 2.191f, 2.205f, 2.200f, 2.20f, 2.2f));
        aspectList.add(new AspectRatio("MPEG-2 for 2.20", 2.206f, 2.272f, 2.210f, 2.21f, 2.2f));
        aspectList.add(new AspectRatio("21:9", 2.273f, 2.342f, 2.333f, 2.33f, 2.3f));
        aspectList.add(new AspectRatio("CinemaScope", 2.343f, 2.360f, 2.350f, 2.35f, 2.4f));
        aspectList.add(new AspectRatio("21:9 Cinema Display", 2.361f, 2.380f, 2.370f, 2.37f, 2.4f));
        aspectList.add(new AspectRatio("Scope", 2.381f, 2.395f, 2.390f, 2.39f, 2.4f));
        aspectList.add(new AspectRatio("Scope", 2.396f, 2.460f, 2.400f, 2.40f, 2.4f));
        aspectList.add(new AspectRatio("Panoramico Alberini", 2.461f, 2.535f, 2.520f, 2.52f, 2.5f));
        aspectList.add(new AspectRatio("Original CinemaScope", 2.536f, 2.555f, 2.550f, 2.55f, 2.6f));
        aspectList.add(new AspectRatio("Original CinemaScope", 2.556f, 2.575f, 2.560f, 2.56f, 2.6f));
        aspectList.add(new AspectRatio("Cinerama full", 2.576f, 2.595f, 2.590f, 2.59f, 2.6f));
        aspectList.add(new AspectRatio("Cinemiracle", 2.596f, 2.633f, 2.600f, 2.60f, 2.6f));
        aspectList.add(new AspectRatio("Super 16mm", 2.634f, 2.673f, 2.666f, 2.66f, 2.7f));
        aspectList.add(new AspectRatio("Cinerama", 2.674f, 2.720f, 2.680f, 2.68f, 2.7f));
        aspectList.add(new AspectRatio("Ultra Panavision 70", 2.721f, 2.825f, 2.760f, 2.76f, 2.8f));
        aspectList.add(new AspectRatio("Ultra Panavision 70", 2.826f, 2.910f, 2.890f, 2.89f, 2.9f));
        aspectList.add(new AspectRatio("MGM Camera 65", 2.911f, 2.965f, 2.930f, 2.93f, 2.9f));
        aspectList.add(new AspectRatio("3:1", 2.966f, 3.500f, 3.000f, 3.00f, 3.0f));
        aspectList.add(new AspectRatio("PolyVision", 3.501f, 8.000f, 4.000f, 4.00f, 4.0f));
        aspectList.add(new AspectRatio("Circle Vision 360°", 8.001f, 12.000f, 12.000f, 12.00f, 12.0f));
    }

    /**
     * The aspect ratio class.
     *
     * @author stuart.boston
     *
     */
    public class AspectRatio {

        private String ratioName;
        private float minFloat;
        private float maxFloat;
        private float ratio3digit;
        private float ratio2digit;
        private float ratio1digit;

        /**
         * Constructor for Aspect Ratio
         *
         * @param ratioName
         * @param minFloat
         * @param maxFloat
         * @param ratio3digit
         * @param ratio2digit
         * @param ratio1digit
         */
        public AspectRatio(String ratioName, float minFloat, float maxFloat, float ratio3digit, float ratio2digit, float ratio1digit) {
            this.ratioName = ratioName;
            this.minFloat = minFloat;
            this.maxFloat = maxFloat;
            this.ratio3digit = ratio3digit;
            this.ratio2digit = ratio2digit;
            this.ratio1digit = ratio1digit;
        }

        /**
         * Default constructor
         */
        public AspectRatio() {
        }

        /**
         * Test to see if a passed value is in the range of this aspect ratio
         *
         * @param ratioValue
         * @return
         */
        public boolean testAspectRatio(float ratioValue) {
            return (ratioValue >= this.minFloat && ratioValue <= this.maxFloat);
        }

        public String getRatioName() {
            return ratioName;
        }

        public float getMinFloat() {
            return minFloat;
        }

        public float getMaxFloat() {
            return maxFloat;
        }

        public float getRatio3digit() {
            return ratio3digit;
        }

        public float getRatio2digit() {
            return ratio2digit;
        }

        public float getRatio1digit() {
            return ratio1digit;
        }

        public void setRatioName(String ratioName) {
            this.ratioName = ratioName;
        }

        public void setMinFloat(float minFloat) {
            this.minFloat = minFloat;
        }

        public void setMaxFloat(float maxFloat) {
            this.maxFloat = maxFloat;
        }

        public void setRatio3digit(float ratio3digit) {
            this.ratio3digit = ratio3digit;
        }

        public void setRatio2digit(float ratio2digit) {
            this.ratio2digit = ratio2digit;
        }

        public void setRatio1digit(float ratio1digit) {
            this.ratio1digit = ratio1digit;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ratioName=");
            builder.append(ratioName);
            builder.append(", minFloat=");
            builder.append(minFloat);
            builder.append(", maxFloat=");
            builder.append(maxFloat);
            builder.append(", ratio3digit=");
            builder.append(ratio3digit);
            builder.append(", ratio2digit=");
            builder.append(ratio2digit);
            builder.append(", ratio1digit=");
            builder.append(ratio1digit);
            return builder.toString();
        }
    }
}
