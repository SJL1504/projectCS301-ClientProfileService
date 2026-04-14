package com.example.client.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class LogPublisher {

    private final SnsClient snsClient;

    public LogPublisher(SnsClient snsClient) {
        this.snsClient = snsClient;
    }

    /**
     * Synchronous publish
     */
    // public void publish(String topicArn, String messageBody) {
    //     try {
    //         // Check if this is a FIFO topic (ends with .fifo)
    //         boolean isFifoTopic = auditTopicArn.endsWith(".fifo");

    //         log.debug("Publishing SNS message to {} : {}", topicArn, messageBody);
    //         PublishRequest.Builder publishRequestBuilder = PublishRequest.builder()
    //                 .topicArn(topicArn)
    //                 .message(messageBody);

            // // Add MessageGroupId for FIFO topics
            // if (isFifoTopic) {
            //     publishRequestBuilder.messageGroupId(auditLog.getClientId()); // Group by target user
            //     // Optional: Add message deduplication ID to prevent duplicates
            //     publishRequestBuilder.messageDeduplicationId(
            //         auditLog.getDateTime() + "-" + auditLog.getTargetId() + "-" + auditLog.getOperation()
            //     );
            // }

    //         PublishRequest req = publishRequestBuilder.build();
    //         PublishResponse resp = snsClient.publish(req);
    //     } catch (SnsException e) {
    //         log.error("SNS publish failed: {}", e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
    //         throw e;
    //     } catch (Exception e) {
    //         log.error("Unexpected error publishing SNS message: {}", e.getMessage(), e);
    //         throw new RuntimeException(e);
    //     }
    // }

    // /**
    //  * Asynchronous publish - returns messageId future
    //  */
    // @Async("logPublisherExecutor")
    // public CompletableFuture<String> publishAsync(String topicArn, String messageBody) {
    //     try {
    //         PublishRequest request = PublishRequest.builder()
    //                 .topicArn(topicArn)
    //                 .message(messageBody)
    //                 .build();

    //         PublishResponse response = snsClient.publish(request);
    //         String messageId = response.messageId();
    //         log.debug("Message published asynchronously with ID: {}", messageId);
    //         return CompletableFuture.completedFuture(messageId);
    //     } catch (SnsException e) {
    //         log.error("Failed to publish async message to SNS: {}", e.getMessage(), e);
    //         return CompletableFuture.failedFuture(e);
    //     }
    // }

    /**
     * Fire-and-forget async publish
     */
    @Async("logPublisherExecutor")
    public void publishFireAndForget(String topicArn, LogEvent logEvent) {
        try {

            //Check if this is a FIFO topic (ends with .fifo)
            boolean isFifoTopic = topicArn.endsWith(".fifo");
            PublishRequest.Builder publishRequestBuilder = PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(logEvent.toJson());

            // Add MessageGroupId for FIFO topics
            if (isFifoTopic) {
                publishRequestBuilder.messageGroupId(logEvent.getClientId()); // Group by target user
                // Optional: Add message deduplication ID to prevent duplicates
                publishRequestBuilder.messageDeduplicationId(
                    logEvent.getDateTime() + "-" + logEvent.getClientId() + "-" + logEvent.getCrud()
                );
            }
            PublishRequest request = publishRequestBuilder.build();
            PublishResponse response = snsClient.publish(request);
            log.debug("Fire-and-forget message published with ID: {}", response.messageId());
        } catch (SnsException e) {
            log.error("Failed to publish fire-and-forget message to SNS: {}", e.getMessage(), e);
            // Swallow exception - keep main flow unaffected
        }
    }
}
