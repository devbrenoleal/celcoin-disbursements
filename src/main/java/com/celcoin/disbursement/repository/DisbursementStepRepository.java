package com.celcoin.disbursement.repository;

import com.celcoin.disbursement.model.entity.DisbursementStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface DisbursementStepRepository extends JpaRepository<DisbursementStep, String> {

    Optional<DisbursementStep> findByExternalId(String externalId);
}
