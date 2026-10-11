package com.cargorent.booking.controller;

import com.cargorent.booking.common.ApiException;
import com.cargorent.booking.common.CurrentUser;
import com.cargorent.booking.dto.ReportSummary;
import com.cargorent.booking.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /** Admin rental report. Defaults to the last 30 days. */
    @GetMapping("/summary")
    public ReportSummary summary(CurrentUser user,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        user.requireAdmin();
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) throw ApiException.badRequest("'from' must be before 'to'");
        if (ChronoUnit.DAYS.between(start, end) > 366) throw ApiException.badRequest("Report range can be at most one year");
        return reportService.summary(start, end);
    }
}
