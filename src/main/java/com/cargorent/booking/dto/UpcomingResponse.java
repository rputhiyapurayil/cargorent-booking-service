package com.cargorent.booking.dto;

import java.util.List;

/** Internal: what the notification-service reminder job needs. */
public record UpcomingResponse(List<BookingResponse> pickups, List<BookingResponse> returns) {
}
