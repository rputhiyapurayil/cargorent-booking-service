package com.cargorent.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReportSummary(LocalDate from, LocalDate to,
                            long totalBookings, long paidBookings, long activeRentals, long completedRentals,
                            long cancelledBookings, BigDecimal totalRevenue, BigDecimal averageBookingValue,
                            long totalRentalDays, Map<String, Long> statusCounts,
                            List<PeriodRow> revenueByDay, List<VehicleRow> topVehicles, List<TypeRow> revenueByType) {

    public record PeriodRow(LocalDate date, long bookings, BigDecimal revenue) { }

    public record VehicleRow(Long vehicleId, String vehicleName, String registrationNumber,
                             long bookings, long days, BigDecimal revenue) { }

    public record TypeRow(String type, long bookings, BigDecimal revenue) { }
}
