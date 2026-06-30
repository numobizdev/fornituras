package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

@Service
public class QrImageService {

	private static final int DPI = 300;
	private static final double CM_TO_PIXELS = DPI / 2.54;

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

	private int cmToPx(double cm) {
		return (int) Math.round(cm * CM_TO_PIXELS);
	}
}
