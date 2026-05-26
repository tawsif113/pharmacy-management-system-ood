package com.arima.pms.domain.entity;

import com.arima.pms.domain.enums.ActionType;
import com.arima.pms.domain.enums.ActionTypeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog extends CreatedAtEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_user_id", nullable = false)
  private User actorUser;

  @Convert(converter = ActionTypeConverter.class)
  @Column(name = "action_type", nullable = false, length = 40)
  private ActionType actionType;

  @Column(name = "entity_type", nullable = false, length = 60)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private UUID entityId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "before_json", columnDefinition = "jsonb")
  private JsonNode beforeJson;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "after_json", columnDefinition = "jsonb")
  private JsonNode afterJson;

  @Column(name = "timestamp", nullable = false)
  private java.time.LocalDateTime timestamp;

  @Column(name = "ip_address", columnDefinition = "inet")
  private String ipAddress;
}


