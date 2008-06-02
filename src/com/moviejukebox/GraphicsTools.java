package com.moviejukebox;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class GraphicsTools {
	
	
	public static BufferedImage createReflectedPicture(BufferedImage avatar) {

		int avatarWidth = avatar.getWidth();
		int avatarHeight = avatar.getHeight();

		BufferedImage gradient = createGradientMask(avatarWidth, avatarHeight);
		BufferedImage buffer = createReflection(avatar, avatarWidth, avatarHeight);

		applyAlphaMask(gradient, buffer, avatarWidth, avatarHeight);

		return buffer;
	}

	private static BufferedImage createGradientMask(int avatarWidth, int avatarHeight) {
		BufferedImage gradient = new BufferedImage(avatarWidth, avatarHeight, BufferedImage.TYPE_4BYTE_ABGR_PRE);
		Graphics2D g = gradient.createGraphics();
		GradientPaint painter = new GradientPaint(0.0f, 0.0f, new Color(1.0f, 1.0f, 1.0f, 0.3f), 0.0f, avatarHeight / 8.0f, new Color(1.0f, 1.0f, 1.0f, 1f));
		g.setPaint(painter);
		g.fill(new Rectangle2D.Double(0, 0, avatarWidth, avatarHeight));

		g.dispose();
		gradient.flush();

		return gradient;
	}

	private static BufferedImage createReflection(BufferedImage avatar, int avatarWidth, int avatarHeight) {

		BufferedImage buffer = new BufferedImage(avatarWidth, avatarHeight + avatarHeight/6, BufferedImage.TYPE_4BYTE_ABGR_PRE);
		Graphics2D g = buffer.createGraphics();

		g.drawImage(avatar, null, null);
		g.translate(0, (avatarHeight << 1) + 2);

		AffineTransform reflectTransform = AffineTransform.getScaleInstance(1.0, -1.0);
		g.drawImage(avatar, reflectTransform, null);
		g.translate(0, -(avatarHeight << 1));

		g.dispose();

		return buffer;
	}

	private static void applyAlphaMask(BufferedImage gradient, BufferedImage buffer, int avatarWidth, int avatarHeight) {

		Graphics2D g2 = buffer.createGraphics();
		g2.setComposite(AlphaComposite.DstOut);
		g2.drawImage(gradient, null, 0, avatarHeight);
		g2.dispose();
	}
}
