package com.cortex.engine.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import jakarta.persistence.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;

  @CreatedDate
  @Column(nullable = false, updatable = false, name = "created_date")
  private LocalDateTime createdDate;

  @LastModifiedDate
  @Column(insertable = false, name = "last_modified_date")
  private LocalDateTime lastModifiedDate;

  @CreatedBy
  @Column(nullable = false, updatable = false, name = "created_by")
  private Long createdBy;

  @LastModifiedBy
  @Column(insertable = false, name = "last_modified_by")
  private Long lastModifiedBy;
}
