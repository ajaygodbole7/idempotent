package com.example.idempotency;

import io.hypersistence.tsid.TSID;
import lombok.experimental.UtilityClass;
import net.datafaker.Faker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class to generate realistic customer data for testing
 */
@UtilityClass
public class CustomerDataProvider {
  private static final Faker faker = new Faker();
  private static final Random random = new Random();

  private static final String[] ADDRESS_TYPES = {"HOME", "WORK", "MAILING", "BILLING", "SHIPPING"};
  private static final String[] CUSTOMER_TYPES = {"INDIVIDUAL", "BUSINESS", "GOVERNMENT", "NON_PROFIT"};

  /**
   * Create a basic customer with minimum information
   */
  public static Customer createBasicCustomer() {
    Instant now = Instant.now();

    return Customer.builder()
        .id(TSID.Factory.getTsid().toLong())
        .type(CUSTOMER_TYPES[0])  // INDIVIDUAL
        .firstName(faker.name().firstName())
        .lastName(faker.name().lastName())
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /**
   * Create a customer with a specific ID
   */
  public static Customer createWithId(Long id) {
    Instant now = Instant.now();

    return Customer.builder()
        .id(id)
        .type(CUSTOMER_TYPES[0])
        .firstName(faker.name().firstName())
        .lastName(faker.name().lastName())
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /**
   * Create a fully populated customer with all information
   */
  public static Customer createFullCustomer() {
    Instant now = Instant.now();

    return Customer.builder()
        .id(TSID.Factory.getTsid().toLong())
        .type(randomCustomerType())
        .firstName(faker.name().firstName())
        .middleName(faker.name().firstName())
        .lastName(faker.name().lastName())
        .suffix(faker.name().suffix())
        .addresses(generateRandomAddresses(1 + random.nextInt(3))) // 1-3 addresses
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  /**
   * Generate a list of random customers
   */
  public static List<Customer> createCustomerList(int count) {
    List<Customer> customers = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      customers.add(createBasicCustomer());
    }
    return customers;
  }

  /**
   * Generate a list of random addresses
   */
  private static List<Address> generateRandomAddresses(int count) {
    List<Address> addresses = new ArrayList<>(count);

    // Ensure we have at least one HOME address
    addresses.add(createAddress("HOME"));

    // Add additional addresses with random types
    for (int i = 1; i < count; i++) {
      addresses.add(createAddress(randomAddressType()));
    }

    return addresses;
  }

  /**
   * Create a random address
   */
  private static Address createAddress(String type) {
    return new Address(
        type,
        faker.address().streetAddress(),
        random.nextBoolean() ? faker.address().secondaryAddress() : null,
        random.nextBoolean() ? faker.company().name() + " Building" : null,
        faker.address().city(),
        faker.address().stateAbbr(),
        faker.address().zipCode(),
        "USA"
    );
  }

  /**
   * Get a random address type
   */
  private static String randomAddressType() {
    return ADDRESS_TYPES[random.nextInt(ADDRESS_TYPES.length)];
  }

  /**
   * Get a random customer type
   */
  private static String randomCustomerType() {
    return CUSTOMER_TYPES[random.nextInt(CUSTOMER_TYPES.length)];
  }
}
