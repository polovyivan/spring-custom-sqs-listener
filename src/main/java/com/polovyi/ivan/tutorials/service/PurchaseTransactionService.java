package com.polovyi.ivan.tutorials.service;

import com.polovyi.ivan.tutorials.dto.PurchaseTransactionResponse;
import com.polovyi.ivan.tutorials.repository.PurchaseTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public record PurchaseTransactionService(PurchaseTransactionRepository purchaseTransactionRepository) {

    public List<PurchaseTransactionResponse> fetchAll() {
        log.info("Fetching purchase transactions");
        return purchaseTransactionRepository.findAll().stream()
                .map(PurchaseTransactionResponse::valueOf)
                .collect(Collectors.toList());
    }
}
