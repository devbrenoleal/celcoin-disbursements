package com.celcoin.disbursement.repository;

import com.celcoin.disbursement.model.entity.DisbursementBatch;
import com.celcoin.disbursement.model.utils.BatchStatus;
import com.celcoin.disbursement.model.utils.ScheduleType;
import com.celcoin.disbursement.model.utils.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DisbursementBatchRepository extends JpaRepository<DisbursementBatch, String> {

    Optional<DisbursementBatch> findByClientCode(String clientCode);

    List<DisbursementBatch> findByStatusAndScheduleTypeAndScheduleDateLessThanEqual(BatchStatus status, ScheduleType type, LocalDateTime time);

    List<DisbursementBatch> findByStatusAndScheduleType(BatchStatus status, ScheduleType type);

    @Query("SELECT count(s) FROM DisbursementStep s WHERE s.batch.id = :batchId AND s.status = :status")
    long countStepsByStatus(@Param("batchId") String batchId, @Param("status") StepStatus status);

    @Query("SELECT count(s) FROM DisbursementStep s WHERE s.batch.id = :batchId")
    long countSteps(@Param("batchId") String batchId);
}
