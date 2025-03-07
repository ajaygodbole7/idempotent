package com.example.idempotency;

import io.cloudevents.CloudEvent;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Simple producer for CloudEvents to Kafka
 */
public class EventProducer implements AutoCloseable {
  private final KafkaProducer<String, CloudEvent> producer;
  private final String topic;
  private final boolean idempotent;

  /**
   * Create a producer with specified idempotency setting
   */
  public EventProducer(boolean idempotent) {
    this.idempotent = idempotent;
    this.topic = ConfluentConfig.TOPIC;

    Properties props = ConfluentConfig.createProducerProps(idempotent);
    ConfluentConfig.printProducerConfig(props);

    this.producer = new KafkaProducer<>(props);
  }

  /**
   * Send a CloudEvent to Kafka
   */
  public RecordMetadata sendEvent(CloudEvent event)
      throws ExecutionException, InterruptedException, TimeoutException {
    String key = event.getSubject(); // Using customer ID as the message key

    ProducerRecord<String, CloudEvent> record = new ProducerRecord<>(topic, key, event);
    Future<RecordMetadata> future = producer.send(record);

    RecordMetadata metadata = future.get(10, TimeUnit.SECONDS);
    System.out.printf("Sent event (type=%s, key=%s) to partition %d, offset %d%n",
                      event.getType(), key, metadata.partition(), metadata.offset());

    return metadata;
  }

  /**
   * Send CloudEvent with manual retry
   */
  public void sendWithRetry(CloudEvent event, int retryCount)
      throws ExecutionException, InterruptedException, TimeoutException {
    System.out.printf("Sending event (key=%s) with %d manual retries...%n",
                      event.getSubject(), retryCount);

    // First send
    sendEvent(event);

    // Manual retries
    for (int i = 0; i < retryCount; i++) {
      try {
        Thread.sleep(100); // Small delay between retries
        System.out.printf("Manual retry %d for event (key=%s)%n",
                          (i + 1), event.getSubject());
        sendEvent(event);
      } catch (Exception e) {
        System.err.printf("Retry %d failed: %s%n", (i + 1), e.getMessage());
      }
    }
  }

  /**
   * Check if this producer is using idempotent configuration
   */
  public boolean isIdempotent() {
    return idempotent;
  }

  @Override
  public void close() {
    if (producer != null) {
      producer.close();
    }
  }
}
