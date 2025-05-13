package hu.bergerdenes.instantpayment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import hu.bergerdenes.instantpayment.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}

