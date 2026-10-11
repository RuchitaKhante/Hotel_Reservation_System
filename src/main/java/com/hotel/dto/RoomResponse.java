package com.hotel.dto;

import com.hotel.entity.Room;

public record RoomResponse(Long id,
                           String roomNumber,
                           String roomType,
                           double pricePerNight,
                           String description,
                           int maxGuests,
                           String imageUrl) {

    public static RoomResponse from(Room r) {
        return new RoomResponse(r.getId(), r.getRoomNumber(), r.getRoomType().name(),
                r.getPricePerNight(), r.getDescription(), r.getMaxGuests(), r.getImageUrl());
    }
}
