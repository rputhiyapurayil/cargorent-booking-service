package com.cargorent.booking.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Rental life cycle:
 * PENDING_PAYMENT -> CONFIRMED (paid) -> APPROVED (admin) -> PICKED_UP -> RETURNED.
 * PENDING_PAYMENT, CONFIRMED and APPROVED can be CANCELLED.
 */
public enum BookingStatus {
    PENDING_PAYMENT, CONFIRMED, APPROVED, PICKED_UP, RETURNED, CANCELLED;

    /** Statuses that hold the vehicle, so they block other bookings on overlapping dates. */
    public static final Set<BookingStatus> BLOCKING = EnumSet.of(PENDING_PAYMENT, CONFIRMED, APPROVED, PICKED_UP);

    /** Statuses whose amount has been paid and counts as revenue. */
    public static final Set<BookingStatus> PAID = EnumSet.of(CONFIRMED, APPROVED, PICKED_UP, RETURNED);
}
