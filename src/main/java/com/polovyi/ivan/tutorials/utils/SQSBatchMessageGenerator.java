package com.polovyi.ivan.tutorials.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.javafaker.CreditCardType;
import com.github.javafaker.Faker;
import com.polovyi.ivan.tutorials.entity.PurchaseTransactionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

@Slf4j
public class SQSBatchMessageGenerator {

    @SneakyThrows
    public static void main(String[] args) {
        Faker faker = new Faker();
        List<SQSBatchMessage> purchaseTransactionEntityList = IntStream.range(0, 100)// max size 1500
                .mapToObj(i -> PurchaseTransactionEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .customerId(UUID.randomUUID().toString())
                        .createdAt(LocalDate.now().minus(Period.ofDays((new Random().nextInt(365 * 10)))))
                        .amount(new BigDecimal(faker.commerce().price().replaceAll(",", ".")))
                        .paymentType(List.of(CreditCardType.values())
                                .get(new Random().nextInt(CreditCardType.values().length)).toString())
                        .build())
                .map(pt -> SQSBatchMessage.builder()
                        .Id("Batch-".concat(UUID.randomUUID().toString()))
                        .MessageBody("{\"id\":\"" + pt.getId() + "\",\"customerId\":\"" + pt.getCustomerId()
                                + "\", \"paymentType\":\"" + pt.getPaymentType() + "\",\"amount\":" + pt.getAmount()
                                + ",\"createdAt\":\"" + pt.getCreatedAt().toString() + "\"}")
                        .DelaySeconds(1)
                        .build())
                .collect(toList());

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        String fileContent = mapper.writeValueAsString(purchaseTransactionEntityList);

        Files.writeString(Path.of("./src/main/resources/docker-compose/init/sqs-messages/batch-message.json"),
                fileContent, Charset.forName("UTF-8"));

        log.info("File has been created!");

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
    static class SQSBatchMessage {
        private String Id;
        private String MessageBody;
        private Integer DelaySeconds;
    }

}
