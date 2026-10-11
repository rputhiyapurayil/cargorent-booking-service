package com.cargorent.booking.service;

import com.cargorent.booking.client.NotificationClient;
import com.cargorent.booking.client.VehicleClient;
import com.cargorent.booking.common.ApiException;
import com.cargorent.booking.common.CurrentUser;
import com.cargorent.booking.dto.*;
import com.cargorent.booking.entity.Booking;
import com.cargorent.booking.entity.BookingStatus;
import com.cargorent.booking.repository.BookingRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.cargorent.booking.entity.BookingStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final int MAX_RENTAL_DAYS = 90;

    private final BookingRepository bookings;
    private final VehicleClient vehicleClient;
    private final NotificationClient notifications;

    // ---------- customer ----------

    public QuoteResponse quote(Long vehicleId, LocalDate start, LocalDate end) {
        validateDates(start, end);
        VehicleInfo v = vehicleClient.get(vehicleId);
        PriceCalculator.Price p = PriceCalculator.calculate(v.pricePerDay(), start, end);
        boolean available = "AVAILABLE".equals(v.status())
                && !bookings.existsOverlap(vehicleId, start, end, BookingStatus.BLOCKING);
        return new QuoteResponse(vehicleId, start, end, p.days(), v.pricePerDay(), p.subtotal(), p.discount(),
                p.total(), available);
    }

    @Transactional
    public BookingResponse create(CurrentUser user, CreateBookingRequest req) {
        Long userId = user.requireId();
        if (user.isAdmin()) throw ApiException.badRequest("Admins cannot make bookings. Use a customer account.");
        validateDates(req.startDate(), req.endDate());

        VehicleInfo v = vehicleClient.get(req.vehicleId());
        if (!"AVAILABLE".equals(v.status())) {
            throw ApiException.conflict("This vehicle is not available for rent right now");
        }
        if (bookings.existsOverlap(v.id(), req.startDate(), req.endDate(), BookingStatus.BLOCKING)) {
            throw ApiException.conflict("Sorry, this vehicle is already booked for some of those dates");
        }

        PriceCalculator.Price price = PriceCalculator.calculate(v.pricePerDay(), req.startDate(), req.endDate());
        Booking b = new Booking();
        b.setUserId(userId);
        b.setCustomerName(user.name());
        b.setCustomerEmail(user.email());
        b.setVehicleId(v.id());
        b.setVehicleName(v.brand() + " " + v.model());
        b.setVehicleType(v.type());
        b.setRegistrationNumber(v.registrationNumber());
        b.setPickupCity(v.city());
        b.setStartDate(req.startDate());
        b.setEndDate(req.endDate());
        b.setDays(price.days());
        b.setPricePerDay(v.pricePerDay());
        b.setSubtotal(price.subtotal());
        b.setDiscount(price.discount());
        b.setTotalAmount(price.total());
        b.setNotes(req.notes());
        b.setStatus(PENDING_PAYMENT);
        bookings.save(b);
        log.info("Booking {} created by user {} for vehicle {}", b.getId(), userId, v.id());
        return BookingResponse.from(b);
    }

    public List<BookingResponse> myBookings(CurrentUser user) {
        return bookings.findByUserIdOrderByCreatedAtDesc(user.requireId()).stream().map(BookingResponse::from).toList();
    }

    public BookingResponse get(CurrentUser user, Long id) {
        Booking b = find(id);
        checkOwnerOrAdmin(user, b);
        return BookingResponse.from(b);
    }

    @Transactional
    public BookingResponse cancel(CurrentUser user, Long id, String reason) {
        Booking b = find(id);
        checkOwnerOrAdmin(user, b);
        Set<BookingStatus> allowed = user.isAdmin()
                ? EnumSet.of(PENDING_PAYMENT, CONFIRMED, APPROVED)
                : EnumSet.of(PENDING_PAYMENT, CONFIRMED);
        if (!allowed.contains(b.getStatus())) {
            throw ApiException.conflict("A booking that is " + label(b.getStatus()) + " cannot be cancelled");
        }
        boolean wasPaid = b.getStatus() != PENDING_PAYMENT;
        b.setStatus(CANCELLED);
        b.setCancelledAt(LocalDateTime.now());
        b.setCancelReason(reason == null || reason.isBlank()
                ? (user.isAdmin() ? "Cancelled by CarGoRent" : "Cancelled by customer") : reason.trim());
        notifications.send(b.getUserId(), "BOOKING_CANCELLED", "Booking #" + b.getId() + " cancelled",
                b.getVehicleName() + " booking was cancelled. " + b.getCancelReason()
                        + (wasPaid ? ". Your refund will be processed in 5-7 working days." : "."), b.getId());
        return BookingResponse.from(b);
    }

    // ---------- admin ----------

    public List<BookingResponse> search(CurrentUser user, BookingStatus status, LocalDate from, LocalDate to, String q) {
        user.requireAdmin();
        Specification<Booking> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("endDate"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("startDate"), to));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("customerName")), like),
                        cb.like(cb.lower(root.get("customerEmail")), like),
                        cb.like(cb.lower(root.get("vehicleName")), like),
                        cb.like(cb.lower(root.get("registrationNumber")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return bookings.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(BookingResponse::from).toList();
    }

    @Transactional
    public BookingResponse approve(CurrentUser user, Long id) {
        user.requireAdmin();
        Booking b = transition(id, CONFIRMED, APPROVED);
        b.setApprovedAt(LocalDateTime.now());
        notifications.send(b.getUserId(), "BOOKING_APPROVED", "Booking #" + b.getId() + " approved",
                "Your " + b.getVehicleName() + " is ready for pickup on " + DATE.format(b.getStartDate())
                        + " in " + b.getPickupCity() + ".", b.getId());
        return BookingResponse.from(b);
    }

    @Transactional
    public BookingResponse pickup(CurrentUser user, Long id) {
        user.requireAdmin();
        Booking b = transition(id, APPROVED, PICKED_UP);
        if (LocalDate.now().isBefore(b.getStartDate())) {
            throw ApiException.conflict("Pickup is scheduled for " + DATE.format(b.getStartDate()));
        }
        b.setPickedUpAt(LocalDateTime.now());
        notifications.send(b.getUserId(), "VEHICLE_PICKED_UP", "Enjoy your ride!",
                "You picked up " + b.getVehicleName() + " (" + b.getRegistrationNumber() + "). Please return it by "
                        + DATE.format(b.getEndDate()) + ". You can track it live from My Bookings.", b.getId());
        return BookingResponse.from(b);
    }

    @Transactional
    public BookingResponse returnVehicle(CurrentUser user, Long id) {
        user.requireAdmin();
        Booking b = transition(id, PICKED_UP, RETURNED);
        b.setReturnedAt(LocalDateTime.now());
        notifications.send(b.getUserId(), "VEHICLE_RETURNED", "Thanks for riding with CarGoRent",
                b.getVehicleName() + " was returned. We hope you enjoyed the trip!", b.getId());
        return BookingResponse.from(b);
    }

    // ---------- internal (service-to-service) ----------

    @Transactional
    public BookingResponse updateStatus(CurrentUser user, Long id, StatusUpdateRequest req) {
        if (!user.isInternal()) throw ApiException.forbidden("Internal endpoint");
        if (req.status() != CONFIRMED) throw ApiException.badRequest("Only CONFIRMED can be set by this endpoint");
        Booking b = transition(id, PENDING_PAYMENT, CONFIRMED);
        b.setConfirmedAt(LocalDateTime.now());
        b.setPaymentId(req.paymentId());
        notifications.send(b.getUserId(), "BOOKING_CONFIRMED", "Booking #" + b.getId() + " confirmed",
                "Payment received for " + b.getVehicleName() + " from " + DATE.format(b.getStartDate()) + " to "
                        + DATE.format(b.getEndDate()) + ". We will notify you once it is approved.", b.getId());
        return BookingResponse.from(b);
    }

    public List<Long> bookedVehicleIds(LocalDate from, LocalDate to, boolean activeOnly) {
        Set<BookingStatus> statuses = activeOnly ? EnumSet.of(PICKED_UP) : BookingStatus.BLOCKING;
        return bookings.findBookedVehicleIds(from, to, statuses);
    }

    public List<BookingResponse> forUser(Long userId) {
        return bookings.findByUserIdOrderByCreatedAtDesc(userId).stream().map(BookingResponse::from).toList();
    }

    public UpcomingResponse upcoming(int days) {
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(Math.max(days, 0));
        return new UpcomingResponse(
                bookings.findByStatusInAndStartDateBetween(EnumSet.of(CONFIRMED, APPROVED), today, until)
                        .stream().map(BookingResponse::from).toList(),
                bookings.findByStatusAndEndDateBetween(PICKED_UP, today, until)
                        .stream().map(BookingResponse::from).toList());
    }

    /** Unpaid bookings hold the vehicle; release them if payment is not made in time. */
    @Transactional
    public int expireUnpaid(int minutes) {
        List<Booking> stale = bookings.findByStatusAndCreatedAtBefore(PENDING_PAYMENT, LocalDateTime.now().minusMinutes(minutes));
        for (Booking b : stale) {
            b.setStatus(CANCELLED);
            b.setCancelledAt(LocalDateTime.now());
            b.setCancelReason("Payment not completed within " + minutes + " minutes");
            notifications.send(b.getUserId(), "BOOKING_CANCELLED", "Booking #" + b.getId() + " expired",
                    "Your booking for " + b.getVehicleName() + " expired because payment was not completed.", b.getId());
        }
        return stale.size();
    }

    // ---------- helpers ----------

    private Booking transition(Long id, BookingStatus from, BookingStatus to) {
        Booking b = find(id);
        if (b.getStatus() != from) {
            throw ApiException.conflict("Booking is " + label(b.getStatus()) + "; it must be " + label(from)
                    + " to become " + label(to));
        }
        b.setStatus(to);
        return b;
    }

    private Booking find(Long id) {
        return bookings.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
    }

    private static void checkOwnerOrAdmin(CurrentUser user, Booking b) {
        if (user.isInternal() || user.isAdmin()) return;
        if (!b.getUserId().equals(user.requireId())) throw ApiException.notFound("Booking not found");
    }

    static void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) throw ApiException.badRequest("Pickup and return dates are required");
        if (start.isBefore(LocalDate.now())) throw ApiException.badRequest("Pickup date cannot be in the past");
        if (!end.isAfter(start)) throw ApiException.badRequest("Return date must be after pickup date");
        if (PriceCalculator.days(start, end) > MAX_RENTAL_DAYS) {
            throw ApiException.badRequest("Bookings can be at most " + MAX_RENTAL_DAYS + " days");
        }
    }

    private static String label(BookingStatus s) {
        return s.name().toLowerCase().replace('_', ' ');
    }
}
