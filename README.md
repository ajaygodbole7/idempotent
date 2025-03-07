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

#### Configuration Explained
- **enable.idempotence**: When set to `true`, the producer will ensure that exactly one copy of each message is written to the stream. This works by assigning a unique producer ID (PID) and sequence numbers to each message.
- **acks**: Must be set to `all` for idempotent producers. This ensures that the message is acknowledged by all in-sync replicas before being considered successful.
- **retries**: Set to `Integer.MAX_VALUE` to allow unlimited retries. With idempotency enabled, retries won't create duplicates.
- **max.in.flight.requests.per.connection**: Limited to 5 for idempotent producers. This controls how many unacknowledged requests the client will send on a single connection before blocking. The limit of 5 ensures ordering while allowing for high throughput.

### Non-Idempotent Producer
```java
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
props.put(ProducerConfig.ACKS_CONFIG, "1");
props.put(ProducerConfig.RETRIES_CONFIG, 3);
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
```

#### Configuration Explained
- **enable.idempotence**: Set to `false`, meaning the producer doesn't deduplicate messages. Each send attempt can result in a duplicate if retried.
- **acks**: Set to `1`, which only requires acknowledgment from the partition leader, not all replicas. Faster but less durable.
- **retries**: Limited to 3 attempts to prevent excessive retries when idempotence is disabled.
- **max.in.flight.requests.per.connection**: Restricted to 1 to maintain message order when idempotence is disabled. This limitation is necessary but reduces throughput.

## How Idempotency Works in Kafka

Kafka producer idempotency addresses the problem of message duplication that can occur when a producer retries a message send operation after not receiving an acknowledgment (due to network issues, broker failures, etc.).

### The Duplication Problem

Without idempotency:
1. Producer sends Message A
2. Network fails before acknowledgment returns
3. Producer retries and sends Message A again
4. Kafka has two copies of Message A

### Kafka's Idempotency Solution

With idempotency enabled:
1. Producer assigns a unique Producer ID (PID) and a sequence number to each message
2. Broker tracks the highest sequence number for each (PID, topic-partition) combination
3. If a duplicate message arrives with an already processed sequence number, the broker discards it
4. Only one copy of each message is stored, even when network failures occur

This mechanism ensures "exactly-once" semantics for message delivery from producer to broker.

## Key Takeaways

1. **Producer Configuration Matters**: Even when Confluent Cloud supports idempotency, the producer must enable it explicitly
2. **Impact of Network Issues**: Network failures and retries naturally occur in distributed systems - idempotency prevents duplicates
3. **Message Key Importance**: Using the customer ID as the message key ensures proper partition assignment and idempotency
4. **Configuration Dependencies**: Some settings have interdependencies - for example, when idempotency is enabled, `acks` must be set to `all`
5. **Performance Trade-offs**: Idempotent producers have slightly higher overhead but eliminate the substantial cost of processing duplicates downstream
