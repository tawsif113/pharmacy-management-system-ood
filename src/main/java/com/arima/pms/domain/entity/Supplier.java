package com.arima.pms.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "suppliers")
public class Supplier extends BaseEntity {

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 40)
  private String phone;

  @Column(length = 160)
  private String email;

  @Column(columnDefinition = "text")
  private String address;

  @Column(nullable = false)
  private boolean active = true;
}


