package com.celcoin.disbursement.model.entity;

import com.celcoin.disbursement.model.utils.Recurrency;
import com.celcoin.disbursement.model.utils.ScheduleType;
import com.celcoin.disbursement.model.utils.BatchStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "disbursement_batch")
@Getter @Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class DisbursementBatch {
    @Id
    private String id;

    private String clientCode;

    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType;

    private LocalDateTime scheduleDate;

    @Enumerated(EnumType.STRING)
    private Recurrency recurrency;

    @Enumerated(EnumType.STRING)
    private BatchStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "batch")
    private List<DisbursementStep> steps = new ArrayList<>();
}
