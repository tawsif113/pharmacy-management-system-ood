package com.arima.pms.repository;

import com.arima.pms.domain.entity.Batch;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<Batch, UUID> {
}
