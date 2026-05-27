package com.arima.pms.service;

import com.arima.pms.domain.entity.Customer;
import com.arima.pms.domain.entity.Prescription;
import com.arima.pms.domain.enums.VerificationStatus;
import com.arima.pms.repository.CustomerRepository;
import com.arima.pms.repository.PrescriptionRepository;
import com.arima.pms.service.command.CreatePrescriptionCommand;
import com.arima.pms.service.command.PrescriptionRejectionCommand;
import com.arima.pms.service.command.PrescriptionVerificationCommand;
import com.arima.pms.service.command.UpdatePrescriptionCommand;
import com.arima.pms.service.exception.InvalidPrescriptionException;
import com.arima.pms.service.exception.ResourceNotFoundException;
import com.arima.pms.web.dto.PrescriptionResponse;
import java.time.LocalDate;
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
public class PrescriptionService {

  private final PrescriptionRepository prescriptionRepository;
  private final CustomerRepository customerRepository;

  public PrescriptionResponse create(CreatePrescriptionCommand command) {
    validateCreate(command);
    Customer customer = loadCustomer(command.customerId());

    Prescription prescription = new Prescription();
    applyCommand(prescription, customer, command.doctorName(), command.doctorRegistrationNo(), command.issueDate(),
        command.expiryDate(), command.fileUrl(), command.verificationStatus() != null ? command.verificationStatus() : VerificationStatus.PENDING);

    return PrescriptionResponse.from(prescriptionRepository.save(prescription));
  }

  @Transactional(readOnly = true)
  public Page<PrescriptionResponse> list(UUID customerId, VerificationStatus verificationStatus, LocalDate expiryBefore, LocalDate expiryAfter, Pageable pageable) {
    Specification<Prescription> specification = (root, query, cb) -> cb.conjunction();
    if (customerId != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.join("customer").get("id"), customerId));
    }
    if (verificationStatus != null) {
      specification = specification.and((root, query, cb) -> cb.equal(root.get("verificationStatus"), verificationStatus));
    }
    if (expiryBefore != null) {
      specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("expiryDate"), expiryBefore));
    }
    if (expiryAfter != null) {
      specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expiryDate"), expiryAfter));
    }
    return prescriptionRepository.findAll(specification, pageable).map(PrescriptionResponse::from);
  }

  @Transactional(readOnly = true)
  public PrescriptionResponse get(UUID id) {
    if (id == null) {
      throw new InvalidPrescriptionException("Prescription id is required");
    }
    return PrescriptionResponse.from(prescriptionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id)));
  }

  public PrescriptionResponse update(UUID id, UpdatePrescriptionCommand command) {
    if (id == null) {
      throw new InvalidPrescriptionException("Prescription id is required");
    }
    if (command == null) {
      throw new InvalidPrescriptionException("Prescription command is required");
    }

    Prescription prescription = prescriptionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));

    Customer customer = command.customerId() != null ? loadCustomer(command.customerId()) : prescription.getCustomer();
    String doctorName = StringUtils.hasText(command.doctorName()) ? command.doctorName().trim() : prescription.getDoctorName();
    LocalDate issueDate = command.issueDate() != null ? command.issueDate() : prescription.getIssueDate();
    LocalDate expiryDate = command.expiryDate() != null ? command.expiryDate() : prescription.getExpiryDate();
    validateDates(issueDate, expiryDate);
    if (!StringUtils.hasText(doctorName)) {
      throw new InvalidPrescriptionException("Doctor name is required");
    }

    applyCommand(
        prescription,
        customer,
        doctorName,
        command.doctorRegistrationNo() != null ? command.doctorRegistrationNo().trim() : prescription.getDoctorRegistrationNo(),
        issueDate,
        expiryDate,
        command.fileUrl() != null ? command.fileUrl().trim() : prescription.getFileUrl(),
        command.verificationStatus() != null ? command.verificationStatus() : prescription.getVerificationStatus()
    );

    return PrescriptionResponse.from(prescriptionRepository.save(prescription));
  }

  public PrescriptionResponse verify(UUID id, PrescriptionVerificationCommand command) {
    if (id == null) {
      throw new InvalidPrescriptionException("Prescription id is required");
    }
    Prescription prescription = prescriptionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
    ensureTransitionAllowed(prescription.getVerificationStatus(), VerificationStatus.VERIFIED);
    prescription.setVerificationStatus(VerificationStatus.VERIFIED);
    return PrescriptionResponse.from(prescriptionRepository.save(prescription));
  }

  public PrescriptionResponse reject(UUID id, PrescriptionRejectionCommand command) {
    if (id == null) {
      throw new InvalidPrescriptionException("Prescription id is required");
    }
    if (command == null || !StringUtils.hasText(command.reason())) {
      throw new InvalidPrescriptionException("Rejection reason is required");
    }
    Prescription prescription = prescriptionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
    ensureTransitionAllowed(prescription.getVerificationStatus(), VerificationStatus.REJECTED);
    prescription.setVerificationStatus(VerificationStatus.REJECTED);
    return PrescriptionResponse.from(prescriptionRepository.save(prescription));
  }

  private void validateCreate(CreatePrescriptionCommand command) {
    if (command == null) {
      throw new InvalidPrescriptionException("Prescription command is required");
    }
    Customer customer = loadCustomer(command.customerId());
    if (!StringUtils.hasText(command.doctorName())) {
      throw new InvalidPrescriptionException("Doctor name is required");
    }
    validateDates(command.issueDate(), command.expiryDate());
  }

  private Customer loadCustomer(UUID customerId) {
    if (customerId == null) {
      throw new InvalidPrescriptionException("Customer id is required");
    }
    return customerRepository.findById(customerId)
        .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
  }

  private static void validateDates(LocalDate issueDate, LocalDate expiryDate) {
    if (issueDate == null) {
      throw new InvalidPrescriptionException("Issue date is required");
    }
    if (expiryDate == null) {
      throw new InvalidPrescriptionException("Expiry date is required");
    }
    if (expiryDate.isBefore(issueDate)) {
      throw new InvalidPrescriptionException("Expiry date cannot be before issue date");
    }
  }

  private static void ensureTransitionAllowed(VerificationStatus current, VerificationStatus target) {
    if (current == target) {
      return;
    }
    if (current == VerificationStatus.VERIFIED && target == VerificationStatus.REJECTED) {
      throw new InvalidPrescriptionException("Verified prescription cannot be rejected");
    }
    if (current == VerificationStatus.REJECTED && target == VerificationStatus.VERIFIED) {
      throw new InvalidPrescriptionException("Rejected prescription cannot be verified");
    }
  }

  private static void applyCommand(Prescription prescription,
      Customer customer,
      String doctorName,
      String doctorRegistrationNo,
      LocalDate issueDate,
      LocalDate expiryDate,
      String fileUrl,
      VerificationStatus verificationStatus) {
    prescription.setCustomer(customer);
    prescription.setDoctorName(doctorName.trim());
    prescription.setDoctorRegistrationNo(blankToNull(doctorRegistrationNo));
    prescription.setIssueDate(issueDate);
    prescription.setExpiryDate(expiryDate);
    prescription.setFileUrl(blankToNull(fileUrl));
    prescription.setVerificationStatus(verificationStatus);
  }

  private static String blankToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
