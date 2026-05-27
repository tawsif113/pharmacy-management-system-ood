package com.arima.pms.repository;

import com.arima.pms.domain.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

  Optional<Product> findBySkuBarcodeIgnoreCase(String skuBarcode);

  boolean existsBySkuBarcodeIgnoreCase(String skuBarcode);

  boolean existsBySkuBarcodeIgnoreCaseAndIdNot(String skuBarcode, UUID id);
}
