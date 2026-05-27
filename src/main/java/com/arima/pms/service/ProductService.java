package com.arima.pms.service;

import com.arima.pms.domain.entity.Product;
import com.arima.pms.repository.ProductRepository;
import com.arima.pms.service.command.CreateProductCommand;
import com.arima.pms.service.command.UpdateProductCommand;
import com.arima.pms.service.exception.DuplicateProductException;
import com.arima.pms.service.exception.InvalidProductException;
import com.arima.pms.service.exception.ResourceNotFoundException;
import com.arima.pms.web.dto.ProductResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

  private final ProductRepository productRepository;

  public ProductResponse create(CreateProductCommand command) {
    validateCreate(command);
    if (productRepository.existsBySkuBarcodeIgnoreCase(command.skuBarcode())) {
      throw new DuplicateProductException("Product SKU already exists: " + command.skuBarcode());
    }

    Product product = new Product();
    applyCommand(product, command.skuBarcode(), command.name(), command.brand(), command.genericName(),
        command.category(), command.dosageForm(), command.strength(), command.packSize(),
        command.prescriptionRequired(), command.reorderLevel(), command.active());

    return ProductResponse.from(productRepository.save(product));
  }

  public ProductResponse update(UUID id, UpdateProductCommand command) {
    if (id == null) {
      throw new InvalidProductException("Product id is required");
    }
    if (command == null) {
      throw new InvalidProductException("Product command is required");
    }

    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

    String skuBarcode = StringUtils.hasText(command.skuBarcode()) ? command.skuBarcode().trim() : product.getSkuBarcode();
    if (StringUtils.hasText(command.skuBarcode())
        && productRepository.existsBySkuBarcodeIgnoreCaseAndIdNot(skuBarcode, id)) {
      throw new DuplicateProductException("Product SKU already exists: " + skuBarcode);
    }

    String name = StringUtils.hasText(command.name()) ? command.name().trim() : product.getName();
    if (!StringUtils.hasText(name)) {
      throw new InvalidProductException("Product name is required");
    }

    Integer reorderLevel = command.reorderLevel() != null ? command.reorderLevel() : product.getReorderLevel();
    if (reorderLevel < 0) {
      throw new InvalidProductException("Reorder level cannot be negative");
    }

    applyCommand(
        product,
        skuBarcode,
        name,
        command.brand() != null ? command.brand().trim() : product.getBrand(),
        command.genericName() != null ? command.genericName().trim() : product.getGenericName(),
        command.category() != null ? command.category().trim() : product.getCategory(),
        command.dosageForm() != null ? command.dosageForm().trim() : product.getDosageForm(),
        command.strength() != null ? command.strength().trim() : product.getStrength(),
        command.packSize() != null ? command.packSize().trim() : product.getPackSize(),
        command.prescriptionRequired() != null ? command.prescriptionRequired() : product.isPrescriptionRequired(),
        reorderLevel,
        command.active() != null ? command.active() : product.isActive()
    );

    return ProductResponse.from(productRepository.save(product));
  }

  @Transactional(readOnly = true)
  public ProductResponse get(UUID id) {
    return ProductResponse.from(productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id)));
  }

  @Transactional(readOnly = true)
  public Page<ProductResponse> list(String query, Boolean active, String category, Boolean prescriptionRequired, Pageable pageable) {
    Specification<Product> specification = (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.conjunction();
    if (StringUtils.hasText(query)) {
      String term = query.trim().toLowerCase();
      specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
          criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%%" + term + "%%"),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("skuBarcode")), "%%" + term + "%%"),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("brand")), "%%" + term + "%%"),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), "%%" + term + "%%")
      ));
    }
    if (active != null) {
      specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("active"), active));
    }
    if (StringUtils.hasText(category)) {
      String value = category.trim().toLowerCase();
      specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
          criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), value));
    }
    if (prescriptionRequired != null) {
      specification = specification.and((root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("prescriptionRequired"), prescriptionRequired));
    }

    return productRepository.findAll(specification, pageable).map(ProductResponse::from);
  }

  private static void validateCreate(CreateProductCommand command) {
    if (command == null) {
      throw new InvalidProductException("Product command is required");
    }
    if (!StringUtils.hasText(command.skuBarcode())) {
      throw new InvalidProductException("SKU barcode is required");
    }
    if (!StringUtils.hasText(command.name())) {
      throw new InvalidProductException("Product name is required");
    }
    if (command.reorderLevel() < 0) {
      throw new InvalidProductException("Reorder level cannot be negative");
    }
  }

  private static void applyCommand(Product product,
      String skuBarcode,
      String name,
      String brand,
      String genericName,
      String category,
      String dosageForm,
      String strength,
      String packSize,
      boolean prescriptionRequired,
      int reorderLevel,
      boolean active) {
    product.setSkuBarcode(skuBarcode.trim());
    product.setName(name.trim());
    product.setBrand(blankToNull(brand));
    product.setGenericName(blankToNull(genericName));
    product.setCategory(blankToNull(category));
    product.setDosageForm(blankToNull(dosageForm));
    product.setStrength(blankToNull(strength));
    product.setPackSize(blankToNull(packSize));
    product.setPrescriptionRequired(prescriptionRequired);
    product.setReorderLevel(reorderLevel);
    product.setActive(active);
  }

  private static String blankToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
