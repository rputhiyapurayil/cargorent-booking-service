package com.cargorent.booking.service;

import com.cargorent.booking.client.NotificationClient;
import com.cargorent.booking.client.VehicleClient;
import com.cargorent.booking.common.ApiException;
import com.cargorent.booking.common.CurrentUser;
import com.cargorent.booking.dto.CreateBookingRequest;
import com.cargorent.booking.dto.StatusUpdateRequest;
import com.cargorent.booking.dto.VehicleInfo;
import com.cargorent.booking.entity.Booking;
import com.cargorent.booking.entity.BookingStatus;
import com.cargorent.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private final BookingRepository repo = mock(BookingRepository.class);
    private final VehicleClient vehicles = mock(VehicleClient.class);
    private final NotificationClient notifications = mock(NotificationClient.class);
    private final BookingService service = new BookingService(repo, vehicles, notifications);

    private final CurrentUser customer = new CurrentUser(7L, "CUSTOMER", "Asha", "asha@example.com");
    private final CurrentUser admin = new CurrentUser(1L, "ADMIN", "Admin", "admin@example.com");
    private final CurrentUser internal = new CurrentUser(null, null, null, null);
    private final LocalDate start = LocalDate.now().plusDays(2);

    @BeforeEach
    void setUp() {
        when(vehicles.get(5L)).thenReturn(new VehicleInfo(5L, "KL07AB1234", "SUV", "Hyundai", "Creta",
                new BigDecimal("3000"), "AVAILABLE", "Kochi"));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createsPendingBookingWithCalculatedPrice() {
        when(repo.existsOverlap(eq(5L), any(), any(), any())).thenReturn(false);
        var res = service.create(customer, new CreateBookingRequest(5L, start, start.plusDays(2), null));
        assertThat(res.status()).isEqualTo("PENDING_PAYMENT");
        assertThat(res.totalAmount()).isEqualByComparingTo("6000");
        assertThat(res.vehicleName()).isEqualTo("Hyundai Creta");
        assertThat(res.customerName()).isEqualTo("Asha");
    }

    @Test
    void rejectsOverlappingBooking() {
        when(repo.existsOverlap(eq(5L), any(), any(), any())).thenReturn(true);
        assertThatThrownBy(() -> service.create(customer, new CreateBookingRequest(5L, start, start.plusDays(2), null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("already booked");
    }

    @Test
    void rejectsReturnBeforePickup() {
        assertThatThrownBy(() -> service.create(customer, new CreateBookingRequest(5L, start, start, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("after pickup");
    }

    @Test
    void paymentConfirmsBookingAndNotifiesCustomer() {
        Booking b = booking(BookingStatus.PENDING_PAYMENT);
        when(repo.findById(10L)).thenReturn(Optional.of(b));
        var res = service.updateStatus(internal, 10L, new StatusUpdateRequest(BookingStatus.CONFIRMED, 99L));
        assertThat(res.status()).isEqualTo("CONFIRMED");
        assertThat(res.paymentId()).isEqualTo(99L);
        verify(notifications).send(eq(7L), eq("BOOKING_CONFIRMED"), anyString(), anyString(), eq(10L));
    }

    @Test
    void statusEndpointIsInternalOnly() {
        assertThatThrownBy(() -> service.updateStatus(customer, 10L, new StatusUpdateRequest(BookingStatus.CONFIRMED, 1L)))
                .isInstanceOf(ApiException.class).hasMessageContaining("Internal");
    }

    @Test
    void cannotPickUpBeforeApproval() {
        when(repo.findById(10L)).thenReturn(Optional.of(booking(BookingStatus.CONFIRMED)));
        assertThatThrownBy(() -> service.pickup(admin, 10L))
                .isInstanceOf(ApiException.class).hasMessageContaining("must be approved");
    }

    @Test
    void customerCannotCancelApprovedBooking() {
        when(repo.findById(10L)).thenReturn(Optional.of(booking(BookingStatus.APPROVED)));
        assertThatThrownBy(() -> service.cancel(customer, 10L, null)).isInstanceOf(ApiException.class);
    }

    @Test
    void customerCannotSeeSomeoneElsesBooking() {
        Booking b = booking(BookingStatus.CONFIRMED);
        b.setUserId(99L);
        when(repo.findById(10L)).thenReturn(Optional.of(b));
        assertThatThrownBy(() -> service.get(customer, 10L)).isInstanceOf(ApiException.class).hasMessageContaining("not found");
    }

    private Booking booking(BookingStatus status) {
        Booking b = new Booking();
        b.setId(10L);
        b.setUserId(7L);
        b.setVehicleId(5L);
        b.setVehicleName("Hyundai Creta");
        b.setStartDate(start);
        b.setEndDate(start.plusDays(2));
        b.setDays(2);
        b.setPricePerDay(new BigDecimal("3000"));
        b.setSubtotal(new BigDecimal("6000"));
        b.setDiscount(BigDecimal.ZERO);
        b.setTotalAmount(new BigDecimal("6000"));
        b.setStatus(status);
        return b;
    }
}
