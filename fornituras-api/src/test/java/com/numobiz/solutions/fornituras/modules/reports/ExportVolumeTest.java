package com.numobiz.solutions.fornituras.modules.reports;

import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.assignments.entity.Assignment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
 * Volumen (T016/SC-002): exportar 10.000 filas en streaming (SXSSF) sin agotar memoria; el archivo
 * contiene las 10.000 filas de datos.
 */
@WithMockUser(roles = "ADMIN")
class ExportVolumeTest extends ReportApiTestSupport {

	private static final int ROWS = 10_000;

	@Test
	void export_tenThousandRows_streamsWithoutExhaustingMemory() throws Exception {
		seedBulk(ROWS);

		MvcResult async = mockMvc.perform(get("/api/v1/reports/active-assignments/export"))
				.andExpect(request().asyncStarted())
				.andReturn();
		byte[] bytes = mockMvc.perform(asyncDispatch(async))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsByteArray();

		try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
			Sheet sheet = workbook.getSheetAt(0);
			// 1 encabezado + ROWS de datos → último índice = ROWS.
			assertThat(sheet.getLastRowNum()).isEqualTo(ROWS);
		}
	}

	private void seedBulk(int n) {
		List<Officer> officers = new ArrayList<>(n);
		List<Equipment> equipment = new ArrayList<>(n);
		long warehouseId = warehouseRepository.findAll().getFirst().getId();
		for (int i = 0; i < n; i++) {
			Officer o = new Officer();
			o.setNombre("Nombre" + i);
			o.setApellidoPaterno("Apellido" + i);
			o.setPlaca("PLA-" + i);
			o.setPlacaNormalizada(CodeNormalizer.normalize("PLA-" + i));
			o.setSexoId(1L);
			o.setMunicipio("Guadalajara");
			o.setActive(true);
			officers.add(o);

			Equipment e = new Equipment();
			e.setCodigoQr("FOR-" + i);
			e.setCodigoNormalizado(CodeNormalizer.normalize("FOR-" + i));
			e.setEquipmentTypeId(1L);
			e.setWarehouseId(warehouseId);
			e.setStatus(EquipmentStatus.ASIGNADA);
			equipment.add(e);
		}
		List<Officer> savedOfficers = officerRepository.saveAll(officers);
		List<Equipment> savedEquipment = equipmentRepository.saveAll(equipment);

		List<Assignment> assignments = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			Assignment a = new Assignment();
			a.setEquipmentId(savedEquipment.get(i).getId());
			a.setOfficerId(savedOfficers.get(i).getId());
			a.setFechaAsignacion(LocalDateTime.now());
			assignments.add(a);
		}
		assignmentRepository.saveAll(assignments);
	}
}
