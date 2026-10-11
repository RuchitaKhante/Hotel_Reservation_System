package com.hotel.controller;

import com.hotel.dto.RoomResponse;
import com.hotel.entity.Room;
import com.hotel.service.RoomService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // GET /api/rooms            -> all rooms in service
    // GET /api/rooms?type=SUITE -> only that type
    @GetMapping
    public List<RoomResponse> rooms(@RequestParam(required = false) String type) {
        List<Room> rooms;
        if (type != null && !type.isBlank() && !type.equalsIgnoreCase("ALL")) {
            try {
                rooms = roomService.getAvailableRoomsByType(Room.RoomType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown room type.");
            }
        } else {
            rooms = roomService.getAvailableRooms();
        }
        return rooms.stream().map(RoomResponse::from).toList();
    }

    @GetMapping("/{id}")
    public RoomResponse room(@PathVariable Long id) {
        return roomService.getRoomById(id)
                .map(RoomResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found."));
    }
}
