package com.arima.pms.repository;

import com.arima.pms.domain.entity.GoodsReceipt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {
}
