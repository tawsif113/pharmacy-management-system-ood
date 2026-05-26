package com.arima.pms.domain.entity;

import com.arima.pms.domain.enums.VerificationStatus;
import com.arima.pms.domain.enums.VerificationStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "prescriptions")
public class Prescription extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Column(name = "doctor_name", nullable = false, length = 200)
  private String doctorName;

  @Column(name = "doctor_registration_no", length = 120)
  private String doctorRegistrationNo;

  @Column(name = "issue_date", nullable = false)
  private LocalDate issueDate;

  @Column(name = "expiry_date", nullable = false)
  private LocalDate expiryDate;

  @Column(name = "file_url", columnDefinition = "text")
  private String fileUrl;

  @Convert(converter = VerificationStatusConverter.class)
  @Column(name = "verification_status", nullable = false, length = 20)
  private VerificationStatus verificationStatus;
}


