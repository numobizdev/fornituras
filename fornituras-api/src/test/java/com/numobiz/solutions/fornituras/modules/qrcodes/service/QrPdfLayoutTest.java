package com.numobiz.solutions.fornituras.modules.qrcodes.service;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LabelPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QrPdfLayoutTest {

	private static final float CM_TO_POINTS = 72f / 2.54f;

	@Test
	void computePageLayout_shouldFitThreeColumnsForTypicalStickerSize() {
		QrPdfService service = new QrPdfService(null);
		QrPdfService.PageLayout layout = service.computePageLayout(4.8f, 0.6f, LabelPosition.BOTTOM);

		assertEquals(3, layout.cols());
		assertEquals(4, layout.rowsPerPage());
		assertEquals(12, layout.capacityPerFullPage());
	}

	@Test
	void calculateMaxColumns_shouldUseMinimumGapToMaximizeColumns() {
		float usableWidthPt = cmToPoints(19f);
		float cellWidthPt = cmToPoints(6f);

		assertEquals(3, QrPdfService.calculateMaxColumns(usableWidthPt, cellWidthPt));
	}

	@Test
	void calculateHorizontalGapPt_shouldDistributeRemainingPageWidth() {
		float usableWidthPt = cmToPoints(19f);
		float cellWidthPt = cmToPoints(6f);
		int cols = 3;

		float gapPt = QrPdfService.calculateHorizontalGapPt(cols, usableWidthPt, cellWidthPt);
		float usedWidthPt = (cols * cellWidthPt) + ((cols - 1) * gapPt);

		assertEquals(usableWidthPt, usedWidthPt, 0.01f);
	}

	private static float cmToPoints(float cm) {
		return cm * CM_TO_POINTS;
	}
}
