package com.moviejukebox.plugin;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.moviejukebox.model.Movie;

public class DefaultThumbnailPlugin implements MovieThumbnailPlugin {

	private static Logger logger = Logger.getLogger("moviejukebox");

	private boolean createReflectionEffect;
	private boolean createGlossyEffect;
	private boolean normalizeThumbnails;
	private boolean drawLogoHD;
	private int thumbWidth;
	private int thumbHeight;

	@Override
	public void init(Properties props) {
		thumbWidth = Integer.parseInt(props.getProperty("thumbnails.width", "180"));
		thumbHeight = Integer.parseInt(props.getProperty("thumbnails.height", "260"));
		createReflectionEffect = Boolean.parseBoolean(props.getProperty("thumbnails.plugin.reflection", "true"));
		createGlossyEffect = Boolean.parseBoolean(props.getProperty("thumbnails.glossy", "false"));
		normalizeThumbnails = Boolean.parseBoolean(props.getProperty("thumbnails.normalize", "false"));
		drawLogoHD = Boolean.parseBoolean(props.getProperty("thumbnails.logoHD", "false"));
	}

	@Override
	public BufferedImage generate(Movie movie, BufferedImage moviePoster) {
		BufferedImage bi = moviePoster;
		
		if (normalizeThumbnails) {
			bi = scaleToSizeNormalized(thumbWidth, thumbHeight, bi);
		} else {
			bi = scaleToSize(thumbWidth, thumbHeight, bi);
		}

		if (drawLogoHD) {
			bi = drawLogoHD(movie, bi);
		}
		
		if (createGlossyEffect) {
			bi = createGlossyPicture(bi);
		}

		if (createReflectionEffect) {
			bi = createReflectedPicture(bi);
		}

		return bi;
	}

