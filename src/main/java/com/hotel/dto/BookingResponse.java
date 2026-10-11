package com.hotel.dto;

import com.hotel.entity.Booking;

public record BookingResponse(Long id,
                              String guestName,
                              String guestEmail,
                              String guestPhone,
                              String userEmail,
                              Long roomId,
                              String roomNumber,
                              String roomType,
                              String checkInDate,
                              String checkOutDate,
                              long numNights,
                              int numGuests,
                              double totalAmount,
                              String status) {

    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getGuestName(),
                b.getGuestEmail(),
                b.getGuestPhone(),
                b.getUserEmail(),
                b.getRoom().getId(),
                b.getRoom().getRoomNumber(),
                b.getRoom().getRoomType().name(),
                String.valueOf(b.getCheckInDate()),
                String.valueOf(b.getCheckOutDate()),
                b.getNumNights(),
                b.getNumGuests(),
                b.getTotalAmount(),
                b.getStatus().name());
    }
}
