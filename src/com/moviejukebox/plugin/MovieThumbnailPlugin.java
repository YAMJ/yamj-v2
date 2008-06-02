package com.moviejukebox.plugin;

import java.awt.image.BufferedImage;
import java.util.Properties;

public interface MovieThumbnailPlugin {
	/**
	 * Called by movie jukebox at program initialisation.
	 * Contains the moviejukebox.properties
	 * 
	 * @param props moviejukebox properties
	 */
	public void init(Properties props);

	public BufferedImage generate(BufferedImage moviePoster);
}
