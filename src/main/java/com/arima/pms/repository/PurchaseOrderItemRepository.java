package com.arima.pms.repository;

import com.arima.pms.domain.entity.PurchaseOrderItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, UUID> {
}
