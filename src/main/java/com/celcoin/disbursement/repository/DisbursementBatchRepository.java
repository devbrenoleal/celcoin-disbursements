package com.celcoin.disbursement.repository;

import com.celcoin.disbursement.model.entity.DisbursementBatch;
import com.celcoin.disbursement.model.utils.BatchStatus;
import com.celcoin.disbursement.model.utils.ScheduleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisbursementBatchRepository extends JpaRepository<DisbursementBatch, String> {

    Optional<DisbursementBatch> findByClientCode(String clientCode);

    List<DisbursementBatch> findByStatusAndScheduleTypeAndScheduleDateLessThanEqual(BatchStatus status, ScheduleType type, LocalDateTime time);

    List<DisbursementBatch> findByStatusAndScheduleType(BatchStatus status, ScheduleType type);
}
