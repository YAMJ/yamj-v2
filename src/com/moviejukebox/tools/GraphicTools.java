package com.moviejukebox.tools;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.jhlabs.image.PerspectiveFilter;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

public class GraphicTools {

	private static Logger logger = Logger.getLogger("moviejukebox");

	////////////////////////////////////////////////////////////////////////////////////////////
	/// Loading / Saving
	//

	public static BufferedImage loadJPEGImage(InputStream fis) {
		// Create BufferedImage
		BufferedImage bi = null;
		try {
			// load file from disk using Sun's JPEGIMageDecoder
			JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(fis);
			bi = decoder.decodeAsBufferedImage();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (Exception e) {
				}
			}
		}
		return bi;
	}

	public static void saveImageAsJpeg(BufferedImage bi, String str) {
		if (bi == null || str == null)
			return;
	
		// save image as Jpeg
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(str);
			JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
			JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(bi);
			param.setQuality(0.95f, false);
	
	        BufferedImage bufImage = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
	        bufImage.createGraphics().drawImage(bi, 0, 0, null, null);
			
			encoder.encode(bufImage);
	
		} catch (Exception e) {
			logger.severe("Failed Saving thumbnail file: " + str);
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
				}
			}
		}
	}

	public static void saveImageAsPng(BufferedImage bi, String str) {
		if (bi == null || str == null)
			return;
	
		// save image as PNG
		try {
			ImageIO.write(bi, "png", new File(str));
		} catch (Exception e) {
			logger.severe("Failed Saving thumbnail file: " + str);
			e.printStackTrace();
		} 
	}

	public static void saveImageToDisk(BufferedImage bi, String str) {
		if (str.endsWith("jpg") | str.endsWith("jpeg")) {
			saveImageAsJpeg(bi,str);
		} else if (str.endsWith("png")) {
			saveImageAsPng(bi,str);
		} else {
			saveImageAsJpeg(bi,str);
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/// Bicubic image scaling
	//

	public static BufferedImage scaleToSize(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
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
	
	public static BufferedImage scaleToSizeBestFit(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
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
		BufferedImage bi = new BufferedImage(tempWidth, tempHeight, BufferedImage.TYPE_INT_RGB);
		bi.getGraphics().drawImage(temp1, 0, 0, null);
		return bi;
	}
	
	public static BufferedImage scaleToSizeNormalized(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
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
		return cropToSize(nMaxWidth, nMaxHeight, bi);
	}
	
	public static BufferedImage cropToSize(int nMaxWidth, int nMaxHeight, BufferedImage imgSrc) {
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

	
	////////////////////////////////////////////////////////////////////////////////////////////
	/// Reflexion effect
	//
	public static BufferedImage createReflectedPicture(BufferedImage avatar) {
		int avatarWidth = avatar.getWidth();
		int avatarHeight = avatar.getHeight();

		BufferedImage gradient = createGradientMask(avatarWidth, avatarHeight);
		BufferedImage buffer = createReflection(avatar, avatarWidth, avatarHeight);

		applyAlphaMask(gradient, buffer, avatarWidth, avatarHeight);

		return buffer;
	}
	
	public static BufferedImage createGradientMask(int avatarWidth, int avatarHeight) {
		BufferedImage gradient = new BufferedImage(avatarWidth, avatarHeight, BufferedImage.TYPE_4BYTE_ABGR_PRE);
		Graphics2D g = gradient.createGraphics();
		GradientPaint painter = new GradientPaint(0.0f, 0.0f, new Color(1.0f, 1.0f, 1.0f, 0.3f), 0.0f, avatarHeight / 8.0f, new Color(1.0f, 1.0f, 1.0f, 1f));
		g.setPaint(painter);
		g.fill(new Rectangle2D.Double(0, 0, avatarWidth, avatarHeight));

		g.dispose();
		gradient.flush();

		return gradient;
	}

	public static BufferedImage createReflection(BufferedImage avatar, int avatarWidth, int avatarHeight) {
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

	public static void applyAlphaMask(BufferedImage gradient, BufferedImage buffer, int avatarWidth, int avatarHeight) {
		Graphics2D g2 = buffer.createGraphics();
		g2.setComposite(AlphaComposite.DstOut);
		g2.drawImage(gradient, null, 0, avatarHeight);
		g2.dispose();
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/// 3D effect
	//
	public static BufferedImage create3DPicture(BufferedImage bi) {
		int w = bi.getWidth();
        int h = bi.getHeight();

        PerspectiveFilter perspectiveFilter = new PerspectiveFilter();
        perspectiveFilter.setCorners(0, 0, w, h/60, w, h - h/30, 0, h);
        return perspectiveFilter.filter(bi, null);
	}
}
