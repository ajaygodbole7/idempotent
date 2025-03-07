package com.example.idempotency;

import io.cloudevents.kafka.CloudEventDeserializer;
import io.cloudevents.kafka.CloudEventSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.UUID;

/**
 * Configuration for Kafka producers and consumers
 */
public class ConfluentConfig {
  // Confluent Cloud configuration
  public static final String BOOTSTRAP_SERVERS = "pkc-lzvrd.us-west4.gcp.confluent.cloud:9092";
  public static final String API_KEY = "VBRXUPZD2SLR6JFK";
  public static final String API_SECRET = "Zt4xfnUb8I3NmvL5EIjT3yl6BvYJ0Lxg12yU4XLbHVdRePURxpPeRRRfnTKNVGLN";
  public static final String TOPIC = "customer-events";

  /**
   * Creates a producer configuration with idempotency enabled or disabled
   */
  public static Properties createProducerProps(boolean idempotentEnabled) {
    Properties props = new Properties();

    // Connection properties
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class.getName());

    // Authentication with Confluent Cloud
    props.put("security.protocol", "SASL_SSL");
    props.put("sasl.mechanism", "PLAIN");
    props.put("sasl.jaas.config",
              "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                  "username=\"" + API_KEY + "\" " +
                  "password=\"" + API_SECRET + "\";");

    // Additional Confluent Cloud specific configurations
    props.put("client.dns.lookup", "use_all_dns_ips");
    props.put("session.timeout.ms", "45000");

    // Idempotence setting
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, idempotentEnabled);

    // Acks and retry settings
    if (idempotentEnabled) {
      // When idempotence is enabled, Kafka requires these settings
      props.put(ProducerConfig.ACKS_CONFIG, "all");
      props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
      props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
    } else {
      props.put(ProducerConfig.ACKS_CONFIG, "1");  // Don't require all replicas
      props.put(ProducerConfig.RETRIES_CONFIG, 3); // Limit retries
      props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
    }

    return props;
  }

  /**
   * Creates a consumer configuration
   */
  public static Properties createConsumerProps() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "demo-group-" + UUID.randomUUID());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CloudEventDeserializer.class.getName());
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

    // Authentication with Confluent Cloud
    props.put("security.protocol", "SASL_SSL");
    props.put("sasl.mechanism", "PLAIN");
    props.put("sasl.jaas.config",
              "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                  "username=\"" + API_KEY + "\" " +
                  "password=\"" + API_SECRET + "\";");

    // Additional Confluent Cloud specific configurations
    props.put("client.dns.lookup", "use_all_dns_ips");
    props.put("session.timeout.ms", "45000");

    return props;
  }

  /**
   * Print producer configuration details
   */
  public static void printProducerConfig(Properties props) {
    System.out.println("Producer Configuration:");
    System.out.println("  enable.idempotence: " + props.get(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG));
    System.out.println("  acks: " + props.get(ProducerConfig.ACKS_CONFIG));
    System.out.println("  retries: " + props.get(ProducerConfig.RETRIES_CONFIG));
    System.out.println("  max.in.flight.requests: " + props.get(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION));
  }
}
