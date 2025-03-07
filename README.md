# Confluent Cloud Producer Idempotency Demo

This project demonstrates the critical importance of producer-side idempotency configuration when working with Confluent Cloud and Apache Kafka. It shows that even when Confluent Cloud supports idempotency, the producer must explicitly enable it in configuration to prevent duplicate messages.

## What This Demo Shows

The project contains two demonstration scenarios:

1. **Simple Demo**: Sends a single customer event with multiple manual retries to clearly show the idempotency behavior
2. **Multi-Event Demo**: Sends multiple customer events with simulated network failures to demonstrate idempotency in a more realistic scenario

Both demos run twice - once with an idempotent producer and once without - and compare the results.

## Prerequisites

- Java 21 or higher
- Maven
- A Confluent Cloud account with a Kafka cluster
- A topic named `customer-events` in your Confluent Cloud cluster

## Configuration

Before running the demo, update your Confluent Cloud credentials in `ConfluentConfig.java`:

```java
public static final String BOOTSTRAP_SERVERS = "YOUR_BOOTSTRAP_SERVERS";
public static final String API_KEY = "YOUR_API_KEY";
public static final String API_SECRET = "YOUR_API_SECRET";
```

## Building the Project

```bash
mvn clean package
```

This will create two executable JAR files:
- `simple-demo-jar-with-dependencies.jar`
- `multi-event-demo-jar-with-dependencies.jar`

## Running the Demos

### Simple Demo

```bash
java -jar target/simple-demo-jar-with-dependencies.jar
```

This demo:
- Creates a single customer with a fixed ID
- Sends the same customer event 6 times (1 initial + 5 retries)
- Shows how idempotent vs. non-idempotent producers handle repeat messages

### Multi-Event Demo

```bash
java -jar target/multi-event-demo-jar-with-dependencies.jar
```

This demo:
- Creates 10 unique customers
- Simulates random network failures that trigger retries
- Shows the real-world impact of idempotency configuration

## Expected Results

### Idempotent Producer
```
=== RESULTS ===
Idempotent Producer: true
Total Messages Sent: 6
Messages Received: 1
Unique Messages: 1
Duplicate Messages: 0
Duplication Rate: 0.00%
```

### Non-Idempotent Producer
```
=== RESULTS ===
Idempotent Producer: false
Total Messages Sent: 6
Messages Received: 6
Unique Messages: 1
Duplicate Messages: 5
Duplication Rate: 500.00%
```

## Key Components

- **ConfluentConfig**: Central Kafka configuration
- **Customer**: Data model for customers
- **CustomerDataProvider**: Generates realistic customer data using DataFaker
- **CloudEventUtil**: Utilities for working with CloudEvents
- **EventProducer**: Kafka producer with configurable idempotency
- **EventConsumer**: Kafka consumer that detects and reports duplicates

## Important Producer Settings

### Idempotent Producer
```java
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
```

### Non-Idempotent Producer
```java
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
props.put(ProducerConfig.ACKS_CONFIG, "1");
props.put(ProducerConfig.RETRIES_CONFIG, 3);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
```

## Key Takeaways

1. **Producer Configuration Matters**: Even when Confluent Cloud supports idempotency, the producer must enable it explicitly
2. **Impact of Network Issues**: Network failures and retries naturally occur in distributed systems - idempotency prevents duplicates
3. **Message Key Importance**: Using the customer ID as the message key ensures proper partition assignment and idempotency

## Security Note

For demonstration purposes, this application includes hardcoded API credentials. In a production environment, you should use secure methods to manage credentials like environment variables or secret management systems.
