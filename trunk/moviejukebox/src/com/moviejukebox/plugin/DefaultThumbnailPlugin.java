package com.moviejukebox.plugin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.GraphicTools;

public class DefaultThumbnailPlugin implements MovieThumbnailPlugin {

	private static Logger logger = Logger.getLogger("moviejukebox");

	private String skinHome; 
	private boolean createReflectionEffect;
	private boolean create3DEffect;
	private boolean normalizeThumbnails;
	private boolean drawLogoHD;
	private int thumbWidth;
	private int thumbHeight;

	@Override
	public void init(Properties props) {
		skinHome = props.getProperty("mjb.skin.dir", "./skins/default");
		thumbWidth = Integer.parseInt(props.getProperty("thumbnails.width", "180"));
		thumbHeight = Integer.parseInt(props.getProperty("thumbnails.height", "260"));
		createReflectionEffect = Boolean.parseBoolean(props.getProperty("thumbnails.plugin.reflection", "true"));
		create3DEffect = Boolean.parseBoolean(props.getProperty("thumbnails.3D", "false"));
		normalizeThumbnails = Boolean.parseBoolean(props.getProperty("thumbnails.normalize", "false"));
		drawLogoHD = Boolean.parseBoolean(props.getProperty("thumbnails.logoHD", "false"));
	}

	@Override
	public BufferedImage generate(Movie movie, BufferedImage moviePoster) {
		BufferedImage bi = moviePoster;
		
		if (normalizeThumbnails) {
			bi = GraphicTools.scaleToSizeNormalized(thumbWidth, thumbHeight, bi);
		} else {
			bi = GraphicTools.scaleToSize(thumbWidth, thumbHeight, bi);
		}

		if (drawLogoHD) {
			bi = drawLogoHD(movie, bi);
		}

		if (createReflectionEffect) {
			bi = GraphicTools.createReflectedPicture(bi);
		}
		
		if (create3DEffect) {
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
				logger.warning("Failed drawind HD logo to thumbnail file: Please check that hd.png is in the resources directory.");
				e.printStackTrace();
			}
		}
		
		return bi;
	}
}
