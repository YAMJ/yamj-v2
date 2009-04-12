package com.moviejukebox.model;

import java.io.File;
import java.util.LinkedHashMap;

public class MovieFile implements Comparable<MovieFile> {

    private String filename = Movie.UNKNOWN;
    private String title = Movie.UNKNOWN;
    private int firstPart = 1; // #1, #2, CD1, CD2, etc.
    private int lastPart = 1;
    private boolean newFile = true; // is new file or already exists in XML data
    private boolean subtitlesExchange = false; // is the subtitles for this file already downloaded/uploaded to the server
    private LinkedHashMap<Integer, String> plots = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> videoImageURL = new LinkedHashMap<Integer, String>();
    private LinkedHashMap<Integer, String> videoImageFile = new LinkedHashMap<Integer, String>();
    private File file;

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

    public String getVideoImageFile(int part) {
        String file = videoImageFile.get(part);
        return file != null ? file : Movie.UNKNOWN;
    }

    public void setVideoImageURL(int part, String videoImageURL) {
        if (videoImageURL == null || videoImageURL.isEmpty()) {
            videoImageURL = Movie.UNKNOWN;
        }
        this.videoImageURL.put(part, videoImageURL);
        // Clear the videoImageFile associated with this part
        this.videoImageFile.put(part, Movie.UNKNOWN);
    }

    public void setVideoImageFile(int part, String videoImageFile) {
        if (videoImageFile == null || videoImageFile.isEmpty()) {
            videoImageFile = Movie.UNKNOWN;
        }
        this.videoImageFile.put(part, videoImageFile);
    }

    public int getFirstPart() {
        return firstPart;
    }

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

    public boolean hasTitle() {
        return !title.equals(Movie.UNKNOWN);
    }

    public boolean isNewFile() {
        return newFile;
    }

    public void setNewFile(boolean newFile) {
        this.newFile = newFile;
    }

    public boolean isSubtitlesExchange() {
        return subtitlesExchange;
    }

    public void setSubtitlesExchange(boolean subtitlesExchange) {
        this.subtitlesExchange = subtitlesExchange;
    }

    /*
     * Compares this object with the specified object for order. Returns a negative integer, zero, or a positive integer as this object is less than, equal to,
     * or greater than the specified object. The implementor must ensure sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) for all x and y. (This implies that
     * x.compareTo(y) must throw an exception iff y.compareTo(x) throws an exception.)
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

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

	public void mergeFileNameDTO(MovieFileNameDTO dto) {
        // TODO do not skip titles (store all provided)
        setTitle(dto.isTrailer() ? dto.getTrailerTitle() : (dto.getSeason() >= 0 ? dto.getEpisodeTitle() : dto.getPartTitle()));

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
}