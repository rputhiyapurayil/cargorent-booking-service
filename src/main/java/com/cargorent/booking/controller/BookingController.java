package com.cargorent.booking.controller;

import com.cargorent.booking.common.CurrentUser;
import com.cargorent.booking.dto.*;
import com.cargorent.booking.entity.BookingStatus;
import com.cargorent.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ----- customer -----

    @GetMapping("/quote")
    @Operation(summary = "Price and availability for a vehicle on given dates")
    public QuoteResponse quote(@RequestParam Long vehicleId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return bookingService.quote(vehicleId, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(CurrentUser user, @Valid @RequestBody CreateBookingRequest req) {
        return bookingService.create(user, req);
    }

    @GetMapping("/my")
    @Operation(summary = "Booking history of the logged-in customer")
    public List<BookingResponse> mine(CurrentUser user) {
        return bookingService.myBookings(user);
    }

    @GetMapping("/{id:\\d+}")
    public BookingResponse get(CurrentUser user, @PathVariable Long id) {
        return bookingService.get(user, id);
    }

    @PatchMapping("/{id:\\d+}/cancel")
    public BookingResponse cancel(CurrentUser user, @PathVariable Long id,
                                  @RequestBody(required = false) CancelRequest req) {
        return bookingService.cancel(user, id, req == null ? null : req.reason());
    }

    // ----- admin: rental management -----

    @GetMapping
    @Operation(summary = "Admin: all bookings with filters")
    public List<BookingResponse> search(CurrentUser user,
                                        @RequestParam(required = false) BookingStatus status,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                        @RequestParam(required = false) String q) {
        return bookingService.search(user, status, from, to, q);
    }

    @PatchMapping("/{id:\\d+}/approve")
    public BookingResponse approve(CurrentUser user, @PathVariable Long id) {
        return bookingService.approve(user, id);
    }

    @PatchMapping("/{id:\\d+}/pickup")
    public BookingResponse pickup(CurrentUser user, @PathVariable Long id) {
        return bookingService.pickup(user, id);
    }

    @PatchMapping("/{id:\\d+}/return")
    public BookingResponse returnVehicle(CurrentUser user, @PathVariable Long id) {
        return bookingService.returnVehicle(user, id);
    }

    // ----- internal: called by other services, blocked at the gateway -----

    @PatchMapping("/{id:\\d+}/status")
    @Operation(summary = "Internal (payment-service): mark booking CONFIRMED after payment")
    public BookingResponse updateStatus(CurrentUser user, @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        return bookingService.updateStatus(user, id, req);
    }

    @GetMapping("/booked-vehicle-ids")
    @Operation(summary = "Internal (vehicle-service): vehicles with a booking overlapping [from, to)")
    public List<Long> bookedVehicleIds(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(defaultValue = "false") boolean activeOnly) {
        return bookingService.bookedVehicleIds(from, to, activeOnly);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Internal (vehicle-service): a user's bookings for recommendations and tracking checks")
    public List<BookingResponse> forUser(CurrentUser user, @PathVariable Long userId) {
        if (!user.isInternal()) user.requireAdmin();
        return bookingService.forUser(userId);
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Internal (notification-service): pickups and returns due within N days")
    public UpcomingResponse upcoming(@RequestParam(defaultValue = "1") int days) {
        return bookingService.upcoming(days);
    }
}
