package com.moviejukebox.plugin;

import java.awt.image.BufferedImage;

import com.moviejukebox.model.Movie;

public interface MovieImagePlugin {
    public BufferedImage generate(Movie movie, BufferedImage moviePoster, String perspectiveDirection);
}