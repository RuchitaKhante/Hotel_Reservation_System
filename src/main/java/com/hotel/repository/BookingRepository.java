package com.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotel.entity.Booking;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserEmailOrderByIdDesc(String userEmail);

    List<Booking> findAllByOrderByIdDesc();

    List<Booking> findByStatus(Booking.BookingStatus status);

    // How many non-cancelled bookings of this room overlap the requested dates?
    @Query("select count(b) from Booking b " +
           "where b.room.id = :roomId and b.status <> :cancelled " +
           "and b.checkInDate < :checkOut and b.checkOutDate > :checkIn")
    long countOverlapping(@Param("roomId") Long roomId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut,
                          @Param("cancelled") Booking.BookingStatus cancelled);
}
