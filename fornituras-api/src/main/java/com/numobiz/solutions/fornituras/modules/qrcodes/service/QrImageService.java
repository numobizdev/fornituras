package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LabelPosition;
import org.springframework.stereotype.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QrImageService {

	private static final int DPI = 300;
	private static final double CM_TO_PIXELS = DPI / 2.54;
	private static final double LABEL_HEIGHT_CM = 0.5;
	private static final float LABEL_FONT_PT = 8f;

	public BufferedImage generateQrImage(String content, double sizeCm) {
		int sizePx = cmToPx(sizeCm);
		if (sizePx < 50) {
			sizePx = 50;
		}

		try {
			QRCodeWriter qrCodeWriter = new QRCodeWriter();
			Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
			hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			hints.put(EncodeHintType.MARGIN, 0);

			BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
			return MatrixToImageWriter.toBufferedImage(bitMatrix);
		} catch (WriterException ex) {
			throw new BadRequestException("Unable to generate QR image for code: " + content);
		}
	}

	public BufferedImage generateStickerImage(String content, double qrSizeCm, double paddingCm, boolean drawBorder) {
		double squareCm = qrSizeCm + (2 * paddingCm);
		int squarePx = cmToPx(squareCm);
		int qrPx = cmToPx(qrSizeCm);
		int paddingPx = (squarePx - qrPx) / 2;

		BufferedImage qr = generateQrImage(content, qrSizeCm);
		BufferedImage canvas = new BufferedImage(squarePx, squarePx, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = canvas.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, squarePx, squarePx);
		graphics.drawImage(qr, paddingPx, paddingPx, qrPx, qrPx, null);

		if (drawBorder) {
			float borderPx = Math.max(1f, (0.5f / 72f) * DPI);
			graphics.setColor(Color.BLACK);
			graphics.setStroke(new BasicStroke(borderPx));
			float inset = borderPx / 2f;
			graphics.drawRect(
					Math.round(inset),
					Math.round(inset),
					Math.round(squarePx - borderPx),
					Math.round(squarePx - borderPx));
		}

		graphics.dispose();
		return canvas;
	}

	public BufferedImage generateCodeUnitImage(String content, double qrSizeCm, double paddingCm,
			LabelPosition labelPosition, boolean drawBorder) {
		BufferedImage sticker = generateStickerImage(content, qrSizeCm, paddingCm, drawBorder);
		int squarePx = sticker.getWidth();
		int labelHeightPx = labelPosition == LabelPosition.NONE ? 0 : cmToPx(LABEL_HEIGHT_CM);
		int totalHeight = squarePx + labelHeightPx;

		BufferedImage canvas = new BufferedImage(squarePx, totalHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = canvas.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, squarePx, totalHeight);

		int stickerY = labelPosition == LabelPosition.TOP ? labelHeightPx : 0;
		graphics.drawImage(sticker, 0, stickerY, null);

		if (labelPosition != LabelPosition.NONE) {
			int labelY = labelPosition == LabelPosition.TOP ? 0 : squarePx;
			drawCenteredLabel(graphics, content, squarePx, labelY, labelHeightPx);
		}

		graphics.dispose();
		return canvas;
	}

	private void drawCenteredLabel(Graphics2D graphics, String text, int width, int y, int height) {
		int fontSizePx = (int) Math.round(LABEL_FONT_PT * DPI / 72.0);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSizePx));
		graphics.setColor(Color.BLACK);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		FontMetrics metrics = graphics.getFontMetrics();
		int textX = (width - metrics.stringWidth(text)) / 2;
		int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
		graphics.drawString(text, textX, textY);
	}

	private int cmToPx(double cm) {
		return (int) Math.round(cm * CM_TO_PIXELS);
	}
}
