package com.arima.pms.service;

import com.arima.pms.domain.entity.Supplier;
import com.arima.pms.repository.SupplierRepository;
import com.arima.pms.service.command.CreateSupplierCommand;
import com.arima.pms.service.command.UpdateSupplierCommand;
import com.arima.pms.service.exception.InvalidSupplierException;
import com.arima.pms.service.exception.ResourceNotFoundException;
import com.arima.pms.web.dto.SupplierResponse;
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
public class SupplierService {

  private final SupplierRepository supplierRepository;

  public SupplierResponse create(CreateSupplierCommand command) {
    validateCreate(command);
    Supplier supplier = new Supplier();
    applyCommand(supplier, command.name(), command.phone(), command.email(), command.address(),
        command.active() != null ? command.active() : true);
    return SupplierResponse.from(supplierRepository.save(supplier));
  }

  @Transactional(readOnly = true)
  public Page<SupplierResponse> list(String search, Boolean active, Pageable pageable) {
    Specification<Supplier> specification = (root, query, cb) -> cb.conjunction();
    if (StringUtils.hasText(search)) {
      String term = search.trim().toLowerCase();
      specification = specification.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("name")), "%%" + term + "%%"),
          cb.like(cb.lower(root.get("phone")), "%%" + term + "%%"),
          cb.like(cb.lower(root.get("email")), "%%" + term + "%%")
      ));
    }
    if (active != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.get("active"), active));
    }
    return supplierRepository.findAll(specification, pageable).map(SupplierResponse::from);
  }

  @Transactional(readOnly = true)
  public SupplierResponse get(UUID id) {
    if (id == null) {
      throw new InvalidSupplierException("Supplier id is required");
    }
    return SupplierResponse.from(supplierRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id)));
  }

  public SupplierResponse update(UUID id, UpdateSupplierCommand command) {
    if (id == null) {
      throw new InvalidSupplierException("Supplier id is required");
    }
    if (command == null) {
      throw new InvalidSupplierException("Supplier command is required");
    }

    Supplier supplier = supplierRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));

    String name = StringUtils.hasText(command.name()) ? command.name().trim() : supplier.getName();
    if (!StringUtils.hasText(name)) {
      throw new InvalidSupplierException("Supplier name is required");
    }

    applyCommand(
        supplier,
        name,
        command.phone() != null ? command.phone().trim() : supplier.getPhone(),
        command.email() != null ? command.email().trim() : supplier.getEmail(),
        command.address() != null ? command.address().trim() : supplier.getAddress(),
        command.active() != null ? command.active() : supplier.isActive()
    );

    return SupplierResponse.from(supplierRepository.save(supplier));
  }

  private static void validateCreate(CreateSupplierCommand command) {
    if (command == null) {
      throw new InvalidSupplierException("Supplier command is required");
    }
    if (!StringUtils.hasText(command.name())) {
      throw new InvalidSupplierException("Supplier name is required");
    }
  }

  private static void applyCommand(Supplier supplier, String name, String phone, String email, String address, boolean active) {
    supplier.setName(name.trim());
    supplier.setPhone(blankToNull(phone));
    supplier.setEmail(blankToNull(email));
    supplier.setAddress(blankToNull(address));
    supplier.setActive(active);
  }

  private static String blankToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
