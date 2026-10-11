package com.cargorent.booking.dto;

import com.cargorent.booking.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BookingResponse(Long id, Long userId, String customerName, String customerEmail,
                              Long vehicleId, String vehicleName, String vehicleType, String registrationNumber,
                              String pickupCity, LocalDate startDate, LocalDate endDate, int days,
                              BigDecimal pricePerDay, BigDecimal subtotal, BigDecimal discount, BigDecimal totalAmount,
                              String status, Long paymentId, String notes, String cancelReason,
                              LocalDateTime createdAt, LocalDateTime confirmedAt, LocalDateTime approvedAt,
                              LocalDateTime pickedUpAt, LocalDateTime returnedAt, LocalDateTime cancelledAt) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getUserId(), b.getCustomerName(), b.getCustomerEmail(),
                b.getVehicleId(), b.getVehicleName(), b.getVehicleType(), b.getRegistrationNumber(),
                b.getPickupCity(), b.getStartDate(), b.getEndDate(), b.getDays(),
                b.getPricePerDay(), b.getSubtotal(), b.getDiscount(), b.getTotalAmount(),
                b.getStatus().name(), b.getPaymentId(), b.getNotes(), b.getCancelReason(),
                b.getCreatedAt(), b.getConfirmedAt(), b.getApprovedAt(),
                b.getPickedUpAt(), b.getReturnedAt(), b.getCancelledAt());
    }
}
