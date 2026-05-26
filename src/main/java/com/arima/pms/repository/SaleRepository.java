package com.arima.pms.repository;

import com.arima.pms.domain.entity.Sale;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, UUID> {
}
