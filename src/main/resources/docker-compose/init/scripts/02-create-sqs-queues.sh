#!/bin/bash
echo "########### Setting SQS names as env variables ###########"
export TUTORIAL_QUEUE=tutorial-queue

echo "########### Creating queues ###########"
aws --endpoint-url=http://localstack:4566 sqs create-queue --queue-name $TUTORIAL_QUEUE

echo "########### Listing queues ###########"
aws --endpoint-url=http://localhost:4566 sqs list-queues

echo "########### Putting  message to the queue in batch from file ###########"
aws --endpoint-url=http://localhost:4566 sqs send-message-batch --queue-url =http://localhost:4566/000000000000/$TUTORIAL_QUEUE --entries file:///tmp/localstack/data/sqs-messages/batch-message.json
