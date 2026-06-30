package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.CodigoQR;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LabelPosition;
import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class QrPdfService {

	private static final Logger log = LoggerFactory.getLogger(QrPdfService.class);

	private static final float CM_TO_POINTS = 72f / 2.54f;
	private static final float PAGE_MARGIN_CM = 1.0f;
	private static final float LABEL_HEIGHT_CM = 0.5f;
	private static final float A4_WIDTH_CM = 21.0f;
	private static final float A4_HEIGHT_CM = 29.7f;
	private static final float MIN_HORIZONTAL_GAP_PT = 5f;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final QrImageService qrImageService;

	public QrPdfService(QrImageService qrImageService) {
		this.qrImageService = qrImageService;
	}

	public byte[] generatePdf(LoteQR lote, List<CodigoQR> codigos) {
		return generatePdf(lote, codigos, lote.getQrSizeCm(), lote.getPaddingCm(), lote.getLabelPosition(),
				lote.isMostrarBordes());
	}

	public byte[] generatePdf(LoteQR lote, List<CodigoQR> codigos, BigDecimal qrSizeCm, BigDecimal paddingCm,
			LabelPosition labelPosition, boolean mostrarBordes) {
		if (codigos.isEmpty()) {
			throw new BadRequestException("No QR codes provided for PDF generation");
		}

		long start = System.nanoTime();
		float qrSize = toFloat(qrSizeCm);
		float padding = toFloat(paddingCm);
		PageLayout layout = computePageLayout(qrSize, padding, labelPosition);

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Document document = new Document(PageSize.A4, cmToPoints(PAGE_MARGIN_CM), cmToPoints(PAGE_MARGIN_CM),
					cmToPoints(PAGE_MARGIN_CM), cmToPoints(PAGE_MARGIN_CM));
			PdfWriter.getInstance(document, outputStream);
			document.open();

			Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
			PdfPTable headerTable = buildHeaderTable(lote, layout);
			float headerHeightPt = headerTable.getTotalHeight();
			if (headerHeightPt <= 0f) {
				headerHeightPt = cmToPoints(3f);
			}
			int rowsFirstPage = Math.max(1,
					(int) Math.floor((layout.usableHeightPt() - headerHeightPt) / layout.cellHeightPt()));
			int rowsFullPage = layout.rowsPerPage();

			document.add(headerTable);

			int index = 0;
			boolean firstPage = true;
			while (index < codigos.size()) {
				if (!firstPage) {
					document.newPage();
				}

				int rowsThisPage = firstPage ? rowsFirstPage : rowsFullPage;
				int capacityThisPage = layout.cols() * rowsThisPage;
				int remaining = Math.min(capacityThisPage, codigos.size() - index);

				document.add(buildCodesTable(codigos, index, remaining, rowsThisPage, layout, labelPosition, qrSize,
						padding, labelFont, mostrarBordes));

				index += remaining;
				firstPage = false;
			}

			document.close();
			byte[] pdf = outputStream.toByteArray();
			long durationMs = (System.nanoTime() - start) / 1_000_000;
			log.info("QR PDF generated for lote {} in {} ms ({} codes, {} KB)",
					lote.getId(), durationMs, codigos.size(), pdf.length / 1024);
			return pdf;
		} catch (Exception ex) {
			throw new BadRequestException("Unable to generate QR PDF");
		}
	}

	PageLayout computePageLayout(float qrSizeCm, float paddingCm, LabelPosition labelPosition) {
		float labelHeightCm = labelPosition == LabelPosition.NONE ? 0f : LABEL_HEIGHT_CM;
		float cellWidthCm = qrSizeCm + (2 * paddingCm);
		float cellHeightCm = qrSizeCm + (2 * paddingCm) + labelHeightCm;

		float usableWidthPt = cmToPoints(A4_WIDTH_CM - (2 * PAGE_MARGIN_CM));
		float usableHeightPt = cmToPoints(A4_HEIGHT_CM - (2 * PAGE_MARGIN_CM));
		float cellWidthPt = cmToPoints(cellWidthCm);
		float cellHeightPt = cmToPoints(cellHeightCm);

		int cols = calculateMaxColumns(usableWidthPt, cellWidthPt);
		float horizontalGapPt = calculateHorizontalGapPt(cols, usableWidthPt, cellWidthPt);
		int rowsPerPage = Math.max(1, (int) Math.floor(usableHeightPt / cellHeightPt));

		return new PageLayout(cols, rowsPerPage, cellWidthPt, cellHeightPt, horizontalGapPt, usableWidthPt, usableHeightPt);
	}

	static int calculateMaxColumns(float usableWidthPt, float cellWidthPt) {
		if (cellWidthPt > usableWidthPt) {
			return 1;
		}
		return Math.max(1, (int) Math.floor((usableWidthPt + MIN_HORIZONTAL_GAP_PT) / (cellWidthPt + MIN_HORIZONTAL_GAP_PT)));
	}

	static float calculateHorizontalGapPt(int cols, float usableWidthPt, float cellWidthPt) {
		if (cols <= 1) {
			return 0f;
		}
		return (usableWidthPt - (cols * cellWidthPt)) / (cols - 1);
	}

	private PdfPTable buildHeaderTable(LoteQR lote, PageLayout layout) throws Exception {
		Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
		Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

		PdfPTable headerTable = new PdfPTable(1);
		headerTable.setTotalWidth(layout.usableWidthPt());
		headerTable.setLockedWidth(true);
		headerTable.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
		headerTable.getDefaultCell().setPadding(0);

		headerTable.addCell(buildHeaderLine("Lote QR #" + lote.getId(), titleFont));
		headerTable.addCell(buildHeaderLine("Descripción: " + lote.getDescripcion(), infoFont));
		if (lote.getCreatedAt() != null) {
			headerTable.addCell(buildHeaderLine("Fecha: " + DATE_FORMATTER.format(lote.getCreatedAt()), infoFont));
		}
		headerTable.addCell(buildHeaderLine("Cantidad de códigos: " + lote.getCantidad(), infoFont));
		headerTable.addCell(buildHeaderLine(
				"Disposición: " + layout.cols() + " por fila × " + layout.rowsPerPage()
						+ " filas por página (máx. " + layout.capacityPerFullPage() + " códigos/página)",
				infoFont));
		headerTable.addCell(buildHeaderSpacer());

		return headerTable;
	}

	private PdfPCell buildHeaderLine(String text, Font font) {
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		cell.setBorder(PdfPCell.NO_BORDER);
		cell.setPadding(0);
		cell.setPaddingBottom(2f);
		return cell;
	}

	private PdfPCell buildHeaderSpacer() {
		PdfPCell cell = new PdfPCell(new Phrase(" "));
		cell.setBorder(PdfPCell.NO_BORDER);
		cell.setPadding(0);
		cell.setFixedHeight(8f);
		return cell;
	}

	private PdfPTable buildCodesTable(List<CodigoQR> codigos, int startIndex, int count, int rowsThisPage,
			PageLayout layout, LabelPosition labelPosition, float qrSize, float padding, Font labelFont,
			boolean mostrarBordes) throws Exception {
		PdfPTable table = new PdfPTable(layout.cols());
		table.setWidths(buildColumnWidths(layout.cols(), layout.cellWidthPt(), layout.horizontalGapPt()));
		table.setTotalWidth(layout.tableWidthPt());
		table.setLockedWidth(true);
		table.setHorizontalAlignment(Element.ALIGN_LEFT);
		table.getDefaultCell().setBorder(PdfPCell.NO_BORDER);

		int capacityThisPage = layout.cols() * rowsThisPage;
		for (int cell = 0; cell < count; cell++) {
			CodigoQR codigoQR = codigos.get(startIndex + cell);
			table.addCell(buildCell(codigoQR.getCodigo(), labelPosition, qrSize, padding, labelFont, mostrarBordes));
		}
		for (int cell = count; cell < capacityThisPage; cell++) {
			table.addCell(buildEmptyCell(layout.cellHeightPt()));
		}

		return table;
	}

	record PageLayout(
			int cols,
			int rowsPerPage,
			float cellWidthPt,
			float cellHeightPt,
			float horizontalGapPt,
			float usableWidthPt,
			float usableHeightPt) {

		float tableWidthPt() {
			if (cols <= 1) {
				return cellWidthPt;
			}
			return (cols * cellWidthPt) + ((cols - 1) * horizontalGapPt);
		}

		int capacityPerFullPage() {
			return cols * rowsPerPage;
		}
	}

	private float[] buildColumnWidths(int cols, float cellWidthPt, float horizontalGapPt) {
		float[] widths = new float[cols];
		for (int i = 0; i < cols - 1; i++) {
			widths[i] = cellWidthPt + horizontalGapPt;
		}
		widths[cols - 1] = cellWidthPt;
		return widths;
	}

	private PdfPCell buildEmptyCell(float cellHeightPt) {
		PdfPCell empty = new PdfPCell(new Phrase(""));
		empty.setBorder(PdfPCell.NO_BORDER);
		empty.setFixedHeight(cellHeightPt);
		empty.setPadding(0);
		return empty;
	}

	private PdfPCell buildCell(String codigo, LabelPosition labelPosition, float qrSizeCm, float paddingCm,
			Font labelFont, boolean mostrarBordes) throws Exception {
		float squarePt = cmToPoints(qrSizeCm + (2 * paddingCm));
		float labelHeightPt = labelPosition == LabelPosition.NONE ? 0f : cmToPoints(LABEL_HEIGHT_CM);
		float cellHeightPt = squarePt + labelHeightPt;

		BufferedImage sticker = qrImageService.generateStickerImage(codigo, qrSizeCm, paddingCm, mostrarBordes);
		Image stickerImage = Image.getInstance(toPngBytes(sticker));
		stickerImage.scaleAbsolute(squarePt, squarePt);

		PdfPTable unit = new PdfPTable(1);
		unit.setTotalWidth(squarePt);
		unit.setLockedWidth(true);
		unit.getDefaultCell().setBorder(PdfPCell.NO_BORDER);
		unit.getDefaultCell().setPadding(0);

		if (labelPosition == LabelPosition.TOP) {
			unit.addCell(buildLabelCell(codigo, labelFont, labelHeightPt));
		}

		PdfPCell stickerCell = new PdfPCell(stickerImage, true);
		stickerCell.setBorder(PdfPCell.NO_BORDER);
		stickerCell.setFixedHeight(squarePt);
		stickerCell.setPadding(0);
		stickerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		stickerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		unit.addCell(stickerCell);

		if (labelPosition == LabelPosition.BOTTOM) {
			unit.addCell(buildLabelCell(codigo, labelFont, labelHeightPt));
		}

		PdfPCell gridCell = new PdfPCell(unit);
		gridCell.setBorder(PdfPCell.NO_BORDER);
		gridCell.setFixedHeight(cellHeightPt);
		gridCell.setPadding(0);
		gridCell.setHorizontalAlignment(Element.ALIGN_LEFT);
		gridCell.setVerticalAlignment(Element.ALIGN_TOP);
		return gridCell;
	}

	private PdfPCell buildLabelCell(String codigo, Font labelFont, float labelHeightPt) {
		PdfPCell labelCell = new PdfPCell(new Phrase(codigo, labelFont));
		labelCell.setBorder(PdfPCell.NO_BORDER);
		labelCell.setFixedHeight(labelHeightPt);
		labelCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		labelCell.setPadding(0);
		return labelCell;
	}

	private byte[] toPngBytes(BufferedImage image) throws Exception {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "PNG", baos);
			return baos.toByteArray();
		}
	}

	private float cmToPoints(float cm) {
		return cm * CM_TO_POINTS;
	}

	private float toFloat(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP).floatValue();
	}
}
