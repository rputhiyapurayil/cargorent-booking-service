package com.cargorent.booking.dto;

import com.cargorent.booking.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;

/** Internal: sent by payment-service after a successful payment. */
public record StatusUpdateRequest(@NotNull BookingStatus status, Long paymentId) {
}