	private BufferedImage drawLogoHD(Movie movie, BufferedImage bi) {
		String videoOutput = movie.getVideoOutput();
		if (  videoOutput.indexOf("720") != -1   
		   || videoOutput.indexOf("1080") != -1) {
			
			try {
				InputStream in = ClassLoader.getSystemResource("hd.png").openStream();
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

	private BufferedImage cropToSize(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
		int nHeight = imgSrc.getHeight();
		int nWidth = imgSrc.getWidth();

		int x1 = 0;
		if (nWidth > nMaxWidth) {
			x1 = (nWidth - nMaxWidth) / 2;
		}

		int l = nMaxWidth;
		if (nWidth < nMaxWidth) {
			l = nWidth;
		}

		int y1 = 0;
		if (nHeight > nMaxHeight) {
			y1 = (nHeight - nMaxHeight) / 2;
		}

		int h = nMaxHeight;
		if (nHeight < nMaxHeight) {
			h = nHeight;
		}

		return imgSrc.getSubimage(x1, y1, l, h);
	}

	private BufferedImage scaleToSize(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
		/* determine thumbnail size from WIDTH and HEIGHT */
		int imageWidth = imgSrc.getWidth(null);
		int imageHeight = imgSrc.getHeight(null);
		
		int tempWidth;
		int tempHeight;
		int y = 0;
		
		tempWidth = nMaxWidth;
		tempHeight = (int) (((double) imageHeight * (double) nMaxWidth) / (double) imageWidth);

		if (nMaxHeight>tempHeight) {
		   	y = nMaxHeight-tempHeight;
		}

		Image temp1 = imgSrc.getScaledInstance(tempWidth, tempHeight, Image.SCALE_SMOOTH);
		BufferedImage bi = new BufferedImage(nMaxWidth, nMaxHeight, BufferedImage.TYPE_INT_RGB);
		bi.getGraphics().drawImage(temp1, 0, y, null);
		return bi;
	}
	
	private BufferedImage scaleToSizeNormalized(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
		/* determine thumbnail size from WIDTH and HEIGHT */
		int imageWidth = imgSrc.getWidth(null);
		int imageHeight = imgSrc.getHeight(null);
		
		double imageRatio = (double) imageHeight / (double) imageWidth;
		double thumbnailRatio = (double) nMaxHeight / (double) nMaxWidth;
		
		int tempWidth;
		int tempHeight;
		
		if (imageRatio>thumbnailRatio) {
			tempWidth = nMaxWidth;
			tempHeight = (int) (((double) imageHeight * (double) nMaxWidth) / (double) imageWidth);
		} else {
			tempWidth = (int) (((double) imageWidth * (double) nMaxHeight) / (double) imageHeight);
			tempHeight = nMaxHeight;
		}

		Image temp1 = imgSrc.getScaledInstance(tempWidth, tempHeight, Image.SCALE_SMOOTH);
		BufferedImage bi = new BufferedImage(tempWidth, tempHeight, BufferedImage.TYPE_INT_RGB);
		bi.getGraphics().drawImage(temp1, 0, 0, null);
		return cropToSize(thumbWidth, thumbHeight, bi);
	}

	public BufferedImage createReflectedPicture(BufferedImage avatar) {

		int avatarWidth = avatar.getWidth();
		int avatarHeight = avatar.getHeight();

		BufferedImage gradient = createGradientMask(avatarWidth, avatarHeight);
		BufferedImage buffer = createReflection(avatar, avatarWidth, avatarHeight);

		applyAlphaMask(gradient, buffer, avatarWidth, avatarHeight);

		return buffer;
	}

	private BufferedImage createGradientMask(int avatarWidth, int avatarHeight) {
		BufferedImage gradient = new BufferedImage(avatarWidth, avatarHeight, BufferedImage.TYPE_4BYTE_ABGR_PRE);
		Graphics2D g = gradient.createGraphics();
		GradientPaint painter = new GradientPaint(0.0f, 0.0f, new Color(1.0f, 1.0f, 1.0f, 0.3f), 0.0f, avatarHeight / 8.0f, new Color(1.0f, 1.0f, 1.0f, 1f));
		g.setPaint(painter);
		g.fill(new Rectangle2D.Double(0, 0, avatarWidth, avatarHeight));

		g.dispose();
		gradient.flush();

		return gradient;
	}

	private BufferedImage createReflection(BufferedImage avatar, int avatarWidth, int avatarHeight) {

		BufferedImage buffer = new BufferedImage(avatarWidth, avatarHeight + avatarHeight / 6, BufferedImage.TYPE_4BYTE_ABGR_PRE);
		Graphics2D g = buffer.createGraphics();

		g.drawImage(avatar, null, null);
		g.translate(0, (avatarHeight << 1) + 2);

		AffineTransform reflectTransform = AffineTransform.getScaleInstance(1.0, -1.0);
		g.drawImage(avatar, reflectTransform, null);
		g.translate(0, -(avatarHeight << 1));

		g.dispose();

		return buffer;
	}

	private void applyAlphaMask(BufferedImage gradient, BufferedImage buffer, int avatarWidth, int avatarHeight) {

		Graphics2D g2 = buffer.createGraphics();
		g2.setComposite(AlphaComposite.DstOut);
		g2.drawImage(gradient, null, 0, avatarHeight);
		g2.dispose();
	}

	public BufferedImage createGlossyPicture(BufferedImage bi) {
		Graphics2D g2 = (Graphics2D) bi.createGraphics();
		g2.setPaint(new GlossyPaint(new Color(255,255,255,128)));
		g2.fillRect(0,0,bi.getWidth(), bi.getHeight());
		g2.dispose();
		return bi;
	}

	class GlossyPaint implements Paint {

		private int gradientStrength = 50;
		private int shinePosition = 50;
		private float shineStrength = 0.3f;
		private int shineCurve = 30;
		private Color color;

		/**
		 * @param color
		 *            color of the background gradient
		 */
		public GlossyPaint(Color color) {
			if (color == null)
				throw new NullPointerException("Color cannot be null");

			this.color = color;
		}

		/**
		 * @param color
		 *            color of the background gradient
		 * @param gradientStrength
		 *            intensity of the background gradient (must be >= 0)
		 * @param shineStrength
		 *            intensity of the bright top part (must be between 0 and 1)
		 * @param shinePosition
		 *            height of the bright top part
		 * @param shineCurve
		 *            curve of the bright top part (must be >= 1)
		 */
		public GlossyPaint(Color color, int gradientStrength, float shineStrength, int shinePosition, int shineCurve) {
			if (color == null)
				throw new NullPointerException("Color cannot be null");
			if (gradientStrength < 0)
				throw new IllegalArgumentException("Gradient strength must be >= 0");
			if (shineStrength < 0 || shineStrength > 1)
				throw new IllegalArgumentException("Shine strength must be between 0 and 1");
			if (shineCurve < 1)
				throw new IllegalArgumentException("Shine curve must be >= 1");

			this.color = color;
			this.gradientStrength = gradientStrength;
			this.shineStrength = shineStrength;
			this.shinePosition = shinePosition;
			this.shineCurve = shineCurve;
		}

		public Color getColor() {
			return color;
		}

		public int getGradientStrength() {
			return gradientStrength;
		}

		public int getShineCurve() {
			return shineCurve;
		}

		public int getShinePosition() {
			return shinePosition;
		}

		public float getShineStrength() {
			return shineStrength;
		}

		public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
			return new GlossyPaintContext(deviceBounds);
		}

		public int getTransparency() {
			return color.getTransparency();
		}

		private class GlossyPaintContext implements PaintContext {

			private Rectangle2D userBounds;
			private int c, r2;
			private int r, g, b, a;

			public GlossyPaintContext(Rectangle2D userBounds) {
				this.userBounds = userBounds;
				// center of the curve
				this.c = (int) (userBounds.getY() + shinePosition + ((userBounds.getWidth() / 2.0f) * (userBounds.getWidth() / 2.0f) - (shineCurve * shineCurve))
						/ (2 * shineCurve));
				// radius² of the curve
				this.r2 = (c - shinePosition + shineCurve) * (c - shinePosition + shineCurve);
				this.r = color.getRed();
				this.g = color.getGreen();
				this.b = color.getBlue();
				this.a = color.getAlpha();
			}

			public void dispose() {
			}

			public ColorModel getColorModel() {
				return ColorModel.getRGBdefault();
			}

			public Raster getRaster(int x, int y, int w, int h) {
				WritableRaster raster = getColorModel().createCompatibleWritableRaster(w, h);
				int[] data = new int[w * h * 4];

				x = (int) (x - userBounds.getX());
				y = (int) (y - userBounds.getY());

				for (int j = 0; j < h; j++) {
					float delta = (float) gradientStrength * (j + y) / (float) userBounds.getHeight(); // for
																										// the
																										// background
																										// gradient
					int valR = (int) (r - delta);
					valR = valR < 0 ? 0 : (valR > 255 ? 255 : valR);
					int valG = (int) (g - delta);
					valG = valG < 0 ? 0 : (valG > 255 ? 255 : valG);
					int valB = (int) (b - delta);
					valB = valB < 0 ? 0 : (valB > 255 ? 255 : valB);
					for (int i = 0; i < w; i++) {
						int base = (j * w + i) * 4;
						data[base] = valR;
						data[base + 1] = valG;
						data[base + 2] = valB;
						data[base + 3] = 0;

						// if in the bright top part :
						if (j + y <= shinePosition)
							if (j + y <= c - Math.sqrt(r2 - (i + x - userBounds.getWidth() / 2.0f) * (i + x - userBounds.getWidth() / 2.0f))) {
								data[base] = (int) (data[base] + (255 - data[base]) * shineStrength);
								data[base + 1] = (int) (data[base + 1] + (255 - data[base + 1]) * shineStrength);
								data[base + 2] = (int) (data[base + 2] + (255 - data[base + 2]) * shineStrength);
								data[base + 3] = a;
							}

					}
				}

				raster.setPixels(0, 0, w, h, data);
				return raster;
			}
		}
	}
}
