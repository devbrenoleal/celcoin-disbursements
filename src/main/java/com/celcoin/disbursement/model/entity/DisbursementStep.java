package com.celcoin.disbursement.model.entity;

import com.celcoin.disbursement.model.utils.StepStatus;
import com.celcoin.disbursement.model.utils.StepType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disbursement_step")
@Getter @Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementStep implements Serializable {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private StepType type;

    private BigDecimal amount;

    @Column(columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    private StepStatus status;

    private String externalId;

    private int attempts;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String failureReason;

    @JoinColumn(name = "batch_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private DisbursementBatch batch;
}
