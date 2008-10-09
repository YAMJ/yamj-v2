package com.moviejukebox.model;

/**
 *
 * @author altman.matthew
 */
public class TrailerFile extends MovieFile {

    public TrailerFile() {}
    
    public TrailerFile(MovieFile mf) {
        this.setFilename(mf.getFilename());
        this.setPart(mf.getPart());
        this.setTitle(mf.getTitle());
        this.setNewFile(mf.isNewFile());
    }
    
    @Override
    public int compareTo(MovieFile that) {
        return this.getFilename().compareToIgnoreCase(that.getFilename());
    }
}
