package com.skytech.crm.entity;

import jakarta.persistence.*;
import java.util.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
public class Setting extends BaseEntity {
  private boolean autoAssignEnabled;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> leadAssignmentConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> generalConfig;
}
