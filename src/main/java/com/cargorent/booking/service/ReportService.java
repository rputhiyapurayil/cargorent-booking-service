package com.cargorent.booking.service;

import com.cargorent.booking.dto.ReportSummary;
import com.cargorent.booking.entity.Booking;
import com.cargorent.booking.entity.BookingStatus;
import com.cargorent.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Rental reports over bookings created in a date range. */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BookingRepository bookings;

    public ReportSummary summary(LocalDate from, LocalDate to) {
        List<Booking> all = bookings.findByCreatedAtBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        return build(from, to, all, bookings.findByStatus(BookingStatus.PICKED_UP).size());
    }

    static ReportSummary build(LocalDate from, LocalDate to, List<Booking> all, long activeNow) {
        List<Booking> paid = all.stream().filter(b -> BookingStatus.PAID.contains(b.getStatus())).toList();
        BigDecimal revenue = sum(paid);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (BookingStatus s : BookingStatus.values()) {
            statusCounts.put(s.name(), all.stream().filter(b -> b.getStatus() == s).count());
        }

        Map<LocalDate, List<Booking>> byDay = paid.stream()
                .collect(Collectors.groupingBy(b -> b.getCreatedAt().toLocalDate()));
        List<ReportSummary.PeriodRow> daily = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<Booking> day = byDay.getOrDefault(d, List.of());
            daily.add(new ReportSummary.PeriodRow(d, day.size(), sum(day)));
        }

        List<ReportSummary.VehicleRow> topVehicles = paid.stream()
                .collect(Collectors.groupingBy(Booking::getVehicleId)).values().stream()
                .map(list -> new ReportSummary.VehicleRow(list.get(0).getVehicleId(), list.get(0).getVehicleName(),
                        list.get(0).getRegistrationNumber(), list.size(),
                        list.stream().mapToLong(Booking::getDays).sum(), sum(list)))
                .sorted(Comparator.comparing(ReportSummary.VehicleRow::revenue).reversed())
                .limit(5).toList();

        List<ReportSummary.TypeRow> byType = paid.stream()
                .collect(Collectors.groupingBy(b -> Objects.requireNonNullElse(b.getVehicleType(), "OTHER"))).entrySet().stream()
                .map(e -> new ReportSummary.TypeRow(e.getKey(), e.getValue().size(), sum(e.getValue())))
                .sorted(Comparator.comparing(ReportSummary.TypeRow::revenue).reversed())
                .toList();

        BigDecimal avg = paid.isEmpty() ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(paid.size()), 2, RoundingMode.HALF_UP);

        return new ReportSummary(from, to, all.size(), paid.size(), activeNow,
                statusCounts.get(BookingStatus.RETURNED.name()), statusCounts.get(BookingStatus.CANCELLED.name()),
                revenue, avg, paid.stream().mapToLong(Booking::getDays).sum(),
                statusCounts, daily, topVehicles, byType);
    }

    private static BigDecimal sum(List<Booking> list) {
        return list.stream().map(Booking::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
