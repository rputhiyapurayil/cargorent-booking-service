package com.cargorent.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuoteResponse(Long vehicleId, LocalDate startDate, LocalDate endDate, int days,
                            BigDecimal pricePerDay, BigDecimal subtotal, BigDecimal discount,
                            BigDecimal totalAmount, boolean available) {
}
