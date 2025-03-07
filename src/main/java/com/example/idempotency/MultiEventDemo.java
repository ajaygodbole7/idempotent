package com.example.idempotency;

import io.cloudevents.CloudEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * More complex demo showing idempotency with multiple events and different simulation approaches
 */
public class MultiEventDemo {
  private static final int CUSTOMER_COUNT = 10;
  private static final double FAILURE_RATE = 0.3; // 30% of messages will be "retried"
  private static final int MAX_RETRIES = 2; // Max retries per failed message

  public static void main(String[] args) throws Exception {
    System.out.println("=================================================");
    System.out.println("Multiple Event Idempotency Demo");
    System.out.println("=================================================");
    System.out.println("This demo sends MULTIPLE events with simulated failures");
    System.out.println("and shows how idempotency affects message duplication.");
    System.out.println("Customer Count: " + CUSTOMER_COUNT);
    System.out.println("Simulated Failure Rate: " + (FAILURE_RATE * 100) + "%");
    System.out.println("Max Retries Per Failed Event: " + MAX_RETRIES);

    // First run with idempotent producer
    System.out.println("\n=== TESTING WITH IDEMPOTENT PRODUCER ===");
    runMultiEventDemo(true);

    // Wait a bit before running the next test
    Thread.sleep(3000);

    // Then run with non-idempotent producer
    System.out.println("\n=== TESTING WITH NON-IDEMPOTENT PRODUCER ===");
    runMultiEventDemo(false);
  }

  /**
   * Run the multi-event demo with either idempotent or non-idempotent producer
   */
  private static void runMultiEventDemo(boolean idempotent) throws Exception {
    try (EventProducer producer = new EventProducer(idempotent);
        EventConsumer consumer = new EventConsumer()) {

      // Give consumer time to initialize
      Thread.sleep(1000);

      // Generate customer events
      List<Customer> customers = CustomerDataProvider.createCustomerList(CUSTOMER_COUNT);
      List<CloudEvent> events = new ArrayList<>(CUSTOMER_COUNT);

      // Create CloudEvents for each customer
      for (Customer customer : customers) {
        CloudEvent event = CloudEventUtil.createCustomerEvent(
            CloudEventUtil.EVENT_TYPE_CREATED, customer);
        events.add(event);
      }

      System.out.println("Generated " + events.size() + " customer events");

      // Track metrics
      int successfulSends = 0;
      int retriedEvents = 0;
      int totalSendAttempts = 0;

      // Send events with simulated failures
      for (CloudEvent event : events) {
        try {
          // Determine if this event will simulate a failure
          boolean simulateFailure = Math.random() < FAILURE_RATE;

          if (simulateFailure) {
            // Simulate a send with retries
            int retries = 1 + (int)(Math.random() * MAX_RETRIES); // 1 to MAX_RETRIES
            producer.sendWithRetry(event, retries);
            retriedEvents++;
            totalSendAttempts += (retries + 1); // initial send + retries
          } else {
            // Normal send without retries
            producer.sendEvent(event);
            totalSendAttempts++;
          }

          successfulSends++;
          Thread.sleep(50); // Small delay between messages

        } catch (Exception e) {
          System.err.println("Error sending event: " + e.getMessage());
        }
      }

      // Give time for all messages to be processed
      System.out.println("\nWaiting for events to be processed...");
      Thread.sleep(2000);

      // Poll for events until no more are available
      int polled;
      do {
        polled = consumer.pollEvents(Duration.ofSeconds(1));
      } while (polled > 0);

      // Print results
      System.out.println("\n=== PRODUCER METRICS ===");
      System.out.println("Idempotent Producer: " + producer.isIdempotent());
      System.out.println("Events Generated: " + events.size());
      System.out.println("Successful Event Sends: " + successfulSends);
      System.out.println("Events With Retries: " + retriedEvents);
      System.out.println("Total Send Attempts: " + totalSendAttempts);

      System.out.println("\n=== CONSUMER METRICS ===");
      System.out.println("Messages Received: " + consumer.getTotalReceived());
      System.out.println("Unique Messages: " + consumer.getUniqueCount());
      System.out.println("Duplicate Messages: " + consumer.getDuplicateCount());

      double duplicationRate = (consumer.getUniqueCount() > 0)
          ? (double) consumer.getDuplicateCount() / consumer.getUniqueCount() * 100
          : 0;

      System.out.printf("Duplication Rate: %.2f%%%n", duplicationRate);

      if (idempotent) {
        System.out.println("\nOBSERVATION: With idempotent producer enabled, message duplicates");
        System.out.println("are prevented, despite network failures and retries.");
      } else {
        System.out.println("\nOBSERVATION: With idempotent producer disabled, network failures");
        System.out.println("and retries lead to duplicate messages.");
      }
    }
  }
}

