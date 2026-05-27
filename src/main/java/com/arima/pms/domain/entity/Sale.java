package com.arima.pms.domain.entity;

import com.arima.pms.domain.enums.PaymentStatus;
import com.arima.pms.domain.enums.SaleStatus;
import com.arima.pms.domain.enums.PaymentStatusConverter;
import com.arima.pms.domain.enums.SaleStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sales")
public class Sale extends BaseEntity {

  @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
  private String invoiceNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prescription_id")
  private Prescription prescription;

  @Convert(converter = SaleStatusConverter.class)
  @Column(nullable = false, length = 20)
  private SaleStatus status;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal tax = BigDecimal.ZERO;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal total = BigDecimal.ZERO;

  @Convert(converter = PaymentStatusConverter.class)
  @Column(name = "payment_status", nullable = false, length = 20)
  private PaymentStatus paymentStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @OneToMany(mappedBy = "sale", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
  private List<SaleItem> items = new ArrayList<>();

  public SaleItem addItem(Product product, Batch batch, int quantity, BigDecimal unitPrice, BigDecimal discount) {
    SaleItem item = new SaleItem();
    item.setSale(this);
    item.setProduct(product);
    item.setBatch(batch);
    item.setQuantity(quantity);
    item.setUnitPrice(unitPrice);
    item.setDiscount(discount == null ? BigDecimal.ZERO : discount);
    item.setLineTotal(calculateLineTotal(unitPrice, quantity, item.getDiscount()));
    items.add(item);
    return item;
  }

  public void clearItems() {
    items.clear();
  }

  public BigDecimal recalculateSubtotal() {
    BigDecimal subtotal = items.stream()
        .map(SaleItem::getLineTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
    this.subtotal = subtotal;
    this.total = subtotal.subtract(defaultZero(discount)).add(defaultZero(tax)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    return subtotal;
  }

  public void applyFinancials(BigDecimal discount, BigDecimal tax) {
    this.discount = defaultZero(discount).setScale(2, RoundingMode.HALF_UP);
    this.tax = defaultZero(tax).setScale(2, RoundingMode.HALF_UP);
    recalculateSubtotal();
  }

  public void markDraft() {
    this.status = SaleStatus.DRAFT;
  }

  public void markConfirmed() {
    this.status = SaleStatus.CONFIRMED;
  }

  public void markCancelled() {
    this.status = SaleStatus.CANCELLED;
  }

  public void markVoided() {
    this.status = SaleStatus.VOIDED;
  }

  public boolean canConfirm() {
    return status == SaleStatus.DRAFT;
  }

  public boolean canCancel() {
    return status == SaleStatus.DRAFT;
  }

  public boolean canVoid() {
    return status == SaleStatus.CONFIRMED;
  }

  private static BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity, BigDecimal discount) {
    BigDecimal subtotal = defaultZero(unitPrice).multiply(BigDecimal.valueOf(quantity));
    return subtotal.subtract(defaultZero(discount)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal defaultZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}


