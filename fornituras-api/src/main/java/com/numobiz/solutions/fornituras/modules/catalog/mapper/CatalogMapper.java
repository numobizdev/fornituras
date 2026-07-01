package com.numobiz.solutions.fornituras.modules.catalog.mapper;

import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogItemSummary;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogSummary;
import com.numobiz.solutions.fornituras.modules.catalog.entity.Catalog;
import com.numobiz.solutions.fornituras.modules.catalog.entity.CatalogItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CatalogMapper {

	CatalogSummary toSummary(Catalog catalog);

	List<CatalogSummary> toSummaryList(List<Catalog> catalogs);

	@Mapping(target = "catalogId", source = "catalog.id")
	@Mapping(target = "catalogCode", source = "catalog.code")
	@Mapping(target = "parentItemId", source = "parentItem.id")
	CatalogItemSummary toItemSummary(CatalogItem item);

	List<CatalogItemSummary> toItemSummaryList(List<CatalogItem> items);
}
