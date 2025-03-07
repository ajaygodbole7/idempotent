package com.example.idempotency;

import lombok.Builder;
import java.time.Instant;
import java.util.List;

@Builder
public record Customer(
    Long id,
    String type,
    String firstName,
    String middleName,
    String lastName,
    String suffix,
    List<Address> addresses,
    Instant createdAt,
    Instant updatedAt
) {
  /**
   * Returns a simple string representation of the customer name
   */
  public String getFullName() {
    StringBuilder sb = new StringBuilder();

    if (firstName != null) {
      sb.append(firstName).append(" ");
    }

    if (middleName != null) {
      sb.append(middleName).append(" ");
    }

    if (lastName != null) {
      sb.append(lastName);
    }

    if (suffix != null) {
      sb.append(", ").append(suffix);
    }

    return sb.toString().trim();
  }

  /**
   * Returns the primary home address if available
   */
  public Address getPrimaryAddress() {
    if (addresses == null || addresses.isEmpty()) {
      return null;
    }

    // Try to find HOME address first
    return addresses.stream()
        .filter(a -> "HOME".equals(a.type()))
        .findFirst()
        .orElse(addresses.get(0)); // Otherwise return the first address
  }
}

record Address(
    String type,
    String line1,
    String line2,
    String line3,
    String city,
    String state,
    String postalCode,
    String country
) {
  /**
   * Returns a formatted address string
   */
  public String getFormattedAddress() {
    StringBuilder sb = new StringBuilder();

    if (line1 != null) {
      sb.append(line1).append("\n");
    }

    if (line2 != null) {
      sb.append(line2).append("\n");
    }

    if (line3 != null) {
      sb.append(line3).append("\n");
    }

    if (city != null) {
      sb.append(city).append(", ");
    }

    if (state != null) {
      sb.append(state).append(" ");
    }

    if (postalCode != null) {
      sb.append(postalCode);
    }

    if (country != null && !country.isEmpty()) {
      sb.append("\n").append(country);
    }

    return sb.toString();
  }
}
