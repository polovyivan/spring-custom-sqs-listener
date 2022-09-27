package com.polovyi.ivan.tutorials.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.polovyi.ivan.tutorials.client.LoyaltyClient;
import com.polovyi.ivan.tutorials.dto.CreateRewardPointsRequest;
import com.polovyi.ivan.tutorials.entity.PurchaseTransactionEntity;
import com.polovyi.ivan.tutorials.repository.PurchaseTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseTransactionListenerService {

    private final AmazonSQS amazonSQSClient;
    private final LoyaltyClient loyaltyClient;
    private final PurchaseTransactionRepository purchaseTransactionRepository;

    @Value("${cloud.aws.sqs.url}")
    private String tutorialSQS;

    @Value("${cloud.aws.sqs.batch-size}")
    private Integer batchSize;

    @Value("${cloud.aws.sqs.poll-wait-time-sec}")
    private Integer pollWaitTimeInSeconds;

    @Value("${cloud.aws.sqs.parallel-processing}")
    private boolean isParallelProcessing;

    private ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Scheduled(fixedRateString = "${cloud.aws.sqs.fixed-poll-rate}")
    public void messageInBatchListener() {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
                tutorialSQS)
                .withMaxNumberOfMessages(batchSize)
                .withWaitTimeSeconds(pollWaitTimeInSeconds);

        List<Message> messages = amazonSQSClient.receiveMessage(receiveMessageRequest).getMessages();

        log.info("Received {} message(s)", messages.size());

        Stream<Message> messageStream = isParallelProcessing ? messages.stream().parallel() : messages.stream();

        List<PurchaseTransactionEntity> purchaseTransactionEntities = messageStream
                .map(this::processMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!purchaseTransactionEntities.isEmpty()) {
            log.info("Saving {} purchase transaction(s)", purchaseTransactionEntities.size());
            purchaseTransactionRepository.saveAll(purchaseTransactionEntities);

            List<Message> processed = messages.stream()
                    .filter(m -> Boolean.parseBoolean(m.getAttributes().get("processed"))).toList();

            deleteMessagesBatch(processed);

            //processed.forEach(this::deleteMessage);
        }

        log.info("{}<<<<<<Finished processing of {} message(s)>>>>>>"
                        + "{}<<<<<<{} message(s) processed successfully>>>>>>"
                        + "{}<<<<<<Failed to process {} message(s). Returning message(s) back to the queue>>>>>>",
                System.getProperty("line.separator"), messages.size(),
                System.getProperty("line.separator"), purchaseTransactionEntities.size(),
                System.getProperty("line.separator"), messages.size() - purchaseTransactionEntities.size());

    }

    private PurchaseTransactionEntity processMessage(Message message) {
        log.info("Processing message with id {}", message.getMessageId());
        try {
            String body = message.getBody();
            PurchaseTransactionEntity purchaseTransactionEntity = mapper.readValue(body,
                    PurchaseTransactionEntity.class);

            createRewardPoints(purchaseTransactionEntity);

            message.addAttributesEntry("processed", "true");

            return purchaseTransactionEntity;

        } catch (Exception ex) {
            log.error(
                    "An error occurred during message processing. Returning a message to a queue. Error details: \n<{}>",
                    ex.getLocalizedMessage());
            return null;
        }
    }

    private void createRewardPoints(PurchaseTransactionEntity purchaseTransaction) {
        CreateRewardPointsRequest createRewardPointsRequest = new CreateRewardPointsRequest(
                purchaseTransaction.getCustomerId(), purchaseTransaction.getAmount().unscaledValue().longValue());
        loyaltyClient.createRewardPoints(createRewardPointsRequest);
    }

    private void deleteMessagesBatch(List<Message> messages) {
        log.info("Deleting {} message(s)", messages.size());
        List<DeleteMessageBatchRequestEntry> entries = messages.stream()
                .map(msg -> new DeleteMessageBatchRequestEntry(msg.getMessageId(), msg.getReceiptHandle()))
                .collect(Collectors.toList());
        amazonSQSClient.deleteMessageBatch(new DeleteMessageBatchRequest(tutorialSQS, entries));
    }

    /*
    First version. Improved with a method deleteMessagesBatch after comment in th blog.
     */
    //    private void deleteMessage(Message message) {
    //        log.info("Deleting message with id {}", message.getMessageId());
    //        amazonSQSClient.deleteMessage(
    //                new DeleteMessageRequest(tutorialSQS, message
    //                        .getReceiptHandle()));
    //        log.info("Message with id {} successfully  deleted.", message.getMessageId());
    //    }

}
