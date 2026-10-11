package com.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotel.entity.Room;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // Find all available rooms
    List<Room> findByAvailable(boolean available);

    // Find by room type
    List<Room> findByRoomType(Room.RoomType roomType);

    // Find available rooms by type
    List<Room> findByRoomTypeAndAvailable(Room.RoomType roomType, boolean available);
}