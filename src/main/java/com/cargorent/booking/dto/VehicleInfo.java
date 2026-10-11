package com.cargorent.booking.dto;

import java.math.BigDecimal;

/** The subset of vehicle-service's vehicle JSON that this service reads. */
public record VehicleInfo(Long id, String registrationNumber, String type, String brand, String model,
                          BigDecimal pricePerDay, String status, String city) {
}
