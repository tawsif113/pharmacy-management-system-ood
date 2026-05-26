package com.arima.pms.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 40)
  private String phone;

  @Column(columnDefinition = "text")
  private String address;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Column(columnDefinition = "text")
  private String notes;
}


