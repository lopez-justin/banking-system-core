package com.bankingsystem.transactionservice.repository;

import com.bankingsystem.transactionservice.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaTransactionRepository extends JpaRepository<TransactionEntity, String> {
    List<TransactionEntity> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);
}
