package com.numobiz.solutions.fornituras.modules.transfers.repository;

import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferItemRepository extends JpaRepository<TransferItem, Long> {

	List<TransferItem> findByTransferId(Long transferId);

	long countByTransferId(Long transferId);
}
