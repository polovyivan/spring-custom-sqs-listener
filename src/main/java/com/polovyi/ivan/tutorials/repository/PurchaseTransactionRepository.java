package com.polovyi.ivan.tutorials.repository;

import com.polovyi.ivan.tutorials.entity.PurchaseTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseTransactionRepository extends JpaRepository<PurchaseTransactionEntity, String> {

}
