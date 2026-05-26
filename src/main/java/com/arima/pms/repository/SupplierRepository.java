package com.arima.pms.repository;

import com.arima.pms.domain.entity.Supplier;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
}
