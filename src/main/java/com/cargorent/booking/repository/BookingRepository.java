package com.cargorent.booking.repository;

import com.cargorent.booking.entity.Booking;
import com.cargorent.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Overlap test for half-open ranges: [start, end) overlaps [from, to) when start < to and end > from. */
    @Query("""
            select count(b) > 0 from Booking b
            where b.vehicleId = :vehicleId and b.status in :statuses
              and b.startDate < :to and b.endDate > :from
            """)
    boolean existsOverlap(@Param("vehicleId") Long vehicleId, @Param("from") LocalDate from,
                          @Param("to") LocalDate to, @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select distinct b.vehicleId from Booking b
            where b.status in :statuses and b.startDate < :to and b.endDate > :from
            """)
    List<Long> findBookedVehicleIds(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                    @Param("statuses") Collection<BookingStatus> statuses);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByStatusInAndStartDateBetween(Collection<BookingStatus> statuses, LocalDate from, LocalDate to);

    List<Booking> findByStatusAndEndDateBetween(BookingStatus status, LocalDate from, LocalDate to);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime before);

    List<Booking> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
