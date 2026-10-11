package com.hotel.service;

import com.hotel.entity.Room;
import com.hotel.repository.RoomRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    // Get ALL rooms
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    // Get only AVAILABLE rooms
    public List<Room> getAvailableRooms() {
        return roomRepository.findByAvailable(true);
    }

    // Get available rooms filtered by type
    public List<Room> getAvailableRoomsByType(Room.RoomType type) {
        return roomRepository.findByRoomTypeAndAvailable(type, true);
    }

    // Get room by ID
    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    // Mark room as unavailable (booked)
    public void markRoomUnavailable(Long roomId) {
        roomRepository.findById(roomId).ifPresent(room -> {
            room.setAvailable(false);
            roomRepository.save(room);
        });
    }

    // Mark room as available again (after cancellation)
    public void markRoomAvailable(Long roomId) {
        roomRepository.findById(roomId).ifPresent(room -> {
            room.setAvailable(true);
            roomRepository.save(room);
        });
    }
}