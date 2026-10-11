package com.cargorent.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnpaidBookingExpiryJob {

    private final BookingService bookingService;

    @Value("${cargorent.booking.payment-timeout-minutes:30}")
    private int timeoutMinutes;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        int expired = bookingService.expireUnpaid(timeoutMinutes);
        if (expired > 0) log.info("Expired {} unpaid bookings", expired);
    }
}
