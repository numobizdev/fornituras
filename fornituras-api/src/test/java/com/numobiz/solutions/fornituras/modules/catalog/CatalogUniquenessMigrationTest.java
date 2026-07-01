package com.numobiz.solutions.fornituras.modules.catalog;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guarda a nivel de <b>esquema</b> de la unicidad de nombre por (catálogo, padre) (complementa T023).
 *
 * <p>Los tests de servicio corren sobre H2, que <b>no soporta índices únicos filtrados</b>
 * ({@code CREATE UNIQUE INDEX … WHERE …}); por eso la garantía real vive en la migración
 * {@code V15__generic_catalog.sql} sobre SQL Server. Un índice único plano no sirve: SQL Server trata
 * los {@code NULL} como distintos, así que la unicidad de los valores globales exige el predicado
 * {@code WHERE parent_item_id IS NULL}. Esta prueba verifica que la migración declara ambos índices
 * filtrados tal como el servicio asume, sin depender de Docker (el motor real se valida en CI, ADR 0009).
 */
class CatalogUniquenessMigrationTest {

	private static final String MIGRATION = "db/migration/V15__generic_catalog.sql";

	private String migrationSql() throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(MIGRATION)) {
			assertThat(in).as("no se encontró la migración %s en el classpath", MIGRATION).isNotNull();
			String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			// Normaliza espacios y mayúsculas para comparar de forma robusta al formato.
			return raw.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
		}
	}

	@Test
	void globalValues_haveFilteredUniqueIndexOnCatalogAndName() throws IOException {
		assertThat(migrationSql())
				.contains("CREATE UNIQUE INDEX UK_CATALOG_ITEM_NAMED ON CATALOG_ITEM (CATALOG_ID, NOMBRE_NORMALIZADO) WHERE PARENT_ITEM_ID IS NULL");
	}

	@Test
	void childValues_haveFilteredUniqueIndexOnCatalogParentAndName() throws IOException {
		assertThat(migrationSql())
				.contains("CREATE UNIQUE INDEX UK_CATALOG_ITEM_NAMED_CHILD ON CATALOG_ITEM (CATALOG_ID, PARENT_ITEM_ID, NOMBRE_NORMALIZADO) WHERE PARENT_ITEM_ID IS NOT NULL");
	}
}
