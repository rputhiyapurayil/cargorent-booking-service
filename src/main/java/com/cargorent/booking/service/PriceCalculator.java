package com.cargorent.booking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Price = days x price per day, with a long-rental discount:
 * 10% off for 7 or more days, 15% off for 30 or more days.
 * Days are counted as nights between pickup and return date (minimum 1).
 */
public final class PriceCalculator {

    public record Price(int days, BigDecimal subtotal, BigDecimal discount, BigDecimal total) { }

    private PriceCalculator() { }

    public static int days(LocalDate start, LocalDate end) {
        return (int) Math.max(1, ChronoUnit.DAYS.between(start, end));
    }

    public static Price calculate(BigDecimal pricePerDay, LocalDate start, LocalDate end) {
        int days = days(start, end);
        BigDecimal subtotal = pricePerDay.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal rate = days >= 30 ? new BigDecimal("0.15") : days >= 7 ? new BigDecimal("0.10") : BigDecimal.ZERO;
        BigDecimal discount = subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new Price(days, subtotal, discount, subtotal.subtract(discount));
    }
}
