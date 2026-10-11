package com.cargorent.booking.dto;

public record NotificationRequest(Long userId, String type, String title, String message, Long bookingId) {
}
