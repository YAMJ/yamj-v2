package com.moviejukebox.plugin;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.GraphicTools;

public class DefaultPosterPlugin implements MovieImagePlugin {

	private static Logger logger = Logger.getLogger("moviejukebox");

	private String skinHome; 
	private boolean addReflectionEffect;
	private boolean addPerspective;
	private boolean normalizePosters;
	private boolean addHDLogo;
	private int posterWidth;
	private int posterHeight;

	@Override
	public void init(Properties props) {
		skinHome = props.getProperty("mjb.skin.dir", "./skins/default");
		posterWidth = Integer.parseInt(props.getProperty("posters.width", "400"));
		posterHeight = Integer.parseInt(props.getProperty("posters.height", "600"));
		addReflectionEffect = Boolean.parseBoolean(props.getProperty("posters.reflection", "true"));
		addPerspective = Boolean.parseBoolean(props.getProperty("posters.perspective", "true"));
		normalizePosters = Boolean.parseBoolean(props.getProperty("posters.normalize", "false"));
		addHDLogo = Boolean.parseBoolean(props.getProperty("posters.logoHD", "false"));
	}

	@Override
	public BufferedImage generate(Movie movie, BufferedImage moviePoster) {
		BufferedImage bi = moviePoster;
		
		if (normalizePosters) {
			bi = GraphicTools.scaleToSizeNormalized(posterWidth, posterHeight, bi);
		} else {
			bi = GraphicTools.scaleToSizeBestFit(posterWidth, posterHeight, bi);
		}

		if (addHDLogo) {
			bi = drawLogoHD(movie, bi);
		}

		if (addReflectionEffect) {
			bi = GraphicTools.createReflectedPicture(bi);
		}
		
		if (addPerspective) {
			bi = GraphicTools.create3DPicture(bi);
		}

		return bi;
	}

	private BufferedImage drawLogoHD(Movie movie, BufferedImage bi) {
		String videoOutput = movie.getVideoOutput();
		if (  videoOutput.indexOf("720") != -1   
		   || videoOutput.indexOf("1080") != -1) {
			
			try {
				InputStream in = new FileInputStream(skinHome + File.separator + "resources" + File.separator + "hd.png");
				BufferedImage biHd = ImageIO.read(in);
				Graphics g = bi.getGraphics();
				g.drawImage(biHd, bi.getWidth() / 2 - biHd.getWidth() / 2, bi.getHeight() - biHd.getHeight() - 5, null);
			} catch (IOException e) {
				logger.warning("Failed drawing HD logo to thumbnail file: Please check that hd.png is in the resources directory.");
				e.printStackTrace();
			}
		}
		
		return bi;
	}
}
