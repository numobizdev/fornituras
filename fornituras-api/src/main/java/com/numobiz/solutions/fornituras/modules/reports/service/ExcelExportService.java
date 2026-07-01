package com.numobiz.solutions.fornituras.modules.reports.service;

import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentSummary;
import com.numobiz.solutions.fornituras.modules.reports.dto.ActiveAssignmentRow;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

/**
 * Genera los reportes en Excel (.xlsx) con Apache POI <b>SXSSF</b> (streaming): mantiene en memoria
 * solo una ventana de filas y vuelca el resto a disco temporal, por lo que escala a decenas de miles
 * de filas sin agotar memoria (SC-002, ADR 0011). Escribe exactamente las filas que recibe (ya
 * filtradas y enmascaradas por el servicio), de modo que el archivo coincide con la vista. Los
 * archivos temporales se limpian siempre con {@code dispose()}.
 */
@Service
public class ExcelExportService {

	/** Nº de filas en memoria antes de volcar a disco (ventana de streaming). */
	private static final int WINDOW = 200;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final String[] ACTIVE_HEADERS = {
			"Código QR", "Descripción", "Elemento", "Placa", "CURP", "RFC", "Municipio", "Estado", "Fecha asignación"
	};
	private static final String[] EQUIPMENT_HEADERS = {
			"Código QR", "Descripción", "Tipo", "Talla", "Almacén", "Estado", "Vigencia", "Fecha vencimiento"
	};

	public void writeActiveAssignments(List<ActiveAssignmentRow> rows, OutputStream out) {
		SXSSFWorkbook workbook = new SXSSFWorkbook(WINDOW);
		try {
			Sheet sheet = workbook.createSheet("Asignaciones activas");
			writeHeader(sheet, ACTIVE_HEADERS);
			int r = 1;
			for (ActiveAssignmentRow row : rows) {
				Row excelRow = sheet.createRow(r++);
				int c = 0;
				set(excelRow, c++, row.codigoQr());
				set(excelRow, c++, row.equipmentDescripcion());
				set(excelRow, c++, row.elementoNombre());
				set(excelRow, c++, row.placa());
				set(excelRow, c++, row.curp());
				set(excelRow, c++, row.rfc());
				set(excelRow, c++, row.municipio());
				set(excelRow, c++, row.estado());
				set(excelRow, c, row.fechaAsignacion() == null ? null : row.fechaAsignacion().format(DATE));
			}
			workbook.write(out);
		} catch (IOException e) {
			throw new UncheckedIOException("No fue posible generar el Excel del reporte.", e);
		} finally {
			// Libera los archivos temporales de SXSSF (ADR 0011).
			workbook.dispose();
			closeQuietly(workbook);
		}
	}

	public void writeEquipment(List<EquipmentSummary> rows, OutputStream out) {
		SXSSFWorkbook workbook = new SXSSFWorkbook(WINDOW);
		try {
			Sheet sheet = workbook.createSheet("Fornituras");
			writeHeader(sheet, EQUIPMENT_HEADERS);
			int r = 1;
			for (EquipmentSummary row : rows) {
				Row excelRow = sheet.createRow(r++);
				int c = 0;
				set(excelRow, c++, row.codigoQr());
				set(excelRow, c++, row.descripcion());
				set(excelRow, c++, row.tipoNombre());
				set(excelRow, c++, row.tallaEtiqueta());
				set(excelRow, c++, row.almacenNombre());
				set(excelRow, c++, row.status() == null ? null : row.status().name());
				set(excelRow, c++, row.vigencia() == null ? null : row.vigencia().name());
				set(excelRow, c, row.fechaVencimiento() == null ? null : row.fechaVencimiento().toString());
			}
			workbook.write(out);
		} catch (IOException e) {
			throw new UncheckedIOException("No fue posible generar el Excel del reporte.", e);
		} finally {
			workbook.dispose();
			closeQuietly(workbook);
		}
	}

	private void writeHeader(Sheet sheet, String[] headers) {
		Row header = sheet.createRow(0);
		for (int i = 0; i < headers.length; i++) {
			set(header, i, headers[i]);
		}
	}

	private void set(Row row, int column, String value) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value == null ? "" : value);
	}

	private void closeQuietly(SXSSFWorkbook workbook) {
		try {
			workbook.close();
		} catch (IOException ignored) {
			// El dispose() ya liberó los temporales; un fallo al cerrar no debe enmascarar el resultado.
		}
	}
}
