package com.hotel.dto;

public record BookingRequest(Long roomId,
                             String guestName,
                             String guestEmail,
                             String guestPhone,
                             String checkInDate,
                             String checkOutDate,
                             int numGuests) {}
