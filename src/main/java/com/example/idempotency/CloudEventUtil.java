package com.example.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Utility for working with CloudEvents
 */
public class CloudEventUtil {
  // Event Types
  public static final String EVENT_TYPE_CREATED = "Customer::created";
  public static final String EVENT_TYPE_UPDATED = "Customer::updated";
  public static final String EVENT_TYPE_DELETED = "Customer::deleted";

  private static final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  /**
   * Create a CloudEvent for a customer
   */
  public static CloudEvent createCustomerEvent(String eventType, Customer customer) {
    if (customer == null || customer.id() == null) {
      throw new IllegalArgumentException("Customer and customer ID cannot be null");
    }

    try {
      var customerJsonNode = objectMapper.valueToTree(customer);

      return CloudEventBuilder.v1()
          .withId(customer.id().toString())  // Using customer ID as the CloudEvent ID for simplicity
          .withSource(URI.create("/customer/events"))
          .withType(eventType)
          .withTime(OffsetDateTime.now(ZoneOffset.UTC))
          .withSubject(customer.id().toString())
          .withDataContentType("application/json")
          .withData(JsonCloudEventData.wrap(customerJsonNode))
          .build();

    } catch (Exception e) {
      throw new RuntimeException("Error creating CloudEvent", e);
    }
  }

  /**
   * Get the ObjectMapper configured for CloudEvents
   */
  public static ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /**
   * Convert a CloudEvent to a Customer object
   */
  public static Customer cloudEventToCustomer(CloudEvent event) {
    try {
      String json = new String(event.getData().toBytes());
      return objectMapper.readValue(json, Customer.class);
    } catch (Exception e) {
      throw new RuntimeException("Error deserializing customer from CloudEvent", e);
    }
  }
}
