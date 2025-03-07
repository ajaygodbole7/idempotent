package com.example.idempotency;

import io.cloudevents.CloudEvent;
import java.time.Duration;

/**
 * Simple demo showing the impact of producer idempotency by repeatedly sending the same event
 */
public class SimpleDemo {
  private static final int RETRY_COUNT = 5; // Number of times to retry/republish the same event

  public static void main(String[] args) throws Exception {
    System.out.println("=================================================");
    System.out.println("Simple Idempotency Demo");
    System.out.println("=================================================");
    System.out.println("This demo sends a SINGLE event with " + RETRY_COUNT +
                           " manual retries and shows how idempotency affects message duplication.");

    // First run with idempotent producer
    System.out.println("\n=== TESTING WITH IDEMPOTENT PRODUCER ===");
    runSimpleDemo(true);

    // Wait a bit before running the next test
    Thread.sleep(3000);

    // Then run with non-idempotent producer
    System.out.println("\n=== TESTING WITH NON-IDEMPOTENT PRODUCER ===");
    runSimpleDemo(false);
  }

  /**
   * Run the simple demo with either idempotent or non-idempotent producer
   */
  private static void runSimpleDemo(boolean idempotent) throws Exception {
    // Create producer and consumer
    try (EventProducer producer = new EventProducer(idempotent);
        EventConsumer consumer = new EventConsumer()) {

      // Give consumer time to initialize
      Thread.sleep(1000);

      // Create a single sample customer with fixed ID for consistency
      Long customerId = 1234567890L;
      Customer customer = CustomerDataProvider.createWithId(customerId);
      System.out.println("Created test customer: " + customer.getFullName() + " (ID: " + customer.id() + ")");

      // Create a CloudEvent for the customer
      CloudEvent event = CloudEventUtil.createCustomerEvent(
          CloudEventUtil.EVENT_TYPE_CREATED, customer);

      // Send the event with manual retries
      producer.sendWithRetry(event, RETRY_COUNT);

      // Give time for all messages to be processed
      System.out.println("\nWaiting for events to be processed...");
      Thread.sleep(2000);

      // Poll for events until no more are available
      int polled;
      do {
        polled = consumer.pollEvents(Duration.ofSeconds(1));
      } while (polled > 0);

      // Print results
      System.out.println("\n=== RESULTS ===");
      System.out.println("Idempotent Producer: " + producer.isIdempotent());
      System.out.println("Total Messages Sent: " + (RETRY_COUNT + 1)); // Initial + retries
      System.out.println("Messages Received: " + consumer.getTotalReceived());
      System.out.println("Unique Messages: " + consumer.getUniqueCount());
      System.out.println("Duplicate Messages: " + consumer.getDuplicateCount());

      double duplicationRate = (consumer.getUniqueCount() > 0)
          ? (double) consumer.getDuplicateCount() / consumer.getUniqueCount() * 100
          : 0;

      System.out.printf("Duplication Rate: %.2f%%%n", duplicationRate);

      if (idempotent) {
        System.out.println("\nOBSERVATION: With idempotent producer enabled, repeated sends of the same");
        System.out.println("message are deduplicated, even with " + RETRY_COUNT + " retries.");
      } else {
        System.out.println("\nOBSERVATION: With idempotent producer disabled, repeated sends of the same");
        System.out.println("message result in duplicates, despite using the same message key.");
      }
    }
  }
}
