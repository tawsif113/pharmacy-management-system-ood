package com.arima.pms.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arima.pms.domain.enums.VerificationStatus;
import com.arima.pms.service.PrescriptionService;
import com.arima.pms.service.command.CreatePrescriptionCommand;
import com.arima.pms.service.command.PrescriptionRejectionCommand;
import com.arima.pms.service.command.PrescriptionVerificationCommand;
import com.arima.pms.service.command.UpdatePrescriptionCommand;
import com.arima.pms.web.dto.PrescriptionResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PrescriptionControllerTest {

  @Mock
  private PrescriptionService prescriptionService;

  @InjectMocks
  private PrescriptionController prescriptionController;

  @Test
  void createDelegatesToService() {
    PrescriptionResponse response = prescriptionResponse();
    when(prescriptionService.create(any(CreatePrescriptionCommand.class))).thenReturn(response);

    PrescriptionResponse actual = prescriptionController.create(new CreatePrescriptionCommand(
        UUID.randomUUID(),
        "Dr. Hasan",
        "REG-123",
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 31),
        null,
        null
    )).getBody();

    assertThat(actual).isEqualTo(response);
    verify(prescriptionService).create(any(CreatePrescriptionCommand.class));
  }

  @Test
  void listDelegatesToService() {
    when(prescriptionService.list(null, null, null, null, PageRequest.of(0, 20))).thenReturn(
        new PageImpl<>(List.of(prescriptionResponse()))
    );

    assertThat(prescriptionController.list(null, null, null, null, PageRequest.of(0, 20))).hasSize(1);
    verify(prescriptionService).list(null, null, null, null, PageRequest.of(0, 20));
  }

  @Test
  void verifyDelegatesToService() {
    UUID id = UUID.randomUUID();
    PrescriptionResponse response = prescriptionResponse();
    when(prescriptionService.verify(any(UUID.class), any(PrescriptionVerificationCommand.class))).thenReturn(response);

    PrescriptionResponse actual = prescriptionController.verify(id, new PrescriptionVerificationCommand("doctor", "ok"));

    assertThat(actual).isEqualTo(response);
    verify(prescriptionService).verify(any(UUID.class), any(PrescriptionVerificationCommand.class));
  }

  @Test
  void rejectDelegatesToService() {
    UUID id = UUID.randomUUID();
    PrescriptionResponse response = prescriptionResponse();
    when(prescriptionService.reject(any(UUID.class), any(PrescriptionRejectionCommand.class))).thenReturn(response);

    PrescriptionResponse actual = prescriptionController.reject(id, new PrescriptionRejectionCommand("bad scan"));

    assertThat(actual).isEqualTo(response);
    verify(prescriptionService).reject(any(UUID.class), any(PrescriptionRejectionCommand.class));
  }

  @Test
  void updateDelegatesToService() {
    UUID id = UUID.randomUUID();
    PrescriptionResponse response = prescriptionResponse();
    when(prescriptionService.update(any(UUID.class), any(UpdatePrescriptionCommand.class))).thenReturn(response);

    PrescriptionResponse actual = prescriptionController.update(id, new UpdatePrescriptionCommand(
        UUID.randomUUID(),
        "Dr. Updated",
        null,
        LocalDate.of(2026, 6, 1),
        LocalDate.of(2026, 6, 30),
        null,
        VerificationStatus.PENDING
    ));

    assertThat(actual).isEqualTo(response);
    verify(prescriptionService).update(any(UUID.class), any(UpdatePrescriptionCommand.class));
  }

  private static PrescriptionResponse prescriptionResponse() {
    UUID customerId = UUID.randomUUID();
    return new PrescriptionResponse(
        UUID.randomUUID(),
        customerId,
        "Rakib Hasan",
        "Dr. Hasan",
        "REG-123",
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2026, 5, 31),
        null,
        VerificationStatus.PENDING,
        null,
        null
    );
  }
}
