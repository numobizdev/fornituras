package com.numobiz.solutions.fornituras.modules.reports;

import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import java.io.ByteArrayInputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exportación a Excel (T014): el .xlsx contiene exactamente las filas del filtro y respeta el
 * enmascaramiento de PII por rol (FR-003/FR-006).
 */
class ExcelExportTest extends ReportApiTestSupport {

	private static final int COL_CURP = 4;

	@Test
	@WithMockUser(roles = "ADMIN")
	void export_containsOnlyFilteredRows_withPiiForAdmin() throws Exception {
		long e1 = seedEquipment("FOR-1", EquipmentStatus.ASIGNADA);
		long e2 = seedEquipment("FOR-2", EquipmentStatus.ASIGNADA);
		long a = seedOfficer("Ana", "López", "PLA-1", "CURP010101MDFXYZ01", "LOAA010101AAA", "Guadalajara");
		long b = seedOfficer("Beto", "Ruiz", "PLA-2", "CURP020202HDFXYZ02", "RUBB020202BBB", "Zapopan");
		seedActiveAssignment(e1, a);
		seedActiveAssignment(e2, b);

		Sheet sheet = export("/api/v1/reports/active-assignments/export?municipio=Guadalajara");

		// 1 fila de encabezado + 1 fila de datos (solo Guadalajara).
		assertThat(sheet.getLastRowNum()).isEqualTo(1);
		Row dataRow = sheet.getRow(1);
		assertThat(dataRow.getCell(COL_CURP).getStringCellValue()).isEqualTo("CURP010101MDFXYZ01");
	}

	@Test
	@WithMockUser(roles = "CAPTURISTA")
	void export_masksPiiForNonAdmin() throws Exception {
		long e = seedEquipment("FOR-1", EquipmentStatus.ASIGNADA);
		long o = seedOfficer("Ana", "López", "PLA-1", "CURP010101MDFXYZ01", "LOAA010101AAA", "Guadalajara");
		seedActiveAssignment(e, o);

		Sheet sheet = export("/api/v1/reports/active-assignments/export");

		String curp = sheet.getRow(1).getCell(COL_CURP).getStringCellValue();
		assertThat(curp).contains("•").isNotEqualTo("CURP010101MDFXYZ01");
	}

	private Sheet export(String url) throws Exception {
		MvcResult async = mockMvc.perform(get(url))
				.andExpect(request().asyncStarted())
				.andReturn();
		byte[] bytes = mockMvc.perform(asyncDispatch(async))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray();
		Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
		return workbook.getSheetAt(0);
	}
}
