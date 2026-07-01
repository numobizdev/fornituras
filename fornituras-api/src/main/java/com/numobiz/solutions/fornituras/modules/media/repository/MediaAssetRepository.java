package com.numobiz.solutions.fornituras.modules.media.repository;

import com.numobiz.solutions.fornituras.modules.media.entity.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Acceso a los metadatos de {@link MediaAsset} (017). */
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
}
