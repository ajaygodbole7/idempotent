package com.example.idempotency;

import io.cloudevents.CloudEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Properties;

/**
 * Simple consumer for CloudEvents from Kafka
 */
public class EventConsumer implements AutoCloseable {
  private final KafkaConsumer<String, CloudEvent> consumer;
  private final Set<String> processedIds = new HashSet<>();
  private final Set<String> duplicateIds = new HashSet<>();
  private int totalReceived = 0;

  /**
   * Create a consumer for the events topic
   */
  public EventConsumer() {
    Properties props = ConfluentConfig.createConsumerProps();

    this.consumer = new KafkaConsumer<>(props);
    this.consumer.subscribe(Collections.singletonList(ConfluentConfig.TOPIC));

    // Initial poll to trigger partition assignment
    this.consumer.poll(Duration.ofMillis(100));
    System.out.println("Consumer subscribed to " + ConfluentConfig.TOPIC);
  }

  /**
   * Poll for events and track duplicates
   */
  public int pollEvents(Duration timeout) {
    int count = 0;
    ConsumerRecords<String, CloudEvent> records = consumer.poll(timeout);

    for (ConsumerRecord<String, CloudEvent> record : records) {
      processEvent(record);
      count++;
    }

    return count;
  }

  /**
   * Process an event and track duplicates
   */
  private void processEvent(ConsumerRecord<String, CloudEvent> record) {
    CloudEvent event = record.value();
    String key = record.key(); // customer ID used as key
    String eventId = event.getId();
    String eventType = event.getType();

    totalReceived++;

    // We're detecting duplicates based on the message key (customer ID)
    if (processedIds.contains(key)) {
      duplicateIds.add(key);
      System.out.printf("DUPLICATE detected: Event ID=%s, Key=%s, Type=%s%n",
                        eventId, key, eventType);
    } else {
      processedIds.add(key);
      System.out.printf("Processed: Event ID=%s, Key=%s, Type=%s%n",
                        eventId, key, eventType);

      // Parse and display customer details
      try {
        Customer customer = CloudEventUtil.cloudEventToCustomer(event);
        System.out.printf("  Customer: %s %s (%s)%n",
                          customer.id(),customer.firstName(), customer.lastName());
      } catch (Exception e) {
        System.err.println("Error deserializing customer: " + e.getMessage());
      }
    }
  }

  /**
   * Get counts of unique messages
   */
  public int getUniqueCount() {
    return processedIds.size();
  }

  /**
   * Get count of duplicate messages
   */
  public int getDuplicateCount() {
    return duplicateIds.size();
  }

  /**
   * Get total messages received
   */
  public int getTotalReceived() {
    return totalReceived;
  }

  /**
   * Reset the metrics
   */
  public void resetMetrics() {
    processedIds.clear();
    duplicateIds.clear();
    totalReceived = 0;
  }

  @Override
  public void close() {
    if (consumer != null) {
      consumer.close();
    }
  }
}
