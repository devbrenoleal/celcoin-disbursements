package com.celcoin.disbursement.repository;

import com.celcoin.disbursement.model.entity.DisbursementStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DisbursementStepRepository extends JpaRepository<DisbursementStep, String> {

    Optional<DisbursementStep> findByExternalId(String externalId);
}
