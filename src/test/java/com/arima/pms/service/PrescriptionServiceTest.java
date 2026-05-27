package com.arima.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.arima.pms.web.dto.PrescriptionResponse;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

  @Mock
  private PrescriptionRepository prescriptionRepository;

  @Mock
  private CustomerRepository customerRepository;

  @InjectMocks
  private PrescriptionService prescriptionService;

  @Test
  void createPersistsPrescription() {
    UUID customerId = UUID.randomUUID();
    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setName("Rakib Hasan");

    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PrescriptionResponse response = prescriptionService.create(new CreatePrescriptionCommand(
        customerId,
        "Dr. Hasan",
        "REG-123",
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 31),
        "https://example.com/file.pdf",
        null
    ));

    assertThat(response.doctorName()).isEqualTo("Dr. Hasan");
    assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.PENDING);

    ArgumentCaptor<Prescription> captor = ArgumentCaptor.forClass(Prescription.class);
    verify(prescriptionRepository).save(captor.capture());
    assertThat(captor.getValue().getCustomer().getId()).isEqualTo(customerId);
  }

  @Test
  void createRejectsInvalidDateRange() {
    UUID customerId = UUID.randomUUID();
    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setName("Rakib Hasan");
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

    InvalidPrescriptionException exception = assertThrows(
        InvalidPrescriptionException.class,
        () -> prescriptionService.create(new CreatePrescriptionCommand(
            customerId,
            "Dr. Hasan",
            null,
            LocalDate.of(2026, 5, 10),
            LocalDate.of(2026, 5, 1),
            null,
            null
        ))
    );

    assertThat(exception.getMessage()).containsIgnoringCase("expiry");
  }

  @Test
  void verifyUpdatesVerificationStatus() {
    Prescription prescription = prescription();
    UUID id = prescription.getId();
    when(prescriptionRepository.findById(id)).thenReturn(Optional.of(prescription));
    when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PrescriptionResponse response = prescriptionService.verify(id, new PrescriptionVerificationCommand("doctor", "ok"));

    assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
  }

  @Test
  void rejectUpdatesVerificationStatus() {
    Prescription prescription = prescription();
    UUID id = prescription.getId();
    when(prescriptionRepository.findById(id)).thenReturn(Optional.of(prescription));
    when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PrescriptionResponse response = prescriptionService.reject(id, new PrescriptionRejectionCommand("bad scan"));

    assertThat(response.verificationStatus()).isEqualTo(VerificationStatus.REJECTED);
  }

  @Test
  void listAppliesCustomerFilter() {
    Prescription prescription = prescription();
    when(prescriptionRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Prescription>>any(), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(prescription)));

    assertThat(prescriptionService.list(prescription.getCustomer().getId(), null, null, null, PageRequest.of(0, 20))).hasSize(1);
  }

  @Test
  void updateChangesMetadata() {
    Prescription prescription = prescription();
    UUID id = prescription.getId();
    Customer newCustomer = new Customer();
    newCustomer.setId(UUID.randomUUID());
    newCustomer.setName("New Customer");

    when(prescriptionRepository.findById(id)).thenReturn(Optional.of(prescription));
    when(customerRepository.findById(newCustomer.getId())).thenReturn(Optional.of(newCustomer));
    when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(invocation -> invocation.getArgument(0));

    PrescriptionResponse response = prescriptionService.update(id, new UpdatePrescriptionCommand(
        newCustomer.getId(),
        "Dr. Updated",
        "REG-999",
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 30),
        "https://example.com/new.pdf",
        VerificationStatus.PENDING
    ));

    assertThat(response.doctorName()).isEqualTo("Dr. Updated");
    assertThat(response.customerId()).isEqualTo(newCustomer.getId());
  }

  private static Prescription prescription() {
    UUID customerId = UUID.randomUUID();
    Customer customer = new Customer();
    customer.setId(customerId);
    customer.setName("Rakib Hasan");

    Prescription prescription = new Prescription();
    prescription.setId(UUID.randomUUID());
    prescription.setCustomer(customer);
    prescription.setDoctorName("Dr. Hasan");
    prescription.setIssueDate(LocalDate.of(2026, 5, 1));
    prescription.setExpiryDate(LocalDate.of(2026, 5, 31));
    prescription.setVerificationStatus(VerificationStatus.PENDING);
    return prescription;
  }
}
